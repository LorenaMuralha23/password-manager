package com.tcc.password_manager.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.password_manager.crypto.CryptoService;
import com.tcc.password_manager.crypto.IVGeneratorService;
import com.tcc.password_manager.crypto.JsonEncryptionService;
import com.tcc.password_manager.dto.PasswordReferenceClearData;
import com.tcc.password_manager.model.AppUser;
import com.tcc.password_manager.model.PasswordReference;
import com.tcc.password_manager.repository.PasswordReferenceRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class PasswordReferenceService {

    private final PasswordReferenceRepository passwordReferenceRepository;
    private final JsonEncryptionService jsonEncService;

    public PasswordReferenceService(PasswordReferenceRepository passwordReferenceRepository) {
        this.passwordReferenceRepository = passwordReferenceRepository;
        this.jsonEncService = new JsonEncryptionService(
                new CryptoService(new IVGeneratorService(), new ObjectMapper()),
                new ObjectMapper()
        );
    }

    /** Cifra o DTO com a UMK do usuário e persiste a entidade. */
    public PasswordReference save(PasswordReferenceClearData dto, AppUser user, byte[] userKey) {
        String encryptedData = jsonEncService.encryptDto(dto, userKey, null, PasswordReferenceClearData.class);
        PasswordReference entity = new PasswordReference(null, encryptedData, user);
        return passwordReferenceRepository.save(entity);
    }

    /** Decifra o encryptedData (string) e retorna o DTO em claro. */
    public PasswordReferenceClearData decryptToDto(String encryptedData, byte[] userKey) {
        return jsonEncService.decryptToDto(encryptedData, userKey, null, PasswordReferenceClearData.class);
    }

    /** Atalho: decifra a partir da própria entidade. */
    public PasswordReferenceClearData decrypt(PasswordReference entity, byte[] userKey) {
        return decryptToDto(entity.getEncryptedData(), userKey);
    }

    public List<PasswordReference> findAll() {
        return passwordReferenceRepository.findAll();
    }

    public Optional<PasswordReference> findById(Long id) {
        return passwordReferenceRepository.findById(id);
    }

    public List<PasswordReference> findByUserId(Long userId) {
        return passwordReferenceRepository.findByUserId(userId);
    }

    public void delete(Long id) {
        passwordReferenceRepository.deleteById(id);
    }
}
