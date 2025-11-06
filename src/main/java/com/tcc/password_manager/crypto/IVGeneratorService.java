package com.tcc.password_manager.crypto;

import com.tcc.password_manager.util.LogTimer;
import java.security.SecureRandom;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    
    private static final Logger log = LoggerFactory.getLogger(IVGeneratorService.class);
    private static final int GCM_IV_SIZE_BYTES = 12; // 96 bits (padrão para GCM)
    
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Gera um novo IV (Initialization Vector) de 12 bytes.
     *
     * @return IV em formato de array de bytes
     */
    public byte[] generateIV() {
        LogTimer timer = LogTimer.start("Generate GCM IV (byte array)");
        try {
            byte[] iv = new byte[GCM_IV_SIZE_BYTES];
            secureRandom.nextBytes(iv);
            log.info("IV gerado com sucesso. Tamanho: {} bytes.", GCM_IV_SIZE_BYTES);
            return iv;
        } catch (Exception e) {
            log.error("Falha ao gerar IV GCM: {}", e.getMessage());
            throw new RuntimeException("Erro ao gerar IV GCM", e);
        } finally {
            timer.stopAndLog(log);
        }
    }

    /**
     * Gera um novo IV (Initialization Vector) e retorna em Base64.
     *
     * @return IV em formato Base64 (string)
     */
    public String generateIVBase64() {
        LogTimer timer = LogTimer.start("Generate GCM IV (Base64)");
        try {
            String base64 = Base64.getEncoder().encodeToString(generateIV());
            log.info("IV GCM gerado e convertido para Base64. Comprimento da string: {} caracteres.", base64.length());
            return base64;
        } catch (Exception e) {
            log.error("Falha ao gerar IV GCM em Base64: {}", e.getMessage());
            throw new RuntimeException("Erro ao gerar IV GCM em Base64", e);
        } finally {
            timer.stopAndLog(log);
        }
    }
    
}
