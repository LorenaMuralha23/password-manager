package com.tcc.password_manager.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.hyperledger.fabric.gateway.Wallet;
import org.hyperledger.fabric.gateway.Wallets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração principal do Fabric Gateway.
 * Responsável por inicializar o wallet e o caminho de configuração de rede.
 */
@Configuration
public class FabricConfig {
    
    private static final Logger log = LoggerFactory.getLogger(FabricConfig.class);

    @Bean
    public Wallet wallet() {
        try {
            Path walletPath = Paths.get("src", "main", "resources", "wallet");

            if (!Files.exists(walletPath)) {
                log.warn("Diretório do wallet não encontrado em: {}", walletPath.toAbsolutePath());
            } else {
                log.info("Wallet localizado em: {}", walletPath.toAbsolutePath());
            }

            return Wallets.newFileSystemWallet(walletPath);
        } catch (IOException e) {
            log.error("Erro ao inicializar o wallet Fabric: {}", e.getMessage(), e);
            throw new RuntimeException("Falha ao carregar o wallet Fabric", e);
        }
    }

    @Bean(name = "networkConfigPath")
    public Path networkConfigPath() {
        // Aponta para o YAML de conexão
        Path path = Paths.get("src", "main", "resources", "connection-org1.yml");

        if (!Files.exists(path)) {
            log.error("Arquivo de configuração Fabric não encontrado: {}", path.toAbsolutePath());
        } else {
            log.info("Caminho de configuração da rede Fabric definido: {}", path.toAbsolutePath());
        }

        return path;
    }
}