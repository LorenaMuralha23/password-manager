package com.tcc.password_manager.crypto;

import com.tcc.password_manager.util.LogTimer;
import java.security.SecureRandom;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

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

@Service
public class KeyGeneratorService {
    
    private static final Logger log = LoggerFactory.getLogger(KeyGeneratorService.class);
    private static final int AES_KEY_SIZE_BYTES = 32; // 256 bits
    
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Gera uma nova chave simétrica AES-256.
     *
     * @return chave em formato de array de bytes (32 bytes)
     */
    public byte[] generateKey() {
        LogTimer timer = LogTimer.start("Generate AES-256 key (byte array)");
        try {
            byte[] key = new byte[AES_KEY_SIZE_BYTES];
            secureRandom.nextBytes(key);
            log.info("Nova chave AES-256 gerada com sucesso. Tamanho: {} bytes.", AES_KEY_SIZE_BYTES);
            return key;
        } catch (Exception e) {
            log.error("Falha ao gerar chave AES-256: {}", e.getMessage());
            throw new RuntimeException("Erro ao gerar chave AES-256", e);
        } finally {
            timer.stopAndLog(log);
        }
    }

    /**
     * Gera uma nova chave simétrica AES-256 e retorna em Base64.
     *
     * @return chave em formato Base64 (string)
     */
    public String generateKeyBase64() {
        LogTimer timer = LogTimer.start("Generate AES-256 key (Base64)");
        try {
            byte[] key = generateKey();
            String base64Key = Base64.getEncoder().encodeToString(key);
            log.info("Nova chave AES-256 gerada e convertida para Base64. Comprimento da string: {} caracteres.", base64Key.length());
            return base64Key;
        } catch (Exception e) {
            log.error("Falha ao gerar chave AES-256 em Base64: {}", e.getMessage());
            throw new RuntimeException("Erro ao gerar chave em Base64", e);
        } finally {
            timer.stopAndLog(log);
        }
    }
    
}
