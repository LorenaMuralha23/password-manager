package com.tcc.password_manager.connector;

import com.tcc.password_manager.util.LogTimer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import org.hyperledger.fabric.gateway.Contract;
import org.hyperledger.fabric.gateway.Gateway;
import org.hyperledger.fabric.gateway.Network;
import org.hyperledger.fabric.gateway.Wallet;
import org.hyperledger.fabric.gateway.Wallets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Responsável por estabelecer conexões seguras com as redes Hyperledger Fabric
 * (Org1 e Org2) e retornar instâncias de contratos (chaincodes) ativos.
 */
@Component
public class FabricConnector {

    private static final Logger log = LoggerFactory.getLogger(FabricConnector.class);

    private final Wallet wallet;
    private final Map<String, NetworkConfig> orgConfigs = new HashMap<>();

    public FabricConnector() throws Exception {
        Path walletPath = Paths.get("wallet");
        this.wallet = Wallets.newFileSystemWallet(walletPath);
        log.info("Wallet inicializada no caminho: {}", walletPath.toAbsolutePath());

        // Pré-carrega as configurações disponíveis
        orgConfigs.put("org1", new NetworkConfig(
                "src/main/resources/connection-org1.yml",
                "mychannel",
                "appUserOrg1"));

        orgConfigs.put("org2", new NetworkConfig(
                "src/main/resources/connection-org2.yml",
                "secondchannel",
                "appUserOrg2"));

        log.info("Configurações das organizações pré-carregadas: {}", orgConfigs.keySet());
    }

    /**
     * Retorna um contrato (chaincode) ativo para a organização informada.
     *
     * @param orgName Nome lógico da organização ("org1" ou "org2")
     * @param chaincode Nome do chaincode (ex: "passwordmanager")
     */
    public Contract getContract(String orgName, String chaincode) throws Exception {
        LogTimer timer = LogTimer.start("Connect to Fabric network (" + orgName + ")");

        try {
            log.info("Solicitação de contrato para a organização '{}' e chaincode '{}'.", orgName, chaincode);

            NetworkConfig config = orgConfigs.get(orgName.toLowerCase());
            if (config == null) {
                log.warn("Organização desconhecida: {}", orgName);
                throw new IllegalArgumentException("Organização desconhecida: " + orgName);
            }

            Path networkConfigPath = Paths.get(config.configPath);
            log.debug("Usando arquivo de configuração: {}", networkConfigPath.toAbsolutePath());
            log.debug("Canal: {}, Identidade: {}", config.channel, config.user);

            Gateway.Builder builder = Gateway.createBuilder()
                    .identity(wallet, config.user)
                    .networkConfig(networkConfigPath)
                    .discovery(true); // Habilita descoberta automática de peers

            log.debug("Estabelecendo conexão com o gateway Fabric para {}", orgName);
            Gateway gateway = builder.connect();

            Network network = gateway.getNetwork(config.channel);
            Contract contract = network.getContract(chaincode);

            log.info("Conexão estabelecida com sucesso. Contrato obtido para '{}'.", orgName);
            return contract;
        } catch (Exception e) {
            log.error("Falha ao conectar à rede Fabric ({}) ou obter contrato: {}", orgName, e.getMessage());
            throw new RuntimeException("Erro ao conectar à rede Fabric: " + e.getMessage(), e);
        } finally {
            timer.stopAndLog(log);
        }
    }

    /**
     * Estrutura auxiliar para armazenar dados de configuração de cada
     * organização.
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

        @Override
        public String toString() {
            return "NetworkConfig{path=" + configPath + ", channel=" + channel + ", user=" + user + "}";
        }
    }
}
