/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tcc.password_manager.connector;

import java.nio.file.Path;
import org.hyperledger.fabric.gateway.Contract;
import org.hyperledger.fabric.gateway.Gateway;
import org.hyperledger.fabric.gateway.Network;
import org.hyperledger.fabric.gateway.Wallet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author Lolo
 */
@Component
public class FabricConnector {

    private final Wallet wallet;
    private final Path networkConfigPath;

    @Autowired
    public FabricConnector(Wallet wallet, Path networkConfigPath) {
        this.wallet = wallet;
        this.networkConfigPath = networkConfigPath;
    }

    public Contract getContract(String channelName, String chaincodeName, String identityLabel) throws Exception {
        Gateway.Builder builder = Gateway.createBuilder()
                .identity(wallet, identityLabel)
                .networkConfig(networkConfigPath)
                .discovery(true);

        Gateway gateway = builder.connect();
        Network network = gateway.getNetwork(channelName);

        return network.getContract(chaincodeName);
    }
}
