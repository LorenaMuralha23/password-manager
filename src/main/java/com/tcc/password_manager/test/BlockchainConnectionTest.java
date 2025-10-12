/*
     * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
     * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tcc.password_manager.test;

import com.tcc.password_manager.service.BlockchainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 *
 * @author Lolo
 */
@Component
public class BlockchainConnectionTest implements CommandLineRunner {

    @Autowired
    private BlockchainService blockchainService;

    @Override
    public void run(String... args) {
        try {
            System.out.println("\n=== 🔗 Teste de Comunicação com Duas Blockchains ===");

            // 🔐 Dados de teste simulados
            String id = "user123_google";          // Identificador lógico (por exemplo, hash da credencial)
            String fragmentOrg1 = "ENC_FRAG_ORG1_ABC123";
            String fragmentOrg2 = "ENC_FRAG_ORG2_DEF456";

            // 🧱 1️⃣ Grava fragmentos nas duas blockchains
            System.out.println("\n➡️ Gravando fragmentos nas blockchains...");
            blockchainService.storeFragments(id, fragmentOrg1, fragmentOrg2);
            System.out.println("✅ Fragmentos gravados com sucesso!");

            // 🔍 2️⃣ Recupera os fragmentos armazenados
            System.out.println("\n➡️ Recuperando fragmentos das blockchains...");
            String[] fragments = blockchainService.retrieveFragments(id);
            System.out.println("🔐 Fragmento Org1 recuperado: " + fragments[0]);
            System.out.println("🔐 Fragmento Org2 recuperado: " + fragments[1]);

            // 🧹 3️⃣ Remove os fragmentos (limpeza do teste)
            System.out.println("\n➡️ Removendo fragmentos de teste das blockchains...");
            blockchainService.deleteFragments(id);
            System.out.println("🧹 Fragmentos removidos com sucesso!");

            System.out.println("\n=== ✅ Teste finalizado com sucesso ===\n");

        } catch (Exception e) {
            System.err.println("\n❌ Erro durante o teste de comunicação com as blockchains:");
            e.printStackTrace();
        }
    }
}
