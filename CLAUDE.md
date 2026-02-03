# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 언어 규칙

- 응답, 코드 주석, 커밋 메시지, 문서화: 한국어
- 변수명/함수명: 영어

## Commands

### 초기 설정

```bash
make init                                          # git hooks 설정
docker compose -f docker/infra-compose.yml up -d   # 인프라 (MySQL, Redis, Kafka) 실행
```

### Red Phase — 테스트 작성 후 실패 확인

```bash
# 단일 테스트 클래스 실행 (작성한 테스트가 실패하는지 확인)
./gradlew :apps:commerce-api:test --tests "com.loopers.domain.example.ExampleModelTest"

# 단일 테스트 메서드 실행
./gradlew :apps:commerce-api:test --tests "com.loopers.domain.example.ExampleModelTest.특정메서드명"
```

### Green Phase — 구현 후 전체 테스트 통과 확인

```bash
# 전체 테스트 실행 (모든 테스트가 통과하는지 확인)
./gradlew test
```

### Refactor Phase — 리팩토링 후 린트 + 전체 테스트

```bash
./gradlew ktlintFormat   # 린트 자동 수정 (unused import 제거 등)
./gradlew ktlintCheck    # 린트 체크
./gradlew test           # 리팩토링 후 전체 테스트 재확인
```

### 커밋 전 최종 검증

```bash
./gradlew ktlintCheck test   # 린트 + 테스트 한 번에 실행
```

### 빌드 및 실행

```bash
./gradlew build                        # 전체 빌드
./gradlew :apps:commerce-api:build     # 특정 모듈 빌드
./gradlew :apps:commerce-api:bootRun   # 애플리케이션 실행
```

## 아키텍처

Kotlin + Spring Boot 3.4.4 + JDK 21 멀티모듈 프로젝트.

### 모듈 구조

- **apps/**: 실행 가능한 Spring Boot 애플리케이션 (commerce-api, commerce-batch, commerce-streamer)
- **modules/**: 인프라 설정 모듈 (jpa, redis, kafka) — `testFixtures` 제공
- **supports/**: 부가 기능 모듈 (jackson, logging, monitoring)

### 레이어드 아키텍처 (apps 내부)

```
interfaces/api/    → Controller, ApiSpec(OpenAPI 인터페이스), Dto
application/       → Facade(오케스트레이션), Info(레이어간 데이터 전달)
domain/            → Entity, Service(@Component), Repository(인터페이스)
infrastructure/    → RepositoryImpl(구현체), JpaRepository
support/error/     → CoreException, ErrorType
```

요청 흐름: `Controller → Facade → Service → Repository(interface) → RepositoryImpl → JpaRepository`

### 핵심 패턴

**Entity**: `BaseEntity` 상속 (id, createdAt, updatedAt, deletedAt 자동관리, soft delete 지원). 프로퍼티는 `protected set`. 도메인 검증은 `init` 블록에서 `CoreException` throw.

**에러 처리**: `CoreException(errorType: ErrorType, customMessage: String?)`. ErrorType enum은 `INTERNAL_ERROR(500)`, `BAD_REQUEST(400)`, `NOT_FOUND(404)`, `CONFLICT(409)`.

**API 응답**: 모든 응답은 `ApiResponse<T>` 래퍼 사용. `ApiResponse.success(data)` / `ApiResponse.fail(errorCode, message)`. `ApiControllerAdvice`에서 전역 예외 처리.

**Repository**: 도메인 레이어에 인터페이스 정의 → infrastructure에서 구현. JPA Repository는 `JpaRepository<Entity, Long>` 상속.

**DTO 변환**: Info/Dto에 `companion object { fun from(...) }` 팩토리 메서드 사용.

## 테스트 패턴

- **단위 테스트**: `@Nested` + `@DisplayName`(한국어) 조합으로 BDD 스타일 구성
- **통합 테스트**: `@SpringBootTest`, `@AfterEach`에서 `databaseCleanUp.truncateAllTables()` 호출
- **E2E 테스트**: `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `TestRestTemplate`
- 테스트 시 MySQL/Redis는 TestContainers로 자동 구동 (프로파일: `test`)
- 테스트 타임존: `Asia/Seoul`

## 설정 파일 위치

- DB 설정: `modules/jpa/src/main/resources/jpa.yml`
- Redis 설정: `modules/redis/src/main/resources/redis.yml`
- Kafka 설정: `modules/kafka/src/main/resources/kafka.yml`
- 앱 설정: `apps/commerce-api/src/main/resources/application.yml`
- JPA 엔티티 스캔: `com.loopers`, Repository 스캔: `com.loopers.infrastructure`

## 개발 방법론: TDD (Kent Beck) + Tidy First

### 증강 코딩 원칙

- **대원칙**: 방향성 및 주요 의사 결정은 개발자에게 제안만 하며, 최종 승인된 사항을 기반으로 작업 수행
- **임의 작업 금지**: 반복적 동작, 요청하지 않은 기능 구현, 테스트 삭제를 임의로 진행하지 않는다
- **설계 주도권**: AI는 임의판단하지 않고 방향성을 제안할 수 있으나, 개발자 승인 후 수행

### plan.md 기반 작업 흐름

`plan.md`의 지시사항을 항상 따른다. "go" 명령 시 `plan.md`에서 다음 미완료 테스트를 찾아 구현하고, 해당 테스트를 통과시키기 위한 최소한의 코드만 작성한다.

### TDD 사이클: Red → Green → Refactor

1. **Red**: 요구사항을 만족하는 실패 테스트를 먼저 작성한다 (한 번에 하나씩)
2. **Green**: 테스트를 통과시키기 위한 최소한의 코드를 구현한다. 오버엔지니어링 금지
3. **Refactor**: 테스트가 통과한 후에만 리팩토링한다
   - 불필요한 private 함수 지양, 객체지향적 코드 작성
   - unused import 제거
   - 성능 최적화
   - 모든 테스트 케이스가 통과해야 함

### 변경 유형 분리 (Tidy First)

- **구조적 변경**: 동작을 바꾸지 않는 코드 재배치 (이름 변경, 메서드 추출, 코드 이동)
- **행위적 변경**: 실제 기능 추가/수정
- 구조적 변경과 행위적 변경을 절대 같은 커밋에 섞지 않는다
- 둘 다 필요하면 구조적 변경을 먼저 수행한다

### 커밋 규칙

- 모든 테스트가 통과하고, 컴파일러/린터 경고가 없을 때만 커밋
- 하나의 논리적 작업 단위로 커밋
- 커밋 메시지에 구조적/행위적 변경 여부를 명시
- 크고 드문 커밋보다 작고 빈번한 커밋을 지향

### 코드 품질 기준

- 중복을 철저히 제거한다
- 이름과 구조로 의도를 명확히 표현한다
- 의존성을 명시적으로 드러낸다
- 메서드는 작게, 단일 책임으로 유지한다
- 가능한 가장 단순한 해결책을 사용한다

### 주의사항

**금지 (Never Do)**
- 실제 동작하지 않는 코드, 불필요한 Mock 데이터를 이용한 구현 금지
- null-safety 하지 않은 코드 작성 금지
- `println` 코드 남기지 않는다

**권장 (Recommendation)**
- 실제 API를 호출해 확인하는 E2E 테스트 코드 작성
- 재사용 가능한 객체 설계
- 성능 최적화에 대한 대안 및 제안
- 개발 완료된 API는 `http/*.http` 파일에 분류하여 작성

**우선순위 (Priority)**
1. 실제 동작하는 해결책만 고려
2. null-safety, thread-safety 고려
3. 테스트 가능한 구조로 설계
4. 기존 코드 패턴 분석 후 일관성 유지

### 테스트 작성 시

- 모든 테스트는 **3A 원칙**으로 작성한다: Arrange(준비) → Act(실행) → Assert(검증)
- 매번 테스트를 하나씩 작성하고, 실행하고, 구조를 개선한다
- 매 단계마다 전체 테스트를 실행한다 (장시간 테스트 제외)
- 테스트 이름은 동작을 설명하도록 작성한다
- 결함 수정 시: API 레벨 실패 테스트 → 최소 재현 테스트 → 둘 다 통과시키기

## 브랜치 및 PR 규칙

### 브랜치 생성

- 기능 단위로 브랜치 생성
- 브랜치 네이밍: `feature/week3-order-cancel`, `fix/week5-coupon-bugfix`, `feat/volume-1-user-tests`
- 브랜치는 항상 `main` 기준으로 생성

```bash
git checkout main
git pull origin main
git checkout -b feat/volume-1-user-tests
```

### PR 생성 워크플로우

```bash
# 변경사항 커밋 후 원격에 푸시
git push -u origin feat/volume-1-user-tests

# gh CLI로 PR 생성
gh pr create --title "[volume-1] 작업 내용 요약" --body-file .github/pull_request_template.md

# 또는 대화형으로 PR 생성
gh pr create
```

### 커밋 메시지

- 명확한 동사 + 작업 대상 구조로 작성
- 커밋 메시지만으로 어떤 작업인지 파악 가능하도록 작성
- 예시:
  - `feat: 주문 생성 기능 구현`
  - `refactor: 장바구니 엔티티 리팩토링`
  - `test: 주문 생성 테스트 코드 추가`

### PR 제목 규칙

- 형식: `[volume-n] 작업 내용 요약`
  - 예: `[volume-1] 회원가입, 로그인 구현`

### PR 작성 시 유의사항

| 항목 | 설명 |
| --- | --- |
| ✅ 리뷰 포인트 필수 | PR 템플릿 내 `💬 리뷰 포인트`는 반드시 채워야 함 |
| 🔍 불필요한 코드 제거 | 디버깅 로그(`println`), 사용하지 않는 클래스/메서드 제거 |
| 🧪 테스트 커버리지 | 기능 구현 시 단위 테스트 또는 통합 테스트 포함 |
| 💄 코드 스타일 | 기본 포맷팅, 컨벤션 준수 (ktlintFormat 활용) |

### PR 템플릿

```markdown
## 📌 Summary
- 어떤 기능/이슈를 해결했는지 요약

## 💬 리뷰 포인트
- 리뷰어가 중점적으로 봐줬으면 하는 부분 (3개 이내)
- 고민했던 설계 포인트나 로직

## ✅ Checklist
- [ ] 테스트 코드 포함
- [ ] 불필요한 코드 제거
- [ ] README or 주석 보강 (필요 시)

## 📎 기타 참고 사항
- 관련 커밋, 이슈, 참고 링크 등
```
