import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.opentelemetry.OpenTelemetryMeterRegistry;
import io.micrometer.opentelemetry.OpenTelemetryMeterRegistryConfig;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenTelemetryMicrometerBridge {

    @Bean
    public OpenTelemetry openTelemetry(MeterRegistry registry) {
        OpenTelemetryMeterRegistry otelRegistry = OpenTelemetryMeterRegistry.builder(OpenTelemetryMeterRegistryConfig.DEFAULT, OpenTelemetry.noop())
                .build();

        Metrics.addRegistry(otelRegistry);

        return OpenTelemetrySdk.builder().build();
    }
}