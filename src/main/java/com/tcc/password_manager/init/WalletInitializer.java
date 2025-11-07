package com.tcc.password_manager.init;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.CertificateException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hyperledger.fabric.gateway.Identities;
import org.hyperledger.fabric.gateway.Identity;
import org.hyperledger.fabric.gateway.Wallet;
import org.hyperledger.fabric.gateway.Wallets;

/**
 * Utilitário independente para registrar identidades das organizações Org1 e
 * Org2 no wallet.
 *
 * Pode ser executado diretamente antes de iniciar a aplicação principal.
 * Exemplo: java -cp target/password-manager.jar
 * com.tcc.password_manager.init.WalletInitializer
 */
public class WalletInitializer {

    private static final Logger log = Logger.getLogger(WalletInitializer.class.getName());

    public static void main(String[] args) {
        try {
            Path walletPath = Paths.get("wallet");
            Wallet wallet = Wallets.newFileSystemWallet(walletPath);

            log.info(() -> "Inicializando wallet em: " + walletPath.toAbsolutePath());

            registerUser(wallet, "Org1", "appUserOrg1",
                    "src/main/resources/crypto/msp-org1/signcerts/cert.pem",
                    "src/main/resources/crypto/msp-org1/keystore/priv_sk",
                    "Org1MSP");

            registerUser(wallet, "Org2", "appUserOrg2",
                    "src/main/resources/crypto/msp-org2/signcerts/cert.pem",
                    "src/main/resources/crypto/msp-org2/keystore/priv_sk",
                    "Org2MSP");

            log.info("Registro de identidades concluido com sucesso.");
        } catch (IOException e) {
            log.log(Level.SEVERE, "Erro de I/O ao inicializar o wallet: {0}", e.getMessage());
        } catch (Exception e) {
            log.log(Level.SEVERE, "Falha inesperada: {0}", e.getMessage());
        }
    }

    private static void registerUser(Wallet wallet, String orgName, String userName,
            String certPath, String keyPath, String mspId) {

        try {
            if (wallet.get(userName) != null) {
                log.info(() -> "Identidade '" + userName + "' ja registrada para " + orgName);
                return;
            }

            Path certFile = Paths.get(certPath);
            Path keyFile = Paths.get(keyPath);

            if (!Files.exists(certFile) || !Files.exists(keyFile)) {
                log.warning(() -> "Certificado ou chave nao encontrados para " + orgName +
                        ". Caminhos esperados: " + certFile + ", " + keyFile);
                return;
            }
            
            log.info(() -> "Lendo certificado e chave privada de " + orgName + "...");

            Identity identity = Identities.newX509Identity(
                    mspId,
                    Identities.readX509Certificate(Files.newBufferedReader(certFile)),
                    Identities.readPrivateKey(Files.newBufferedReader(keyFile))
            );

            wallet.put(userName, identity);
            log.info(() -> "Identidade '" + userName + "' registrada com sucesso no wallet para " + orgName);

        } catch (IOException | CertificateException e) {
            log.log(Level.SEVERE, "Erro ao registrar identidade para " + orgName + ": {0}", e.getMessage());
        } catch (Exception e) {
            log.log(Level.SEVERE, "Falha inesperada ao registrar " + userName + ": {0}", e.getMessage());
        }
    }
}
