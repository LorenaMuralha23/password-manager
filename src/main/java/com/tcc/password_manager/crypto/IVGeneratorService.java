package com.tcc.password_manager.crypto;

import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.stereotype.Service;

/**
 * Serviço responsável por gerar vetores de inicialização (IVs) para AES-GCM.
 *
 * <p>No AES-GCM, cada operação de criptografia deve usar um IV único,
 * tipicamente com 12 bytes (96 bits) de comprimento.</p>
 *
 * <p>O IV não precisa ser secreto e deve ser armazenado junto ao ciphertext
 * no banco de dados. Entretanto, a reutilização de IV com a mesma chave
 * compromete a segurança do esquema.</p>
 */

@Service
public class IVGeneratorService {
    
    private static final int GCM_IV_SIZE_BYTES = 12; // 96 bits (padrão para GCM)
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Gera um novo IV (Initialization Vector) de 12 bytes.
     *
     * @return IV em formato de array de bytes
     */
    public byte[] generateIV() {
        byte[] iv = new byte[GCM_IV_SIZE_BYTES];
        secureRandom.nextBytes(iv);
        return iv;
    }

    /**
     * Gera um novo IV (Initialization Vector) e retorna em Base64.
     *
     * @return IV em formato Base64 (string)
     */
    public String generateIVBase64() {
        return Base64.getEncoder().encodeToString(generateIV());
    }
    
}
