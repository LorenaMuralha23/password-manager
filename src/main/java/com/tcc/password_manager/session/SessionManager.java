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
 * Gerencia autenticação e sessão do usuário. Realiza login, logout e
 * verificação de tempo ocioso.
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
     * Autentica o usuário e estabelece a sessão em memória.
     */
    public void login(Long userId, String masterKey) {
        LogTimer timer = LogTimer.start("User session login");
        try {
            log.info("Iniciando autenticação para o usuário ID: {}", userId);

            // 1. Busca usuário no repositório
            AppUser entity = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
            log.debug("Usuário encontrado no banco. Iniciando derivação de chave.");

            // 2. Deriva passwordKey a partir da senha mestre
            SecretKeySpec passwordKey = keyDerivation.deriveKey(masterKey);
            log.debug("Chave derivada a partir da senha mestre (ocultando valor real).");

            // 3. Desfaz o wrap da UMK
            EncryptedPayload umkPayload = cryptoService.fromJson(entity.getUmkWrapped());
            byte[] umk = cryptoService.decrypt(passwordKey.getEncoded(), umkPayload, null);
            log.debug("UMK decifrada com sucesso e pronta para uso em sessão.");

            // 4. Valida hash da UMK
            String recoveredHash = hashService.hashToBase64(umk);
            if (!recoveredHash.equals(entity.getUmkHash())) {
                log.error("Hash da UMK não corresponde ao armazenado. Falha de autenticação para o usuário ID: {}", userId);
                throw new RuntimeException("Senha incorreta ou dados adulterados!");
            }

            // 5. Guarda a UMK em sessão
            session.establish(userId, umk);
            log.info("Sessão autenticada com sucesso para o usuário ID: {}", userId);
        } catch (Exception e) {
            log.error("Falha ao autenticar o usuário ID {}: {}", userId, e.getMessage());
            throw new RuntimeException("Erro durante o login da sessão", e);
        } finally {
            timer.stopAndLog(log);
        }
    }

    /**
     * Encerra a sessão atual.
     */
    public void logout() {
        LogTimer timer = LogTimer.start("User session logout");
        try {
            if (session.isAuthenticated()) {
                log.info("Encerrando sessão do usuário ID: {}", session.getUserId());
            } else {
                log.warn("Solicitação de logout recebida sem sessão ativa.");
            }
            session.clear();
            log.info("Sessão encerrada com sucesso.");
        } catch (Exception e) {
            log.error("Erro ao encerrar sessão: {}", e.getMessage());
            throw new RuntimeException("Falha ao realizar logout", e);
        } finally {
            timer.stopAndLog(log);
        }
    }

    /**
     * Verifica se há uma sessão ativa e se o tempo de inatividade é válido.
     */
    public void requireActiveSession() {
        try {
            session.requireAuthenticated();
            Instant last = session.getLastActivityAt();

            if (last != null && Duration.between(last, Instant.now()).compareTo(IDLE_TIMEOUT) > 0) {
                log.warn("Sessão expirada por inatividade. Tempo máximo permitido: {} minutos.", IDLE_TIMEOUT.toMinutes());
                session.clear();
                throw new IllegalStateException("Sessão expirada por inatividade. Faça login novamente.");
            }

            session.touch();
            log.debug("Sessão validada e tempo de atividade atualizado. Usuário ID: {}", session.getUserId());
        } catch (IllegalStateException e) {
            log.error("Sessão inválida ou expirada: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Erro ao validar sessão ativa: {}", e.getMessage());
            throw new RuntimeException("Falha ao verificar sessão ativa", e);
        }
    }
}
