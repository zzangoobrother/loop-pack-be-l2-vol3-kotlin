E2E 테스트를 작성한다: 실제 API를 호출해 전체 흐름을 검증한다.

대상 API: $ARGUMENTS

## 절차

1. `$ARGUMENTS`로 지정된 API 엔드포인트를 확인한다
   - 지정되지 않으면 최근 구현된 API를 분석해 E2E 테스트가 없는 것을 찾는다
2. 해당 API의 전체 흐름을 파악한다: Controller → Facade → Service → Repository
3. E2E 테스트를 작성한다:
   - `@SpringBootTest(webEnvironment = RANDOM_PORT)` 사용
   - `TestRestTemplate`으로 실제 HTTP 요청 수행
   - `ApiResponse` 래퍼를 고려한 응답 검증
4. 테스트 시나리오를 작성한다:
   - 정상 케이스 (Happy Path)
   - 실패 케이스 (잘못된 입력, 존재하지 않는 리소스 등)
   - 에러 응답 형식 검증 (`ApiResponse.fail`)
5. 테스트를 실행해 결과를 확인한다:
   ```
   ./gradlew :apps:commerce-api:test --tests "해당테스트클래스"
   ```
6. 결과를 보고한다:
   - 작성한 테스트 목록
   - 각 테스트 통과/실패 여부
   - 전체 테스트 결과

## 테스트 구조

```kotlin
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class XxxApiE2ETest(
    @Autowired val restTemplate: TestRestTemplate,
    @Autowired val databaseCleanUp: DatabaseCleanUp,
) {
    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @Nested
    @DisplayName("POST /api/v1/xxx")
    inner class CreateXxx {
        @Test
        @DisplayName("정상적인 요청이면 201을 반환한다")
        fun success() {
            // Arrange
            // Act
            // Assert
        }
    }
}
```

## 규칙

- 실제 HTTP 요청을 수행해 전체 레이어를 검증한다
- Mock 사용 금지 — 실제 DB, 실제 서비스를 사용한다
- `@AfterEach`에서 `databaseCleanUp.truncateAllTables()` 호출
- `@Nested` + `@DisplayName`(한국어) BDD 스타일
- 테스트 타임존: `Asia/Seoul`
