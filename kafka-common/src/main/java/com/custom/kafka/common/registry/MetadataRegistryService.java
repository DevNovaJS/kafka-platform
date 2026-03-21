package com.custom.kafka.common.registry;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MetadataRegistryService {
    private final MongoTemplate mongoTemplate;
    private final MetadataRegistryRepository metadataRegistryRepository;

    public void registerIfNew(String serviceName, String domain) {
        mongoTemplate.upsert(
                Query.query(Criteria.where("serviceName").is(serviceName).and("domain").is(domain)),
                new Update()
                        .setOnInsert("serviceName", serviceName)
                        .setOnInsert("domain", domain)
                        .setOnInsert("firstSeenAt", Instant.now()),
                MetadataRegistry.class
        );
    }

    public List<MetadataRegistry> findAll() {
        return metadataRegistryRepository.findAll();
    }
}
