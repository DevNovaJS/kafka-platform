package com.custom.kafka.sample.activity;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserActivityLogRepository extends MongoRepository<UserActivityLog, String> {}
