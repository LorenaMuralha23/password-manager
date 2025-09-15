package com.tcc.password_manager.crypto;

/**
 * Representa o envelope de criptografia utilizado no sistema.
 *
 * <p>Todo dado sensível armazenado no banco (campo encrypted_data) segue esse formato,
 * que contém não apenas o texto cifrado, mas também os parâmetros necessários para
 * a descriptografia.</p>
 *
 * <p>Formato do JSON esperado/armazenado:</p>
 * <pre>
 * {
 *   "alg": "AES-GCM-256",      // Algoritmo utilizado
 *   "ver": 1,                  // Versão do esquema de cifra
 *   "iv": "base64...",          // Vetor de inicialização (IV), 12 bytes no binário
 *   "ciphertext": "base64...",  // Texto cifrado (sem a tag)
 *   "tag": "base64..."          // Tag de autenticação, 16 bytes no binário
 * }
 * </pre>
 *
 * <p>Esse objeto é construído pelo {@link CryptoService} no momento da cifragem
 * e utilizado novamente no processo de decifragem.</p>
 */

public class EncryptedPayload {
    /**
     * Nome do algoritmo utilizado na cifragem.
     * Exemplo: "AES-GCM-256".
     */
    private String alg;

    /**
     * Versão do esquema de criptografia.
     * Útil para migrações futuras (ex.: v1 = AES-GCM, v2 = ChaCha20-Poly1305).
     */
    private int ver;

    /**
     * Vetor de inicialização (IV) em Base64.
     * Deve ter 12 bytes no binário.
     */
    private String iv;

    /**
     * Texto cifrado em Base64 (sem a tag).
     */
    private String ciphertext;

    /**
     * Tag de autenticação em Base64.
     * Deve ter 16 bytes no binário.
     */
    private String tag;

    /**
     * Construtor vazio necessário para serialização/deserialização com Jackson.
     */
    public EncryptedPayload() {}

    /**
     * Construtor completo.
     *
     * @param alg algoritmo (ex.: "AES-GCM-256")
     * @param ver versão do esquema (ex.: 1)
     * @param iv IV em Base64
     * @param ciphertext texto cifrado em Base64
     * @param tag tag de autenticação em Base64
     */
    public EncryptedPayload(String alg, int ver, String iv, String ciphertext, String tag) {
        this.alg = alg;
        this.ver = ver;
        this.iv = iv;
        this.ciphertext = ciphertext;
        this.tag = tag;
    }

    // Getters e Setters

    public String getAlg() {
        return alg;
    }

    public void setAlg(String alg) {
        this.alg = alg;
    }

    public int getVer() {
        return ver;
    }

    public void setVer(int ver) {
        this.ver = ver;
    }

    public String getIv() {
        return iv;
    }

    public void setIv(String iv) {
        this.iv = iv;
    }

    public String getCiphertext() {
        return ciphertext;
    }

    public void setCiphertext(String ciphertext) {
        this.ciphertext = ciphertext;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }
    
}
