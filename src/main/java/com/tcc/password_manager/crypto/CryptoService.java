package com.tcc.password_manager.crypto;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Serviço central de criptografia do sistema.
 *
 * <p>
 * Responsável por:
 * <ul>
 *   <li>Cifrar dados em claro (UTF-8 ou bytes) com AES-256-GCM</li>
 *   <li>Produzir envelopes {@link EncryptedPayload} contendo todos os parâmetros
 *       necessários para a decifragem (iv, ciphertext, tag)</li>
 *   <li>Decifrar envelopes {@link EncryptedPayload} e recuperar o plaintext</li>
 *   <li>Serializar/desserializar envelopes em JSON para armazenamento no banco</li>
 * </ul>
 * </p>
 *
 * <h2>Detalhes de implementação</h2>
 * <ul>
 *   <li>Algoritmo: AES/GCM/NoPadding</li>
 *   <li>Tamanho da chave: 256 bits (32 bytes)</li>
 *   <li>IV: 12 bytes (96 bits), aleatório e único por operação</li>
 *   <li>Tag de autenticação: 16 bytes (128 bits), separada explicitamente do ciphertext</li>
 *   <li>Codificação: todos os binários são convertidos para Base64
 *       antes de salvar no banco (facilita uso em colunas de texto)</li>
 *   <li>AAD (Associated Authenticated Data): opcional; garante
 *       integridade de metadados (ex.: userId, tabela, versão)</li>
 * </ul>
 *
 * <h2>Fluxo de cifra (encrypt)</h2>
 * <ol>
 *   <li>Recebe plaintext (bytes ou string)</li>
 *   <li>Gera IV aleatório de 12 bytes via {@link IVGeneratorService}</li>
 *   <li>Configura AES-GCM com a chave (32 bytes), IV e AAD (se fornecida)</li>
 *   <li>Executa {@code doFinal}, obtendo {@code ciphertext || tag}</li>
 *   <li>Separa ciphertext e tag em arrays distintos</li>
 *   <li>Constrói um {@link EncryptedPayload} com {alg, ver, iv, ciphertext, tag}</li>
 * </ol>
 *
 * <h2>Fluxo de decifra (decrypt)</h2>
 * <ol>
 *   <li>Lê JSON do banco e reconstrói {@link EncryptedPayload}</li>
 *   <li>Extrai iv, ciphertext e tag (decodificados de Base64)</li>
 *   <li>Concatena novamente ciphertext||tag</li>
 *   <li>Configura AES-GCM com a mesma chave, IV e AAD</li>
 *   <li>Executa {@code doFinal}; se algo tiver sido alterado,
 *       o GCM detecta e lança exceção</li>
 *   <li>Retorna plaintext (bytes ou string UTF-8)</li>
 * </ol>
 *
 * <h2>Segurança</h2>
 * <ul>
 *   <li>Reutilização de IV com mesma chave → <b>proibido</b>; o serviço gera IVs únicos</li>
 *   <li>Qualquer alteração em iv, ciphertext, tag ou AAD → falha na validação</li>
 *   <li>Todos os blobs cifrados são autocontidos (carregam IV e tag)</li>
 * </ul>
 */

public class CryptoService {
    
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_BITS = 128; // 16 bytes
    private static final int AES_256_KEY_LEN = 32; // 32 bytes

    private final IVGeneratorService ivGenerator;
    private final ObjectMapper mapper;
    
    /**
     * Construtor.
     *
     * @param ivGenerator serviço de geração de IVs (12 bytes para GCM)
     * @param mapper Jackson ObjectMapper para serializar/desserializar JSON
     */
    public CryptoService(IVGeneratorService ivGenerator, ObjectMapper mapper) {
        this.ivGenerator = ivGenerator;
        this.mapper = mapper;
    }
    
    /* =============================
       Helpers internos de suporte
       ============================= */

    /**
     * Converte um array de bytes em uma chave AES válida.
     *
     * @param key chave em bytes (deve ter 32 bytes)
     * @return chave formatada para uso no Cipher
     */
    private SecretKeySpec toAesKey(byte[] key) {
        if (key == null || key.length != AES_256_KEY_LEN) {
            throw new IllegalArgumentException("A chave AES deve ter 32 bytes (256 bits).");
        }
        return new SecretKeySpec(key, "AES");
    }

    /**
     * Concatena dois arrays de bytes.
     *
     * @param a primeiro array
     * @param b segundo array
     * @return array resultante de a||b
     */
    private byte[] join(byte[] a, byte[] b) {
        byte[] out = new byte[a.length + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }
    
    /* =============================
       Criptografia (Encrypt)
       ============================= */

    /**
     * Cifra um plaintext (bytes) com AES-256-GCM e retorna um envelope EncryptedPayload.
     *
     * @param key chave AES-256 (32 bytes)
     * @param plaintext dados em claro (bytes)
     * @param aad dados adicionais autenticados (opcional; pode ser null)
     * @return EncryptedPayload contendo iv, ciphertext e tag em Base64
     */
    public EncryptedPayload encrypt(byte[] key, byte[] plaintext, byte[] aad) {
        try {
            byte[] iv = ivGenerator.generateIV();

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_BITS, iv);
            cipher.init(Cipher.ENCRYPT_MODE, toAesKey(key), spec);

            if (aad != null && aad.length > 0) {
                cipher.updateAAD(aad);
            }

            byte[] ctAndTag = cipher.doFinal(plaintext);

            int tagLen = GCM_TAG_BITS / 8;
            int ctLen = ctAndTag.length - tagLen;
            byte[] ciphertext = new byte[ctLen];
            byte[] tag = new byte[tagLen];

            System.arraycopy(ctAndTag, 0, ciphertext, 0, ctLen);
            System.arraycopy(ctAndTag, ctLen, tag, 0, tagLen);

            return new EncryptedPayload(
                "AES-GCM-256",
                1,
                Base64.getEncoder().encodeToString(iv),
                Base64.getEncoder().encodeToString(ciphertext),
                Base64.getEncoder().encodeToString(tag)
            );
        } catch (Exception e) {
            throw new RuntimeException("Falha ao cifrar com AES-GCM", e);
        }
    }

    /**
     * Versão conveniente de {@link #encrypt(byte[], byte[], byte[])}
     * que aceita String UTF-8 como plaintext.
     */
    public EncryptedPayload encryptString(byte[] key, String plaintextUtf8, byte[] aad) {
        byte[] bytes = plaintextUtf8.getBytes(StandardCharsets.UTF_8);
        return encrypt(key, bytes, aad);
    }
    
    /* =============================
       Descriptografia (Decrypt)
       ============================= */

    /**
     * Decifra um EncryptedPayload com AES-256-GCM e retorna o plaintext.
     *
     * @param key chave AES-256 (32 bytes)
     * @param payload envelope cifrado {alg, ver, iv, ciphertext, tag}
     * @param aad dados adicionais autenticados (devem ser idênticos aos usados na cifra)
     * @return plaintext (bytes)
     */
    public byte[] decrypt(byte[] key, EncryptedPayload payload, byte[] aad) {
        try {
            if (!"AES-GCM-256".equals(payload.getAlg())) {
                throw new IllegalArgumentException("Algoritmo não suportado: " + payload.getAlg());
            }

            byte[] iv = Base64.getDecoder().decode(payload.getIv());
            byte[] ciphertext = Base64.getDecoder().decode(payload.getCiphertext());
            byte[] tag = Base64.getDecoder().decode(payload.getTag());

            byte[] ctAndTag = join(ciphertext, tag);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_BITS, iv);
            cipher.init(Cipher.DECRYPT_MODE, toAesKey(key), spec);

            if (aad != null && aad.length > 0) {
                cipher.updateAAD(aad);
            }

            return cipher.doFinal(ctAndTag);
        } catch (Exception e) {
            throw new RuntimeException("Falha ao decifrar com AES-GCM", e);
        }
    }

    /**
     * Versão conveniente de {@link #decrypt(byte[], EncryptedPayload, byte[])}
     * que retorna o plaintext como String UTF-8.
     */
    public String decryptToString(byte[] key, EncryptedPayload payload, byte[] aad) {
        byte[] plain = decrypt(key, payload, aad);
        return new String(plain, StandardCharsets.UTF_8);
    }
    
    /* =============================
       Serialização (para o banco)
       ============================= */

    /**
     * Serializa um EncryptedPayload para JSON.
     * Esse JSON é o que será armazenado em encrypted_data no banco.
     *
     * @param payload envelope cifrado
     * @return string JSON
     */
    public String toJson(EncryptedPayload payload) {
        try {
            return mapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new RuntimeException("Falha ao serializar EncryptedPayload em JSON", e);
        }
    }

    /**
     * Desserializa JSON do banco em EncryptedPayload.
     *
     * @param json string JSON lida do campo encrypted_data
     * @return objeto EncryptedPayload
     */
    public EncryptedPayload fromJson(String json) {
        try {
            return mapper.readValue(json, EncryptedPayload.class);
        } catch (Exception e) {
            throw new RuntimeException("Falha ao desserializar EncryptedPayload do JSON", e);
        }
    }
    
}
