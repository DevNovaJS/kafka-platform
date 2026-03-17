# Task Specifications

CLAUDE.md 기준으로 도출한 구현 태스크 목록.

## 태스크 목록

| # | 태스크 | 모듈 | 파일 |
|---|---|---|---|
| 01 | 인프라 및 빌드 설정 | 전체 | [01-infra-build-setup.md](01-infra-build-setup.md) |
| 02 | Kafka 설정 클래스 | kafka-common | [02-common-kafka-config.md](02-common-kafka-config.md) |
| 03 | 메시지 헤더 | kafka-common | [03-common-message-headers.md](03-common-message-headers.md) |
| 04 | 멱등성 AOP 처리 | kafka-common | [04-common-idempotency-aop.md](04-common-idempotency-aop.md) |
| 05 | 메시지 이력 관리 | kafka-common | [05-common-message-history.md](05-common-message-history.md) |
| 06 | DLT 발송 | kafka-common | [06-common-dlt-sender.md](06-common-dlt-sender.md) |
| 07 | 슬랙 알림 | kafka-common | [07-common-slack-notification.md](07-common-slack-notification.md) |
| 08 | DLT 수신/저장 앱 | kafka-dlt | [08-dlt-app.md](08-dlt-app.md) |
| 09 | 사용자 활동 샘플 컨슈머 앱 | kafka-sample | [09-sample-app.md](09-sample-app.md) |
| 10 | 프로파일 및 환경 설정 | kafka-dlt, kafka-sample | [10-profile-config.md](10-profile-config.md) |

## 의존 관계 (구현 순서)

```
01 인프라/빌드
 ├── 02 Kafka 설정
 ├── 03 메시지 헤더
 ├── 05 메시지 이력
 │    └── 04 멱등성 AOP ←── 03, 05, 06, 07
 ├── 06 DLT 발송 ←── 02, 03
 ├── 07 슬랙 알림 ←── 05
 ├── 08 DLT 앱 ←── 02, kafka-common 전체
 ├── 09 Sample 앱 ←── 02, 04, kafka-common 전체
 └── 10 프로파일 설정 (08, 09와 병행)
```

## 권장 구현 순서

1. **Phase 1 — 기반**: 01
2. **Phase 2 — kafka-common 독립 컴포넌트**: 02, 03, 05 (병렬 가능)
3. **Phase 3 — kafka-common 의존 컴포넌트**: 06, 07 (02/03/05 완료 후)
4. **Phase 4 — kafka-common AOP 통합**: 04 (03/05/06/07 완료 후)
5. **Phase 5 — Boot 앱 + 설정**: 08, 09, 10 (병렬 가능, kafka-common 완료 후)
