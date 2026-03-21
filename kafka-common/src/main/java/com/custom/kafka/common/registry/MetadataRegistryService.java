package com.custom.kafka.common.registry;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class MetadataRegistryService {
    private final MongoTemplate mongoTemplate;
    private final MetadataRegistryRepository metadataRegistryRepository;
    private final Set<String> knownCombinations = ConcurrentHashMap.newKeySet();

    @PostConstruct
    void init() {
        metadataRegistryRepository.findAll().forEach(
                registry -> knownCombinations.add(toKey(registry.serviceName(), registry.domain()))
        );
        log.info("메타데이터 레지스트리 초기화 완료: {}건 로드", knownCombinations.size());
    }

    public void registerIfNew(String serviceName, String domain) {
        String key = toKey(serviceName, domain);
        if (knownCombinations.contains(key)) {
            return;
        }

        mongoTemplate.upsert(
                Query.query(Criteria.where("serviceName").is(serviceName).and("domain").is(domain)),
                new Update()
                        .setOnInsert("serviceName", serviceName)
                        .setOnInsert("domain", domain)
                        .setOnInsert("firstSeenAt", Instant.now()),
                MetadataRegistry.class
        );

        knownCombinations.add(key);
        log.info("새 서비스/도메인 등록: serviceName={}, domain={}", serviceName, domain);
    }

    public List<MetadataRegistry> findAll() {
        return metadataRegistryRepository.findAll();
    }

    private static String toKey(String serviceName, String domain) {
        return serviceName + ":" + domain;
    }
}
