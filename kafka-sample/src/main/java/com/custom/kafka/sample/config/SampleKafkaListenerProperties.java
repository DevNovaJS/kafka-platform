package com.custom.kafka.sample.config;

import com.custom.kafka.common.config.CustomKafkaListenerProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "kafka.listener")
public record SampleKafkaListenerProperties(
        @Valid @NotNull @NestedConfigurationProperty CustomKafkaListenerProperties payment,
        @Valid @NotNull @NestedConfigurationProperty CustomKafkaListenerProperties product
) {
}
