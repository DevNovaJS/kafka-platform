package com.custom.kafka.dlt.repository;

import com.custom.kafka.dlt.document.DltMessage;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface DltMessageRepository extends MongoRepository<DltMessage, String> {
    Optional<DltMessage> findByEventKeyAndEventId(String eventKey, String eventId);
}
