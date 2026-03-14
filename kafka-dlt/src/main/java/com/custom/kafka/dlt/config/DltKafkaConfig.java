package com.custom.kafka.dlt.config;

import com.custom.kafka.common.config.KafkaContainerFactoryBuilder;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.VirtualThreadTaskExecutor;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;

@Configuration
public class DltKafkaConfig {
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> dltKafkaListenerContainerFactory(
            KafkaProperties kafkaProperties
    ) {
        return KafkaContainerFactoryBuilder
                .from(kafkaProperties)
                .taskExecutor(new VirtualThreadTaskExecutor())
                .build();
    }
}
