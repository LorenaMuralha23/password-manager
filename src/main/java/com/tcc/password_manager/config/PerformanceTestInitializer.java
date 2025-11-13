package com.tcc.password_manager.config;

import com.tcc.password_manager.controller.PasswordManagerController;
import com.tcc.password_manager.dto.PasswordReferenceClearData;
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
 * Classe exclusiva para testes de desempenho.
 * 
 * Objetivo:
 * - Executar ciclos automáticos de cadastro, login, armazenamento e recuperação
 * - Medir tempos médios de T_enc, T_frag, T_store e T_rec
 * - Gerar logs detalhados para análise de desempenho
 * 
 * Cenários:
 *   A - 1 credencial (10 execuções)
 *   B - 10 credenciais (10 execuções)
 *   C - 50 credenciais (10 execuções)
 */
//@Configuration
public class PerformanceTestInitializer {

    private static final Logger log = LoggerFactory.getLogger(PerformanceTestInitializer.class);

    // Ajuste aqui o cenário desejado (A, B ou C)
    private static final int NUM_CREDENCIAIS = 50;   // 1 → cenário A | 10 → B | 50 → C
    private static final int NUM_EXECUCOES = 10;    // repetições do teste

    @Bean
    CommandLineRunner runPerformanceTests(PasswordManagerController controller) {
        return args -> {

            log.info(System.lineSeparator());
            log.info("==============================================");
            log.info("INICIALIZACAO DOS TESTES DE DESEMPENHO");
            log.info("==============================================");
            log.info(System.lineSeparator());

            for (int exec = 1; exec <= NUM_EXECUCOES; exec++) {
                log.info(System.lineSeparator());
                log.info("========== EXECUCAO {} / {} ==========", exec, NUM_EXECUCOES);
                log.info(System.lineSeparator());

                try {
                    // === CADASTRO DE USUÁRIO ===
                    LogTimer timerRegister = LogTimer.start("Cadastro de Usuario");
                    String senhaMestre = "senha123";
                    String mfaSecret = "mfa1";
                    AppUser user = controller.registerUser(senhaMestre, mfaSecret);
                    timerRegister.stopAndLog(log);

                    // === LOGIN ===
                    LogTimer timerLogin = LogTimer.start("Login de Usuario");
                    controller.login(user.getId(), senhaMestre);
                    timerLogin.stopAndLog(log);

                    // === CADASTRO DE CREDENCIAIS ===
                    LogTimer timerAdd = LogTimer.start("Cadastro de Credenciais");
                    for (int i = 1; i <= NUM_CREDENCIAIS; i++) {
                        String nickname = "cred_" + exec + "_" + i;
                        String password = "senhaTeste_" + exec + "_" + i;
                        PasswordReference ref = controller.addPassword(nickname, password);
                        log.info("-> Credencial adicionada: {}", nickname);
                    }
                    timerAdd.stopAndLog(log);

                    // === RECUPERAÇÃO DE UMA CREDENCIAL ===
                    LogTimer timerRetrieve = LogTimer.start("Recuperacao Completa da Senha");
                    List<PasswordReference> all = controller.listPasswords();
                    if (!all.isEmpty()) {
                        PasswordReference first = all.get(0);
                        String plain = controller.retrievePasswordPlain(first.getId());
                        log.info("Senha recuperada com sucesso | ID: {} | Valor: {}", first.getId(), plain);
                    }
                    timerRetrieve.stopAndLog(log);

                    // === LOGOUT ===
                    LogTimer timerLogout = LogTimer.start("Logout do Usuario");
                    controller.logout();
                    timerLogout.stopAndLog(log);

                } catch (Exception e) {
                    log.error("Falha durante a execucao {}: {}", exec, e.getMessage(), e);
                }

                log.info(System.lineSeparator());
                log.info("========== FIM DA EXECUCAO {} ==========", exec);
                log.info(System.lineSeparator());
            }

            log.info("==============================================");
            log.info("TESTES DE DESEMPENHO FINALIZADOS");
            log.info("Cenario: {} credenciais * {} execucoes", NUM_CREDENCIAIS, NUM_EXECUCOES);
            log.info("Logs disponiveis para analise em password-manager.log");
            log.info("==============================================");
            log.info(System.lineSeparator());
        };
    }
}