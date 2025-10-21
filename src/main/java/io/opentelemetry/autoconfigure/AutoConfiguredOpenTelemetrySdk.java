package io.opentelemetry.sdk.autoconfigure;

import io.opentelemetry.sdk.OpenTelemetrySdk;

/**
 * Mock simplificado do AutoConfiguredOpenTelemetrySdk usado pelo Hyperledger Fabric.
 * Ele apenas retorna instâncias fictícias para satisfazer as chamadas do SDK.
 */
public class AutoConfiguredOpenTelemetrySdk {

    public static AutoConfiguredOpenTelemetrySdkBuilder builder() {
        return new AutoConfiguredOpenTelemetrySdkBuilder();
    }

    // ✅ Novo método que o Fabric tenta invocar
    public OpenTelemetrySdk getOpenTelemetrySdk() {
        return new OpenTelemetrySdk();
    }
}
