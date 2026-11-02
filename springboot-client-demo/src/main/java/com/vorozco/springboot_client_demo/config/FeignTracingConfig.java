package com.vorozco.springboot_client_demo.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapSetter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignTracingConfig {

    private static final TextMapSetter<RequestTemplate> SETTER =
            (template, key, value) -> {
                if (template != null) {
                    template.header(key, value);
                }
            };

    // Feign is not auto-instrumented by the OTel starter, so inject trace context manually
    @Bean
    public RequestInterceptor otelFeignInterceptor(OpenTelemetry openTelemetry) {
        return template -> openTelemetry.getPropagators()
                .getTextMapPropagator()
                .inject(Context.current(), template, SETTER);
    }
}
