package com.tcc.password_manager.crypto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.password_manager.util.LogTimer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Serviço de alto nível que faz a ponte entre DTOs em claro e Entities cifradas.
 *
 * <p>
 * Utiliza o {@link CryptoService} para:
 * <ul>
 *   <li>Cifrar um DTO (serializado em JSON) e retornar uma string pronta para salvar
 *       no campo encrypted_data da entity</li>
 *   <li>Decifrar uma string do banco (encrypted_data) e reconstruir o DTO original</li>
 * </ul>
 * </p>
 *
 * <h2>Fluxo de uso</h2>
 *
 * <p><b>Salvar:</b></p>
 * <pre>
 *   String encData = jsonEncryptionService.encryptDto(clearDto, userKey, aad, PasswordReferenceClearData.class);
 *   passwordReference.setEncryptedData(encData);
 * </pre>
 *
 * <p><b>Ler:</b></p>
 * <pre>
 *   PasswordReferenceClearData clearDto =
 *       jsonEncryptionService.decryptToDto(passwordReference.getEncryptedData(), userKey, aad, PasswordReferenceClearData.class);
 * </pre>
 */

@Service
public class JsonEncryptionService {
    
    private static final Logger log = LoggerFactory.getLogger(JsonEncryptionService.class);
    
    private final CryptoService cryptoService;
    private final ObjectMapper mapper;

    public JsonEncryptionService(CryptoService cryptoService, ObjectMapper mapper) {
        this.cryptoService = cryptoService;
        this.mapper = mapper;
    }
    
    /**
     * Cifra um DTO (em claro) e retorna string JSON para armazenar em encrypted_data.
     *
     * @param dto objeto em claro (ex.: PasswordReferenceClearData)
     * @param key chave AES-256 do usuário (32 bytes)
     * @param aad dados adicionais autenticados (opcional; pode ser null)
     * @param <T> tipo do DTO
     * @return string JSON representando um EncryptedPayload
     */
    public <T> String encryptDto(T dto, byte[] key, byte[] aad, Class<T> dtoClass) {
        String dtoName = dtoClass != null ? dtoClass.getSimpleName() : (dto != null ? dto.getClass().getSimpleName() : "UnknownDTO");
        LogTimer timer = LogTimer.start("Encrypt DTO to encrypted_data (" + dtoName + ")");
        try {
            log.info("Iniciando cifragem do DTO para armazenamento seguro. Tipo: {}", dtoName);
            
            // 1. Serializar DTO em JSON (claro)
            String plaintextJson = mapper.writeValueAsString(dto);
            log.debug("DTO serializado em JSON. Tamanho: {} caracteres.", plaintextJson.length());

            // 2. Cifrar JSON com AES-GCM → EncryptedPayload
            EncryptedPayload payload = cryptoService.encryptString(key, plaintextJson, aad);
            log.debug("Payload cifrado gerado com sucesso.");

            // 3. Serializar EncryptedPayload em JSON (para salvar no banco)
            String encryptedData = cryptoService.toJson(payload);
            log.info("Cifragem do DTO concluida. Tamanho do encrypted_data: {} caracteres.", encryptedData.length());

            return encryptedData;
        } catch (Exception e) {
            log.error("Falha ao cifrar DTO {} para encrypted_data: {}", dtoName, e.getMessage());
            throw new RuntimeException("Falha ao cifrar DTO para encrypted_data", e);
        } finally {
            timer.stopAndLog(log);
        }
    }

    /**
     * Decifra string JSON (lida do banco) e reconstrói DTO em claro.
     *
     * @param encryptedData string JSON armazenada em encrypted_data
     * @param key chave AES-256 do usuário (32 bytes)
     * @param aad dados adicionais autenticados (mesmos usados na cifra; pode ser null)
     * @param dtoClass classe alvo do DTO
     * @param <T> tipo do DTO
     * @return DTO reconstruído em claro
     */
    public <T> T decryptToDto(String encryptedData, byte[] key, byte[] aad, Class<T> dtoClass) {
        String dtoName = dtoClass != null ? dtoClass.getSimpleName() : "UnknownDTO";
        LogTimer timer = LogTimer.start("Decrypt encrypted_data to DTO (" + dtoName + ")");
        try {
            log.info("Iniciando decifragem do campo encrypted_data para o DTO: {}", dtoName);
            log.debug("Tamanho do encrypted_data: {} caracteres.", encryptedData != null ? encryptedData.length() : 0);

            // 1. Desserializar EncryptedPayload
            EncryptedPayload payload = cryptoService.fromJson(encryptedData);
            log.debug("EncryptedPayload desserializado com sucesso.");

            // 2. Decifrar para JSON claro
            String plaintextJson = cryptoService.decryptToString(key, payload, aad);
            log.debug("Campo encrypted_data decifrado. Tamanho do JSON em claro: {} caracteres.", plaintextJson.length());

            // 3. Reconstituir o DTO a partir do JSON
            T dto = mapper.readValue(plaintextJson, dtoClass);
            log.info("Decifragem concluida. DTO {} reconstruído com sucesso.", dtoName);
            return dto;
        } catch (Exception e) {
            log.error("Falha ao decifrar encrypted_data para DTO {}: {}", dtoName, e.getMessage());
            throw new RuntimeException("Falha ao decifrar encrypted_data para DTO", e);
        } finally {
            timer.stopAndLog(log);
        }
    }
    
}
