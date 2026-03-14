package com.custom.kafka.common.history;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface MessageHistoryRepository extends MongoRepository<MessageHistory, String> {
    boolean existsByMessageIdAndFailCount(String messageId, int failCount);
}
