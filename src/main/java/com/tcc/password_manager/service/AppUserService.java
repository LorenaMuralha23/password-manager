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
     * Realiza o registro de um novo usuário:
     *
     * 1. Gera uma nova UMK (User Master Key), chave AES-256 aleatória exclusiva
     * do usuário. 2. Deriva a passwordKey a partir da senha mestre informada
     * pelo usuário. (atualmente derivação simples → no futuro substituir por
     * PBKDF2/Argon2). 3. Usa a passwordKey para cifrar a UMK (wrap da UMK) em
     * modo AES-GCM. O resultado (EncryptedPayload em JSON) é armazenado em
     * umkWrapped. 4. Calcula o hash da UMK em claro e armazena em umkHash. Esse
     * valor será usado no login para validar se a UMK foi recuperada
     * corretamente. 5. Monta um DTO em claro (AppUserClearData) contendo
     * mfaSecret, umkWrapped e umkHash. 6. Cifra esse DTO com a própria UMK,
     * gerando o campo encryptedData. Esse campo contém as informações do
     * usuário em formato seguro. 7. Persiste no banco a entidade AppUser com: -
     * encryptedData (DTO cifrado com a UMK) - umkWrapped (UMK cifrada com a
     * senha do usuário) - umkHash (hash da UMK em claro, para validação
     * futura).
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
        AppUser entity = new AppUser();
        entity.setEncryptedData(encryptedData);
        entity.setUmkWrapped(umkWrappedJson);
        entity.setUmkHash(umkHash);
        return userRepository.save(entity);
    }

    /**
     * Realiza o login do usuário: 1. Recupera a entidade do banco pelo userId.
     * 2. Deriva a chave simétrica (passwordKey) a partir da senha mestre
     * informada. 3. Usa a passwordKey para decifrar o umkWrapped armazenado →
     * obtém a UMK real. 4. Calcula o hash da UMK e compara com o umkHash salvo
     * no banco. - Se não coincidir, a senha está incorreta ou os dados foram
     * adulterados. 5. Se coincidir, usa a UMK para decifrar o encryptedData do
     * usuário. 6. Retorna o DTO em claro (AppUserClearData).
     */
    public AppUserClearData login(Long userId, String masterKey) {
        // 1. Recupera entity
        AppUser entity = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        // 2. Deriva passwordKey a partir da senha do usuário
        SecretKeySpec passwordKey = passwordKeyDerivation.deriveKey(masterKey);

        // 3. Faz o unwrap da UMK com a passwordKey
        EncryptedPayload umkPayload = cryptoService.fromJson(entity.getUmkWrapped());
        byte[] umk = cryptoService.decrypt(passwordKey.getEncoded(), umkPayload, null);

        // 4. Valida hash da UMK
        String recoveredHash = hashService.hashToBase64(umk);
        if (!recoveredHash.equals(entity.getUmkHash())) {
            throw new RuntimeException("Senha incorreta ou dados adulterados!");
        }

        // 5. Decifra os dados do usuário com a UMK
        AppUserClearData clearData = jsonEncService.decryptToDto(
                entity.getEncryptedData(),
                umk,
                null,
                AppUserClearData.class
        );

        // 6. Retorna o DTO em claro
        return clearData;
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
