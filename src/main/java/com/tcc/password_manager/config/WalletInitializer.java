package com.tcc.password_manager.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.CertificateException;
import org.hyperledger.fabric.gateway.Identities;
import org.hyperledger.fabric.gateway.Identity;
import org.hyperledger.fabric.gateway.Wallet;
import org.hyperledger.fabric.gateway.Wallets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Inicializa o wallet local, garantindo que a identidade 'appUser' esteja
 * registrada. Executa automaticamente ao iniciar a aplicação.
 */
@Component
public class WalletInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(WalletInitializer.class);
    private static final String USERNAME = "appUser";
    private static final String MSP_ID = "Org1MSP";

    @Override
    public void run(String... args) {
        try {
            Path walletPath = Paths.get("src", "main", "resources", "wallet");
            Wallet wallet = Wallets.newFileSystemWallet(walletPath);

            log.info("Verificando wallet em: {}", walletPath.toAbsolutePath());

            if (wallet.get(USERNAME) != null) {
                log.info("Identidade '{}' ja esta registrada no wallet.", USERNAME);
                return;
            }

            log.info("Registrando nova identidade '{}' no wallet...", USERNAME);

            // Caminhos dos arquivos PEM
//            Path certPath = Paths.get("src", "main", "resources", "wallet", "appUser", "cert.pem");
//            Path keyPath = Paths.get("src", "main", "resources", "wallet", "appUser", "key.pem");
            Path certPath = walletPath.resolve(USERNAME).resolve("cert.pem");
            Path keyPath = walletPath.resolve(USERNAME).resolve("key.pem");

            if (!Files.exists(certPath) || !Files.exists(keyPath)) {
                log.error("Arquivos de identidade nao encontrados. Esperado em: {}, {}", certPath, keyPath);
                return;
            }

            // Carrega o certificado e a chave privada
            String certPem = Files.readString(certPath);
            String keyPem = Files.readString(keyPath);

            Identity identity = Identities.newX509Identity(
                    MSP_ID,
                    Identities.readX509Certificate(certPem),
                    Identities.readPrivateKey(keyPem)
            );

            wallet.put(USERNAME, identity);
            log.info("Identidade '{}' registrada com sucesso no wallet.", USERNAME);
        } catch (IOException | CertificateException e) {
            log.error("Erro ao inicializar o wallet Fabric: {}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("Falha inesperada ao registrar identidade '{}': {}", USERNAME, e.getMessage(), e);
        }
    }
}
