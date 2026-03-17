# Task 08: kafka-dlt — DLT 수신/저장 앱

## 목표
모든 토픽의 DLT 메시지를 자동으로 수신하여 MongoDB에 저장하는 Spring Boot 앱을 구현한다.

## 패키지
`com.custom.kafka.dlt`

## 구현 대상

### DltKafkaConfig (`config/`)
- DLT 전용 Kafka 컨슈머 설정
- `KafkaContainerFactoryBuilder` (Task 02) 활용하여 리스너 컨테이너 팩토리 구성

### DltConsumer (`consumer/`)
- `@KafkaListener(topicPattern = ".*-DLT")` — 모든 DLT 토픽 자동 구독
- `metadata.max.age.ms`로 신규 DLT 토픽 감지 주기 조절
- 수신한 DLT 메시지를 `DltMessage`로 변환하여 MongoDB 저장
- `kafka.dlt.max-retry-count` 설정값 참조 (`@Value`)
  - 기본값 3, 초과 시 DLT 발송 중단

### DltMessage (`document/`)
- **Record 클래스**로 구현 (JDK 25 원칙)
- MongoDB 컬렉션: `kafka_dlt_message`
- DLT 수신 원문 저장
- 필드: 원본 메시지 payload, 헤더 정보, 수신 시각 등

### DltMessageRepository (`repository/`)
- Spring Data MongoDB Repository
- `DltMessage` CRUD

## DLT 자동 구독 메커니즘
- `topicPattern = ".*-DLT"` 정규식으로 모든 DLT 토픽 매칭
- `metadata.max.age.ms` 설정으로 브로커 메타데이터 갱신 주기 조절
- 신규 토픽 생성 시 자동으로 구독 시작

## 의존 관계
- `kafka-common` 모듈 (`implementation project(':kafka-common')`)

## 산출물
- `DltKafkaConfig.java`
- `DltConsumer.java`
- `DltMessage.java`
- `DltMessageRepository.java`
- Spring Boot 메인 클래스

## 검증 기준
- `.*-DLT` 패턴의 토픽 메시지 자동 수신
- 수신한 DLT 메시지가 MongoDB `kafka_dlt_message` 컬렉션에 저장
- `max-retry-count` 초과 시 추가 DLT 발송 중단
- `./gradlew :kafka-dlt:bootRun --args='--spring.profiles.active=local'` 정상 기동
