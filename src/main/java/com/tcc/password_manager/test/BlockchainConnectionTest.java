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
            System.out.println("\n=== 🔗 Teste de Conexão com Blockchain ===");

            // 🔐 Dados de teste
            String channel = "mychannel";                // Canal ativo
            String chaincode = "passwordmanager";         // Nome do chaincode
            String identity = "appUser";                  // Identidade registrada no wallet
            String key = "user123_google";                // Chave única
            String encryptedPassword = "ENC_TEST_123456"; // Senha fictícia

            // 🧱 1. Grava a senha
            System.out.println("\n➡️ Gravando senha no ledger...");
            blockchainService.invoke(channel, chaincode, "savePassword", identity, key, encryptedPassword);
            System.out.println("✅ Senha gravada com sucesso!");

            // 🔍 2. Consulta a senha
            System.out.println("\n➡️ Consultando senha do ledger...");
            String storedPassword = blockchainService.query(channel, chaincode, "getPassword", identity, key);
            System.out.println("🔐 Senha criptografada recuperada: " + storedPassword);

            // 🧹 3. Limpa o teste (opcional)
            System.out.println("\n➡️ Removendo senha de teste...");
            blockchainService.invoke(channel, chaincode, "deletePassword", identity, key);
            System.out.println("🧹 Senha de teste removida do ledger.");

            System.out.println("\n=== ✅ Teste finalizado com sucesso ===\n");

        } catch (Exception e) {
            System.err.println("\n❌ Erro durante o teste de conexão com a blockchain:");
            e.printStackTrace();
        }
    }
}
