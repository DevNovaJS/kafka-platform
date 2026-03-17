# Task 06: kafka-common — DLT 발송

## 목표
메시지 처리 실패 시 해당 메시지를 DLT(Dead Letter Topic)로 발송하는 공통 컴포넌트를 구현한다.

## 패키지
`com.custom.kafka.common.dlt`

## 구현 대상

### DltSender
- 실패한 메시지를 원본 토픽의 DLT(`{원본토픽}-DLT`)로 발송
- 발송 시 Kafka Header에 `failCount + 1` 값을 설정 (Task 03 KafkaMessageHeaders 활용)
- KafkaTemplate을 이용한 메시지 전송

## DLT 토픽 네이밍 규칙
- 원본 토픽명에 `-DLT` 접미사 추가
- 예: `user-activity` → `user-activity-DLT`

## 의존 관계
- `KafkaProducerConfig` (Task 02) — KafkaTemplate
- `KafkaMessageHeaders` (Task 03) — failCount 헤더 설정

## 설계 원칙
- failCount를 Kafka Header로만 전달하여 payload 오염 방지
- AOP(Task 04)에서 실패 시 자동 호출

## 산출물
- `DltSender.java`

## 검증 기준
- 실패 메시지가 `{원본토픽}-DLT`로 정상 발송
- 발송된 메시지의 `X-Fail-Count` 헤더가 기존 failCount + 1
- 원본 메시지 payload 변경 없음
