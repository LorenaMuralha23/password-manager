package com.tcc.password_manager.crypto;

import com.tcc.password_manager.util.LogTimer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Serviço utilitário para geração de hashes criptográficos.
 *
 * <p>
 * Atualmente utiliza SHA-256, adequado para validar integridade e consistência
 * de dados como a chave-mestra (UMK) do usuário.</p>
 *
 * <p>
 * Observações:</p>
 * <ul>
 * <li>O hash é determinístico: a mesma entrada sempre gera o mesmo
 * resultado.</li>
 * <li>O hash não deve ser usado como substituto de KDF (ex.: Argon2,
 * PBKDF2).</li>
 * <li>No futuro, pode ser substituído por HMAC ou SHA-512 com sal, sem mudar a
 * interface.</li>
 * </ul>
 */
@Service
public class HashService {

    private static final Logger log = LoggerFactory.getLogger(HashService.class);

    /**
     * Gera o hash SHA-256 de um array de bytes e retorna em Base64.
     *
     * @param data dados de entrada (em bytes)
     * @return hash em formato Base64
     */
    public String hashToBase64(byte[] data) {
        LogTimer timer = LogTimer.start("Generate SHA-256 hash (bytes)");
        try {
            log.debug("Iniciando geracao de hash SHA-256 a partir de bytes. Tamanho da entrada: {} bytes.",
                    data != null ? data.length : 0);

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            String base64Hash = Base64.getEncoder().encodeToString(hash);

            log.info("Hash SHA-256 gerado com sucesso. Tamanho do digest: {} bytes, formato Base64: {} caracteres.",
                    hash.length, base64Hash.length());
            return base64Hash;
        } catch (NoSuchAlgorithmException e) {
            log.error("Falha ao calcular hash SHA-256: {}", e.getMessage());
            throw new RuntimeException("Falha ao calcular hash SHA-256", e);
        } finally {
            timer.stopAndLog(log);
        }
    }

    /**
     * Gera o hash SHA-256 de uma string (UTF-8) e retorna em Base64.
     *
     * @param data string de entrada
     * @return hash em formato Base64
     */
    public String hashToBase64(String data) {
        LogTimer timer = LogTimer.start("Generate SHA-256 hash (string)");
        try {
            log.debug("Iniciando geracao de hash SHA-256 a partir de string. Comprimento: {} caracteres.",
                    data != null ? data.length() : 0);

            String hash = hashToBase64(data.getBytes(StandardCharsets.UTF_8));
            log.info("Hash SHA-256 gerado a partir de string. Comprimento final Base64: {} caracteres.",
                    hash.length());
            return hash;
        } catch (Exception e) {
            log.error("Falha ao calcular hash SHA-256 de string: {}", e.getMessage());
            throw new RuntimeException("Falha ao calcular hash SHA-256 de string", e);
        } finally {
            timer.stopAndLog(log);
        }
    }

    /**
     * Compara de forma segura (constant-time) dois hashes em Base64.
     *
     * @param expected valor esperado
     * @param provided valor fornecido
     * @return true se iguais, false caso contrário
     */
    public boolean verifyHash(String expected, String provided) {
        LogTimer timer = LogTimer.start("Verify hash equality (constant-time)");
        try {
            if (expected == null || provided == null) {
                log.warn("Tentativa de comparacao de hash com valores nulos.");
                return false;
            }

            byte[] a = expected.getBytes(StandardCharsets.UTF_8);
            byte[] b = provided.getBytes(StandardCharsets.UTF_8);

            if (a.length != b.length) {
                log.debug("Hashes com tamanhos diferentes: {} e {} bytes.", a.length, b.length);
                return false;
            }

            int result = 0;
            for (int i = 0; i < a.length; i++) {
                result |= a[i] ^ b[i];
            }

            boolean equals = (result == 0);
            log.info("Comparação de hash concluida. Resultado: {}", equals ? "iguais" : "diferentes");
            return equals;
        } catch (Exception e) {
            log.error("Erro durante verificacao de hash: {}", e.getMessage());
            throw new RuntimeException("Falha ao comparar hashes", e);
        } finally {
            timer.stopAndLog(log);
        }
    }

}
