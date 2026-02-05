TDD Red Phase: 요구사항에 대한 실패하는 테스트를 작성한다.

요구사항: $ARGUMENTS

## 절차

1. 요구사항을 분석하고, 어떤 종류의 테스트가 적절한지 판단한다:
   - **단위 테스트**: 도메인 로직, Entity 검증, 순수 비즈니스 규칙
   - **통합 테스트**: Service + Repository 연동, DB 관련 로직
   - **E2E 테스트**: Controller → Facade → Service → Repository 전체 흐름
2. 기존 테스트 코드의 패턴을 참고해 일관성을 유지한다
3. 3A 원칙을 따른다: Arrange(준비) → Act(실행) → Assert(검증)
4. `@Nested` + `@DisplayName`(한국어)으로 BDD 스타일 구성
5. 테스트를 실행해 실패하는지 확인한다:
   ```
   ./gradlew :apps:commerce-api:test --tests "해당테스트클래스.해당테스트메서드"
   ```
6. 실패 결과를 보고한다:
   - 테스트 클래스명, 메서드명
   - 실패 사유 (컴파일 에러 / assertion 실패 / 예외 등)
   - 다음 단계(Green)에서 구현해야 할 내용 요약

## 테스트 유형별 가이드

### 단위 테스트
- 외부 의존성 없이 순수 로직만 테스트
- Entity `init` 블록 검증, 도메인 메서드 검증
- `CoreException` 발생 여부 확인

### 통합 테스트
- `@SpringBootTest` 사용
- `@AfterEach`에서 `databaseCleanUp.truncateAllTables()` 호출
- TestContainers로 MySQL/Redis 자동 구동

### E2E 테스트
- `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `TestRestTemplate`
- `ApiResponse` 래퍼를 고려한 응답 검증

## 규칙

- 한 번에 하나의 테스트만 작성
- 테스트 이름은 동작을 설명하도록 한국어로 작성
- 불필요한 Mock 데이터 사용 금지
- 테스트 가능한 구조로 설계
- null-safety 준수
