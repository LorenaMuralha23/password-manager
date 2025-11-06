import java.util.logging.Logger;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Default;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ChaincodeStub;

/**
 * Chaincode simples para gerenciamento de senhas.
 * Usa apenas Fabric Contract API, sem Spring Boot.
 */
@Contract(name = "PasswordManager")
@Default
public class Chaincode implements ContractInterface {
    
    private static final Logger logger = Logger.getLogger(Chaincode.class.getName());

    /**
     * Salva ou atualiza uma senha no ledger.
     * 
     * @param ctx contexto da transação
     * @param key identificador único (ex: userId + serviço)
     * @param encryptedPassword senha criptografada
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void savePassword(Context ctx, String key, String encryptedPassword) {
        ChaincodeStub stub = ctx.getStub();
        stub.putStringState(key, encryptedPassword);
        logger.info(() -> String.format("Senha armazenada/atualizada no ledger [key=%s]", key));
    }

    /**
     * Recupera uma senha pelo identificador.
     * 
     * @param ctx contexto da transação
     * @param key identificador único
     * @return senha criptografada
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String getPassword(Context ctx, String key) {
        ChaincodeStub stub = ctx.getStub();
        String encryptedPassword = stub.getStringState(key);

        if (encryptedPassword == null || encryptedPassword.isEmpty()) {
            String msg = "Senha não encontrada para a chave: " + key;
            logger.warning(msg);
            throw new ChaincodeException(msg, "PASSWORD_NOT_FOUND");
        }

        logger.info(() -> String.format("Senha recuperada do ledger [key=%s]", key));
        return encryptedPassword;
    }

    /**
     * Remove uma senha do ledger.
     * 
     * @param ctx contexto da transação
     * @param key identificador único
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void deletePassword(Context ctx, String key) {
        ChaincodeStub stub = ctx.getStub();
        String existing = stub.getStringState(key);

        if (existing == null || existing.isEmpty()) {
            String msg = "Tentativa de remoção falhou — chave inexistente: " + key;
            logger.warning(msg);
            throw new ChaincodeException(msg, "PASSWORD_NOT_FOUND");
        }

        stub.delState(key);
        logger.info(() -> String.format("Senha removida do ledger [key=%s]", key));
    }
}