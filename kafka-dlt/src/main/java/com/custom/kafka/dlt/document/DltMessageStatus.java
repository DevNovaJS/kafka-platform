package com.custom.kafka.dlt.document;

public enum DltMessageStatus {
    PENDING,            // DLT 수신 완료, 재처리 대기
    IN_PROGRESS,        // 재처리 진행 중
    COMPLETED,          // 재처리 성공
    PERMANENTLY_FAILED  // maxRetryCount 초과 — 재처리 불가
}
