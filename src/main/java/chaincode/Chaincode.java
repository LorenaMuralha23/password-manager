import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Default;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.shim.ChaincodeStub;

/**
 * Chaincode simples para gerenciamento de senhas.
 * Usa apenas Fabric Contract API, sem Spring Boot.
 */
@Contract(name = "PasswordManager")
@Default
public class Chaincode implements ContractInterface {

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
            throw new RuntimeException("Senha não encontrada para a chave: " + key);
        }

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
        stub.delState(key);
    }
}