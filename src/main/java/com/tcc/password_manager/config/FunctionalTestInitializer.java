package com.tcc.password_manager.config;

import com.tcc.password_manager.controller.PasswordManagerController;
import com.tcc.password_manager.model.AppUser;
import com.tcc.password_manager.model.PasswordReference;
import com.tcc.password_manager.util.LogTimer;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Classe de automação seletiva dos testes funcionais (CT01 e CT02).
 *
 * Objetivo: Permitir a execução controlada e independente de cada caso de teste
 * funcional. O teste a ser executado é definido pela constante TEST_CASE.
 *
 * Casos disponíveis: CT01 - Cadastro e armazenamento de credencial CT02 -
 * Criação e recuperação de credencial
 *
 * Configurações: - NUM_EXECUCOES: número de iterações consecutivas - TEST_CASE:
 * define qual caso será executado
 *
 * Observação: - A primeira iteração é considerada warm-up (será desconsiderada
 * na análise) - Os resultados são registrados no log padrão para posterior
 * análise
 */
@Configuration
public class FunctionalTestInitializer {

    private static final Logger log = LoggerFactory.getLogger(FunctionalTestInitializer.class);

    // Escolha qual caso de teste executar (CT01 ou CT02)
    private static final String TEST_CASE = "CT02";  // altere aqui o caso desejado
    private static final int NUM_EXECUCOES = 10;     // número de execuções consecutivas
    private static final String SENHA_MESTRE = "Senha-12345";

    @Bean
    CommandLineRunner runFunctionalTest(PasswordManagerController controller) {
        return args -> {
            log.info("\n===========================================================");
            log.info("INICIO DOS TESTES FUNCIONAIS AUTOMATIZADOS");
            log.info("===========================================================\n");
            log.info("Caso de teste selecionado: {}", TEST_CASE);

            AppUser user = null;

            switch (TEST_CASE.toUpperCase()) {
                case "CT01" -> {
                    // === CT01: cria e autentica novo usuário ===
                    log.info("Preparando ambiente para CT01 (novo usuario)...");
                    user = controller.registerUser(SENHA_MESTRE, "");
                    controller.login(user.getId(), SENHA_MESTRE);
                    executarCT01(controller);
                }

                case "CT02" -> {
                    // === CT02: utiliza usuário e credenciais já existentes ===
                    log.info("Preparando ambiente para CT02 (usuario existente ID=1)...");
                    long existingUserId = 1L;
                    try {
                        controller.login(existingUserId, SENHA_MESTRE);
                        log.info("Login realizado com sucesso para o usuario existente ID={}", existingUserId);
                    } catch (Exception e) {
                        log.error("Falha ao autenticar o usuario existente (ID=1). Certifique-se de executar o CT01 antes.");
                        return;
                    }
                    executarCT02(controller);
                }

                default -> {
                    log.error("Caso de teste invalido: {} (use CT01 ou CT02)", TEST_CASE);
                    return;
                }
            }

            controller.logout();

            log.info("\n===========================================================");
            log.info("TESTE {} FINALIZADO", TEST_CASE);
            log.info("Total de execucoes: {}", NUM_EXECUCOES);
            log.info("A primeira execucao deve ser desconsiderada como warm-up.");
            log.info("Logs disponiveis em: password-manager.log");
            log.info("===========================================================\n");
        };
    }

    // ===========================================================
    // CT01 — Cadastro e armazenamento de credenciais
    // ===========================================================
    private void executarCT01(PasswordManagerController controller) {
        log.info("\n-----------------------------------------------------------");
        log.info("CT01 - Cadastro e Armazenamento de Credenciais");
        log.info("-----------------------------------------------------------");

        for (int i = 1; i <= NUM_EXECUCOES; i++) {
            String nickname = "Facebook_CT01_" + i;
            String password = "Senha" + i + "-12345";

            LogTimer timer = LogTimer.start("CT01_Execucao_" + i);

            try {
                controller.addPassword(nickname, password);
                timer.stopAndLog(log);
                log.info("Execucao {} concluida com sucesso.", i);
            } catch (Exception e) {
                log.error("Falha na execucao {} do CT01: {}", i, e.getMessage(), e);
            }
        }
    }

    // ===========================================================
    // CT02 — Recuperação e decifragem de credenciais existentes
    // ===========================================================
    private void executarCT02(PasswordManagerController controller) {
        log.info("\n-----------------------------------------------------------");
        log.info("CT02 - Recuperacao e Decifragem de Credenciais Existentes");
        log.info("-----------------------------------------------------------");

        try {
            // Obtém todas as credenciais associadas ao usuário padrão (ID = 1)
            List<PasswordReference> referencias = controller.listPasswords();

            if (referencias == null || referencias.isEmpty()) {
                log.warn("Nenhuma credencial encontrada. Execute o CT01 antes do CT02.");
                return;
            }

            for (int i = 1; i <= NUM_EXECUCOES; i++) {
                // Seleciona a credencial da vez (revezando entre as existentes)
                PasswordReference ref = referencias.get((i - 1) % referencias.size());
                LogTimer timer = LogTimer.start("CT02_Execucao_" + i);

                try {
                    // Recupera e decifra a senha associada à referência
                    String senhaDecifrada = controller.retrievePasswordPlain(ref.getId());
                    timer.stopAndLog(log);

                    log.info("Senha recuperada com sucesso | ID: {} | Valor: {}", ref.getId(), senhaDecifrada);
                    log.info("Execucao {} concluida com sucesso.", i);
                } catch (Exception e) {
                    log.error("Falha na execucao {} do CT02: {}", i, e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            log.error("Erro ao preparar o CT02: {}", e.getMessage(), e);
        }
    }

}
