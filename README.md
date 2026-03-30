# Kafka Platform

Kafka 컨슈머 애플리케이션을 위한 공통 처리 프레임워크.
**멱등성 보장, 메시지 이력 관리, Dead Letter Topic(DLT) 처리, Slack 알림**을 AOP 기반으로 자동화하여,
컨슈머 개발자가 비즈니스 로직에만 집중 가능.

| 항목 | 내용 |
|---|---|
| **언어** | Java 25 |
| **프레임워크** | Spring Boot 4.0.3, Spring Kafka, Spring Data MongoDB |
| **빌드** | Gradle 9.3.1 (멀티모듈) |
| **DB** | MongoDB |
| **알림** | Slack Webhook (Block Kit) |

## 프로젝트 구조

```
kafka-platform/
├── kafka-common/    # 공통 라이브러리 — 멱등성, 이력, DLT, 알림 등 핵심 기능
├── kafka-dlt/       # DLT 메시지 수신/저장/관리
└── kafka-sample/    # 컨슈머 구현 예제 (사용자 활동 이벤트)
```

- `kafka-common`은 라이브러리 모듈로, 컨슈머 앱에서 `implementation project(':kafka-common')`으로 의존함.

---

## 활용 방안

### 컨슈머 개발 단순화

`@KafkaMessageHandler` 애너테이션 하나로 멱등성, 이력, DLT, 알림이 자동 적용됨. 컨슈머 개발자는 비즈니스 로직만 작성하면 됨.

```java
@KafkaListener(topics = "user-activity", groupId = "activity-consumer")
@KafkaMessageHandler
public void consume(ConsumerRecord<String, String> record) {
    KafkaEventMessage<UserActivityEvent> message = objectMapper.readValue(
        record.value(), new TypeReference<>() {}
    );
    userActivityService.process(message.eventKey(), message.eventId(), message.payload());
}
```

### 메시지 유실 및 중복 처리 방지

- AOP 기반 멱등성 체크로 동일 메시지의 중복 처리를 원천 차단함.
- 실패 메시지는 DLT로 자동 라우팅되어 유실 없이 보존됨.
- 모든 처리 결과가 MongoDB에 이력으로 남아 추적 가능.

### 장애 인지 자동화

- ERROR 로그 발생 시 Slack으로 즉시 알림이 전송됨. 별도 모니터링 설정 불필요.
- DLT 메시지가 임계치를 초과하면 자동으로 경고가 발송됨.
- Rate Limiting으로 알림 폭주를 방지하면서도 중요한 에러를 놓치지 않음.

### 신규 컨슈머 확장 용이

- `kafka-common` 의존성 추가 후 `@KafkaMessageHandler`를 적용하면 즉시 모든 공통 기능 사용 가능.
- DLT 토픽은 패턴 기반 자동 구독이므로 신규 토픽에 대한 별도 설정 불필요.
- `KafkaContainerFactoryBuilder`로 리스너 설정을 선언적으로 구성 가능.

---

## 정책

### 메시지 구조

- 모든 메시지는 `KafkaEventMessage<T>` 포맷을 따름: `eventKey`(작업 단위) + `eventId`(작업 식별자) + `payload`(비즈니스 데이터).
- `serviceName`, `domain`, `failCount` 등 운영 메타데이터는 Kafka Header로 전달하여 페이로드 오염 없음.

### 멱등성

- 멱등키는 `{eventKey, eventId, failCount}` 조합.
- MongoDB compound index로 관리하며, 동일 키의 메시지는 `SKIPPED` 처리됨.
- 재시도 시 `failCount`가 증가하므로 동일 메시지의 재처리 가능.

### DLT (Dead Letter Topic)

- 처리 실패 메시지는 원본 토픽명 + `-dlt` 접미사 토픽으로 자동 발송됨.
- 최대 재시도 횟수(`kafka.dlt.max-retry-count`, 기본 3) 초과 시 `PERMANENTLY_FAILED`로 전환하여 무한 재시도 방지.
- DLT 수신 원문은 MongoDB에 보존됨.

### Slack 알림 Rate Limiting

- `loggerName:exceptionClassName` 키 기반으로 동일 에러의 중복 알림 억제.
- 기본 60초 간격으로 제한하며, 재발송 시 생략된 건수 포함.

### 설정 프로파일

| 파일 | 역할 |
|---|---|
| `application.yml` | 환경 무관 공통 설정 |
| `application-local.yml` | 로컬 전용 (localhost URL) |
| `application-prod.yml` | 운영 전용 (환경 변수 참조) |

---

## 기능 상세

### AOP 기반 멱등성 처리

컨슈머 메서드에 `@KafkaMessageHandler`를 붙이면 `KafkaMessageProcessingAspect`가 다음 흐름을 자동 처리함.

```
메시지 수신
  → 멱등키(eventKey + eventId + failCount) 중복 검사
    → 이미 처리됨 → SKIPPED 이력 저장, 메시지 무시
    → 신규 메시지 → 비즈니스 로직 실행
      → 성공 → SUCCESS 이력 저장, 메타데이터 레지스트리 등록
      → 실패 → FAILED 이력 저장, DLT 발송, Slack 알림
```

### 메시지 이력 관리

모든 메시지 처리 결과가 MongoDB `kafka_message_history` 컬렉션에 자동 기록됨.

| 필드 | 설명 |
|---|---|
| `eventKey` / `eventId` | 비즈니스 식별자 (페이로드에서 추출) |
| `failCount` | 재시도 횟수 (Kafka Header `X-Fail-Count`) |
| `status` | `SUCCESS`, `FAILED`, `SKIPPED` |
| `topic` / `partition` / `offset` | Kafka 메시지 위치 |
| `serviceName` / `domain` | 운영 메타데이터 (Kafka Header) |
| `errorMessage` | 실패 시 에러 메시지 |

### DLT 자동 처리

- `DltSender`가 실패 메시지를 DLT 토픽으로 라우팅함. `failCount`를 증가시킨 헤더와 함께 전달됨.
- `DltConsumer`가 `topicPattern = ".*-dlt"` 패턴으로 모든 DLT 토픽을 자동 구독함. 신규 DLT 토픽이 추가되어도 별도 설정 없이 자동 감지됨.
- `max-retry-count`(기본 3) 초과 시 `PERMANENTLY_FAILED` 상태로 전환되어 무한 재시도 방지.
- DLT 수신 원문은 MongoDB `kafka_dlt_message` 컬렉션에 저장됨.

### Slack 알림

| 상황 | 알림 내용 |
|---|---|
| 애플리케이션 ERROR 로그 발생 | 에러 메시지, 스택트레이스 요약(상위 N줄), 발생 위치(로거, 스레드), 시각 |
| 토픽별 DLT 메시지 임계치 초과 | 대상 토픽, 누적 건수, 임계치 설정값 |

- ERROR 로그 알림은 Logback Appender로 동작하여 애플리케이션 기동 시 자동 등록됨. 별도 설정 파일 불필요.
- 동일 에러는 Rate Limiting(기본 60초)으로 중복 발송이 억제되며, 재발송 시 억제된 건수를 함께 표시함.
- DLT 임계치 알림은 `@Scheduled`로 주기적으로 검사하여 발송됨.
- 모든 알림은 Block Kit 포맷으로 전송됨.

### 서비스/도메인 메타데이터 레지스트리

메시지 처리 성공 시 `serviceName + domain` 조합을 `kafka_metadata_registry` 컬렉션에 자동 등록함. 인메모리 캐시로 이미 등록된 조합은 DB 호출 없이 건너뜀.

### Kafka 리스너 팩토리 빌더

`KafkaContainerFactoryBuilder`로 리스너 컨테이너 팩토리를 선언적으로 구성 가능.

```java
ConcurrentKafkaListenerContainerFactory<String, String> factory =
    KafkaContainerFactoryBuilder
        .from(kafkaProperties, customListenerProperties)
        .errorHandler(commonErrorHandler)
        .recordInterceptor(headerInterceptor)
        .taskExecutor(virtualThreadExecutor)
        .build();
```

---

## 빌드 및 실행

```bash
# 전체 빌드
./gradlew build

# 모듈별 빌드
./gradlew :kafka-common:build
./gradlew :kafka-dlt:build
./gradlew :kafka-sample:build

# 앱 실행 (프로파일 필수)
./gradlew :kafka-dlt:bootRun --args='--spring.profiles.active=local'
./gradlew :kafka-sample:bootRun --args='--spring.profiles.active=local'

# 테스트
./gradlew test

# Native Image 빌드 (GraalVM)
./gradlew :kafka-dlt:nativeCompile
./gradlew :kafka-sample:nativeCompile
```

## 운영 환경 변수

| 환경 변수 | 대상 모듈 | 설명 |
|---|---|---|
| `KAFKA_BOOTSTRAP_SERVERS` | dlt, sample | Kafka 브로커 주소 |
| `MONGODB_URI` | dlt, sample | MongoDB 연결 URI |
| `SLACK_WEBHOOK_URL` | dlt, sample | Slack Webhook URL |

## 주요 설정

| 설정 키 | 기본값 | 설명 |
|---|---|---|
| `kafka.dlt.max-retry-count` | 3 | DLT 최대 재시도 횟수 |
| `slack.error-log.enabled` | true | ERROR 로그 Slack 알림 활성화 |
| `slack.error-log.rate-limit-seconds` | 60 | 동일 에러 중복 발송 방지 간격(초) |
| `slack.error-log.stacktrace-lines` | 5 | 스택트레이스 포함 줄 수 |
