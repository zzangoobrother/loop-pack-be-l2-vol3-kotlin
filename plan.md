# Plan: 5.2 내 정보 조회 API 구현

## 개요
- **기능**: `GET /api/v1/users/me` - 인증된 사용자의 정보 조회
- **인증**: `X-Loopers-LoginId`, `X-Loopers-LoginPw` 헤더 기반
- **응답**: loginId, name(마스킹), birthday, email

## 구현 순서 (TDD)

### Phase 1: UserService 인증 로직 (우선)

- [ ] **1.1 ErrorType에 UNAUTHORIZED 추가**
  - `UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Unauthorized", "인증에 실패했습니다.")`
  - 파일: `support/error/ErrorType.kt`

- [ ] **1.2 UserRepository에 findByLoginId 추가**
  - `fun findByLoginId(loginId: String): User?`
  - 파일: `domain/example/UserRepository.kt`, `infrastructure/example/UserRepositoryImpl.kt`, `infrastructure/example/UserJpaRepository.kt`

- [ ] **1.3 UserService.authenticate 테스트 작성**
  - 테스트 케이스:
    - 정상 인증 시 User 반환
    - 존재하지 않는 loginId → UNAUTHORIZED 예외
    - 비밀번호 불일치 → UNAUTHORIZED 예외
  - 파일: `domain/example/UserServiceTest.kt`

- [ ] **1.4 UserService.authenticate 구현**
  - loginId로 사용자 조회
  - passwordEncoder.matches로 비밀번호 검증
  - 파일: `domain/example/UserService.kt`

### Phase 2: 이름 마스킹 (Value Object)

- [ ] **2.1 MaskedName Value Object 테스트 작성**
  - 테스트 케이스:
    - "홍길동" → "홍길*"
    - "김수" → "김*"
    - "A" → "*"
    - 빈 문자열 → 예외 또는 빈 문자열
  - 파일: `domain/example/MaskedNameTest.kt`

- [ ] **2.2 MaskedName Value Object 구현**
  - 마지막 글자를 `*`로 치환하는 로직
  - 파일: `domain/example/MaskedName.kt`

### Phase 3: 내 정보 조회 서비스

- [ ] **3.1 UserFacade.getMe 구현**
  - authenticate 호출 후 UserInfo 반환
  - 파일: `application/user/UserFacade.kt`

- [ ] **3.2 UserInfo에 마스킹된 이름 필드 추가**
  - maskedName 필드 추가
  - 파일: `application/user/UserInfo.kt`

### Phase 4: API 엔드포인트

- [ ] **4.1 UserDto.MeResponse 추가**
  - loginId, name(마스킹), birthday, email
  - 파일: `interfaces/api/user/UserDto.kt`

- [ ] **4.2 UserApiSpec에 getMe 추가**
  - `@Operation` 문서화
  - 파일: `interfaces/api/user/UserApiSpec.kt`

- [ ] **4.3 UserController.getMe 구현**
  - `@GetMapping("/me")`
  - 헤더에서 인증 정보 추출
  - 파일: `interfaces/api/user/UserController.kt`

### Phase 5: E2E 테스트

- [ ] **5.1 내 정보 조회 E2E 테스트 작성**
  - 테스트 케이스:
    - 정상 조회 → 200 OK, 마스킹된 이름 확인
    - 헤더 누락 → 401 UNAUTHORIZED
    - 잘못된 loginId → 401 UNAUTHORIZED
    - 잘못된 비밀번호 → 401 UNAUTHORIZED
  - 파일: `interfaces/api/UserApiE2ETest.kt`

## 검증 규칙 정리

| 항목 | 규칙 |
|------|------|
| 인증 헤더 | `X-Loopers-LoginId`, `X-Loopers-LoginPw` 필수 |
| 인증 실패 | 401 UNAUTHORIZED 반환 |
| 이름 마스킹 | 마지막 글자를 `*`로 치환 |

## 파일 변경 목록

### 신규 생성
- `domain/example/MaskedName.kt`
- `domain/example/MaskedNameTest.kt`

### 수정
- `support/error/ErrorType.kt` - UNAUTHORIZED 추가
- `domain/example/UserRepository.kt` - findByLoginId 추가
- `domain/example/UserService.kt` - authenticate 추가
- `infrastructure/example/UserRepositoryImpl.kt` - findByLoginId 구현
- `infrastructure/example/UserJpaRepository.kt` - findByLoginId 쿼리
- `application/user/UserFacade.kt` - getMe 추가
- `application/user/UserInfo.kt` - 마스킹 이름 추가
- `interfaces/api/user/UserDto.kt` - MeResponse 추가
- `interfaces/api/user/UserApiSpec.kt` - getMe 스펙 추가
- `interfaces/api/user/UserController.kt` - getMe 엔드포인트 추가
- `interfaces/api/UserApiE2ETest.kt` - E2E 테스트 추가
