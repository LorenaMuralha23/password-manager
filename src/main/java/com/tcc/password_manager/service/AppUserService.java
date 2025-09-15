package com.tcc.password_manager.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.password_manager.crypto.CryptoService;
import com.tcc.password_manager.crypto.IVGeneratorService;
import com.tcc.password_manager.crypto.JsonEncryptionService;
import com.tcc.password_manager.dto.AppUserClearData;
import com.tcc.password_manager.model.AppUser;
import com.tcc.password_manager.repository.AppUserRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class AppUserService {

    private final AppUserRepository userRepository;
    private final JsonEncryptionService jsonEncService;

    // Mantendo a simplicidade: instanciando dependências aqui.
    public AppUserService(AppUserRepository userRepository) {
        this.userRepository = userRepository;
        this.jsonEncService = new JsonEncryptionService(
                new CryptoService(new IVGeneratorService(), new ObjectMapper()),
                new ObjectMapper()
        );
    }

    /** Cifra o DTO e persiste a entidade. */
    public AppUser save(AppUserClearData dto, byte[] userKey) {
        String encryptedData = jsonEncService.encryptDto(dto, userKey, null, AppUserClearData.class);
        AppUser entity = new AppUser(null, encryptedData);
        return userRepository.save(entity);
    }

    /** Decifra o encryptedData (string) e retorna o DTO em claro. */
    public AppUserClearData decryptToDto(String encryptedData, byte[] userKey) {
        return jsonEncService.decryptToDto(encryptedData, userKey, null, AppUserClearData.class);
    }

    /** Atalho: decifra a partir da própria entidade. */
    public AppUserClearData decrypt(AppUser entity, byte[] userKey) {
        return decryptToDto(entity.getEncryptedData(), userKey);
    }

    public List<AppUser> findAll() {
        return userRepository.findAll();
    }

    public Optional<AppUser> findById(Long id) {
        return userRepository.findById(id);
    }

    public void delete(Long id) {
        userRepository.deleteById(id);
    }
}
