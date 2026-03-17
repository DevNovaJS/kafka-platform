# Task 05: kafka-common — 메시지 이력 관리

## 목표
Kafka 메시지 처리 이력을 MongoDB에 저장하고 멱등성 체크를 지원하는 계층을 구현한다.

## 패키지
`com.custom.kafka.common.history`

## 구현 대상

### MessageHistory (MongoDB Document)
- **Record 클래스**로 구현 (JDK 25 원칙)
- MongoDB 컬렉션: `kafka_message_history`
- 필드:
  - `messageId` — 메시지 식별자
  - `failCount` — 실패 횟수
  - `status` — 처리 상태 (SUCCESS / FAILED / SKIPPED)
  - 기타 메타데이터 (topic, partition, offset, timestamp 등)
- **Compound Unique Index**: `{messageId, failCount}` — 멱등성 체크 핵심

### MessageHistoryRepository
- Spring Data MongoDB Repository
- `existsByMessageIdAndFailCount(messageId, failCount)` — 멱등성 체크 쿼리

### MessageHistoryService
- 이력 저장 로직
- 멱등성 체크 위임 (`existsByMessageIdAndFailCount`)
- AOP(`KafkaMessageProcessingAspect`)에서 호출

### TopicCount
- **Record 클래스**로 구현
- 토픽별 메시지 카운트 집계용 DTO
- DLT 임계치 모니터링에서 활용

## 산출물
- `MessageHistory.java`
- `MessageHistoryRepository.java`
- `MessageHistoryService.java`
- `TopicCount.java`

## 검증 기준
- MessageHistory 저장/조회 정상 동작
- `existsByMessageIdAndFailCount` true/false 정확히 반환
- compound unique index로 동일 `{messageId, failCount}` 중복 저장 방지
