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
import com.tcc.password_manager.util.LogTimer;
import java.util.List;
import java.util.Optional;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Serviço responsável pelo registro, autenticação e recuperação de usuarios.
 * Controla todo o ciclo de vida da User Master Key (UMK).
 */
@Service
public class AppUserService {

    private static final Logger log = LoggerFactory.getLogger(AppUserService.class);

    private final AppUserRepository userRepository;
    private final JsonEncryptionService jsonEncService;
    private final CryptoService cryptoService;
    private final KeyGeneratorService keyGen;
    private final PasswordKeyDerivation passwordKeyDerivation;
    private final HashService hashService;

    public AppUserService(AppUserRepository userRepository) {
        this.userRepository = userRepository;

        ObjectMapper mapper = new ObjectMapper();
        this.cryptoService = new CryptoService(new IVGeneratorService(), mapper);
        this.jsonEncService = new JsonEncryptionService(cryptoService, mapper);
        this.keyGen = new KeyGeneratorService();
        this.passwordKeyDerivation = new PasswordKeyDerivation();
        this.hashService = new HashService();
    }

    /**
     * Realiza o registro de um novo usuario:
     *
     * 1. Gera uma nova UMK (User Master Key), chave AES-256 aleatória exclusiva
     * do usuario.
     *
     * 2. Deriva a passwordKey a partir da senha mestre informada pelo usuario.
     * (atualmente derivação simples → no futuro substituir por PBKDF2/Argon2).
     *
     * 3. Usa a passwordKey para cifrar a UMK (wrap da UMK) em modo AES-GCM. O
     * resultado (EncryptedPayload em JSON) é armazenado em umkWrapped.
     *
     * 4. Calcula o hash da UMK em claro e armazena em umkHash. Esse valor será
     * usado no login para validar se a UMK foi recuperada corretamente.
     *
     * 5. Monta um DTO em claro (AppUserClearData) contendo mfaSecret,
     * umkWrapped e umkHash.
     *
     * 6. Cifra esse DTO com a própria UMK, gerando o campo encryptedData. Esse
     * campo contém as informações do usuario em formato seguro.
     *
     * 7. Persiste no banco a entidade AppUser com: - encryptedData (DTO cifrado
     * com a UMK) - umkWrapped (UMK cifrada com a senha do usuario) - umkHash
     * (hash da UMK em claro, para validação futura).
     */
    public AppUser registerUser(String masterKey, String mfaSecret) {
        LogTimer timer = LogTimer.start("Register new user");

        try {
            log.info("Iniciando registro de novo usuario.");
            log.debug("Gerando User Master Key (UMK) e derivando chave a partir da senha mestre.");

            // 1. Gera UMK
            byte[] umk = keyGen.generateKey();

            // 2. Deriva chave a partir da senha do usuario
            SecretKeySpec passwordKey = passwordKeyDerivation.deriveKey(masterKey);
            log.debug("Chave derivada a partir da senha mestre. Tamanho: {} bytes.", passwordKey.getEncoded().length);

            // 3. Wrap da UMK (cifra a UMK com a senha)
            EncryptedPayload umkWrappedPayload = cryptoService.encrypt(passwordKey.getEncoded(), umk, null);
            String umkWrappedJson = cryptoService.toJson(umkWrappedPayload);
            log.info("UMK cifrada com a senha do usuario (wrap concluido).");

            // 4. Gera hash da UMK (para validar no login)
            String umkHash = hashService.hashToBase64(umk);
            log.debug("Hash da UMK gerado com sucesso (mantido em sigilo).");

            // 5. Cria DTO em claro
            AppUserClearData userDto = new AppUserClearData(
                    mfaSecret,
                    umkWrappedJson,
                    umkHash
            );

            // 6. Cifra o DTO com a própria UMK
            String encryptedData = jsonEncService.encryptDto(userDto, umk, null, AppUserClearData.class);
            log.debug("DTO do usuario cifrado com a propria UMK. Tamanho: {} caracteres.", encryptedData.length());

            // 7. Persiste no banco
            AppUser entity = new AppUser();
            entity.setEncryptedData(encryptedData);
            entity.setUmkWrapped(umkWrappedJson);
            entity.setUmkHash(umkHash);
            AppUser saved = userRepository.save(entity);

            log.info("Registro de usuario concluido com sucesso. ID: {}", saved.getId());
            return saved;
        } catch (Exception e) {
            log.error("Falha no registro de usuario: {}", e.getMessage());
            throw new RuntimeException("Erro ao registrar usuario", e);
        } finally {
            timer.stopAndLog(log);
        }
    }

    /**
     * Realiza o login do usuario: 1. Recupera a entidade do banco pelo userId.
     *
     * 2. Deriva a chave simétrica (passwordKey) a partir da senha mestre
     * informada.
     *
     * 3. Usa a passwordKey para decifrar o umkWrapped armazenado → obtém a UMK
     * real.
     *
     * 4. Calcula o hash da UMK e compara com o umkHash salvo no banco. - Se nao
     * coincidir, a senha está incorreta ou os dados foram adulterados.
     *
     * 5. Se coincidir, usa a UMK para decifrar o encryptedData do usuario.
     *
     * 6. Retorna o DTO em claro (AppUserClearData).
     */
    public AppUserClearData login(Long userId, String masterKey) {
        LogTimer timer = LogTimer.start("User login");

        try {
            log.info("Iniciando processo de login para o usuario ID: {}", userId);

            // 1. Recupera entity
            AppUser entity = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Usuario nao encontrado"));

            // 2. Deriva passwordKey a partir da senha do usuario
            SecretKeySpec passwordKey = passwordKeyDerivation.deriveKey(masterKey);
            log.debug("Chave derivada a partir da senha mestre para autenticacao.");

            // 3. Faz o unwrap da UMK com a passwordKey
            EncryptedPayload umkPayload = cryptoService.fromJson(entity.getUmkWrapped());
            byte[] umk = cryptoService.decrypt(passwordKey.getEncoded(), umkPayload, null);
            log.debug("UMK decifrada com sucesso a partir da senha do usuario.");

            // 4. Valida hash da UMK
            String recoveredHash = hashService.hashToBase64(umk);
            if (!recoveredHash.equals(entity.getUmkHash())) {
                log.warn("Hash da UMK nao corresponde. Possivel senha incorreta ou dados adulterados.");
                throw new RuntimeException("Senha incorreta ou dados adulterados!");
            }

            // 5. Decifra os dados do usuario com a UMK
            AppUserClearData clearData = jsonEncService.decryptToDto(
                    entity.getEncryptedData(),
                    umk,
                    null,
                    AppUserClearData.class
            );
            log.info("Login realizado com sucesso para o usuario ID: {}", userId);

            return clearData;
        } catch (Exception e) {
            log.error("Falha durante o login do usuario ID {}: {}", userId, e.getMessage());
            throw new RuntimeException("Erro ao realizar login", e);
        } finally {
            timer.stopAndLog(log);
        }
    }

    /**
     * Decifra o encryptedData e retorna o DTO em claro.
     */
    public AppUserClearData decryptToDto(String encryptedData, byte[] userKey) {
        LogTimer timer = LogTimer.start("Decrypt user encrypted data");
        try {
            log.debug("Iniciando decifragem do campo encryptedData do usuario.");
            AppUserClearData dto = jsonEncService.decryptToDto(encryptedData, userKey, null, AppUserClearData.class);
            log.info("Campo encryptedData decifrado com sucesso.");
            return dto;
        } catch (Exception e) {
            log.error("Falha ao decifrar dados do usuario: {}", e.getMessage());
            throw new RuntimeException("Erro ao decifrar dados do usuario", e);
        } finally {
            timer.stopAndLog(log);
        }
    }

    /**
     * Versão auxiliar para decifrar diretamente a partir da entidade.
     */
    public AppUserClearData decrypt(AppUser entity, byte[] userKey) {
        LogTimer timer = LogTimer.start("Decrypt user entity");
        try {
            log.debug("Decifrando entidade de usuario ID: {}", entity.getId());
            AppUserClearData dto = decryptToDto(entity.getEncryptedData(), userKey);
            log.info("Entidade de usuario ID {} decifrada com sucesso.", entity.getId());
            return dto;
        } catch (Exception e) {
            log.error("Falha ao decifrar entidade de usuario ID {}: {}", entity.getId(), e.getMessage());
            throw new RuntimeException("Erro ao decifrar entidade de usuario", e);
        } finally {
            timer.stopAndLog(log);
        }
    }

    // --- CRUD básico ---

    public List<AppUser> findAll() {
        LogTimer timer = LogTimer.start("Find all users");
        try {
            List<AppUser> list = userRepository.findAll();
            log.info("Consulta de usuarios concluida. Total encontrado: {}", list.size());
            return list;
        } catch (Exception e) {
            log.error("Falha ao listar usuarios: {}", e.getMessage());
            throw new RuntimeException("Erro ao listar usuarios", e);
        } finally {
            timer.stopAndLog(log);
        }
    }

    public Optional<AppUser> findById(Long id) {
        LogTimer timer = LogTimer.start("Find user by ID");
        try {
            Optional<AppUser> user = userRepository.findById(id);
            if (user.isPresent()) {
                log.info("Usuario encontrado para o ID: {}", id);
            } else {
                log.warn("Nenhum usuario encontrado para o ID: {}", id);
            }
            return user;
        } catch (Exception e) {
            log.error("Erro ao buscar usuario ID {}: {}", id, e.getMessage());
            throw new RuntimeException("Erro ao buscar usuario por ID", e);
        } finally {
            timer.stopAndLog(log);
        }
    }

    public void delete(Long id) {
        LogTimer timer = LogTimer.start("Delete user by ID");
        try {
            userRepository.deleteById(id);
            log.info("Usuario removido com sucesso. ID: {}", id);
        } catch (Exception e) {
            log.error("Falha ao remover usuario ID {}: {}", id, e.getMessage());
            throw new RuntimeException("Erro ao remover usuario", e);
        } finally {
            timer.stopAndLog(log);
        }
    }
}
