/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tcc.password_manager.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.hyperledger.fabric.gateway.Identities;
import org.hyperledger.fabric.gateway.Identity;
import org.hyperledger.fabric.gateway.Wallet;
import org.hyperledger.fabric.gateway.Wallets;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class WalletInitializer implements CommandLineRunner {

    @Override
    public void run(String... args) throws Exception {
        Path walletPath = Paths.get("src", "main", "resources", "wallet");
        Wallet wallet = Wallets.newFileSystemWallet(walletPath);

        if (wallet.get("appUser") != null) {
            System.out.println("✅ Identidade 'appUser' já está registrada no wallet.");
            return;
        }

        System.out.println("🪪 Registrando identidade 'appUser' no wallet...");

        // Caminhos dos arquivos PEM
        Path certPath = Paths.get("src", "main", "resources", "wallet", "appUser", "cert.pem");
        Path keyPath = Paths.get("src", "main", "resources", "wallet", "appUser", "key.pem");

        // Carrega o certificado e a chave privada
        String certPem = Files.readString(certPath);
        String keyPem = Files.readString(keyPath);

        Identity identity = Identities.newX509Identity("Org1MSP",
                Identities.readX509Certificate(certPem),
                Identities.readPrivateKey(keyPem));

        wallet.put("appUser", identity);

        System.out.println("✅ Identidade 'appUser' registrada com sucesso!");
    }
}
