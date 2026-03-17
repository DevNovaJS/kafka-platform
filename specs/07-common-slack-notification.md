# Task 07: kafka-common — 슬랙 알림

## 목표
에러 발생 시 즉시 슬랙 알림과 DLT 임계치 초과 시 주기적 알림을 구현한다.

## 패키지
`com.custom.kafka.common.notification`

## 구현 대상

### SlackNotifier
- 에러 발생 시 슬랙 Webhook으로 즉시 알림 발송
- **@Async** 비동기 처리
- 알림 내용:
  - topic
  - partition
  - offset
  - messageId
  - failCount
  - exception 정보
- **Text block** 활용 (JDK 25 원칙) — 슬랙 메시지 템플릿

### DltThresholdMonitor
- **@Scheduled** 기반 주기적 DLT 임계치 체크
- 시간 윈도우 기반으로 DLT 메시지 수 집계
- 임계치 초과 시 슬랙 알림 발송
- `TopicCount` (Task 05) 활용

## 설정 항목
- Slack Webhook URL (환경별 설정)
  - `application-local.yml`: 선택적
  - `application-prod.yml`: 필수 (`SLACK_WEBHOOK_URL` 환경 변수)
- DLT 임계치 관련 설정 (`application.yml` 공통)

## 의존 관계
- `MessageHistoryService` / `TopicCount` (Task 05) — DLT 메시지 수 집계
- AOP(Task 04)에서 실패 시 `SlackNotifier` 호출

## 산출물
- `SlackNotifier.java`
- `DltThresholdMonitor.java`

## 검증 기준
- 에러 발생 시 슬랙 알림이 비동기로 발송
- 알림 메시지에 topic/partition/offset/messageId/failCount/exception 포함
- DLT 임계치 초과 시 스케줄링된 알림 발송
- Slack URL 미설정 시 graceful 처리 (에러 미발생)
