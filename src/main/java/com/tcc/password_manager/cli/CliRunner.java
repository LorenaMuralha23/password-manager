package com.tcc.password_manager.cli;

import com.tcc.password_manager.controller.PasswordManagerController;
import com.tcc.password_manager.dto.PasswordReferenceClearData;
import com.tcc.password_manager.model.AppUser;
import com.tcc.password_manager.model.PasswordReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.Console;
import java.io.InputStreamReader;
import java.util.List;

/**
 * Interface CLI do Password Manager. Permite interação via terminal para
 * registro, autenticação e gerenciamento de credenciais.
 */
@Component
public class CliRunner {

    private static final Logger log = LoggerFactory.getLogger(CliRunner.class);
    private final PasswordManagerController controller;

    public CliRunner(PasswordManagerController controller) {
        this.controller = controller;
    }

    public void start() {
        printHeader();
        log.info("Inicializando CLI do Password Manager...");

        try (BufferedReader in = new BufferedReader(new InputStreamReader(System.in))) {
            boolean running = true;

            while (running) {
                printMenu();
                String op = in.readLine();

                if (op == null) {
                    break;
                }

                try {
                    switch (op.trim()) {
                        case "1" ->
                            handleRegisterUser(in);
                        case "2" ->
                            handleLogin(in);
                        case "3" ->
                            handleLogout();
                        case "4" ->
                            handleAddPassword(in);
                        case "5" ->
                            handleListPasswords();
                        case "6" ->
                            handleRetrievePassword(in);        // metadados
                        case "7" ->
                            handleRetrievePasswordPlain(in);   // senha em claro
                        case "8" -> {
                            System.out.println("Ate logo!");
                            log.info("Encerrando execucao do CLI pelo usuario.");
                            running = false;
                        }
                        default ->
                            System.out.println("Opcao invalida.\n");
                    }
                } catch (Exception e) {
                    log.error("Erro ao executar operacao '{}': {}", op, e.getMessage(), e);
                    System.out.println("Erro: " + (e.getMessage() == null ? e.toString() : e.getMessage()));
                }

                System.out.println();
            }

        } catch (Exception e) {
            log.error("Falha inesperada no CLI: {}", e.getMessage(), e);
            System.out.println("Erro fatal: " + e.getMessage());
        }

        log.info("CLI encerrada com seguranca.");
    }

    // ===== Handlers =====
    
    private void handleRegisterUser(BufferedReader in) throws Exception {
        System.out.println("\n== REGISTRAR USUARIO ==");
        char[] master = promptSecret("Senha mestre");
        String mfa = prompt(in, "MFA secret (TOTP) [ENTER p/ ignorar]");

        AppUser user = controller.registerUser(
                new String(master),
                (mfa == null || mfa.isBlank()) ? null : mfa
        );
        zero(master);

        System.out.println("Usuario registrado | ID: " + user.getId());
        log.info("Novo usuario registrado com ID {}", user.getId());
    }

    private void handleLogin(BufferedReader in) throws Exception {
        System.out.println("\n== LOGIN ==");
        Long userId = promptLong(in, "User ID (numerico)");
        char[] master = promptSecret("Senha mestre");

        controller.login(userId, new String(master));
        zero(master);

        System.out.println("Login realizado com sucesso.");
        log.info("Login bem-sucedido para o usuario ID {}", userId);
    }

    private void handleLogout() {
        System.out.println("\n== LOGOUT ==");
        controller.logout();
        System.out.println("Sessao encerrada.");
        log.info("Logout executado com sucesso.");
    }

    private void handleAddPassword(BufferedReader in) throws Exception {
        System.out.println("\n== ADICIONAR SENHA ==");
        String nickname = prompt(in, "Nickname (ex.: GitHub)");
        String plain = prompt(in, "Senha (ENTER para informar depois/colar)");

        PasswordReference saved = controller.addPassword(nickname, plain);
        System.out.println("Credencial registrada | ID: " + saved.getId());
        log.info("Nova credencial '{}' registrada (ID {}).", nickname, saved.getId());
    }

    private void handleListPasswords() {
        System.out.println("\n== LISTAR SENHAS ==");
        List<PasswordReference> refs = controller.listPasswords();
        
        if (refs == null || refs.isEmpty()) {
            System.out.println("(Nenhuma credencial encontrada)");
            return;
        }
        
        System.out.println("ID\tEncryptedData(bytes)");
        for (PasswordReference r : refs) {
            int n = (r.getEncryptedData() == null) ? 0 : r.getEncryptedData().length();
            System.out.println(r.getId() + "\t" + n);
        }
        
        log.info("Listagem concluida. Total de credenciais: {}", refs.size());
    }

    private void handleRetrievePassword(BufferedReader in) throws Exception {
        System.out.println("\n== RECUPERAR SENHA ==");
        Long id = promptLong(in, "ID da credencial");
        PasswordReferenceClearData dto = controller.retrievePassword(id);

        if (dto == null) {
            System.out.println("Nao encontrada.");
            return;
        }

        // Metadados decifrados
        System.out.println("NICKNAME..............: " + safe(dto.getNickname()));
        System.out.println("Blockchain Reference 1: " + safe(dto.getBlockchainReference1()));
        System.out.println("Blockchain Reference 2: " + safe(dto.getBlockchainReference2()));
        System.out.println("Fragmentation Map.....: " + summarize(safe(dto.getFragmentationReference()), 160));
        
        log.info("Metadados recuperados para credencial ID {}", id);
    }

    private void handleRetrievePasswordPlain(BufferedReader in) throws Exception {
        System.out.println("\n== RECUPERAR SENHA (TEXTO CLARO) ==");
        Long id = promptLong(in, "ID da credencial");
        String plain = controller.retrievePasswordPlain(id);

        if (plain == null) {
            System.out.println("Nao encontrada.");
            return;
        }
        
        System.out.println("Senha (texto claro): " + plain);
        log.info("Senha decifrada com sucesso para credencial ID {}", id);
    }

    // ===== Utils =====
    
    private static void printHeader() {
        System.out.println("");
        System.out.println("""
        ---------------------------------------------------------
        Password Manager CLI  -  AES-256-GCM - Dual-Blockchain
        ---------------------------------------------------------""");
    }

    private static void printMenu() {
        System.out.println("""
        Selecione uma operacao:
        [1] REGISTRAR USUARIO
        [2] LOG IN
        [3] LOG OUT
        [4] ADICIONAR SENHA
        [5] LISTAR SENHAS
        [6] RECUPERAR SENHA
        [7] RECUPERAR SENHA (TEXTO CLARO)
        [8] SAIR
        > """);
        System.out.print("");
    }

    private static String prompt(BufferedReader in, String label) throws Exception {
        System.out.print(label + ": ");
        return in.readLine();
    }

    private static Long promptLong(BufferedReader in, String label) throws Exception {
        while (true) {
            String s = prompt(in, label);
            try {
                return Long.parseLong(s.trim());
            } catch (Exception e) {
                System.out.println("Valor invalido. Tente novamente.");
            }
        }
    }

    private static char[] promptSecret(String label) {
        Console console = System.console();
        if (console != null) {
            return console.readPassword(label + ": ");
        }
        System.out.print(label + " (visivel por limitacao do ambiente): ");
        try {
            return new BufferedReader(new InputStreamReader(System.in)).readLine().toCharArray();
        } catch (Exception e) {
            return new char[0];
        }
    }

    private static void zero(char[] arr) {
        if (arr != null) {
            java.util.Arrays.fill(arr, '\0');
        }
    }

    private static String safe(String s) {
        return s == null ? "(null)" : s;
    }

    private static String summarize(String s, int max) {
        if (s == null) {
            return "(null)";
        }
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
