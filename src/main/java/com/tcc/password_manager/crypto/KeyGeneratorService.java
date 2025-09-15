package com.tcc.password_manager.crypto;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Serviço responsável por gerar chaves simétricas (AES-256) de forma segura.
 *
 * <p>As chaves geradas por este serviço são utilizadas como "User Master Key" (UMK),
 * ou seja, a chave mestra individual de cada usuário.</p>
 *
 * <p>Detalhes técnicos:</p>
 * <ul>
 *   <li>Algoritmo alvo: AES-256</li>
 *   <li>Tamanho da chave: 32 bytes (256 bits)</li>
 *   <li>Fonte de aleatoriedade: {@link SecureRandom}, seguro para uso criptográfico</li>
 * </ul>
 *
 * <p>Observação: A chave gerada nunca deve ser armazenada em claro no banco.
 * Ela deve ser protegida (ex.: cifrada com a senha do usuário via AES-GCM)
 * antes de ser persistida.</p>
 */

public class KeyGeneratorService {
    private static final int AES_KEY_SIZE_BYTES = 32; // 256 bits
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Gera uma nova chave simétrica AES-256.
     *
     * @return chave em formato de array de bytes (32 bytes)
     */
    public byte[] generateKey() {
        byte[] key = new byte[AES_KEY_SIZE_BYTES];
        secureRandom.nextBytes(key);
        return key;
    }

    /**
     * Gera uma nova chave simétrica AES-256 e retorna em Base64.
     *
     * @return chave em formato Base64 (string)
     */
    public String generateKeyBase64() {
        return Base64.getEncoder().encodeToString(generateKey());
    }
    
}
