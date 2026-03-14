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

# 앱 실행 (Boot 모듈만)
./gradlew :kafka-dlt:bootRun
./gradlew :kafka-sample:bootRun

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
│       ├── message/       # KafkaMessageHeaders
│       ├── processor/     # KafkaMessageHandler (애너테이션), KafkaMessageProcessingAspect (AOP)
│       ├── history/       # MessageHistory (MongoDB Document), MessageHistoryRepository, MessageHistoryService, TopicCount
│       ├── dlt/           # DltSender
│       └── notification/  # SlackNotifier, DltThresholdMonitor
├── kafka-dlt/             # spring-boot app — DLT 메시지 수신/저장
│   └── com.custom.kafka.dlt
│       ├── config/        # DltKafkaConfig
│       ├── consumer/      # DltConsumer (@KafkaListener topicPattern=".*\\.DLT")
│       ├── document/      # DltMessage (MongoDB Document)
│       └── repository/    # DltMessageRepository
└── kafka-sample/          # spring-boot app — 결제/상품 샘플 컨슈머
    └── com.custom.kafka.sample
        ├── config/        # SampleKafkaConfig, SampleKafkaListenerProperties
        ├── event/         # PaymentEvent, ProductEvent
        ├── payment/       # PaymentConsumer
        └── product/       # ProductConsumer
```

- `kafka-common`: 라이브러리. Boot 앱 아님. 다른 모듈이 `implementation project(':kafka-common')`으로 의존
- `kafka-dlt`, `kafka-sample`: 각각 독립 실행 가능한 Spring Boot 앱

## Architecture

### 멱등성 처리 흐름 (AOP — KafkaMessageProcessingAspect)

컨슈머 메서드에 `@KafkaMessageHandler`를 붙이면 AOP가 아래 흐름을 자동 처리한다.

```
메시지 수신 (messageId from header, failCount from header)
  → existsByMessageIdAndFailCount(messageId, failCount)
    → true  → SKIPPED 이력 저장 후 return
    → false → joinPoint.proceed() 실행
      → SUCCESS → 이력 저장 (SUCCESS)
      → FAIL    → 이력 저장 (FAILED) → DLT 발송 (failCount+1) → 슬랙 에러 알림
```

- 멱등키: `{messageId, failCount}` MongoDB compound unique index
- failCount는 Kafka Header (`X-Fail-Count`)로 전달, payload 오염 없음
- 컨슈머는 비즈니스 로직만 구현. 멱등성/이력/DLT/알림은 AOP가 담당

### DB: MongoDB

- `kafka_message_history` — 모든 메시지 처리 이력 (멱등성 체크 용도)
- `kafka_dlt_message` — DLT 수신 원문 저장

### DLT 자동 구독

- `@KafkaListener(topicPattern = ".*\\.DLT")` → 신규 토픽 DLT 자동 수신
- `metadata.max.age.ms`로 감지 주기 조절

### 슬랙 알림

- 에러 발생 시: topic/partition/offset/messageId/failCount/exception 즉시 발송 (`@Async`)
- DLT 임계치: 시간 윈도우 기반 `@Scheduled` 체크 → 초과 시 발송

## JDK 25 적용 원칙

- **Record**: Document, Event, DTO, Properties 전반 (MessageHistory, DltMessage, PaymentEvent, ProductEvent, TopicCount, CustomKafkaListenerProperties 등)
- **Pattern matching switch**: 이벤트 상태/타입 분기 (PaymentConsumer, ProductConsumer)
- **Virtual Thread**: Kafka listener thread pool (`spring.threads.virtual.enabled=true`, `SimpleAsyncTaskExecutor`)
- **Text block**: 슬랙 메시지 템플릿 (SlackNotifier)

## 설정 파일

- 각 Boot 모듈의 `src/main/resources/application.yml`에서 MongoDB, Kafka, 슬랙 webhook, DLT 임계치 등 설정
- `CustomKafkaListenerProperties`로 concurrency/ackMode/syncCommits/pollTimeout/batchListener 모듈별 커스텀 가능
- `kafka.dlt.max-retry-count`: DLT 최대 재시도 횟수 (기본값 3, 초과 시 발송 중단)
