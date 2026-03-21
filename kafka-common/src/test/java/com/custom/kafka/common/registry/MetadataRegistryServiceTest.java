package com.custom.kafka.common.registry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MetadataRegistryServiceTest {

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private MetadataRegistryRepository metadataRegistryRepository;

    @InjectMocks
    private MetadataRegistryService metadataRegistryService;

    @Test
    void registerIfNew_upsertsToDb() {
        // when
        metadataRegistryService.registerIfNew("payment-service", "billing");

        // then
        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).upsert(queryCaptor.capture(), any(Update.class), eq(MetadataRegistry.class));

        String queryStr = queryCaptor.getValue().toString();
        assertThat(queryStr).contains("payment-service");
        assertThat(queryStr).contains("billing");
    }

    @Test
    void registerIfNew_sameCombinationTwice_upsertsBothTimes() {
        // when
        metadataRegistryService.registerIfNew("payment-service", "billing");
        metadataRegistryService.registerIfNew("payment-service", "billing");

        // then — $setOnInsert makes duplicate upserts safe at DB level
        verify(mongoTemplate, org.mockito.Mockito.times(2))
                .upsert(any(Query.class), any(Update.class), eq(MetadataRegistry.class));
    }

    @Test
    void findAll_delegatesToRepository() {
        // given
        MetadataRegistry registry = MetadataRegistry.builder()
                .serviceName("order-service")
                .domain("commerce")
                .build();
        when(metadataRegistryRepository.findAll()).thenReturn(List.of(registry));

        // when
        List<MetadataRegistry> result = metadataRegistryService.findAll();

        // then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().serviceName()).isEqualTo("order-service");
    }
}
