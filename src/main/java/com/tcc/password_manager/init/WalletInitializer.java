/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tcc.password_manager.init;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.hyperledger.fabric.gateway.Identities;
import org.hyperledger.fabric.gateway.Identity;
import org.hyperledger.fabric.gateway.Wallet;
import org.hyperledger.fabric.gateway.Wallets;

/**
 *
 * @author Lolo
 */
public class WalletInitializer {
     public static void main(String[] args) throws Exception {
        Path walletPath = Paths.get("wallet");
        Wallet wallet = Wallets.newFileSystemWallet(walletPath);

        registerUser(wallet, "Org1", "appUserOrg1",
                "src/main/resources/crypto/msp-org1/signcerts/cert.pem",
                "src/main/resources/crypto/msp-org1/keystore/priv_sk",
                "Org1MSP");

        registerUser(wallet, "Org2", "appUserOrg2",
                "src/main/resources/crypto/msp-org2/signcerts/cert.pem",
                "src/main/resources/crypto/msp-org2/keystore/priv_sk",
                "Org2MSP");
    }

    private static void registerUser(Wallet wallet, String orgName, String userName,
                                     String certPath, String keyPath, String mspId) throws Exception {

        if (wallet.get(userName) != null) {
            System.out.println("✅ Identidade '" + userName + "' já registrada para " + orgName);
            return;
        }

        Path certFile = Paths.get(certPath);
        Path keyFile = Paths.get(keyPath);

        if (!Files.exists(certFile) || !Files.exists(keyFile)) {
            throw new RuntimeException("Arquivos de certificado ou chave não encontrados para " + orgName);
        }

        Identity identity = Identities.newX509Identity(
                mspId,
                Identities.readX509Certificate(Files.newBufferedReader(certFile)),
                Identities.readPrivateKey(Files.newBufferedReader(keyFile))
        );

        wallet.put(userName, identity);
        System.out.println("🎉 Identidade '" + userName + "' registrada com sucesso no wallet!");
    }
}
