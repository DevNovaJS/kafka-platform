package com.custom.kafka.common.registry;

import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Builder
@Document("kafka_metadata_registry")
@CompoundIndex(def = "{'serviceName': 1, 'domain': 1}", unique = true)
public record MetadataRegistry(
        @Id String id,
        String serviceName,
        String domain,
        Instant firstSeenAt
) {
}
