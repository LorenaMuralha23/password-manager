package io.opentelemetry.sdk.autoconfigure;

public class AutoConfiguredOpenTelemetrySdkBuilder {

    public AutoConfiguredOpenTelemetrySdkBuilder setResultAsGlobal(boolean value) {
        return this;
    }

    public AutoConfiguredOpenTelemetrySdkBuilder registerShutdownHook(boolean value) {
        return this;
    }

    // Adiciona o método build() que o Fabric tenta chamar
    public AutoConfiguredOpenTelemetrySdk build() {
        return new AutoConfiguredOpenTelemetrySdk();
    }
}
