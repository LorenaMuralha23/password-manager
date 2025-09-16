package com.tcc.password_manager.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.password_manager.crypto.CryptoService;
import com.tcc.password_manager.crypto.EncryptedPayload;
import com.tcc.password_manager.crypto.HashService;
import com.tcc.password_manager.crypto.IVGeneratorService;
import com.tcc.password_manager.crypto.JsonEncryptionService;
import com.tcc.password_manager.crypto.KeyGeneratorService;
import com.tcc.password_manager.crypto.PasswordKeyDerivation;
import com.tcc.password_manager.dto.AppUserClearData;
import com.tcc.password_manager.model.AppUser;
import com.tcc.password_manager.repository.AppUserRepository;
import java.util.List;
import java.util.Optional;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

@Service
public class AppUserService {

    private final AppUserRepository userRepository;
    private final JsonEncryptionService jsonEncService;
    private final CryptoService cryptoService;
    private final KeyGeneratorService keyGen;
    private final PasswordKeyDerivation passwordKeyDerivation;
    private final HashService hashService;

    public AppUserService(AppUserRepository userRepository) {
        this.userRepository = userRepository;

        // Simples: instanciando dependências aqui.
        // Futuro: mover para Beans configurados.
        ObjectMapper mapper = new ObjectMapper();
        this.cryptoService = new CryptoService(new IVGeneratorService(), mapper);
        this.jsonEncService = new JsonEncryptionService(cryptoService, mapper);
        this.keyGen = new KeyGeneratorService();
        this.passwordKeyDerivation = new PasswordKeyDerivation();
        this.hashService = new HashService();
    }

    /**
     * Realiza o registro de um novo usuário: - Gera a UMK (User Master Key). -
     * Deriva a chave da senha mestre do usuário (passwordKey). - Faz wrap da
     * UMK com a passwordKey (AES-GCM). - Gera hash da UMK (para validação). -
     * Monta DTO em claro. - Cifra DTO com a própria UMK e salva no banco.
     */
    public AppUser registerUser(String masterKey, String mfaSecret) {
        // 1. Gera UMK
        byte[] umk = keyGen.generateKey();

        // 2. Deriva chave a partir da senha do usuário
        SecretKeySpec passwordKey = passwordKeyDerivation.deriveKey(masterKey);

        // 3. Wrap da UMK (cifra a UMK com a senha)
        EncryptedPayload umkWrappedPayload = cryptoService.encrypt(passwordKey.getEncoded(), umk, null);
        String umkWrappedJson = cryptoService.toJson(umkWrappedPayload);

        // 4. Gera hash da UMK (para validar no login)
        String umkHash = hashService.hashToBase64(umk);

        // 5. Cria DTO em claro
        AppUserClearData userDto = new AppUserClearData(
                mfaSecret,
                umkWrappedJson,
                umkHash
        );

        // 6. Cifra o DTO com a própria UMK
        String encryptedData = jsonEncService.encryptDto(userDto, umk, null, AppUserClearData.class);

        // 7. Salva entidade
        AppUser entity = new AppUser(null, encryptedData);
        return userRepository.save(entity);
    }

    /**
     * Decifra o encryptedData e retorna o DTO em claro. (Precisa da UMK já
     * recuperada antes no fluxo de login.)
     */
    public AppUserClearData decryptToDto(String encryptedData, byte[] userKey) {
        return jsonEncService.decryptToDto(encryptedData, userKey, null, AppUserClearData.class);
    }

    public AppUserClearData decrypt(AppUser entity, byte[] userKey) {
        return decryptToDto(entity.getEncryptedData(), userKey);
    }

    // --- CRUD básico ---
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
