package com.tcc.password_manager.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.password_manager.crypto.CryptoService;
import com.tcc.password_manager.crypto.IVGeneratorService;
import com.tcc.password_manager.crypto.JsonEncryptionService;
import com.tcc.password_manager.dto.PasswordReferenceClearData;
import com.tcc.password_manager.model.AppUser;
import com.tcc.password_manager.model.PasswordReference;
import com.tcc.password_manager.repository.PasswordReferenceRepository;
import com.tcc.password_manager.util.LogTimer;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Serviço responsável pela persistência e recuperação de registros de senha no
 * banco local. Executa a criptografia e descriptografia dos metadados
 * utilizando a UMK do usuário.
 */
@Service
public class PasswordReferenceService {

    private static final Logger log = LoggerFactory.getLogger(PasswordReferenceService.class);

    private final PasswordReferenceRepository passwordReferenceRepository;
    private final JsonEncryptionService jsonEncService;

    public PasswordReferenceService(PasswordReferenceRepository passwordReferenceRepository) {
        this.passwordReferenceRepository = passwordReferenceRepository;
        this.jsonEncService = new JsonEncryptionService(
                new CryptoService(new IVGeneratorService(), new ObjectMapper()),
                new ObjectMapper()
        );
    }

    /**
     * Cifra o DTO com a UMK do usuário e persiste a entidade no banco local.
     */
    public PasswordReference save(PasswordReferenceClearData dto, AppUser user, byte[] userKey) {
        LogTimer timer = LogTimer.start("Encrypt and persist password reference");
        
        try {
            log.info("Iniciando processo de cifragem e persistencia de credencial para o usuario ID: {}", user.getId());

            String encryptedData = jsonEncService.encryptDto(dto, userKey, null, PasswordReferenceClearData.class);
            log.debug("Metadados cifrados. Tamanho do payload: {} caracteres.", encryptedData.length());

            PasswordReference entity = new PasswordReference(null, encryptedData, user);
            PasswordReference saved = passwordReferenceRepository.save(entity);

            log.info("Credencial persistida com sucesso. ID gerado: {}", saved.getId());
            return saved;
        } catch (Exception e) {
            log.error("Falha ao cifrar ou persistir credencial: {}", e.getMessage());
            throw new RuntimeException("Erro ao salvar referencia de senha", e);
        } finally {
            timer.stopAndLog(log);
        }
    }

    /**
     * Decifra os metadados cifrados (string JSON) e retorna o DTO em claro.
     */
    public PasswordReferenceClearData decryptToDto(String encryptedData, byte[] userKey) {
        LogTimer timer = LogTimer.start("Decrypt password reference from JSON string");
        
        try {
            log.debug("Iniciando decifragem de metadados armazenados. Tamanho: {} caracteres.", encryptedData.length());
            PasswordReferenceClearData clear = jsonEncService.decryptToDto(encryptedData, userKey, null, PasswordReferenceClearData.class);
            log.info("Metadados decifrados com sucesso.");
            return clear;
        } catch (Exception e) {
            log.error("Falha ao decifrar metadados: {}", e.getMessage());
            throw new RuntimeException("Erro ao decifrar dados criptografados", e);
        } finally {
            timer.stopAndLog(log);
        }
    }

    /**
     * Decifra uma entidade PasswordReference completa e retorna o DTO correspondente.
     */
    public PasswordReferenceClearData decrypt(PasswordReference entity, byte[] userKey) {
        LogTimer timer = LogTimer.start("Decrypt password reference from entity");
        
        try {
            log.debug("Decifrando entidade ID: {}", entity.getId());
            PasswordReferenceClearData dto = decryptToDto(entity.getEncryptedData(), userKey);
            log.info("Entidade ID {} decifrada com sucesso.", entity.getId());
            return dto;
        } catch (Exception e) {
            log.error("Falha ao decifrar entidade ID {}: {}", entity.getId(), e.getMessage());
            throw new RuntimeException("Erro ao decifrar referencia de senha", e);
        } finally {
            timer.stopAndLog(log);
        }
    }

    /**
     * Recupera todas as referências de senha do banco local.
     */
    public List<PasswordReference> findAll() {
        LogTimer timer = LogTimer.start("Find all password references");
        
        try {
            List<PasswordReference> list = passwordReferenceRepository.findAll();
            log.info("Consulta concluida. Total de registros encontrados: {}", list.size());
            return list;
        } catch (Exception e) {
            log.error("Falha ao consultar todas as referencias: {}", e.getMessage());
            throw new RuntimeException("Erro ao buscar todas as referencias", e);
        } finally {
            timer.stopAndLog(log);
        }
    }

    /**
     * Busca uma referência de senha pelo ID.
     */
    public Optional<PasswordReference> findById(Long id) {
        LogTimer timer = LogTimer.start("Find password reference by ID");
        try {
            Optional<PasswordReference> ref = passwordReferenceRepository.findById(id);
            if (ref.isPresent()) {
                log.info("Referencia encontrada para o ID: {}", id);
            } else {
                log.warn("Nenhuma referencia encontrada para o ID: {}", id);
            }
            return ref;
        } catch (Exception e) {
            log.error("Erro ao buscar referencia ID {}: {}", id, e.getMessage());
            throw new RuntimeException("Erro ao buscar referencia por ID", e);
        } finally {
            timer.stopAndLog(log);
        }
    }

    /**
     * Busca todas as referências associadas a um usuário específico.
     */
    public List<PasswordReference> findByUserId(Long userId) {
        LogTimer timer = LogTimer.start("Find password references by user ID");
        try {
            List<PasswordReference> refs = passwordReferenceRepository.findByUserId(userId);
            log.info("Consulta concluida. Total de registros encontrados para o usuario {}: {}", userId, refs.size());
            return refs;
        } catch (Exception e) {
            log.error("Falha ao buscar referências para o usuario {}: {}", userId, e.getMessage());
            throw new RuntimeException("Erro ao buscar referencias por usuario", e);
        } finally {
            timer.stopAndLog(log);
        }
    }

    /**
     * Remove uma referência de senha pelo ID.
     */
    public void delete(Long id) {
        LogTimer timer = LogTimer.start("Delete password reference by ID");
        try {
            passwordReferenceRepository.deleteById(id);
            log.info("Referencia de senha removida com sucesso. ID: {}", id);
        } catch (Exception e) {
            log.error("Falha ao remover referencia ID {}: {}", id, e.getMessage());
            throw new RuntimeException("Erro ao remover referencia de senha", e);
        } finally {
            timer.stopAndLog(log);
        }
    }
}
