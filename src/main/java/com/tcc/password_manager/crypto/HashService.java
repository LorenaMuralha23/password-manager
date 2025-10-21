package com.tcc.password_manager.crypto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import org.springframework.stereotype.Service;

/**
 * Serviço utilitário para geração de hashes criptográficos.
 *
 * <p>Atualmente utiliza SHA-256, adequado para validar integridade e consistência
 * de dados como a chave-mestra (UMK) do usuário.</p>
 *
 * <p>Observações:</p>
 * <ul>
 *   <li>O hash é determinístico: a mesma entrada sempre gera o mesmo resultado.</li>
 *   <li>O hash não deve ser usado como substituto de KDF (ex.: Argon2, PBKDF2).</li>
 *   <li>No futuro, pode ser substituído por HMAC ou SHA-512 com sal, sem mudar a interface.</li>
 * </ul>
 */

@Service
public class HashService {
    
    /**
     * Gera o hash SHA-256 de um array de bytes e retorna em Base64.
     *
     * @param data dados de entrada (em bytes)
     * @return hash em formato Base64
     */
    public String hashToBase64(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Falha ao calcular hash SHA-256", e);
        }
    }

    /**
     * Gera o hash SHA-256 de uma string (UTF-8) e retorna em Base64.
     *
     * @param data string de entrada
     * @return hash em formato Base64
     */
    public String hashToBase64(String data) {
        return hashToBase64(data.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Compara de forma segura (constant-time) dois hashes em Base64.
     *
     * @param expected valor esperado
     * @param provided valor fornecido
     * @return true se iguais, false caso contrário
     */
    public boolean verifyHash(String expected, String provided) {
        if (expected == null || provided == null) return false;
        byte[] a = expected.getBytes(StandardCharsets.UTF_8);
        byte[] b = provided.getBytes(StandardCharsets.UTF_8);
        if (a.length != b.length) return false;

        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i];
        }
        return result == 0;
    }
    
}
