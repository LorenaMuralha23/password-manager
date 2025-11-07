package com.tcc.password_manager.session;

import com.tcc.password_manager.crypto.CryptoService;
import com.tcc.password_manager.crypto.EncryptedPayload;
import com.tcc.password_manager.crypto.HashService;
import com.tcc.password_manager.crypto.PasswordKeyDerivation;
import com.tcc.password_manager.model.AppUser;
import com.tcc.password_manager.repository.AppUserRepository;
import com.tcc.password_manager.util.LogTimer;
import org.springframework.stereotype.Component;
import javax.crypto.spec.SecretKeySpec;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Gerencia autenticacao e sessao do usuario. Realiza login, logout e
 * verificacao de tempo ocioso.
 */
@Component
public class SessionManager {

    private static final Logger log = LoggerFactory.getLogger(SessionManager.class);
    private static final Duration IDLE_TIMEOUT = Duration.ofMinutes(20);

    private final SessionContext session = new SessionContext();

    private final AppUserRepository userRepository;
    private final CryptoService cryptoService;
    private final PasswordKeyDerivation keyDerivation;
    private final HashService hashService;

    public SessionManager(AppUserRepository userRepository,
            CryptoService cryptoService,
            PasswordKeyDerivation keyDerivation,
            HashService hashService) {
        this.userRepository = userRepository;
        this.cryptoService = cryptoService;
        this.keyDerivation = keyDerivation;
        this.hashService = hashService;
    }

    public SessionContext getSession() {
        return session;
    }

    /**
     * Autentica o usuario e estabelece a sessao em memória.
     */
    public void login(Long userId, String masterKey) {
        LogTimer timer = LogTimer.start("User session login");
        try {
            log.info("Iniciando autenticacao para o usuario ID: {}", userId);

            // 1. Busca usuario no repositório
            AppUser entity = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Usuario nao encontrado"));
            log.debug("Usuario encontrado no banco. Iniciando derivacao de chave.");

            // 2. Deriva passwordKey a partir da senha mestre
            SecretKeySpec passwordKey = keyDerivation.deriveKey(masterKey);
            log.debug("Chave derivada a partir da senha mestre (ocultando valor real).");

            // 3. Desfaz o wrap da UMK
            EncryptedPayload umkPayload = cryptoService.fromJson(entity.getUmkWrapped());
            byte[] umk = cryptoService.decrypt(passwordKey.getEncoded(), umkPayload, null);
            log.debug("UMK decifrada com sucesso e pronta para uso em sessao.");

            // 4. Valida hash da UMK
            String recoveredHash = hashService.hashToBase64(umk);
            if (!recoveredHash.equals(entity.getUmkHash())) {
                log.error("Hash da UMK nao corresponde ao armazenado. Falha de autenticacao para o usuario ID: {}", userId);
                throw new RuntimeException("Senha incorreta ou dados adulterados!");
            }

            // 5. Guarda a UMK em sessao
            session.establish(userId, umk);
            log.info("Sessao autenticada com sucesso para o usuario ID: {}", userId);
        } catch (Exception e) {
            log.error("Falha ao autenticar o usuario ID {}: {}", userId, e.getMessage());
            throw new RuntimeException("Erro durante o login da sessao", e);
        } finally {
            timer.stopAndLog(log);
        }
    }

    /**
     * Encerra a sessao atual.
     */
    public void logout() {
        LogTimer timer = LogTimer.start("User session logout");
        try {
            if (session.isAuthenticated()) {
                log.info("Encerrando sessao do usuario ID: {}", session.getUserId());
            } else {
                log.warn("Solicitacao de logout recebida sem sessao ativa.");
            }
            session.clear();
            log.info("Sessao encerrada com sucesso.");
        } catch (Exception e) {
            log.error("Erro ao encerrar sessao: {}", e.getMessage());
            throw new RuntimeException("Falha ao realizar logout", e);
        } finally {
            timer.stopAndLog(log);
        }
    }

    /**
     * Verifica se ha uma sessao ativa e se o tempo de inatividade é valido.
     */
    public void requireActiveSession() {
        try {
            session.requireAuthenticated();
            Instant last = session.getLastActivityAt();

            if (last != null && Duration.between(last, Instant.now()).compareTo(IDLE_TIMEOUT) > 0) {
                log.warn("Sessao expirada por inatividade. Tempo maximo permitido: {} minutos.", IDLE_TIMEOUT.toMinutes());
                session.clear();
                throw new IllegalStateException("Sessao expirada por inatividade. Faça login novamente.");
            }

            session.touch();
            log.debug("Sessao validada e tempo de atividade atualizado. Usuario ID: {}", session.getUserId());
        } catch (IllegalStateException e) {
            log.error("Sessao invalida ou expirada: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Erro ao validar sessao ativa: {}", e.getMessage());
            throw new RuntimeException("Falha ao verificar sessao ativa", e);
        }
    }
}
