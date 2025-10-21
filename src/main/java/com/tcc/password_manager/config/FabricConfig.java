/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tcc.password_manager.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.hyperledger.fabric.gateway.Wallet;
import org.hyperledger.fabric.gateway.Wallets;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 *
 * @author Lolo
 */
@Configuration
public class FabricConfig {

    @Bean
    public Wallet wallet() throws Exception {
        Path walletPath = Paths.get("src", "main", "resources", "wallet");
        return Wallets.newFileSystemWallet(walletPath);
    }

    @Bean(name = "networkConfigPath")
    public Path networkConfigPath() {
        // Aponta para o YAML de conexão
        return Paths.get("src", "main", "resources", "connection-org1.yml");
    }
}