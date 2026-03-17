# Task 03: kafka-common — 메시지 헤더

## 목표
Kafka 메시지 헤더에서 사용하는 키 상수와 헤더 유틸리티를 정의한다.

## 패키지
`com.custom.kafka.common.message`

## 구현 대상

### KafkaMessageHeaders
- Kafka 커스텀 헤더 키 상수 정의
- 필수 헤더:
  - `messageId` — 메시지 고유 식별자 (멱등키의 일부)
  - `X-Fail-Count` — 실패 횟수 (DLT 재시도 시 증가, payload 오염 없이 헤더로 전달)
- 헤더 추출/설정 유틸리티 메서드

## 설계 원칙
- failCount는 Kafka Header로만 전달하여 payload 오염 방지
- 멱등키 조합: `{messageId, failCount}` → MongoDB compound unique index에서 사용

## 산출물
- `KafkaMessageHeaders.java`

## 검증 기준
- 상수 값이 정의되어 있고 다른 모듈에서 참조 가능
- 헤더 추출 유틸리티가 정상 동작
