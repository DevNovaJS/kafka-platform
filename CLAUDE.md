# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
# 전체 빌드
./gradlew build

# 모듈별 빌드
./gradlew :kafka-common:build
./gradlew :kafka-dlt:build
./gradlew :kafka-sample:build

# 앱 실행 (Boot 모듈만) — 프로파일 필수
./gradlew :kafka-dlt:bootRun --args='--spring.profiles.active=local'
./gradlew :kafka-sample:bootRun --args='--spring.profiles.active=local'

# 테스트
./gradlew test                    # 전체
./gradlew :kafka-common:test      # 공통모듈만

# Native image (GraalVM)
./gradlew :kafka-dlt:nativeCompile
./gradlew :kafka-sample:nativeCompile
```

## Tech Stack

- **Java 25** (toolchain)
- **Gradle 9.3.1** (멀티모듈)
- **Spring Boot 4.0.3**
- **Spring Data MongoDB** (메시지 이력 + DLT 저장)
- **Spring Kafka** (컨슈머 공통 처리)
- **Lombok** (annotation processor)
- **AspectJ** (`org.aspectj:aspectjweaver` 직접 의존성 — spring-boot-starter-aop 없음)
- **GraalVM native image** support

## Project Structure (Gradle Multi-Module)

```
kafka-platform/
├── kafka-common/          # java-library — 모든 컨슈머가 의존하는 공통모듈
│   └── com.custom.kafka.common
│       ├── config/        # KafkaContainerFactoryBuilder, KafkaProducerConfig, CustomKafkaListenerProperties
│       ├── message/       # KafkaEventMessage, KafkaMessageHeaders, CommonConstants (DLT_TOPIC_SUFFIX, SLACK_TIME_FORMAT)
│       ├── processor/     # KafkaMessageHandler (애너테이션), KafkaMessageProcessingAspect (AOP)
│       ├── history/       # MessageHistory (MongoDB Document), MessageHistoryRepository, MessageHistoryService, TopicCount
│       ├── registry/      # MetadataRegistry (MongoDB Document), MetadataRegistryRepository, MetadataRegistryService
│       ├── dlt/           # DltSender
│       └── notification/  # SlackNotifier, DltThresholdMonitor, SlackErrorLogAppender
├── kafka-dlt/             # spring-boot app — DLT 메시지 수신/저장
│   └── com.custom.kafka.dlt
│       ├── config/        # DltKafkaConfig
│       ├── consumer/      # DltConsumer (@KafkaListener topicPattern=".*-DLT")
│       ├── document/      # DltMessage (MongoDB Document)
│       └── repository/    # DltMessageRepository
└── kafka-sample/          # spring-boot app — 사용자 활동 샘플 컨슈머
    └── com.custom.kafka.sample
        ├── config/        # SampleKafkaConfig, SampleKafkaListenerProperties
        ├── activity/      # UserActivityEvent, UserActivityLog, UserActivityStats, UserActivityService, UserActivityLogRepository
        └── consumer/      # ActivityConsumer
```

- `kafka-common`: 라이브러리. Boot 앱 아님. 다른 모듈이 `implementation project(':kafka-common')`으로 의존
- `kafka-dlt`, `kafka-sample`: 각각 독립 실행 가능한 Spring Boot 앱

## Architecture

### 멱등성 처리 흐름 (AOP — KafkaMessageProcessingAspect)

컨슈머 메서드에 `@KafkaMessageHandler`를 붙이면 AOP가 아래 흐름을 자동 처리한다.

```
메시지 수신 (serviceName/domain from header, failCount from header)
  → eventKey/eventId from JSON payload
  → existsByEventKeyAndEventIdAndFailCount(eventKey, eventId, failCount)
    → true  → SKIPPED 이력 저장 후 return
    → false → joinPoint.proceed() 실행
      → SUCCESS → 이력 저장 (SUCCESS) → MetadataRegistry 등록
      → FAIL    → 이력 저장 (FAILED) → DLT 발송 (failCount+1) → log.error() → SlackErrorLogAppender 경유 슬랙 알림
```

- 멱등키: `{eventKey, eventId, failCount}` MongoDB compound index (non-unique — 중복 저장 허용, 멱등성 체크는 앱 레벨 `existsByEventKeyAndEventIdAndFailCount`로 수행)
- eventKey(작업 단위) + eventId(작업 식별 단위)는 페이로드(`KafkaEventMessage`)에서 추출 — 비즈니스 식별자
- serviceName/domain은 Kafka Header (`X-Service-Name`, `X-Domain`) — 운영 메타데이터
- failCount는 Kafka Header (`X-Fail-Count`)로 전달, payload 오염 없음
- 컨슈머는 비즈니스 로직만 구현. 멱등성/이력/DLT/알림은 AOP가 담당

### 서비스/도메인 레지스트리 (MetadataRegistry)

- `kafka_metadata_registry` 컬렉션에 서비스/도메인 조합 저장
- 인메모리 `Set<String>` 캐시 — 이미 등록된 조합은 DB 호출 없이 skip
- `@PostConstruct`에서 기존 레지스트리 로드 → Set 초기화
- AOP 성공 처리 시 `registerIfNew(serviceName, domain)` 호출

### DB: MongoDB

- `kafka_message_history` — 모든 메시지 처리 이력 (멱등성 체크 용도)
- `kafka_dlt_message` — DLT 수신 원문 저장
- `kafka_metadata_registry` — 서비스/도메인 조합 레지스트리

### DLT 자동 구독

- `@KafkaListener(topicPattern = ".*-DLT")` → 신규 토픽 DLT 자동 수신
- `metadata.max.age.ms`로 감지 주기 조절

### 슬랙 알림

세 가지 경로로 Slack 알림이 발송된다.

| 경로 | 트리거 | 담당 클래스 | 포맷 |
|---|---|---|---|
| Kafka 처리 오류 + 모든 ERROR 로그 | Logback Appender (ERROR 레벨) 자동 감지 | `SlackErrorLogAppender` → 주입받은 `RestClient` 직접 발송 | Block Kit |
| DLT 임계치 초과 | `@Scheduled` 주기 체크 | `SlackNotifier.sendDltThresholdAlert()` | Block Kit |

#### ERROR 로그 자동 알림 (SlackErrorLogAppender)

`UnsynchronizedAppenderBase<ILoggingEvent>`를 상속한 Custom Logback Appender. `@Component` + `@PostConstruct`로 root logger에 프로그래밍 방식으로 등록한다 (logback XML 없음).

```
ERROR 로그 발생
  → SlackErrorLogAppender.append(event)
  → ERROR 레벨 + webhookUrl 체크
  → Block Kit JSON 구성 → 자체 RestClient로 Slack Webhook 직접 발송
  → 발송 실패 시 addError() (Logback 내부 오류 처리)
```

**설계 원칙:**
- `@Component` Spring Bean — `@PostConstruct`에서 root logger에 등록, `RestClient`는 Spring Bean 주입
- `SlackNotifier`를 거치지 않음 — 주입받은 `RestClient`로 Slack Webhook 직접 발송
- 발송 실패 시 `addError()` 사용 — `log.error()` 호출 금지 (재귀 방지)
- Rate Limiting: Caffeine Cache(`Cache<String, AtomicInteger>`) 기반 — `loggerName:exceptionClassName` 키로 동일 에러는 `rate-limit-seconds` 이내 발송 억제, 재발송 시 생략 건수 포함

#### Slack 메시지 포맷 (Block Kit 공통)

모든 알림이 Block Kit 구조를 사용한다.

```
header        → 알림 유형 아이콘 + 제목
section fields → 메타데이터 (2열 레이아웃: 앱이름, 로거/토픽, 스레드, 시각 등)
section text   → 에러 메시지 (코드블록)
section text   → 스택트레이스 요약 (코드블록, 상위 N줄만)
```

#### 설정 항목

```yaml
slack:
  error-log:
    enabled: true                    # Appender 활성화 여부
    rate-limit-seconds: 60           # 동일 메시지 중복 발송 방지 간격
    stacktrace-lines: 5              # 스택트레이스 포함 줄 수
```

## JDK 25 적용 원칙

- **Record**: Document, Event, DTO, Properties 전반 (MessageHistory, DltMessage, UserActivityEvent, UserActivityLog, UserActivityStats, TopicCount, CustomKafkaListenerProperties 등)
- **Virtual Thread**: Kafka listener thread pool (`spring.threads.virtual.enabled=true`, `SimpleAsyncTaskExecutor`)
- **Text block**: 슬랙 메시지 템플릿 (SlackNotifier)

## 설정 파일 구조 (Spring Profile 분리)

각 Boot 모듈(`kafka-dlt`, `kafka-sample`)은 세 파일로 설정을 분리한다.

| 파일 | 역할 |
|---|---|
| `application.yml` | 환경 무관 공통 설정 (포트, consumer 직렬화, 스레드, management, DLT 임계치 등) |
| `application-local.yml` | 로컬 전용 (localhost URL, Slack URL 선택) |
| `application-prod.yml` | 운영 전용 (환경 변수 참조, Slack URL 필수) |

### 프로파일 활성화

```bash
# 로컬 실행
./gradlew :kafka-dlt:bootRun --args='--spring.profiles.active=local'
./gradlew :kafka-sample:bootRun --args='--spring.profiles.active=local'

# 운영 실행 (환경 변수 필수)
--spring.profiles.active=prod
```

### 환경 변수 (prod 프로파일)

| 환경 변수 | 대상 모듈 | 설명 |
|---|---|---|
| `KAFKA_BOOTSTRAP_SERVERS` | dlt, sample | Kafka 브로커 주소 |
| `MONGODB_URI` | dlt, sample | MongoDB 연결 URI |
| `SLACK_WEBHOOK_URL` | dlt, sample | Slack Webhook URL (필수) |
| `KAFKA_ACTIVITY_CONCURRENCY` | sample | activity 컨슈머 동시성 (기본 3) |
| `KAFKA_ACTIVITY_ACK_MODE` | sample | activity ACK 모드 (기본 BATCH) |
| `KAFKA_ACTIVITY_SYNC_COMMITS` | sample | activity 동기 커밋 (기본 true) |
| `KAFKA_ACTIVITY_POLL_TIMEOUT` | sample | activity poll timeout ms (기본 5000) |
| `KAFKA_ACTIVITY_BATCH_LISTENER` | sample | activity 배치 리스너 여부 (기본 false) |

### 주요 설정 항목

- `CustomKafkaListenerProperties`로 concurrency/ackMode/syncCommits/pollTimeout/batchListener 모듈별 커스텀 가능
- `kafka.dlt.max-retry-count`: DLT 최대 재시도 횟수 (기본값 3, 초과 시 발송 중단) — `DltConsumer` `@Value` 참조
- `slack.error-log.enabled`: ERROR 로그 Slack 자동 알림 활성화 (기본 true)
- `slack.error-log.rate-limit-seconds`: 동일 에러 중복 발송 방지 간격 (기본 60)
- `slack.error-log.stacktrace-lines`: 스택트레이스 포함 줄 수 (기본 5)
