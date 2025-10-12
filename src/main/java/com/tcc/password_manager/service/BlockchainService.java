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

    
    private final FabricConnector connector;

    public BlockchainService() throws Exception {
        this.connector = new FabricConnector();
    }

    /**
     * Grava dois fragmentos — um em cada blockchain (Org1 e Org2).
     *
     * @param id Identificador lógico do segredo (ex: hash ou UUID)
     * @param fragment1 Fragmento criptografado destinado à Org1
     * @param fragment2 Fragmento criptografado destinado à Org2
     */
    public void storeFragments(String id, String fragment1, String fragment2) throws Exception {
        // === Org1 ===
        Contract contractOrg1 = connector.getContract("org1", "passwordmanager");
        System.out.println("📤 Gravando fragmento 1 na blockchain Org1...");
        contractOrg1.submitTransaction("savePassword", id, fragment1);

        // === Org2 ===
        Contract contractOrg2 = connector.getContract("org2", "passwordmanager");
        System.out.println("📤 Gravando fragmento 2 na blockchain Org2...");
        contractOrg2.submitTransaction("savePassword", id, fragment2);

        System.out.println("✅ Fragmentos gravados com sucesso em ambas as blockchains!");
    }

    /**
     * Recupera os fragmentos armazenados nas duas blockchains.
     *
     * @param id Identificador lógico do segredo (mesmo usado no storeFragments)
     * @return Array contendo [fragmentOrg1, fragmentOrg2]
     */
    public String[] retrieveFragments(String id) throws Exception {
        System.out.println("📥 Recuperando fragmentos das blockchains...");

        Contract contractOrg1 = connector.getContract("org1", "passwordmanager");
        Contract contractOrg2 = connector.getContract("org2", "passwordmanager");

        byte[] response1 = contractOrg1.evaluateTransaction("getPassword", id);
        byte[] response2 = contractOrg2.evaluateTransaction("getPassword", id);

        String fragment1 = new String(response1);
        String fragment2 = new String(response2);

        System.out.println("✅ Fragmentos recuperados com sucesso!");
        return new String[]{fragment1, fragment2};
    }

    /**
     * Remove os fragmentos associados ao ID informado em ambas as blockchains.
     *
     * @param id Identificador lógico do segredo
     */
    public void deleteFragments(String id) throws Exception {
        System.out.println("❌ Removendo fragmentos das blockchains...");

        Contract contractOrg1 = connector.getContract("org1", "passwordmanager");
        Contract contractOrg2 = connector.getContract("org2", "passwordmanager");

        contractOrg1.submitTransaction("deletePassword", id);
        contractOrg2.submitTransaction("deletePassword", id);

        System.out.println("✅ Fragmentos removidos com sucesso!");
    }
}
