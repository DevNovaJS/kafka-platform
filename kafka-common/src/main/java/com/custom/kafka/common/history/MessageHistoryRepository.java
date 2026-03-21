package com.custom.kafka.common.history;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface MessageHistoryRepository extends MongoRepository<MessageHistory, String> {
    boolean existsByEventKeyAndEventIdAndFailCount(String eventKey, String eventId, int failCount);
}
