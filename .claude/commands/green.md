TDD Green Phase: 현재 실패 중인 테스트를 통과시키는 최소한의 코드를 구현한다.

## 절차

1. 현재 실패 중인 테스트를 파악한다:
   - 직전 Red Phase에서 작성한 테스트가 있으면 해당 테스트 확인
   - 불확실하면 `./gradlew test`로 실패 테스트 목록 확인
2. 실패 원인을 분석한다 (컴파일 에러 / assertion 실패 / 예외 등)
3. 테스트를 통과시키기 위한 **최소한의 코드만** 작성한다
4. 레이어드 아키텍처 패턴을 따른다:
   - **Entity**: `BaseEntity` 상속, 프로퍼티는 `protected set`, `init` 블록에서 검증
   - **Repository**: 도메인 레이어에 인터페이스, infrastructure에 구현체
   - **Service**: `@Component` 어노테이션, Repository 인터페이스 의존
   - **Facade**: 오케스트레이션, Info 객체로 데이터 전달
   - **Controller**: `ApiResponse` 래퍼 사용, ApiSpec 인터페이스 구현
5. 구현 후 해당 테스트만 먼저 실행해 통과를 확인한다:
   ```
   ./gradlew :apps:commerce-api:test --tests "해당테스트클래스.해당테스트메서드"
   ```
6. 전체 테스트를 실행해 기존 테스트가 깨지지 않았는지 확인한다:
   ```
   ./gradlew test
   ```
7. 결과를 보고한다:
   - 구현한 코드 파일 목록
   - 해당 테스트 통과 여부
   - 전체 테스트 통과 여부

## 규칙

- 오버엔지니어링 금지 — 테스트를 통과시키는 데 필요한 코드만 작성
- 실제 동작하는 코드만 구현 (가짜 구현 금지)
- null-safety 준수
- println 금지
- 기존 코드 패턴과 일관성 유지
- DTO 변환은 `companion object { fun from(...) }` 팩토리 메서드 사용
- 에러 처리는 `CoreException(errorType, customMessage)` 사용
