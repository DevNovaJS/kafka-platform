# Task 10: 프로파일 및 환경 설정

## 목표
Spring Profile 기반으로 환경별 설정 파일을 분리하고, 환경 변수 바인딩을 구성한다.

## 범위
`kafka-dlt`, `kafka-sample` 각 Boot 모듈에 적용

## 설정 파일 구조

### application.yml (환경 무관 공통)
- 서버 포트
- Kafka consumer 직렬화 설정
- Virtual Thread 활성화 (`spring.threads.virtual.enabled=true`)
- Management 설정
- DLT 임계치 설정

### application-local.yml (로컬 전용)
- `localhost` 기반 Kafka/MongoDB URL
- Slack Webhook URL 선택적

### application-prod.yml (운영 전용)
- 환경 변수 참조 방식
- Slack Webhook URL 필수

## 환경 변수 (prod 프로파일)

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

## Virtual Thread 설정
- `spring.threads.virtual.enabled=true`
- Kafka listener thread pool: `SimpleAsyncTaskExecutor` 활용

## 주요 설정 항목
- `kafka.dlt.max-retry-count`: DLT 최대 재시도 횟수 (기본값 3)

## 산출물
- `kafka-dlt/src/main/resources/application.yml`
- `kafka-dlt/src/main/resources/application-local.yml`
- `kafka-dlt/src/main/resources/application-prod.yml`
- `kafka-sample/src/main/resources/application.yml`
- `kafka-sample/src/main/resources/application-local.yml`
- `kafka-sample/src/main/resources/application-prod.yml`

## 검증 기준
- `--spring.profiles.active=local` 로 기동 시 로컬 설정 적용
- `--spring.profiles.active=prod` 로 기동 시 환경 변수 참조
- 환경 변수 미설정 시 기본값 적용 (sample 모듈)
- Virtual Thread 활성화 확인
