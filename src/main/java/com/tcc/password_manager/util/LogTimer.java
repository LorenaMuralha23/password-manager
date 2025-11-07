package com.tcc.password_manager.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utilitário para medição padronizada de tempo de execução e registro de logs.
 * 
 * 🔹 Objetivos:
 * - Medir o tempo total de execução de operações (ex.: criptografia, fragmentação, armazenamento)
 * - Padronizar mensagens de início e fim em todas as classes
 * - Evitar repetição de código e garantir clareza analítica nos testes
 * 
 * Exemplo de uso:
 * 
 * private static final Logger log = LoggerFactory.getLogger(CryptoService.class);
 * 
 * LogTimer timer = LogTimer.start("Criptografia de credenciais");
 * try {
 *     // ... operação ...
 * } finally {
 *     timer.stopAndLog(log);
 * }
 */
public class LogTimer {

    private final long startTime;
    private final String description;

    private LogTimer(String description) {
        this.description = description;
        this.startTime = System.currentTimeMillis();
    }

    /**
     * Inicia o cronômetro e registra o log de início.
     */
    public static LogTimer start(String description) {
        Logger log = LoggerFactory.getLogger(getCallerClassName());
        log.info("=== [INICIO] {} ===", description);
        return new LogTimer(description);
    }

    /**
     * Finaliza o cronômetro e registra o tempo total de execução.
     */
    public void stopAndLog(Logger log) {
        long elapsed = System.currentTimeMillis() - startTime;
        String formattedTime = formatTime(elapsed);
        log.info("[FIM] {} | Tempo total: {}", description, formattedTime);
    }

    /**
     * Versão alternativa: permite encerrar e logar sem precisar passar o logger explicitamente.
     */
    public void stopAndLog() {
        Logger log = LoggerFactory.getLogger(getCallerClassName());
        long elapsed = System.currentTimeMillis() - startTime;
        String formattedTime = formatTime(elapsed);
        log.info("[FIM] {} | Tempo total: {}", description, formattedTime);
    }

    /**
     * Formata o tempo decorrido em milissegundos ou segundos, dependendo da duração.
     */
    private String formatTime(long elapsedMs) {
        if (elapsedMs < 1000) {
            return elapsedMs + " ms";
        }
        double seconds = elapsedMs / 1000.0;
        return String.format("%.3f s", seconds);
    }

    /**
     * Recupera o nome da classe que chamou o utilitário (para uso automático do logger).
     */
    private static Class<?> getCallerClassName() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        // Pula as primeiras posições que pertencem à própria classe LogTimer
        for (StackTraceElement element : stackTrace) {
            if (!element.getClassName().equals(LogTimer.class.getName())
                    && !element.getClassName().startsWith("java.lang")) {
                try {
                    return Class.forName(element.getClassName());
                } catch (ClassNotFoundException ignored) {
                }
            }
        }
        return LogTimer.class;
    }
}
