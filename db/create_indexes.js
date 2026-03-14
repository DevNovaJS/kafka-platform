const db = db.getSiblingDB('kafka_mono');

// =============================================
// kafka_message_history
// =============================================
// 중복 저장 허용 (멱등성 체크는 앱 레벨에서만 수행)
// {messageId, failCount} 복합 인덱스 — existsByMessageIdAndFailCount 조회용
// {status, createdAt} 인덱스 — countFailedByTopicAfter 집계 쿼리용

db.kafka_message_history.createIndex(
    { messageId: 1, failCount: 1 },
    { name: "history_messageId_failCount" }
);

db.kafka_message_history.createIndex(
    { status: 1, createdAt: 1 },
    { name: "history_status_createdAt" }
);

// =============================================
// kafka_dlt_message
// =============================================
// messageId unique — 중복 수신 시 failCount + 1 후 upsert (중복 저장 불가)

db.kafka_dlt_message.createIndex(
    { messageId: 1 },
    { name: "dlt_messageId", unique: true }
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
// messageId unique — 메시지당 로그 1건 보장 (멱등성 2차 방어)
// userId 인덱스 — 유저별 활동 조회용

db.user_activity_logs.createIndex(
    { messageId: 1 },
    { name: "activity_log_messageId", unique: true }
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
