package com.tcc.password_manager.session;

import java.time.Instant;
import java.util.Arrays;

/**
 * {@code SessionContext} mantém informações básicas sobre a sessão de usuário
 * na aplicação CLI. Diferente da versão anterior, ele não armazena mais chaves
 * criptográficas (UMK, salt), pois a responsabilidade de lidar com esses dados
 * pertence ao {@code AppUserService}.
 *
 * <h2>Responsabilidades principais:</h2>
 * <ul>
 * <li>Indicar se há um usuário autenticado.</li>
 * <li>Armazenar identidade do usuário (id).</li>
 * <li>Gerenciar ciclo de vida da sessão (abrir, atualizar, encerrar).</li>
 * <li>Controlar timestamps de criação e última atividade.</li>
 * </ul>
 *
 * <h2>Uso típico:</h2>
 * <ol>
 * <li>Após login bem-sucedido, chamar {@link #establish(Long, String)}.</li>
 * <li>No início de operações protegidas, chamar
 * {@link #requireAuthenticated()}.</li>
 * <li>No logout ou expiração, chamar {@link #clear()}.</li>
 * </ol>
 */
public class SessionContext {

    /**
     * Identificador único do usuário logado.
     */
    private Long userId;

    /**
     * User Master Key (UMK) derivada a partir da senha mestre do usuário.
     * Mantida em memória apenas durante a sessão. Nunca deve ser persistida nem
     * registrada em logs.
     */
    private byte[] umk;

    /**
     * Indica se há um usuário autenticado nesta sessão.
     */
    private boolean authenticated;

    /**
     * Timestamp de criação da sessão.
     */
    private Instant createdAt;

    /**
     * Última atividade registrada (atualizada por {@link #touch()}).
     */
    private Instant lastActivityAt;

    // ==================== Getters ====================
    /**
     * @return true se há um usuário autenticado nesta sessão.
     */
    public synchronized boolean isAuthenticated() {
        return authenticated;
    }

    /**
     * @return identificador do usuário autenticado.
     */
    public synchronized Long getUserId() {
        return userId;
    }

    /**
     * @return chave mestre derivada (UMK) do usuário.
     */
    public synchronized byte[] getUmk() {
        return umk;
    }

    /**
     * @return instante da última atividade registrada.
     */
    public synchronized Instant getLastActivityAt() {
        return lastActivityAt;
    }

    // ==================== Controle de sessão ====================
    /**
     * Estabelece (abre ou substitui) uma sessão autenticada. Deve ser chamado
     * após um login bem-sucedido.
     *
     * @param userId identificador do usuário
     * @param umk User Master Key derivada da senha mestre
     */
    public synchronized void establish(Long userId, byte[] umk) {
        clear(); // garante estado limpo
        this.userId = userId;
        this.umk = umk;
        this.authenticated = true;
        this.createdAt = Instant.now();
        this.lastActivityAt = this.createdAt;
    }

    /**
     * Atualiza a marca de tempo da última atividade da sessão. Deve ser chamado
     * no início de cada operação sensível.
     */
    public synchronized void touch() {
        this.lastActivityAt = Instant.now();
    }

    /**
     * Exige que a sessão esteja autenticada.
     *
     * @throws IllegalStateException se não houver usuário autenticado
     */
    public synchronized void requireAuthenticated() {
        if (!authenticated) {
            throw new IllegalStateException("Faca login antes de executar esta operacao.");
        }
    }

    /**
     * Limpa completamente a sessão, removendo identidade, timestamps
     * e sobrescrevendo arrays sensíveis (UMK).
     * Deve ser chamado explicitamente no logout ou em casos de expiração.
     */
    public synchronized void clear() {
        this.authenticated = false;
        this.userId = null;
        if (this.umk != null) {
            Arrays.fill(this.umk, (byte) 0); // higiene
            this.umk = null;
        }
        this.createdAt = null;
        this.lastActivityAt = null;
    }
}
