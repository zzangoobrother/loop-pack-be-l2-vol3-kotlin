# PRD: 회원 관리 API

## 1. 개요

회원가입, 내 정보 조회, 비밀번호 수정 기능을 제공하는 REST API를 구현한다.
인증은 커스텀 헤더 기반으로 처리하며, 비밀번호는 암호화하여 저장한다.

## 2. 기술 스택

| 항목 | 스펙 |
|------|------|
| Language | Kotlin (JDK 21) |
| Framework | Spring Boot 3.4.4 |
| ORM | Spring Data JPA |
| DB | MySQL |
| 기존 Entity | `User` (loginId, password, name, birthday, email) extends `BaseEntity` |

## 3. 아키텍처

기존 프로젝트 레이어드 아키텍처를 따른다.

```
interfaces/api/   → Controller, DTO, ApiSpec
application/      → Facade, Info
domain/           → Entity, Service, Repository(interface)
infrastructure/   → Repository(구현체), JpaRepository
```

## 4. 인증 방식

모든 인증 필요 API는 아래 커스텀 헤더를 통해 사용자를 식별한다.

| 헤더 | 값 |
|------|------|
| `X-Loopers-LoginId` | 로그인 ID |
| `X-Loopers-LoginPw` | 비밀번호 (평문) |

서버는 헤더의 loginId로 사용자를 조회한 뒤, 비밀번호를 검증한다. 불일치 시 `401 Unauthorized`를 반환한다.

## 5. 기능 명세

---

### 5.1 회원가입

**`POST /api/v1/users`**

#### 요청 Body

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| loginId | String | O | 로그인 ID |
| password | String | O | 비밀번호 |
| name | String | O | 이름 |
| birthday | String | O | 생년월일 (yyyy-MM-dd) |
| email | String | O | 이메일 |

#### 검증 규칙

| 필드 | 규칙 | 에러 |
|------|------|------|
| loginId | 영문, 숫자만 허용. 빈 값 불가. 중복 불가 | `400 BAD_REQUEST` / `409 CONFLICT` |
| password | 8~16자, 영문 대소문자 + 숫자 + 특수문자만 허용. 생년월일(`yyyyMMdd`) 포함 불가 | `400 BAD_REQUEST` |
| name | 빈 값 불가 | `400 BAD_REQUEST` |
| birthday | `yyyy-MM-dd` 형식. 유효한 날짜여야 함 | `400 BAD_REQUEST` |
| email | 이메일 형식 검증 (RFC 5322) | `400 BAD_REQUEST` |

#### 비밀번호 저장

- 암호화(BCrypt 등)하여 저장한다.
- 평문 비밀번호는 저장하지 않는다.

#### 응답

**성공 (200)**
```json
{
  "meta": { "result": "SUCCESS", "errorCode": null, "message": null },
  "data": null
}
```

**실패 - 중복 로그인 ID (409)**
```json
{
  "meta": { "result": "FAIL", "errorCode": "CONFLICT", "message": "이미 존재하는 로그인 ID 입니다." },
  "data": null
}
```

---

### 5.2 내 정보 조회

**`GET /api/v1/users/me`**

#### 인증

| 헤더 | 필수 |
|------|------|
| `X-Loopers-LoginId` | O |
| `X-Loopers-LoginPw` | O |

#### 응답 Body

| 필드 | 타입 | 설명 |
|------|------|------|
| loginId | String | 로그인 ID |
| name | String | 이름 (마지막 글자 `*` 마스킹) |
| birthday | String | 생년월일 (yyyy-MM-dd) |
| email | String | 이메일 |

#### 마스킹 규칙

| 원본 | 마스킹 결과 |
|------|------|
| 홍길동 | 홍길* |
| 김수 | 김* |
| A | * |

#### 응답

**성공 (200)**
```json
{
  "meta": { "result": "SUCCESS", "errorCode": null, "message": null },
  "data": {
    "loginId": "testuser1",
    "name": "홍길*",
    "birthday": "1995-03-15",
    "email": "test@example.com"
  }
}
```

**실패 - 인증 실패 (401)**
```json
{
  "meta": { "result": "FAIL", "errorCode": "UNAUTHORIZED", "message": "인증에 실패했습니다." },
  "data": null
}
```

---

### 5.3 비밀번호 수정

**`PATCH /api/v1/users/me/password`**

#### 인증

| 헤더 | 필수 |
|------|------|
| `X-Loopers-LoginId` | O |
| `X-Loopers-LoginPw` | O |

#### 요청 Body

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| currentPassword | String | O | 현재 비밀번호 |
| newPassword | String | O | 새 비밀번호 |

#### 검증 규칙

| 규칙 | 에러 |
|------|------|
| 새 비밀번호: 8~16자, 영문 대소문자 + 숫자 + 특수문자만 허용 | `400 BAD_REQUEST` |
| 새 비밀번호에 생년월일(`yyyyMMdd`) 포함 불가 | `400 BAD_REQUEST` |
| 현재 비밀번호와 새 비밀번호가 동일하면 불가 | `400 BAD_REQUEST` |
| 요청 Body의 currentPassword가 저장된 비밀번호와 불일치 | `401 UNAUTHORIZED` |

#### 응답

**성공 (200)**
```json
{
  "meta": { "result": "SUCCESS", "errorCode": null, "message": null },
  "data": null
}
```

---

## 6. 에러 코드 정의

| ErrorType | HTTP Status | 사용 케이스 |
|-----------|-------------|-------------|
| `BAD_REQUEST` | 400 | 입력값 검증 실패 |
| `UNAUTHORIZED` | 401 | 인증 실패 (헤더 누락, 비밀번호 불일치) |
| `NOT_FOUND` | 404 | 사용자 조회 실패 |
| `CONFLICT` | 409 | 로그인 ID 중복 |

> 기존 `ErrorType` enum에 `UNAUTHORIZED(401)` 추가가 필요하다.

## 7. 데이터 모델

### users 테이블

| 컬럼 | 타입 | 제약조건 | 설명 |
|------|------|----------|------|
| id | BIGINT | PK, AUTO_INCREMENT | BaseEntity |
| login_id | VARCHAR | UNIQUE, NOT NULL | 로그인 ID |
| password | VARCHAR | NOT NULL | 암호화된 비밀번호 |
| name | VARCHAR | NOT NULL | 이름 |
| birthday | DATE | NOT NULL | 생년월일 |
| email | VARCHAR | NOT NULL | 이메일 |
| created_at | DATETIME | NOT NULL | BaseEntity |
| updated_at | DATETIME | NOT NULL | BaseEntity |
| deleted_at | DATETIME | NULLABLE | BaseEntity (soft delete) |

## 8. 구현 파일 구조 (예상)

```
apps/commerce-api/src/main/kotlin/com/loopers/
├── interfaces/api/user/
│   ├── UserV1Controller.kt
│   ├── UserV1ApiSpec.kt
│   └── UserV1Dto.kt
├── application/user/
│   ├── UserFacade.kt
│   └── UserInfo.kt
├── domain/user/
│   ├── User.kt              (기존 파일 이동 + 검증 로직 추가)
│   ├── UserService.kt
│   └── UserRepository.kt
├── infrastructure/user/
│   ├── UserJpaRepository.kt
│   └── UserRepositoryImpl.kt
└── support/error/
    └── ErrorType.kt          (UNAUTHORIZED 추가)
```

## 9. 주요 구현 고려사항

1. **비밀번호 암호화**: Spring Security의 `BCryptPasswordEncoder` 사용 권장. `matches()`로 검증
2. **생년월일 비밀번호 포함 검증**: `yyyyMMdd` 형식(예: `19950315`)이 비밀번호 문자열에 포함되는지 확인
3. **헤더 인증**: 모든 인증 필요 API에서 헤더 값 추출 → 사용자 조회 → 비밀번호 검증 흐름을 공통화
4. **마스킹**: 도메인 레이어가 아닌 응답 DTO 변환 시점에 적용
