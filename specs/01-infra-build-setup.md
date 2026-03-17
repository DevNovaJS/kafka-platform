# Task 01: 인프라 및 빌드 설정

## 목표
Gradle 멀티모듈 프로젝트 구조를 구성하고 JDK 25 툴체인, GraalVM Native Image 지원을 설정한다.

## 범위

### Gradle 설정
- Gradle 9.3.1 기반 멀티모듈 구성
- 루트 `settings.gradle`: `kafka-common`, `kafka-dlt`, `kafka-sample` 세 모듈 포함
- 루트 `build.gradle`: 공통 플러그인/의존성 관리

### 모듈 구성
| 모듈 | 타입 | 설명 |
|---|---|---|
| `kafka-common` | `java-library` | 공통 라이브러리, Boot 앱 아님 |
| `kafka-dlt` | Spring Boot app | DLT 메시지 수신/저장 |
| `kafka-sample` | Spring Boot app | 사용자 활동 샘플 컨슈머 |

### 의존 관계
- `kafka-dlt` → `implementation project(':kafka-common')`
- `kafka-sample` → `implementation project(':kafka-common')`

### JDK 25 툴체인
- Java toolchain 25 설정
- Record 클래스 활용 (Document, Event, DTO, Properties 전반)
- Virtual Thread 지원 (`spring.threads.virtual.enabled=true`)

### 주요 의존성
- Spring Boot 4.0.3
- Spring Data MongoDB
- Spring Kafka
- Lombok (annotation processor)
- AspectJ (`org.aspectj:aspectjweaver` 직접 의존 — `spring-boot-starter-aop` 없음)

### GraalVM Native Image
- `kafka-dlt`, `kafka-sample` 모듈에 native image 빌드 지원
- `./gradlew :kafka-dlt:nativeCompile`, `./gradlew :kafka-sample:nativeCompile`

## 산출물
- `settings.gradle`
- `build.gradle` (루트)
- `kafka-common/build.gradle`
- `kafka-dlt/build.gradle`
- `kafka-sample/build.gradle`

## 검증 기준
- `./gradlew build` 성공
- 각 모듈별 `./gradlew :모듈명:build` 성공
- `./gradlew :kafka-dlt:nativeCompile` / `./gradlew :kafka-sample:nativeCompile` 정상 실행
