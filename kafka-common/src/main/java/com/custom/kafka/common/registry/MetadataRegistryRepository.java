package com.custom.kafka.common.registry;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface MetadataRegistryRepository extends MongoRepository<MetadataRegistry, String> {
}
