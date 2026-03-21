const db = db.getSiblingDB('kafka_mono');

// =============================================
// kafka_message_history
// =============================================
// 중복 저장 허용 (멱등성 체크는 앱 레벨에서만 수행)
// {eventKey, eventId, failCount} 복합 인덱스 — existsByEventKeyAndEventIdAndFailCount 조회용
// {eventKey} 인덱스 — eventKey 단독 조회용
// {serviceName, domain} 인덱스 — 서비스/도메인별 조회용
// {status, createdAt} 인덱스 — countFailedByTopicAfter 집계 쿼리용

db.kafka_message_history.createIndex(
    { eventKey: 1, eventId: 1, failCount: 1 },
    { name: "history_eventKey_eventId_failCount" }
);

db.kafka_message_history.createIndex(
    { eventKey: 1 },
    { name: "history_eventKey" }
);

db.kafka_message_history.createIndex(
    { serviceName: 1, domain: 1 },
    { name: "history_serviceName_domain" }
);

db.kafka_message_history.createIndex(
    { status: 1, createdAt: 1 },
    { name: "history_status_createdAt" }
);

// =============================================
// kafka_dlt_message
// =============================================
// {eventKey, eventId} unique — 중복 수신 시 failCount + 1 후 upsert (중복 저장 불가)

db.kafka_dlt_message.createIndex(
    { eventKey: 1, eventId: 1 },
    { name: "dlt_eventKey_eventId", unique: true }
);

db.kafka_dlt_message.createIndex(
    { serviceName: 1, domain: 1 },
    { name: "dlt_serviceName_domain" }
);

db.kafka_dlt_message.createIndex(
    { originalTopic: 1 },
    { name: "dlt_originalTopic" }
);

db.kafka_dlt_message.createIndex(
    { status: 1 },
    { name: "dlt_status" }
);

// =============================================
// user_activity_logs
// =============================================
// {eventKey, eventId} unique — 메시지당 로그 1건 보장 (멱등성 2차 방어)
// userId 인덱스 — 유저별 활동 조회용

db.user_activity_logs.createIndex(
    { eventKey: 1, eventId: 1 },
    { name: "activity_log_eventKey_eventId", unique: true }
);

db.user_activity_logs.createIndex(
    { userId: 1 },
    { name: "activity_log_userId" }
);

// =============================================
// user_activity_stats
// =============================================
// {activityType, targetId} 복합 unique — 사전 집계 upsert 키

db.user_activity_stats.createIndex(
    { activityType: 1, targetId: 1 },
    { name: "activity_stats_type_target", unique: true }
);

// =============================================
// kafka_metadata_registry
// =============================================
// {serviceName, domain} unique — 서비스/도메인 조합 유일 보장

db.kafka_metadata_registry.createIndex(
    { serviceName: 1, domain: 1 },
    { name: "registry_serviceName_domain", unique: true }
);
