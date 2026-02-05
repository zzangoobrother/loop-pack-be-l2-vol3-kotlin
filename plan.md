# 기능명: 5.3 비밀번호 수정 API

## 개요
- **엔드포인트**: `PATCH /api/v1/users/me/password`
- **인증**: `X-Loopers-LoginId`, `X-Loopers-LoginPw` 헤더 기반
- **요청**: currentPassword, newPassword
- **응답**: 성공 시 200, 실패 시 400/401

## 구현 계획

### 1. 도메인 모델 (User Entity)
- [ ] 현재 비밀번호와 새 비밀번호가 동일하면 예외 발생
- [ ] User.changePassword 메서드 구현 (비밀번호 변경)

### 2. 서비스 (UserService)
- [ ] 현재 비밀번호 검증 실패 시 UNAUTHORIZED 예외 발생
- [ ] UserService.changePassword 메서드 구현

### 3. Facade (UserFacade)
- [ ] UserFacade.changePassword 메서드 구현

### 4. API (Controller, DTO, ApiSpec)
- [ ] UserDto.ChangePasswordRequest 추가
- [ ] UserApiSpec에 changePassword 스펙 추가
- [ ] UserController.changePassword 엔드포인트 구현

### 5. E2E 테스트
- [ ] 비밀번호 변경 성공 → 200 OK
- [ ] 현재 비밀번호 불일치 → 401 UNAUTHORIZED
- [ ] 새 비밀번호 형식 오류 → 400 BAD_REQUEST
- [ ] 새 비밀번호에 생년월일 포함 → 400 BAD_REQUEST
- [ ] 현재/새 비밀번호 동일 → 400 BAD_REQUEST
- [ ] 인증 헤더 누락 → 401 UNAUTHORIZED
