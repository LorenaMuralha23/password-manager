package com.tcc.password_manager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.password_manager.classes.ShuffleMap;
import com.tcc.password_manager.crypto.JsonEncryptionService;
import com.tcc.password_manager.dto.PasswordReferenceClearData;
import com.tcc.password_manager.dto.AppUserClearData;
import com.tcc.password_manager.dto.FragmentedData;
import com.tcc.password_manager.model.AppUser;
import com.tcc.password_manager.model.PasswordReference;
import com.tcc.password_manager.service.AppUserService;
import com.tcc.password_manager.service.BlockchainService;
import com.tcc.password_manager.service.FragmentationService;
import com.tcc.password_manager.service.PasswordReferenceService;
import com.tcc.password_manager.session.SessionManager;
import com.tcc.password_manager.session.SessionContext;
import java.util.ArrayList;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Controller principal da aplicação. Responsável por orquestrar o fluxo
 * completo: - Registro e autenticação - Criptografia local - Fragmentação -
 * Armazenamento distribuído em blockchains - Persistência de metadados cifrados
 */
@Component
public class PasswordManagerController {

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

    // ==================== Usuário ====================
    /**
     * Cadastro de novo usuário.
     */
    public AppUser registerUser(String senhaMestre, String mfaSecret) {
        return appUserService.registerUser(senhaMestre, mfaSecret);
    }

    /**
     * Login: valida senha mestre, recupera UMK e popula sessão.
     */
    public void login(Long userId, String senhaMestre) {
        sessionManager.login(userId, senhaMestre);
    }

    /**
     * Logout: encerra a sessão atual.
     */
    public void logout() {
        sessionManager.logout();
    }

    // ==================== Senhas ====================
    /**
     * Adiciona uma nova senha para o usuário logado. Fluxo: 1. Cifra com a UMK
     * 2. Fragmenta o ciphertext 3. Grava fragmentos nas blockchains 4. Persiste
     * referências cifradas no banco
     */
    public PasswordReference addPassword(String nickname, String plainPassword) {
        sessionManager.requireActiveSession();
        SessionContext session = sessionManager.getSession();

        Long userId = session.getUserId();
        byte[] umk = session.getUmk();

        // busca o usuário dono
        AppUser user = appUserService.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        try {
            // 1. Cifra a senha com a UMK
            String encryptedPassword = jsonEncService.encryptDto(
                    plainPassword,
                    umk,
                    null,
                    String.class
            );

            // 2. Fragmenta o ciphertext
            FragmentedData fragmented = fragmentationService.fragmentPassword(encryptedPassword);

            // 3. Gera um ID lógico único
            String fragmentId = UUID.randomUUID().toString();

            // 4. Grava fragmentos nas blockchains (Org1 e Org2)
            blockchainService.storeFragments(fragmentId, fragmented.getFragment1(), fragmented.getFragment2());

            // 5. Serializa o mapa de cortes para reconstrução futura
            String fragRef = mapper.writeValueAsString(fragmented.getCutPointsMap());

            // 6. Monta DTO com referências reais
            PasswordReferenceClearData dto = new PasswordReferenceClearData();
            dto.setNickname(nickname);
            dto.setBlockchainReference1("Org1:" + fragmentId);
            dto.setBlockchainReference2("Org2:" + fragmentId);
            dto.setFragmentationReference(fragRef);

            // 7. Persiste no banco (DTO cifrado com a UMK)
            return passwordService.save(dto, user, umk);

        } catch (Exception e) {
            throw new RuntimeException("Erro ao adicionar senha: " + e.getMessage(), e);
        }
//        // TODO: no futuro aqui entrará a lógica de:
//        // 1. criptografar a senha com a UMK
//        // 2. fragmentar o ciphertext
//        // 3. gravar cada fragmento nas blockchains
//        // 4. gerar references reais
//        // valores mock por enquanto
//        String ref1 = "ledger1-" + UUID.randomUUID();
//        String ref2 = "ledger2-" + UUID.randomUUID();
//        String fragRef = "frag-" + UUID.randomUUID();
//
//        // monta DTO
//        PasswordReferenceClearData dto = new PasswordReferenceClearData();
//        dto.setNickname(nickname);
//        dto.setBlockchainReference1(ref1);
//        dto.setBlockchainReference2(ref2);
//        dto.setFragmentationReference(fragRef);
//
//        // service cifra o DTO com a UMK e persiste
//        return passwordService.save(dto, user, umk);
    }

    /**
     * Lista todas as senhas do usuário logado (somente metadados).
     */
    public List<PasswordReference> listPasswords() {
        sessionManager.requireActiveSession();
        Long userId = sessionManager.getSession().getUserId();
        return passwordService.findByUserId(userId);
    }

    /**
     * Recupera uma senha do usuário: 1. Busca metadados cifrados 2. Recupera
     * fragmentos nas blockchains 3. Junta fragmentos e reconstrói o ciphertext
     * 4. Decifra com a UMK
     */
    public PasswordReferenceClearData retrievePassword(Long passwordId) {
        sessionManager.requireActiveSession();
        SessionContext session = sessionManager.getSession();

        Long userId = session.getUserId();
        byte[] umk = session.getUmk();

        PasswordReference entity = passwordService.findById(passwordId)
                .orElseThrow(() -> new RuntimeException("Senha não encontrada"));

        if (!entity.getUser().getId().equals(userId)) {
            throw new SecurityException("Tentativa de acessar senha de outro usuário!");
        }

        try {
            // 1) Decifra metadados (refs + mapa)
            PasswordReferenceClearData dto = passwordService.decrypt(entity, umk);

            // 2) Extrai o ID lógico (armazenado como "Org1:<id>")
            String id = dto.getBlockchainReference1().split(":")[1];

            // 3) Recupera fragmentos reais das blockchains
            String[] fragments = blockchainService.retrieveFragments(id);
            String fragment1 = fragments[0];
            String fragment2 = fragments[1];

            // 4) Desserializa o mapa de cortes e converte para ArrayList
            List<ShuffleMap> mapped
                    = mapper.readValue(dto.getFragmentationReference(),
                            mapper.getTypeFactory().constructCollectionType(List.class, ShuffleMap.class));
            ArrayList<ShuffleMap> cutPoints = new ArrayList<>(mapped);

            // 5) Recompõe o ciphertext (opcional nesta rotina)
            String reconstructedCipher = fragmentationService.joinPassword(fragment1 + fragment2, cutPoints);

            // 6) (Opcional) Decifrar aqui só se você quiser validar internamente:
            // String plainPassword = jsonEncService.decryptToDto(reconstructedCipher, umk, null, String.class);
            // → Este método, por contrato, retorna apenas metadados (DTO)
            return dto;

        } catch (Exception e) {
            throw new RuntimeException("Erro ao recuperar senha: " + e.getMessage(), e);
        }

//        // decifra para DTO
//        return passwordService.decrypt(entity, umk);
    }

    public String retrievePasswordPlain(Long passwordId) {
        sessionManager.requireActiveSession();
        SessionContext session = sessionManager.getSession();

        Long userId = session.getUserId();
        byte[] umk = session.getUmk();

        PasswordReference entity = passwordService.findById(passwordId)
                .orElseThrow(() -> new RuntimeException("Senha não encontrada"));

        if (!entity.getUser().getId().equals(userId)) {
            throw new SecurityException("Tentativa de acessar senha de outro usuário!");
        }

        try {
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

            // 🔐 aqui o nome correto é decryptToDto
            String plainPassword = jsonEncService.decryptToDto(reconstructedCipher, umk, null, String.class);
            return plainPassword;

        } catch (Exception e) {
            throw new RuntimeException("Erro ao recuperar senha em claro: " + e.getMessage(), e);
        }
    }

}
