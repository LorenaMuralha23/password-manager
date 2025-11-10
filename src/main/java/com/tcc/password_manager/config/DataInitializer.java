package com.tcc.password_manager.config;

import com.fasterxml.jackson.databind.ObjectMapper;
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
 * Classe de inicialização usada para validar o fluxo completo da arquitetura: -
 * Registro e login do usuário - Criptografia local (UMK) - Fragmentação e
 * distribuição blockchain - Persistência e recuperação de dados
 */
//@Configuration
// Descomentar a linha acima fará com que a classe seja executada junto com o Spring
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    CommandLineRunner initDatabase(PasswordManagerController controller) {

        return args -> {
            ObjectMapper mapper = new ObjectMapper();

            log.info(System.lineSeparator());
            log.info("==============================================");
            log.info("INICIALIZACAO DO TESTE DE FLUXO");
            log.info("==============================================");
            log.info(System.lineSeparator());
            
            // === CADASTRO DE USUÁRIO ===
            LogTimer timerRegister = LogTimer.start("Cadastro de Usuario");
            String senhaMestre = "senha123";
            String mfaSecret = "mfa1";
            AppUser user = controller.registerUser(senhaMestre, mfaSecret);

            log.info("User ID........: {}", user.getId());
            log.debug("EncryptedData..: {}", user.getEncryptedData());
            log.debug("UMK Wrapped....: {}", user.getUmkWrapped());
            log.debug("UMK Hash.......: {}", user.getUmkHash());
            timerRegister.stopAndLog(log);

            // === LOGIN ===
            LogTimer timerLogin = LogTimer.start("Login de Usuario");
            try {
                controller.login(user.getId(), senhaMestre);
                log.info("Login bem-sucedido | User ID: {}", user.getId());
            } catch (Exception e) {
                log.error("Falha no login: {}", e.getMessage());
                return;
            } finally {
                timerLogin.stopAndLog(log);
            }

            // === ADICIONAR SENHAS ===
            LogTimer timerAdd = LogTimer.start("Cadastro de Credenciais");
            try {
                log.info("Iniciando fragmentacao e armazenamento em blockchain...");

                PasswordReference github = controller.addPassword("GitHub", "senhaGitHub@123");
                PasswordReference gmail = controller.addPassword("Gmail", "senhaGmail@456");

                log.info("Credenciais registradas com sucesso:");
                log.info("→ GitHub: ID {}", github.getId());
                log.info("→ Gmail:  ID {}", gmail.getId());

                log.debug("GitHub EncryptedData: {}", github.getEncryptedData());
                log.debug("Gmail  EncryptedData: {}", gmail.getEncryptedData());

            } catch (Exception e) {
                log.error("Falha ao adicionar senhas: {}", e.getMessage());
            } finally {
                timerAdd.stopAndLog(log);
            }

            // === LISTAR SENHAS ===
            LogTimer timerList = LogTimer.start("Listagem de Credenciais");
            try {
                List<PasswordReference> all = controller.listPasswords();
                if (all.isEmpty()) {
                    log.warn("Nenhuma credencial encontrada no banco local.");
                } else {
                    log.info("Total de credenciais encontradas: {}", all.size());
                    for (PasswordReference ref : all) {
                        log.debug("ID: {} | EncryptedData: {}", ref.getId(), ref.getEncryptedData());
                    }
                }
            } catch (Exception e) {
                log.error("Falha ao listar senhas: {}", e.getMessage());
            } finally {
                timerList.stopAndLog(log);
            }

            // === RECUPERAR METADADOS ===
            LogTimer timerMeta = LogTimer.start("Recuperacao de Metadados");
            try {
                List<PasswordReference> all = controller.listPasswords();
                if (!all.isEmpty()) {
                    Long refId = all.get(0).getId();
                    PasswordReferenceClearData clear = controller.retrievePassword(refId);

                    log.info("Metadados decifrados | ID: {}", refId);
                    log.debug("Nickname........: {}", clear.getNickname());
                    log.debug("BlockchainRef1..: {}", clear.getBlockchainReference1());
                    log.debug("BlockchainRef2..: {}", clear.getBlockchainReference2());
                    log.debug("FragmentMap.....: {}", clear.getFragmentationReference());
                } else {
                    log.warn("Nenhuma credencial para recuperar metadados.");
                }
            } catch (Exception e) {
                log.error("Falha ao recuperar metadados: {}", e.getMessage());
            } finally {
                timerMeta.stopAndLog(log);
            }

            // === RECUPERAR SENHA ORIGINAL ===
            LogTimer timerRetrieve = LogTimer.start("Recuperacao Completa da Senha");
            try {
                List<PasswordReference> all = controller.listPasswords();
                if (!all.isEmpty()) {
                    Long refId = all.get(0).getId();
                    String plainPassword = controller.retrievePasswordPlain(refId);

                    log.info("Senha recuperada com sucesso | ID: {}", refId);
                    log.debug("Senha Original..: {}", plainPassword);
                } else {
                    log.warn("Nenhuma senha para recuperar.");
                }
            } catch (Exception e) {
                log.error("Falha ao recuperar senha em claro: {}", e.getMessage());
            } finally {
                timerRetrieve.stopAndLog(log);
            }

            // === LOGOUT ===
            LogTimer timerLogout = LogTimer.start("Logout do Usuario");
            try {
                controller.logout();
                log.info("Logout realizado com sucesso.");
            } catch (Exception e) {
                log.error("Falha ao realizar logout: {}", e.getMessage());
            } finally {
                timerLogout.stopAndLog(log);
            }

            log.info(System.lineSeparator());
            log.info("==============================================");
            log.info("TESTE FINALIZADO COM SUCESSO");
            log.info("Fluxo validado: cadastro -> login -> addPassword -> listPasswords -> retrievePassword -> retrievePasswordPlain -> logout");
            log.info("==============================================");
            log.info(System.lineSeparator());
        };
    }
}
