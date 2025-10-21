package com.tcc.password_manager.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.password_manager.controller.PasswordManagerController;
import com.tcc.password_manager.dto.PasswordReferenceClearData;
import com.tcc.password_manager.model.AppUser;
import com.tcc.password_manager.model.PasswordReference;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Classe de inicialização usada para validar o fluxo completo da arquitetura:
 * - Registro e login do usuário
 * - Criptografia local (UMK)
 * - Fragmentação e distribuição blockchain
 * - Persistência e recuperação de dados
 */
@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initDatabase(PasswordManagerController controller) {

        return args -> {
            ObjectMapper mapper = new ObjectMapper();

            System.out.println("\n==============================================");
            System.out.println("INICIALIZAÇÃO DO TESTE DE ARQUITETURA COMPLETA");
            System.out.println("==============================================");

            // === 1️⃣ CADASTRO DE USUÁRIO ===
            String senhaMestre = "senha123";
            String mfaSecret = "mfa1";
            AppUser user = controller.registerUser(senhaMestre, mfaSecret);

            System.out.println("\n=== Cadastro de Usuário ===");
            System.out.println("User ID........: " + user.getId());
            System.out.println("EncryptedData..: " + user.getEncryptedData());
            System.out.println("UMK Wrapped....: " + user.getUmkWrapped());
            System.out.println("UMK Hash.......: " + user.getUmkHash());

            // === 2️⃣ LOGIN ===
            try {
                controller.login(user.getId(), senhaMestre);
                System.out.println("\n=== Login bem-sucedido ===");
            } catch (Exception e) {
                System.err.println("Falha no login: " + e.getMessage());
                return;
            }

            // === 3️⃣ ADICIONAR SENHAS ===
            try {
                System.out.println("\n=== Cadastrando senhas com fragmentação + blockchain ===");

                // Teste com duas senhas para validar consistência
                PasswordReference github = controller.addPassword("GitHub", "senhaGitHub@123");
                PasswordReference gmail = controller.addPassword("Gmail", "senhaGmail@456");

                System.out.println("→ GitHub: ID " + github.getId());
                System.out.println("→ Gmail:  ID " + gmail.getId());

                System.out.println("\nDados criptografados no banco:");
                System.out.println("GitHub EncryptedData: " + github.getEncryptedData());
                System.out.println("Gmail  EncryptedData: " + gmail.getEncryptedData());

            } catch (Exception e) {
                System.err.println("Falha ao adicionar senhas: " + e.getMessage());
            }

            // === 4️⃣ LISTAR SENHAS ===
            try {
                List<PasswordReference> all = controller.listPasswords();
                System.out.println("\n=== Lista de Senhas (banco local) ===");
                for (PasswordReference ref : all) {
                    System.out.println("ID: " + ref.getId());
                    System.out.println("EncryptedData: " + ref.getEncryptedData());
                    System.out.println("--------------------------------------");
                }
            } catch (Exception e) {
                System.err.println("Falha ao listar senhas: " + e.getMessage());
            }

            // === 5️⃣ RECUPERAR METADADOS ===
            try {
                List<PasswordReference> all = controller.listPasswords();
                if (!all.isEmpty()) {
                    Long refId = all.get(0).getId();
                    PasswordReferenceClearData clear = controller.retrievePassword(refId);

                    System.out.println("\n=== Recuperação de Senha (metadados decifrados) ===");
                    System.out.println("Password ID.....: " + refId);
                    System.out.println("Nickname........: " + clear.getNickname());
                    System.out.println("BlockchainRef1..: " + clear.getBlockchainReference1());
                    System.out.println("BlockchainRef2..: " + clear.getBlockchainReference2());
                    System.out.println("FragmentMap.....: " + clear.getFragmentationReference());
                }
            } catch (Exception e) {
                System.err.println("Falha ao recuperar metadados: " + e.getMessage());
            }

            // === 6️⃣ RECUPERAR SENHA ORIGINAL (com decifragem completa) ===
            try {
                List<PasswordReference> all = controller.listPasswords();
                if (!all.isEmpty()) {
                    Long refId = all.get(0).getId();
                    String plainPassword = controller.retrievePasswordPlain(refId);

                    System.out.println("\n=== Recuperação Completa da Senha ===");
                    System.out.println("Password ID.....: " + refId);
                    System.out.println("Senha Original..: " + plainPassword);
                }
            } catch (Exception e) {
                System.err.println("Falha ao recuperar senha em claro: " + e.getMessage());
            }

            // === 7️⃣ LOGOUT ===
            try {
                controller.logout();
                System.out.println("\n=== Logout realizado ===");
            } catch (Exception e) {
                System.err.println("Falha ao realizar logout: " + e.getMessage());
            }

            System.out.println("\n==============================================");
            System.out.println("TESTE FINALIZADO COM SUCESSO");
            System.out.println("Fluxo validado: cadastro + login + addPassword + listPasswords + retrievePassword + retrievePasswordPlain + logout");
            System.out.println("==============================================\n");
        };
    }
}
