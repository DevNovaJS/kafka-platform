// kafka_message_history
// SUCCESS/FAILED 에만 unique 제약 적용 (partial index)
// SKIPPED는 동일 {messageId, failCount}로 중복 저장 허용
//
// 기존 non-partial unique index가 있으면 먼저 drop 후 실행
// db.kafka_message_history.dropIndex("messageId_failCount_unique")

db.kafka_message_history.createIndex(
    { messageId: 1, failCount: 1 },
    {
        name: "messageId_failCount_unique",
        unique: true,
        partialFilterExpression: { status: { $in: ["SUCCESS", "FAILED"] } }
    }
);
