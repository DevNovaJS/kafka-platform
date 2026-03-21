package com.custom.kafka.common.registry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MetadataRegistryServiceTest {

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private MetadataRegistryRepository metadataRegistryRepository;

    @Test
    void init_loadsExistingRegistries() {
        // given
        MetadataRegistry existing = MetadataRegistry.builder()
                .serviceName("order-service")
                .domain("commerce")
                .build();
        when(metadataRegistryRepository.findAll()).thenReturn(List.of(existing));

        MetadataRegistryService service = new MetadataRegistryService(mongoTemplate, metadataRegistryRepository);

        // when
        service.init();

        // then — registerIfNew should skip known combination (no DB call)
        service.registerIfNew("order-service", "commerce");
        verifyNoInteractions(mongoTemplate);
    }

    @Test
    void registerIfNew_newCombination_upsertsToDb() {
        // given
        when(metadataRegistryRepository.findAll()).thenReturn(List.of());
        MetadataRegistryService service = new MetadataRegistryService(mongoTemplate, metadataRegistryRepository);
        service.init();

        // when
        service.registerIfNew("payment-service", "billing");

        // then
        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).upsert(queryCaptor.capture(), any(Update.class), eq(MetadataRegistry.class));

        String queryStr = queryCaptor.getValue().toString();
        assertThat(queryStr).contains("payment-service");
        assertThat(queryStr).contains("billing");
    }

    @Test
    void registerIfNew_sameCombinationTwice_upsertsOnlyOnce() {
        // given
        when(metadataRegistryRepository.findAll()).thenReturn(List.of());
        MetadataRegistryService service = new MetadataRegistryService(mongoTemplate, metadataRegistryRepository);
        service.init();

        // when
        service.registerIfNew("payment-service", "billing");
        service.registerIfNew("payment-service", "billing");

        // then — only one DB call
        verify(mongoTemplate, times(1)).upsert(any(Query.class), any(Update.class), eq(MetadataRegistry.class));
    }

    @Test
    void findAll_delegatesToRepository() {
        // given
        MetadataRegistry registry = MetadataRegistry.builder()
                .serviceName("order-service")
                .domain("commerce")
                .build();
        when(metadataRegistryRepository.findAll()).thenReturn(List.of(registry));
        MetadataRegistryService service = new MetadataRegistryService(mongoTemplate, metadataRegistryRepository);

        // when
        List<MetadataRegistry> result = service.findAll();

        // then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().serviceName()).isEqualTo("order-service");
    }
}
