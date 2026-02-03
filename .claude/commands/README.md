# Claude Code 커스텀 커맨드

TDD (Red → Green → Refactor) + Tidy First 개발 방법론에 맞춘 슬래시 커맨드 모음.

## 커맨드 목록

### TDD 사이클

| 커맨드 | 설명 | 인자 |
|--------|------|------|
| `/go` | plan.md 기반 TDD 자동 수행 | 없음 |
| `/red` | 실패 테스트 작성 | 요구사항 설명 |
| `/green` | 테스트 통과 최소 구현 | 없음 |
| `/refactor` | 코드 품질 개선 | 대상/방향 (선택) |

### 검증 및 커밋

| 커맨드 | 설명 | 인자 |
|--------|------|------|
| `/verify` | 린트 + 테스트 최종 검증 | 없음 |
| `/commit` | Tidy First 규칙 커밋 | 없음 |

### 계획 및 설계

| 커맨드 | 설명 | 인자 |
|--------|------|------|
| `/plan` | plan.md 작성/업데이트 | 요구사항/PRD (선택) |

### 테스트

| 커맨드 | 설명 | 인자 |
|--------|------|------|
| `/e2e` | E2E 테스트 작성 | 대상 API (선택) |

### 결함 수정

| 커맨드 | 설명 | 인자 |
|--------|------|------|
| `/fix` | 버그 재현 테스트 → 수정 | 결함 설명 |

### Tidy First

| 커맨드 | 설명 | 인자 |
|--------|------|------|
| `/tidy` | 동작 변경 없는 구조적 변경 | 대상/방향 (선택) |

### 문서화

| 커맨드 | 설명 | 인자 |
|--------|------|------|
| `/http` | API HTTP 요청 파일 작성 | 대상 API (선택) |

---

## 일반적인 작업 흐름

### A. 새 기능 개발 (plan.md 기반)

```
/plan 요구사항 설명             (plan.md 작성)
/go → /go → /go → ...         (plan.md 항목을 하나씩 진행)
/refactor                      (누적된 코드 정리)
/verify                        (최종 검증)
/commit                        (커밋)
/http                          (API HTTP 파일 작성)
```

### B. 단계별 수동 제어

```
/red 요구사항 설명              (실패 테스트 작성)
/green                         (최소 구현)
/refactor                      (코드 정리)
/e2e                           (E2E 테스트 추가)
/verify                        (최종 검증)
/commit                        (커밋)
```

### C. 결함 수정

```
/fix 결함 설명                  (재현 테스트 + 수정)
/verify                        (최종 검증)
/commit                        (커밋)
```

### D. 코드 정리 (구조적 변경)

```
/tidy 대상/방향                 (구조적 변경)
/verify                        (최종 검증)
/commit                        (구조적 변경 커밋)
```

---

## 브랜치 및 PR 워크플로우

### 브랜치 생성

```bash
git checkout main
git pull origin main
git checkout -b feat/volume-1-user-tests
```

### 작업 완료 후 PR 생성

```bash
# 변경사항 푸시
git push -u origin feat/volume-1-user-tests

# gh CLI로 PR 생성
gh pr create --title "[volume-1] 작업 내용 요약"

# 또는 대화형으로 PR 생성
gh pr create
```

### PR 제목 규칙

- 형식: `[volume-n] 작업 내용 요약`
- 예: `[volume-1] 회원가입, 로그인 구현`
