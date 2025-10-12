/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tcc.password_manager.service;

import com.tcc.password_manager.connector.FabricConnector;
import org.hyperledger.fabric.gateway.Contract;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 *
 * @author Lolo
 */
@Service
public class BlockchainService {

    private final FabricConnector fabricConnector;

    @Autowired
    public BlockchainService(FabricConnector fabricConnector) {
        this.fabricConnector = fabricConnector;
    }

    public String query(String channel, String chaincode, String function, String identity, String... args) throws Exception {
        Contract contract = fabricConnector.getContract(channel, chaincode, identity);
        byte[] result = contract.evaluateTransaction(function, args);
        return new String(result);
    }

    public String invoke(String channel, String chaincode, String function, String identity, String... args) throws Exception {
        Contract contract = fabricConnector.getContract(channel, chaincode, identity);
        byte[] result = contract.submitTransaction(function, args);
        return new String(result);
    }
}
