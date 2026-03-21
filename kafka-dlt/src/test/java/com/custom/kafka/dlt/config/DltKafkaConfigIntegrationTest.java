package com.custom.kafka.dlt.config;

import com.custom.kafka.common.history.MessageHistoryRepository;
import com.custom.kafka.common.history.MessageHistoryService;
import com.custom.kafka.common.registry.MetadataRegistryRepository;
import com.custom.kafka.common.registry.MetadataRegistryService;
import com.custom.kafka.dlt.repository.DltMessageRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka
class DltKafkaConfigIntegrationTest {

    @Autowired
    private ConcurrentKafkaListenerContainerFactory<String, String> dltKafkaListenerContainerFactory;

    @MockitoBean
    private DltMessageRepository dltMessageRepository;

    @MockitoBean
    private MessageHistoryRepository messageHistoryRepository;

    @MockitoBean
    private MessageHistoryService messageHistoryService;

    @MockitoBean
    private MetadataRegistryRepository metadataRegistryRepository;

    @MockitoBean
    private MetadataRegistryService metadataRegistryService;

    @Test
    void dltKafkaListenerContainerFactory_hasExpectedConfiguration() {
        assertThat(dltKafkaListenerContainerFactory).isNotNull();
        assertThat(dltKafkaListenerContainerFactory.getContainerProperties().getListenerTaskExecutor())
                .isNotNull();
    }
}
