package com.tcc.password_manager.controller;

import com.tcc.password_manager.dto.PasswordReferenceClearData;
import com.tcc.password_manager.dto.AppUserClearData;
import com.tcc.password_manager.model.AppUser;
import com.tcc.password_manager.model.PasswordReference;
import com.tcc.password_manager.service.AppUserService;
import com.tcc.password_manager.service.PasswordReferenceService;
import com.tcc.password_manager.session.SessionManager;
import com.tcc.password_manager.session.SessionContext;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Controller principal da aplicação. Orquestra o fluxo de cadastro,
 * login/logout, e gerenciamento de senhas. Garante que a sessão esteja válida
 * antes de operações protegidas.
 */
@Component
public class PasswordManagerController {

    private final AppUserService appUserService;
    private final PasswordReferenceService passwordService;
    private final SessionManager sessionManager;

    public PasswordManagerController(AppUserService appUserService,
            PasswordReferenceService passwordService,
            SessionManager sessionManager) {
        this.appUserService = appUserService;
        this.passwordService = passwordService;
        this.sessionManager = sessionManager;
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
     * Adiciona uma nova senha para o usuário logado. Neste momento, blockchain
     * e fragmentação são placeholders.
     *
     * @param nickname rótulo da senha (ex.: "GitHub")
     * @return entidade persistida no banco
     */
    public PasswordReference addPassword(String nickname) {
        sessionManager.requireActiveSession();
        SessionContext session = sessionManager.getSession();

        Long userId = session.getUserId();
        byte[] umk = session.getUmk();

        // busca o usuário dono
        AppUser user = appUserService.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        // TODO: no futuro aqui entrará a lógica de:
        // 1. criptografar a senha com a UMK
        // 2. fragmentar o ciphertext
        // 3. gravar cada fragmento nas blockchains
        // 4. gerar references reais
        // valores mock por enquanto
        String ref1 = "ledger1-" + UUID.randomUUID();
        String ref2 = "ledger2-" + UUID.randomUUID();
        String fragRef = "frag-" + UUID.randomUUID();

        // monta DTO
        PasswordReferenceClearData dto = new PasswordReferenceClearData();
        dto.setNickname(nickname);
        dto.setBlockchainReference1(ref1);
        dto.setBlockchainReference2(ref2);
        dto.setFragmentationReference(fragRef);

        // service cifra o DTO com a UMK e persiste
        return passwordService.save(dto, user, umk);
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
     * Recupera metadados de uma senha (sem retornar senha em claro, já que os
     * DTOs atuais não têm esse campo).
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

        // decifra para DTO
        return passwordService.decrypt(entity, umk);
    }
}
