package com.custom.kafka.common.config;

import org.springframework.kafka.listener.ContainerProperties;

public record CustomKafkaListenerProperties(
        int concurrency,
        ContainerProperties.AckMode ackMode,
        boolean syncCommits,
        long pollTimeout,
        boolean batchListener
) {
    public static CustomKafkaListenerProperties defaults() {
        return new CustomKafkaListenerProperties(
                1,
                ContainerProperties.AckMode.BATCH,
                true,
                5000L,
                false
        );
    }
}
