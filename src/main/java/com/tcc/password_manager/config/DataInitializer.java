package com.tcc.password_manager.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.password_manager.controller.PasswordManagerController;
import com.tcc.password_manager.dto.AppUserClearData;
import com.tcc.password_manager.dto.PasswordReferenceClearData;
import com.tcc.password_manager.model.AppUser;
import com.tcc.password_manager.model.PasswordReference;
import com.tcc.password_manager.service.AppUserService;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initDatabase(PasswordManagerController controller) {

        return args -> {
            ObjectMapper mapper = new ObjectMapper();

            // === Cadastro de usuário ===
            String senhaMestre = "senha123";
            String mfaSecret = "mfa1";

            AppUser user = controller.registerUser(senhaMestre, mfaSecret);
            System.out.println("=== Cadastro ===");
            System.out.println("User ID: " + user.getId());
            System.out.println("EncryptedData: " + user.getEncryptedData());
            System.out.println("umkWrapped: " + user.getUmkWrapped());
            System.out.println("umkHash: " + user.getUmkHash());

            // === Login ===
            try {
                controller.login(user.getId(), senhaMestre);
                System.out.println("\n=== Login bem-sucedido ===");
            } catch (Exception e) {
                System.err.println("Falha no login: " + e.getMessage());
                return;
            }

            // === Adicionar senha (mock blockchain refs) ===
            try {
                PasswordReference saved = controller.addPassword("GitHub");
                System.out.println("\n=== Senha cadastrada ===");
                System.out.println("PasswordReference ID: " + saved.getId());
                System.out.println("EncryptedData: " + saved.getEncryptedData());
            } catch (Exception e) {
                System.err.println("Falha ao adicionar senha: " + e.getMessage());
            }

            // === Listar senhas ===
            try {
                List<PasswordReference> all = controller.listPasswords();
                System.out.println("\n=== Lista de senhas ===");
                all.forEach(ref
                        -> System.out.println("ID: " + ref.getId() + " | Data (encrypted): " + ref.getEncryptedData()));
            } catch (Exception e) {
                System.err.println("Falha ao listar senhas: " + e.getMessage());
            }

            // === Recuperar senha (metadados decifrados) ===
            try {
                List<PasswordReference> all = controller.listPasswords();
                if (!all.isEmpty()) {
                    Long refId = all.get(0).getId();
                    PasswordReferenceClearData clear = controller.retrievePassword(refId);
                    System.out.println("\n=== Recuperação de senha ===");
                    System.out.println("DTO decifrado: " + mapper.writeValueAsString(clear));
                }
            } catch (Exception e) {
                System.err.println("Falha ao recuperar senha: " + e.getMessage());
            }

            // === Logout ===
            controller.logout();
            System.out.println("\n=== Logout realizado ===");

            System.out.println("\nTeste finalizado: cadastro + login + addPassword + listPasswords + retrievePassword + logout.");
        };
    }
}
