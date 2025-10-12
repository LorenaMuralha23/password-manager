package io.opentelemetry.instrumentation.grpc.v1_6;

import io.opentelemetry.api.OpenTelemetry;
import io.grpc.ClientInterceptor;
import io.grpc.ClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Channel;
import io.grpc.CallOptions; // ✅ import que faltava

/**
 * Stub placeholder for missing OpenTelemetry class used by Hyperledger Fabric SDK.
 * Prevents runtime errors when tracing is disabled.
 */
public class GrpcTracing implements ClientInterceptor {

    private static final GrpcTracing INSTANCE = new GrpcTracing();

    /** Criação padrão (sem parâmetro). */
    public static GrpcTracing create() {
        return INSTANCE;
    }

    /** Criação com OpenTelemetry (sobrecarga chamada pelo Fabric SDK). */
    public static GrpcTracing create(OpenTelemetry ignored) {
        return INSTANCE;
    }

    /** Retorna o interceptor atual (não nulo). */
    public ClientInterceptor newClientInterceptor() {
        return INSTANCE;
    }

    /** Implementação vazia do interceptor (não altera o tráfego). */
    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method,
            CallOptions callOptions,
            Channel next) {
        return next.newCall(method, callOptions);
    }
}
