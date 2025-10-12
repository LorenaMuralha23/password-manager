/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tcc.password_manager.connector;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import org.hyperledger.fabric.gateway.Contract;
import org.hyperledger.fabric.gateway.Gateway;
import org.hyperledger.fabric.gateway.Network;
import org.hyperledger.fabric.gateway.Wallet;
import org.hyperledger.fabric.gateway.Wallets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author Lolo
 */
@Component
public class FabricConnector {

    private final Wallet wallet;
    private final Map<String, NetworkConfig> orgConfigs = new HashMap<>();

    public FabricConnector() throws Exception {
        Path walletPath = Paths.get("wallet");
        this.wallet = Wallets.newFileSystemWallet(walletPath);

        // Pré-carrega as configurações disponíveis
        orgConfigs.put("org1", new NetworkConfig(
                "src/main/resources/connection-org1.yml",
                "mychannel",
                "appUserOrg1"));

        orgConfigs.put("org2", new NetworkConfig(
                "src/main/resources/connection-org2.yml",
                "secondchannel",
                "appUserOrg2"));
    }

    /**
     * Retorna um contrato (chaincode) ativo para a organização informada.
     *
     * @param orgName   Nome lógico da organização ("org1" ou "org2")
     * @param chaincode Nome do chaincode (ex: "passwordmanager")
     */
    public Contract getContract(String orgName, String chaincode) throws Exception {
        NetworkConfig config = orgConfigs.get(orgName.toLowerCase());
        if (config == null) {
            throw new IllegalArgumentException("Organização desconhecida: " + orgName);
        }

        Path networkConfigPath = Paths.get(config.configPath);

        Gateway.Builder builder = Gateway.createBuilder()
                .identity(wallet, config.user)
                .networkConfig(networkConfigPath)
                .discovery(true); // Habilita descoberta automática de peers

        Gateway gateway = builder.connect();
        Network network = gateway.getNetwork(config.channel);
        return network.getContract(chaincode);
    }

    /**
     * Estrutura auxiliar para armazenar dados de configuração de cada organização.
     */
    private static class NetworkConfig {
        String configPath;
        String channel;
        String user;

        NetworkConfig(String configPath, String channel, String user) {
            this.configPath = configPath;
            this.channel = channel;
            this.user = user;
        }
    }
}
