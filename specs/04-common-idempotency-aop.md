# Task 04: kafka-common — 멱등성 AOP 처리

## 목표
컨슈머 메서드에 애너테이션을 붙이면 멱등성 체크, 이력 저장, DLT 발송, 슬랙 알림을 자동 처리하는 AOP를 구현한다.

## 패키지
`com.custom.kafka.common.processor`

## 구현 대상

### KafkaMessageHandler (애너테이션)
- 컨슈머 메서드에 부착하는 마커 애너테이션
- AOP 포인트컷으로 사용

### KafkaMessageProcessingAspect (AOP)
- `@Around` 어드바이스로 아래 흐름을 자동 처리:

```
메시지 수신 (messageId from header, failCount from header)
  → existsByMessageIdAndFailCount(messageId, failCount)
    → true  → SKIPPED 이력 저장 후 return
    → false → joinPoint.proceed() 실행
      → SUCCESS → 이력 저장 (SUCCESS)
      → FAIL    → 이력 저장 (FAILED) → DLT 발송 (failCount+1) → 슬랙 에러 알림
```

## 의존 관계
- `MessageHistoryService` (Task 05) — 멱등성 체크 및 이력 저장
- `DltSender` (Task 06) — 실패 시 DLT 발송
- `SlackNotifier` (Task 07) — 실패 시 슬랙 알림
- `KafkaMessageHeaders` (Task 03) — 헤더에서 messageId, failCount 추출

## 멱등키 정의
- 조합: `{messageId, failCount}`
- MongoDB compound unique index 기반
- failCount가 다르면 같은 messageId도 별도 처리 대상 (DLT 재시도 구분)

## 처리 상태 값
- `SUCCESS` — 정상 처리 완료
- `FAILED` — 처리 실패 (DLT 발송 후)
- `SKIPPED` — 중복 메시지, 처리 생략

## 설계 원칙
- 컨슈머는 비즈니스 로직만 구현
- 멱등성, 이력, DLT, 알림은 모두 AOP가 담당
- AspectJ (`org.aspectj:aspectjweaver`) 직접 의존

## 산출물
- `KafkaMessageHandler.java`
- `KafkaMessageProcessingAspect.java`

## 검증 기준
- 동일 `{messageId, failCount}` 메시지 재수신 시 SKIPPED 처리
- 처리 성공 시 SUCCESS 이력 저장
- 처리 실패 시 FAILED 이력 저장 + DLT 발송 + 슬랙 알림
- 컨슈머 메서드에 `@KafkaMessageHandler`만 붙이면 위 흐름 자동 적용
