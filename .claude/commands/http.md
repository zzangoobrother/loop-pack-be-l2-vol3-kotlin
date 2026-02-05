개발 완료된 API에 대한 HTTP 요청 파일을 작성하거나 업데이트한다.

대상 API: $ARGUMENTS

## 절차

1. `$ARGUMENTS`가 있으면 해당 API를 대상으로 한다
2. `$ARGUMENTS`가 없으면 최근 구현된 API 중 HTTP 파일이 없는 것을 찾는다
3. `http/` 디렉토리를 확인한다
   - 디렉토리가 없으면 생성 여부를 개발자에게 확인한다
   - 기존 HTTP 파일이 있으면 패턴을 참고한다
4. 해당 API의 Controller와 DTO를 분석한다
5. HTTP 요청 파일을 작성한다:
   - 도메인별로 파일을 분류한다 (예: `http/user.http`, `http/product.http`)
   - 정상 케이스와 에러 케이스를 모두 포함한다
6. 결과를 보고한다: 작성/수정한 파일 목록

## HTTP 파일 형식

```http
### 기능 설명 — 정상 케이스
POST http://localhost:8080/api/v1/users
Content-Type: application/json

{
  "loginId": "testuser",
  "password": "password123",
  "name": "테스트유저"
}

### 기능 설명 — 실패 케이스 (빈 로그인 ID)
POST http://localhost:8080/api/v1/users
Content-Type: application/json

{
  "loginId": "",
  "password": "password123",
  "name": "테스트유저"
}

### 단건 조회
GET http://localhost:8080/api/v1/users/1

### 목록 조회
GET http://localhost:8080/api/v1/users?page=0&size=10
```

## 규칙

- IntelliJ HTTP Client 형식으로 작성 (`.http` 확장자)
- 각 요청 사이에 `###`으로 구분하고 설명을 달 수 있다
- 도메인별로 파일을 분리한다
- 정상 케이스와 주요 에러 케이스를 모두 포함한다
- 요청 본문은 실제로 동작하는 값을 사용한다
- 환경 변수가 필요하면 `http-client.env.json`도 함께 작성한다
