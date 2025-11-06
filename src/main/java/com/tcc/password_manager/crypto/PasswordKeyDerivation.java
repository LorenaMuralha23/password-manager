package com.tcc.password_manager.crypto;

import com.tcc.password_manager.util.LogTimer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Classe simples para derivar uma chave AES-256 a partir da senha do usuário.
 * (No futuro, substituir por PBKDF2/Argon2.)
 */
@Service
public class PasswordKeyDerivation {

    private static final Logger log = LoggerFactory.getLogger(PasswordKeyDerivation.class);

    /**
     * Deriva uma chave AES-256 a partir da senha informada.
     * Observação: derivação simplificada para fins de protótipo.
     */
    public SecretKeySpec deriveKey(String password) {
        LogTimer timer = LogTimer.start("Derive key from master password (simplified)");
        try {
            log.info("Iniciando derivação de chave a partir da senha mestre.");
            byte[] passwordBytes = password.getBytes(StandardCharsets.UTF_8);
            byte[] keyBytes = Arrays.copyOf(passwordBytes, 32); // garante 32 bytes

            SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");
            log.debug("Derivação concluída. Tamanho da chave: {} bytes.", keyBytes.length);
            return key;
        } catch (Exception e) {
            log.error("Falha ao derivar chave a partir da senha: {}", e.getMessage());
            throw e;
        } finally {
            timer.stopAndLog(log);
        }
    }
}
