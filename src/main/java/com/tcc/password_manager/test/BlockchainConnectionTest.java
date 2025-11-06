package com.tcc.password_manager.test;

import com.tcc.password_manager.service.BlockchainService;
import com.tcc.password_manager.util.LogTimer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Executa um teste automatizado de comunicação com ambas as blockchains (Org1 e Org2).
 * 
 * Fluxo testado:
 * Grava fragmentos nas duas blockchains
 * Recupera os fragmentos
 * Remove os fragmentos (limpeza)
 */
@Component
public class BlockchainConnectionTest implements CommandLineRunner {
    
    private static final Logger log = LoggerFactory.getLogger(BlockchainConnectionTest.class);

    @Autowired
    private BlockchainService blockchainService;

    @Override
    public void run(String... args) {
        LogTimer timer = LogTimer.start("Teste de Comunicação com Duas Blockchains");

        try {
            // Dados de teste simulados
            String id = "user123_google";          // Identificador lógico (por exemplo, hash da credencial)
            String fragmentOrg1 = "ENC_FRAG_ORG1_ABC123";
            String fragmentOrg2 = "ENC_FRAG_ORG2_DEF456";

            // Gravação
            log.info("Gravando fragmentos nas blockchains...");
            blockchainService.storeFragments(id, fragmentOrg1, fragmentOrg2);
            log.info("Fragmentos gravados com sucesso!");

            // Recuperação
            log.info("Recuperando fragmentos das blockchains...");
            String[] fragments = blockchainService.retrieveFragments(id);
            log.info("Fragmento Org1 recuperado: {}", fragments[0]);
            log.info("Fragmento Org2 recuperado: {}", fragments[1]);

            // Remoção
            log.info("Removendo fragmentos de teste das blockchains...");
            blockchainService.deleteFragments(id);
            log.info("Fragmentos removidos com sucesso!");

            log.info("=== Teste finalizado com sucesso ===");

        } catch (Exception e) {
            log.error("Erro durante o teste de comunicação com as blockchains: {}", e.getMessage(), e);
        } finally {
            timer.stopAndLog(log);
        }
    }
}
