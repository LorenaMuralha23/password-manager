package com.tcc.password_manager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.password_manager.classes.ShuffleMap;
import com.tcc.password_manager.crypto.JsonEncryptionService;
import com.tcc.password_manager.dto.PasswordReferenceClearData;
import com.tcc.password_manager.dto.FragmentedData;
import com.tcc.password_manager.model.AppUser;
import com.tcc.password_manager.model.PasswordReference;
import com.tcc.password_manager.service.AppUserService;
import com.tcc.password_manager.service.BlockchainService;
import com.tcc.password_manager.service.FragmentationService;
import com.tcc.password_manager.service.PasswordReferenceService;
import com.tcc.password_manager.session.SessionManager;
import com.tcc.password_manager.session.SessionContext;
import com.tcc.password_manager.util.LogTimer;
import java.util.ArrayList;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller principal da aplicação. Responsável por orquestrar o fluxo
 * completo: - Registro e autenticação - Criptografia local - Fragmentação -
 * Armazenamento distribuído em blockchains - Persistência de metadados cifrados
 */
@Component
public class PasswordManagerController {

    private static final Logger log = LoggerFactory.getLogger(PasswordManagerController.class);

    private final AppUserService appUserService;
    private final PasswordReferenceService passwordService;
    private final SessionManager sessionManager;
    private final BlockchainService blockchainService;
    private final FragmentationService fragmentationService;
    private final JsonEncryptionService jsonEncService;
    private final ObjectMapper mapper = new ObjectMapper();

    public PasswordManagerController(AppUserService appUserService,
            PasswordReferenceService passwordService,
            SessionManager sessionManager,
            FragmentationService fragmentationService,
            BlockchainService blockchainService,
            JsonEncryptionService jsonEncService) {
        this.appUserService = appUserService;
        this.passwordService = passwordService;
        this.sessionManager = sessionManager;
        this.fragmentationService = fragmentationService;
        this.blockchainService = blockchainService;
        this.jsonEncService = jsonEncService;
    }

    // ==================== Usuario ====================
    /**
     * Cadastro de novo usuario.
     */
    public AppUser registerUser(String senhaMestre, String mfaSecret) {
        LogTimer timer = LogTimer.start("Registro de Usuario");
        try {
            AppUser user = appUserService.registerUser(senhaMestre, mfaSecret);
            log.info("Usuario registrado com sucesso | ID: {}", user.getId());
            log.debug("EncryptedData..: {}", user.getEncryptedData());
            log.debug("UMK Wrapped....: {}", user.getUmkWrapped());
            return user;
        } catch (Exception e) {
            log.error("Erro ao registrar usuario: {}", e.getMessage());
            throw e;
        } finally {
            timer.stopAndLog(log);
        }
    }

    /**
     * Login: valida senha mestre, recupera UMK e popula sessão.
     */
    public void login(Long userId, String senhaMestre) {
        LogTimer timer = LogTimer.start("Login de Usuario");
        try {
            sessionManager.login(userId, senhaMestre);
            log.info("Login realizado com sucesso | User ID: {}", userId);
        } catch (Exception e) {
            log.error("Falha no login: {}", e.getMessage());
            throw e;
        } finally {
            timer.stopAndLog(log);
        }
    }

    /**
     * Logout: encerra a sessão atual.
     */
    public void logout() {
        LogTimer timer = LogTimer.start("Logout de Usuario");
        try {
            sessionManager.logout();
            log.info("Logout realizado com sucesso.");
        } catch (Exception e) {
            log.error("Falha ao realizar logout: {}", e.getMessage());
            throw e;
        } finally {
            timer.stopAndLog(log);
        }
    }

    // ==================== Senhas ====================
    /**
     * Adiciona uma nova senha para o usuario logado. Fluxo: 1. Cifra com a UMK
     * 2. Fragmenta o ciphertext 3. Grava fragmentos nas blockchains 4. Persiste
     * referências cifradas no banco
     */
    public PasswordReference addPassword(String nickname, String plainPassword) {
        sessionManager.requireActiveSession();
        SessionContext session = sessionManager.getSession();

        Long userId = session.getUserId();
        byte[] umk = session.getUmk();

        AppUser user = appUserService.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario não encontrado"));

        LogTimer timer = LogTimer.start("Cadastro de Credencial (" + nickname + ")");
        try {
            log.debug("Iniciando processo de criptografia para '{}'", nickname);

            // 1. Cifra a senha com a UMK
            String encryptedPassword = jsonEncService.encryptDto(
                    plainPassword,
                    umk,
                    null,
                    String.class
            );
            log.info("Criptografia concluida | Tamanho: {} bytes", encryptedPassword.length());

            // 2. Fragmenta o ciphertext
            FragmentedData fragmented = fragmentationService.fragmentPassword(encryptedPassword);
            log.info("Fragmentacao concluida | Fragmentos: 2");
            log.debug("Fragmento 1: {} bytes | Fragmento 2: {} bytes",
                    fragmented.getFragment1().length(), fragmented.getFragment2().length());

            // 3. Gera um ID lógico único
            String fragmentId = UUID.randomUUID().toString();

            // 4. Grava fragmentos nas blockchains (Org1 e Org2)
            blockchainService.storeFragments(fragmentId, fragmented.getFragment1(), fragmented.getFragment2());
            log.info("Fragmentos armazenados nas blockchains | ID logico: {}", fragmentId);

            // 5. Serializa o mapa de cortes para reconstrução futura
            String fragRef = mapper.writeValueAsString(fragmented.getCutPointsMap());
            log.debug("Mapa de fragmentacao: {}", fragRef);

            // 6. Monta DTO com referências reais
            PasswordReferenceClearData dto = new PasswordReferenceClearData();
            dto.setNickname(nickname);
            dto.setBlockchainReference1("Org1:" + fragmentId);
            dto.setBlockchainReference2("Org2:" + fragmentId);
            dto.setFragmentationReference(fragRef);

            // 7. Persiste no banco (DTO cifrado com a UMK)
            PasswordReference saved = passwordService.save(dto, user, umk);
            log.info("Credencial '{}' registrada com sucesso | ID: {}", nickname, saved.getId());

            return saved;

        } catch (Exception e) {
            log.error("Erro ao adicionar senha '{}': {}", nickname, e.getMessage());
            throw new RuntimeException("Erro ao adicionar senha: " + e.getMessage(), e);
        } finally {
            timer.stopAndLog(log);
        }
    }

    /**
     * Lista todas as senhas do usuario logado (somente metadados).
     */
    public List<PasswordReference> listPasswords() {
        sessionManager.requireActiveSession();
        Long userId = sessionManager.getSession().getUserId();

        LogTimer timer = LogTimer.start("Listagem de Credenciais");
        try {
            List<PasswordReference> refs = passwordService.findByUserId(userId);
            log.info("Total de credenciais encontradas: {}", refs.size());
            refs.forEach(ref -> log.debug("ID: {} | EncryptedData length: {}", ref.getId(),
                    ref.getEncryptedData() != null ? ref.getEncryptedData().length() : 0));
            return refs;
        } catch (Exception e) {
            log.error("Erro ao listar credenciais: {}", e.getMessage());
            throw e;
        } finally {
            timer.stopAndLog(log);
        }
    }

    /**
     * Recupera uma senha do usuario (somente metadados decifrados).
     */
    public PasswordReferenceClearData retrievePassword(Long passwordId) {
        sessionManager.requireActiveSession();
        SessionContext session = sessionManager.getSession();

        Long userId = session.getUserId();
        byte[] umk = session.getUmk();

        LogTimer timer = LogTimer.start("Recuperacao de Metadados | Password ID: " + passwordId);

        try {
            PasswordReference entity = passwordService.findById(passwordId)
                    .orElseThrow(() -> new RuntimeException("Senha nao encontrada"));

            if (!entity.getUser().getId().equals(userId)) {
                throw new SecurityException("Tentativa de acessar senha de outro usuario!");
            }
            // 1) Decifra metadados (refs + mapa)
            PasswordReferenceClearData dto = passwordService.decrypt(entity, umk);
            log.info("Metadados decifrados | Nickname: {}", dto.getNickname());

            // 2) Extrai o ID lógico (armazenado como "Org1:<id>")
            String id = dto.getBlockchainReference1().split(":")[1];

            // 3) Recupera fragmentos reais das blockchains
            String[] fragments = blockchainService.retrieveFragments(id);
            log.debug("Fragmentos recuperados | Tamanhos: [{} , {}]", fragments[0].length(), fragments[1].length());

            return dto;

        } catch (Exception e) {
            log.error("Erro ao recuperar metadados (ID {}): {}", passwordId, e.getMessage());
            throw new RuntimeException("Erro ao recuperar senha: " + e.getMessage(), e);
        } finally {
            timer.stopAndLog(log);
        }
    }

    /**
     * Recupera a senha original (decifragem completa).
     */
    public String retrievePasswordPlain(Long passwordId) {
        sessionManager.requireActiveSession();
        SessionContext session = sessionManager.getSession();

        Long userId = session.getUserId();
        byte[] umk = session.getUmk();

        LogTimer timer = LogTimer.start("Recuperacao Completa da Senha | ID: " + passwordId);

        try {
            PasswordReference entity = passwordService.findById(passwordId)
                    .orElseThrow(() -> new RuntimeException("Senha nao encontrada"));

            if (!entity.getUser().getId().equals(userId)) {
                throw new SecurityException("Tentativa de acessar senha de outro usuario!");
            }

            PasswordReferenceClearData dto = passwordService.decrypt(entity, umk);

            String id = dto.getBlockchainReference1().split(":")[1];
            String[] fragments = blockchainService.retrieveFragments(id);
            String fragment1 = fragments[0];
            String fragment2 = fragments[1];

            List<ShuffleMap> mapped
                    = mapper.readValue(dto.getFragmentationReference(),
                            mapper.getTypeFactory().constructCollectionType(List.class, ShuffleMap.class));
            ArrayList<ShuffleMap> cutPoints = new ArrayList<>(mapped);

            String reconstructedCipher = fragmentationService.joinPassword(fragment1 + fragment2, cutPoints);
            String plainPassword = jsonEncService.decryptToDto(reconstructedCipher, umk, null, String.class);

            log.info("Senha recuperada com sucesso | ID: {}", passwordId);
            log.debug("Senha Original..: {}", plainPassword);

            return plainPassword;

        } catch (Exception e) {
            log.error("Erro ao recuperar senha em claro (ID {}): {}", passwordId, e.getMessage());
            throw new RuntimeException("Erro ao recuperar senha em claro: " + e.getMessage(), e);
        } finally {
            timer.stopAndLog(log);
        }
    }

}
