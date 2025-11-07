package com.tcc.password_manager.service;

import com.tcc.password_manager.connector.FabricConnector;
import com.tcc.password_manager.util.LogTimer;
import org.hyperledger.fabric.gateway.Contract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Serviço responsável pela comunicação com as duas redes blockchain privadas
 * (Org1 e Org2). Gerencia a gravação, recuperação e exclusão de fragmentos
 * criptografados.
 */
@Service
public class BlockchainService {

    private static final Logger log = LoggerFactory.getLogger(BlockchainService.class);
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
        LogTimer timer = LogTimer.start("Store fragments in blockchains");

        try {
            log.info("Iniciando gravacao dos fragmentos nas blockchains para o ID: {}", id);

            Contract contractOrg1 = connector.getContract("org1", "passwordmanager");
            log.debug("Obtido contrato da Org1. Enviando transacao para salvar fragmento 1.");
            contractOrg1.submitTransaction("savePassword", id, fragment1);
            log.info("Fragmento 1 gravado com sucesso na Org1.");

            Contract contractOrg2 = connector.getContract("org2", "passwordmanager");
            log.debug("Obtido contrato da Org2. Enviando transacao para salvar fragmento 2.");
            contractOrg2.submitTransaction("savePassword", id, fragment2);
            log.info("Fragmento 2 gravado com sucesso na Org2.");

            log.info("Fragmentos gravados com sucesso em ambas as blockchains.");
        } catch (Exception e) {
            log.error("Falha ao gravar fragmentos nas blockchains: {}", e.getMessage());
            throw new RuntimeException("Erro ao armazenar fragmentos nas blockchains", e);
        } finally {
            timer.stopAndLog(log);
        }
    }

    /**
     * Recupera os fragmentos armazenados nas duas blockchains.
     *
     * @param id Identificador lógico do segredo (mesmo usado no storeFragments)
     * @return Array contendo [fragmentOrg1, fragmentOrg2]
     */
    public String[] retrieveFragments(String id) throws Exception {
        LogTimer timer = LogTimer.start("Retrieve fragments from blockchains");

        try {
            log.info("Iniciando recuperacao dos fragmentos das blockchains para o ID: {}", id);

            Contract contractOrg1 = connector.getContract("org1", "passwordmanager");
            Contract contractOrg2 = connector.getContract("org2", "passwordmanager");

            byte[] response1 = contractOrg1.evaluateTransaction("getPassword", id);
            byte[] response2 = contractOrg2.evaluateTransaction("getPassword", id);

            String fragment1 = new String(response1);
            String fragment2 = new String(response2);

            log.debug("Tamanhos dos fragmentos recuperados - Org1: {} bytes, Org2: {} bytes",
                    fragment1.length(), fragment2.length());

            if (fragment1.isEmpty() || fragment2.isEmpty()) {
                log.warn("Um ou mais fragmentos retornaram vazios. Verifique a integridade dos dados para o ID: {}", id);
            } else {
                log.info("Fragmentos recuperados com sucesso para o ID: {}", id);
            }

            return new String[]{fragment1, fragment2};
        } catch (Exception e) {
            log.error("Falha ao recuperar fragmentos nas blockchains: {}", e.getMessage());
            throw new RuntimeException("Erro ao recuperar fragmentos das blockchains", e);
        } finally {
            timer.stopAndLog(log);
        }
    }

    /**
     * Remove os fragmentos associados ao ID informado em ambas as blockchains.
     *
     * @param id Identificador lógico do segredo
     */
    public void deleteFragments(String id) throws Exception {
        LogTimer timer = LogTimer.start("Delete fragments from blockchains");

        try {
            log.info("Iniciando remocao dos fragmentos das blockchains para o ID: {}", id);

            Contract contractOrg1 = connector.getContract("org1", "passwordmanager");
            Contract contractOrg2 = connector.getContract("org2", "passwordmanager");

            contractOrg1.submitTransaction("deletePassword", id);
            log.debug("Fragmento removido da Org1 com sucesso.");

            contractOrg2.submitTransaction("deletePassword", id);
            log.debug("Fragmento removido da Org2 com sucesso.");

            log.info("Fragmentos removidos com sucesso em ambas as blockchains.");
        } catch (Exception e) {
            log.error("Falha ao remover fragmentos das blockchains: {}", e.getMessage());
            throw new RuntimeException("Erro ao remover fragmentos das blockchains", e);
        } finally {
            timer.stopAndLog(log);
        }
    }
}
