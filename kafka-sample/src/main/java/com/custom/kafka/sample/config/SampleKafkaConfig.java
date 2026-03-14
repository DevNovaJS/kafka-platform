package com.custom.kafka.sample.config;

import com.custom.kafka.common.config.KafkaContainerFactoryBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(SampleKafkaListenerProperties.class)
public class SampleKafkaConfig {
    private final SampleKafkaListenerProperties listenerProperties;

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> activityKafkaListenerContainerFactory(
            KafkaProperties kafkaProperties
    ) {
        SimpleAsyncTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor("activity-consumer-");
        taskExecutor.setVirtualThreads(true);

        return KafkaContainerFactoryBuilder
                .from(kafkaProperties, listenerProperties.activity())
                .taskExecutor(taskExecutor)
                .build();
    }
}
