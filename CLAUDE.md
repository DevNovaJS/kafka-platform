# CLAUDE.md

## Project Structure

- **kafka-common** — java-library. 멱등성 AOP, 메시지 이력, DLT 발송, 슬랙 알림 등 공통 기능
- **kafka-dlt** — Spring Boot app. `topicPattern=".*-DLT"`로 DLT 메시지 수신/저장
- **kafka-sample** — Spring Boot app. 사용자 활동 샘플 컨슈머

## Architecture

### 멱등성 처리 (KafkaMessageProcessingAspect)

`@KafkaMessageHandler` → AOP가 멱등성/이력/DLT/알림 자동 처리. 컨슈머는 비즈니스 로직만 구현.

- 멱등키: `{eventKey, eventId, failCount}` — eventKey/eventId는 페이로드(`KafkaEventMessage`), failCount는 Header(`X-Fail-Count`)
- serviceName/domain: Header (`X-Service-Name`, `X-Domain`)
- 중복 → SKIPPED, 성공 → SUCCESS + MetadataRegistry 등록, 실패 → FAILED + DLT 발송 + 슬랙 알림

### 슬랙 알림

| 트리거 | 클래스 |
|---|---|
| ERROR 로그 (Logback Appender) | `SlackErrorLogAppender` — `@Component`, `@PostConstruct`로 root logger 등록, Caffeine 기반 rate limiting, 발송 실패 시 `addError()` (재귀 방지) |
| DLT 임계치 (`@Scheduled`) | `SlackNotifier.sendDltThresholdAlert()` |

### DB: MongoDB

- `kafka_message_history` — 메시지 처리 이력 (멱등성 체크)
- `kafka_dlt_message` — DLT 수신 원문
- `kafka_metadata_registry` — 서비스/도메인 조합 (인메모리 Set 캐시 + DB)