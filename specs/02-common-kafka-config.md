# Task 02: kafka-common — Kafka 설정 클래스

## 목표
Kafka 컨슈머/프로듀서 공통 설정을 제공하는 클래스들을 구현한다.

## 패키지
`com.custom.kafka.common.config`

## 구현 대상

### KafkaContainerFactoryBuilder
- Kafka 리스너 컨테이너 팩토리를 빌드하는 유틸리티
- 각 Boot 모듈이 자체 설정에 맞게 컨테이너 팩토리를 생성할 수 있도록 지원
- concurrency, ackMode, syncCommits, pollTimeout, batchListener 등 설정 반영

### KafkaProducerConfig
- Kafka 프로듀서 공통 설정
- DLT 발송 등에 사용되는 KafkaTemplate 구성

### CustomKafkaListenerProperties
- **Record 클래스**로 구현 (JDK 25 원칙)
- 모듈별 커스텀 가능한 리스너 속성:
  - `concurrency` — 동시성
  - `ackMode` — ACK 모드
  - `syncCommits` — 동기 커밋 여부
  - `pollTimeout` — poll timeout (ms)
  - `batchListener` — 배치 리스너 여부

## 설계 원칙
- kafka-common은 라이브러리이므로 `@AutoConfiguration` 또는 수동 Bean 등록 방식
- Boot 모듈(`kafka-dlt`, `kafka-sample`)이 각자의 설정 파일에서 이 빌더/Properties를 활용

## 산출물
- `KafkaContainerFactoryBuilder.java`
- `KafkaProducerConfig.java`
- `CustomKafkaListenerProperties.java`

## 검증 기준
- `./gradlew :kafka-common:build` 성공
- 다른 모듈에서 import하여 컨테이너 팩토리 생성 가능
