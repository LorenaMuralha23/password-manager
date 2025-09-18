package com.tcc.password_manager.session;

import com.tcc.password_manager.crypto.CryptoService;
import com.tcc.password_manager.crypto.EncryptedPayload;
import com.tcc.password_manager.crypto.HashService;
import com.tcc.password_manager.crypto.PasswordKeyDerivation;
import com.tcc.password_manager.model.AppUser;
import com.tcc.password_manager.repository.AppUserRepository;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import java.time.Duration;
import java.time.Instant;

/**
 * {@code SessionManager} autentica o usuário com base no userId e senha mestre,
 * recupera a UMK em claro e mantém essa chave em memória para operações futuras.
 */
@Component
public class SessionManager {

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

    public SessionContext getSession() { return session; }

    public void login(Long userId, String masterKey) {
        // 1. Busca usuário
        AppUser entity = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        // 2. Deriva passwordKey a partir da senha mestre
        SecretKeySpec passwordKey = keyDerivation.deriveKey(masterKey);

        // 3. Desfaz wrap da UMK
        EncryptedPayload umkPayload = cryptoService.fromJson(entity.getUmkWrapped());
        byte[] umk = cryptoService.decrypt(passwordKey.getEncoded(), umkPayload, null);

        // 4. Valida hash da UMK
        String recoveredHash = hashService.hashToBase64(umk);
        if (!recoveredHash.equals(entity.getUmkHash())) {
            throw new RuntimeException("Senha incorreta ou dados adulterados!");
        }

        // 5. Guarda a UMK em sessão
        session.establish(userId, umk);
    }

    public void logout() {
        session.clear();
    }

    public void requireActiveSession() {
        session.requireAuthenticated();
        Instant last = session.getLastActivityAt();
        if (last != null &&
            Duration.between(last, Instant.now()).compareTo(IDLE_TIMEOUT) > 0) {
            session.clear();
            throw new IllegalStateException("Sessão expirada por inatividade. Faça login novamente.");
        }
        session.touch();
    }
}
