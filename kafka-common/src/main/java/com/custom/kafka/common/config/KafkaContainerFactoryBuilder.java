package com.custom.kafka.common.config;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.RecordInterceptor;
import org.springframework.util.backoff.FixedBackOff;

import java.util.Map;

@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class KafkaContainerFactoryBuilder {
    private final KafkaProperties kafkaProperties;
    private final CustomKafkaListenerProperties listenerProperties;
    private CommonErrorHandler errorHandler;
    private RecordInterceptor<String, String> recordInterceptor;
    private AsyncTaskExecutor taskExecutor;

    public static KafkaContainerFactoryBuilder from(
            KafkaProperties kafkaProperties,
            CustomKafkaListenerProperties listenerProperties
    ) {
        return new KafkaContainerFactoryBuilder(kafkaProperties, listenerProperties);
    }

    public static KafkaContainerFactoryBuilder from(KafkaProperties kafkaProperties) {
        return new KafkaContainerFactoryBuilder(kafkaProperties, CustomKafkaListenerProperties.defaults());
    }

    public KafkaContainerFactoryBuilder errorHandler(CommonErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
        return this;
    }

    public KafkaContainerFactoryBuilder recordInterceptor(RecordInterceptor<String, String> recordInterceptor) {
        this.recordInterceptor = recordInterceptor;
        return this;
    }

    public KafkaContainerFactoryBuilder taskExecutor(AsyncTaskExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
        return this;
    }

    public ConcurrentKafkaListenerContainerFactory<String, String> build() {
        Map<String, Object> props = kafkaProperties.buildConsumerProperties();

        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(props));
        factory.setConcurrency(listenerProperties.concurrency());
        factory.setBatchListener(listenerProperties.batchListener());
        factory.setCommonErrorHandler(errorHandler != null ? errorHandler : defaultErrorHandler());

        if (recordInterceptor != null) {
            factory.setRecordInterceptor(recordInterceptor);
        }

        ContainerProperties containerProps = factory.getContainerProperties();
        containerProps.setAckMode(listenerProperties.ackMode());
        containerProps.setSyncCommits(listenerProperties.syncCommits());
        containerProps.setPollTimeout(listenerProperties.pollTimeout());

        if (taskExecutor != null) {
            containerProps.setListenerTaskExecutor(taskExecutor);
        }

        return factory;
    }

    private CommonErrorHandler defaultErrorHandler() {
        return new DefaultErrorHandler(
                (record, ex) -> log.error(
                        "error occurs : topic={}, partition={}, offset={}",
                        record.topic(),
                        record.partition(),
                        record.offset(),
                        ex
                ),
                new FixedBackOff(0L, 0L)
        );
    }
}
