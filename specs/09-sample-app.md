# Task 09: kafka-sample — 사용자 활동 샘플 컨슈머 앱

## 목표
사용자 활동 이벤트를 수신하여 처리하는 샘플 Spring Boot 앱을 구현한다.

## 패키지
`com.custom.kafka.sample`

## 구현 대상

### SampleKafkaConfig (`config/`)
- 샘플 컨슈머 전용 Kafka 설정
- `KafkaContainerFactoryBuilder` (Task 02) 활용

### SampleKafkaListenerProperties (`config/`)
- `CustomKafkaListenerProperties` (Task 02) 기반 모듈 전용 리스너 속성
- 환경 변수로 오버라이드 가능 (prod 프로파일):
  - `KAFKA_ACTIVITY_CONCURRENCY` (기본 3)
  - `KAFKA_ACTIVITY_ACK_MODE` (기본 BATCH)
  - `KAFKA_ACTIVITY_SYNC_COMMITS` (기본 true)
  - `KAFKA_ACTIVITY_POLL_TIMEOUT` (기본 5000)
  - `KAFKA_ACTIVITY_BATCH_LISTENER` (기본 false)

### UserActivityEvent (`activity/`)
- **Record 클래스** — 수신 이벤트 DTO

### UserActivityLog (`activity/`)
- **Record 클래스** — 활동 로그

### UserActivityStats (`activity/`)
- **Record 클래스** — 활동 통계

### UserActivityService (`activity/`)
- 사용자 활동 비즈니스 로직 처리

### UserActivityLogRepository (`activity/`)
- Spring Data MongoDB Repository

### ActivityConsumer (`consumer/`)
- `@KafkaListener`로 사용자 활동 토픽 구독
- `@KafkaMessageHandler` (Task 04) 부착 → 멱등성/이력/DLT/알림 자동 처리
- 비즈니스 로직만 구현 (AOP가 나머지 담당)

## 의존 관계
- `kafka-common` 모듈 (`implementation project(':kafka-common')`)

## 산출물
- `SampleKafkaConfig.java`
- `SampleKafkaListenerProperties.java`
- `UserActivityEvent.java`
- `UserActivityLog.java`
- `UserActivityStats.java`
- `UserActivityService.java`
- `UserActivityLogRepository.java`
- `ActivityConsumer.java`
- Spring Boot 메인 클래스

## 검증 기준
- 사용자 활동 메시지 정상 수신 및 처리
- `@KafkaMessageHandler`에 의한 멱등성 처리 동작 확인
- 환경 변수로 리스너 속성 오버라이드 가능
- `./gradlew :kafka-sample:bootRun --args='--spring.profiles.active=local'` 정상 기동
