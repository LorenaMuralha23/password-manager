package com.tcc.password_manager.crypto;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import javax.crypto.spec.SecretKeySpec;

/**
 * Classe simples para derivar uma chave AES-256 a partir da senha do usuário.
 * (No futuro, substituir por PBKDF2/Argon2.)
 */
public class PasswordKeyDerivation {

    public SecretKeySpec deriveKey(String password) {
        byte[] passwordBytes = password.getBytes(StandardCharsets.UTF_8);
        byte[] keyBytes = Arrays.copyOf(passwordBytes, 32); // garante 32 bytes
        return new SecretKeySpec(keyBytes, "AES");
    }
}
