# Round 5 학습 노트 — 읽기 성능 최적화 / 인덱스 / 비정규화 / Redis 캐시 / Pre-aggregation

## 학습 개요
- 학습 일자: 2026-04-28
- 라운드 주제: DB Index · 복합 인덱스 설계 · 카디널리티 · EXPLAIN · 비정규화 · Materialized View · Redis 캐시 (TTL/무효화) · Pre-aggregation
- 참조 문서: `docs/quests/round-5.md`
- 진행 방식: 발제 흐름(인덱스 → 정렬/비정규화 → 캐시 → Pre-aggregation)을 따라가되, 사용자의 답변 깊이에 따라 유동적으로 조절. 마지막에 백지 설계 테스트.
- 이전 라운드 연결:
  - Round 1~3 → 도메인/레이어드/유스케이스 협력 (읽기 경로 설계의 토대)
  - Round 4 → 동시성 (좋아요 카운트 비정규화 시 동시성 문제로 직결)

---

## Q1. "쓰기보다 읽기가 문제다" — 본질적 이유와 세 무기의 대가

**[질문]**
(a) 트래픽이 많아질수록 왜 읽기가 쓰기보다 더 자주 병목이 될까?
(b) 그렇다면 쓰기는 병목이 안 된다는 뜻인가? 쓰기 병목 시나리오는?
(c) 읽기 병목 해결의 무기 3가지를 들고, 각각이 본질적으로 어떤 비용을 줄이는지 설명.

**[1차 답변]**
> a : 많은 양의 데이터를 자주 읽고, 검색도 하고, 사용자에 맞는 데이터를 다양한 조건에 맞게 흩러진 테이블에서 가져오기 때문에 병목이 된다.
>
> b : 병목이 될 수 있지만 사용자의 95% 이상은 읽기를, 나머지는 쓰기를 한다. 쓰기가 병목이 되는거는 선착순 이 될수 있다
>
> c : 인덱스 사용, db 조회 비용을 줄인다 / 캐시 사용, db 조회를 안하고 미리 가지고 있는 데이터를 그대로 반환한다 / 비정규화, 미리 읽기에 필요한 데이터를 만들어 db 테이블에 가지고 있고, 단순히 조회만들 통해 반환한다

**[1차 평가]**
- (a) 부분 정답. 가장 본질적인 이유 = "읽기 트래픽 자체가 95:5 비율로 압도적으로 많다" — 이건 (b)에서 무심코 답함
- (b) "선착순"은 너무 좁음 → Round 4 동시성 영역과 디스크 I/O로 확장 필요
- (c) 인덱스의 표현이 추상적 ("조회 비용 줄임") → 본질을 한 단계 더 파야 함

**[후속 질문 1: 쓰기 병목, 더 다양한 케이스]**
b-1. 핫한 인기 상품에 동시 좋아요 → 왜 느려지나?
b-2. 인덱스 5개 걸린 테이블에 INSERT 한 번 → 무슨 일이?
b-3. 결제 로그 INSERT만 하는 테이블 → 느려질 수 있나?

**[후속 질문 2: 세 무기의 대가]**
c-1. 인덱스의 비용은? c-2. 캐시의 비용은? c-3. 비정규화의 비용은?

**[2차 답변]**
> b-1 : 쓰기 병목 발생, 한번에 많은 트래픽이 쓰기를 실행하면서 인덱스도 변경이 되고, 좋아요 수도 정합성 유지를 위해 락이 걸려있다면 락 경합으로 하나씩 실행이 될것이다.
> b-2 : 인덱스 5개 모두 변경이 발생한다
> b-3 : 느려지진 않을 것이다.
>
> c-1 : 쓰기 지연 발생 가능
> c-2 : 데이터 불일치 발생 가능성 높음
> c-3 : 데이터 정합성 확인 필요, 락이나 이벤트 등을 이용할 가능성 높음

**[2차 평가]**
- b-1 ✅ 락 경합 + 인덱스 갱신 → 직렬화 (Round 4 Lost Update 메커니즘 연결)
- b-2 ✅ 인덱스가 많을수록 쓰기 비용은 비례해 증가
- b-3 ⚠️ "INSERT만 하면 안 느릴 것"은 실무에서 깨지는 위험한 직관
- c-1 ✅ 시간 비용 정답, 단 공간 비용 누락
- c-2 ✅ 정확
- c-3 ✅ "쓰기 시점에 동기화 책임이 추가된다" — 본질

**[악마의 변호인 (b-3): INSERT는 락이 없으면 정말 안 느릴까?]**
디스크 IOPS 한계 / WAL fsync / SSD도 한계 있음 시나리오 제시.

**[3차 답변]**
> db는 커밋할 때 물리 저장 장치에 저장이 되는데 하드디스크라면 디스크가 돌면서 저장되기 때문에 시간이 오래 걸린다

**[3차 평가]**
- ✅ 핵심 본질 (디스크 I/O 한계) 정확히 잡음
- 보강: DB는 커밋 응답 전에 redo log(WAL)를 디스크에 fsync. SSD도 IOPS 한계 존재 (단지 HDD보다 훨씬 높을 뿐). 락 없이도 IOPS 한계 초과 시 응답 누적

**[Q1 종합 정리]**
- 읽기 병목의 본질 = (1) 트래픽 비율 95:5 (2) 다양한 조건/조인 (3) 사용자 경험 직결
- 쓰기 병목의 다층 구조 = (1) 락 경합 (2) 인덱스 갱신 (3) WAL fsync / 디스크 I/O (4) 핫 row 경합
- 읽기 최적화 3대 무기와 대가:
  - **인덱스**: 시간(쓰기 지연) + **공간(메모리/디스크)**
  - **캐시**: 정합성 (stale data) + 인프라 비용 + 캐시 미스 폭주 위험
  - **비정규화**: 쓰기 시점에 동기화 책임 + 정합성 부담

[글감] "INSERT만 하니까 안 느릴 것"이라는 위험한 직관 — 쓰기 비용은 락 외에도 디스크 I/O / WAL / 인덱스 / 버퍼 풀 등 다층에 분포

---

## Q2. 인덱스는 왜 빠른가? — Full Scan vs B-Tree, 카디널리티의 본질

**[질문]**
(a) 1000만 row 테이블에 인덱스 없이 `WHERE email = ...` 실행 시 DB 동작 / 시간 복잡도?
(b) 인덱스 추가 후 동작 / 자료구조 / 시간 복잡도?
(c) 카디널리티 낮은 컬럼(gender)과 높은 컬럼(email) 인덱스 효과 차이?

**[1차 답변]**
> a : 모든 데이터에서 찾을거야. 디스크에서 모든 데이터를 찾을 거야. 시간 복잡도는 n
> b : b-tree 자료구조로 저장. 시간 복잡도는 log(n)
> c : gender는 두 종류라 절반 정도의 데이터를 읽음. email은 unique라 트리구조로 왼/중앙/오른쪽으로 하나씩 타고 내려가서 찾음

**[1차 평가]**
- (a) ✅ Full Scan / O(n)
- (b) ✅ 정답. 정확히는 **B+Tree** (리프 노드만 데이터 + 리프끼리 연결, 범위 검색에 유리)
- (c) ✅ 핵심 직관 잡음

**[악마의 변호인 — 카디널리티는 "값의 종류 수"인가?]**
A 사이트(99% 남성 / 1% 여성) 시나리오 제시:
- c-1. `WHERE gender='F'` (1% 매칭)에서 인덱스 효과적인가?
- c-2. `WHERE gender='M'` (99% 매칭)은? 옵티마이저가 쓸까?
- c-3. 카디널리티의 진짜 의미는?

**[2차 답변]**
> c-1 : 효과적이야. 여성 고객 데이터가 적기 때문에 효과적이야.
> c-2 : 느릴거야, 인덱스 안탈거야. full scan 해도 비슷하기 때문에 오히려 인덱스로 인해 손해를 볼 수 있어
> c-3 : 데이터의 분포 여부야

**[2차 평가]**
- c-1 ✅ "데이터가 적기 때문에 효과적" — selectivity 개념 정확
- c-2 ✅ "인덱스로 인해 손해" — random I/O > sequential scan 임계점 인지
- c-3 ✅ "데이터의 분포" — selectivity의 본질을 한 단어로 압축

**[Q2 종합 정리]**
- **카디널리티 vs 선택도(selectivity)**: 카디널리티는 컬럼의 고유값 수(통계값), selectivity는 한 조건으로 걸러지는 row 비율(실효성). **인덱스 효과의 본질은 selectivity**.
- **옵티마이저의 합리적 판단**: 통계(히스토그램)로 selectivity 추정. 매칭률이 일정 임계(보통 20~30%)를 넘으면 인덱스 버리고 풀 스캔 선택.
- **PK Lookup (Bookmark Lookup) 비용**:
  - InnoDB Secondary Index 탐색 → 리프에서 PK 획득 → Clustered Index로 다시 탐색 → row 획득
  - 매칭 row가 많으면 random I/O 폭증 → 풀 스캔의 sequential I/O가 더 빠른 역전 발생
- **B+Tree** (B-Tree 아님): 리프 노드에 데이터, 리프끼리 연결 → 범위 검색(BETWEEN, ORDER BY)에 강함

[글감] "인덱스를 걸었는데 안 타요" — 옵티마이저는 selectivity와 PK lookup 비용을 비교해 합리적으로 선택한다

---

## Q3. 복합 인덱스 — 왼쪽에서 오른쪽 순서가 왜 중요한가?

**[질문]**
```sql
SELECT * FROM products WHERE brand_id = 1 ORDER BY price ASC LIMIT 20;
```
인덱스 후보:
- A. (brand_id)
- B. (brand_id, price)
- C. (price, brand_id)

(a) 세 인덱스 각각이 WHERE/ORDER BY를 어떻게 처리하는지 분석
(b) B와 C 모두 두 컬럼 포함하는데 왜 효과가 정반대인가? B+Tree 구조 관점에서 본질 설명

**[1차 답변]**
> a : brand_id = 1 는 빠르게 찾을 수 있어. 그리고 Using filesort 가 발생할거야
> b : (brand_id, price) 는 brand_id 를 먼저 찾고 price 정렬하기 때문에 B+Tree의 한쪽 영역을 모두 읽고, (price, brand_id) 에서는 brand_id 가 여기저기 흩어져 있기 때문에 B+Tree 를 여기저기 찾아 다닐것이다

**[1차 평가]**
- (a) ✅ A 인덱스 분석은 정확. A/B/C 비교는 표로 정리해줌
- (b) ✅ "한쪽 영역을 모두 읽고" vs "여기저기 흩어져 있다" — 복합 인덱스 본질을 한 줄로 압축

**[정리: 세 인덱스 비교표]**

| 인덱스 | WHERE brand_id=1 | ORDER BY price | 결과 |
|---|---|---|---|
| A. (brand_id) | ✅ 빠름 | ❌ Using filesort | 정렬 추가 비용 |
| B. (brand_id, price) | ✅ 빠름 | ✅ 인덱스 자체가 정렬 | **최적** (Using index) |
| C. (price, brand_id) | ❌ leftmost prefix 위반 | ❌ | 의미 없음 |

**핵심 본질**: 복합 인덱스 `(A, B)` = A로 1차 정렬, 동일 A 내에서 B로 2차 정렬된 책. A로 좁혀진 영역 안은 이미 B 순으로 정렬되어 있음.

**[후속 질문: 범위 조건 함정 (range condition trap)]**
인덱스 `(brand_id, price, likes_count)` + 쿼리 `WHERE brand_id=1 AND price > 10000 ORDER BY likes_count DESC LIMIT 20`
→ likes_count 정렬을 filesort 없이 처리할 수 있는가?

**[힌트 (B+Tree 시각화 제공)]**
brand_id=1 영역에서 인덱스의 실제 정렬 모습:
- price=8000 → likes_count: [10, 50, 200, 800] (같은 price 안에서만 정렬)
- price=12000 → likes_count: [5, 30, 100, 600, 1500] (같은 price 안에서만 정렬)
- price=18000, 25000, 30000 ... 각각 같은 price 그룹 안에서만 정렬됨

시나리오 비교:
- A. `price = 12000` (등치) → likes_count 정렬 살아있음
- B. `price > 10000` (범위) → 여러 price 그룹을 가로지르므로 likes_count 순서 깨짐

**[답변]**
> 시나리오 A : n (filesort 불필요)
> 시나리오 B : 따로 정렬
> price = 12000 인 데이터만 sort 하기 때문에 b+tree 에서 다른 데를 갈 필요 없지만
> price > 10000 이면 모든 데이터를 가져오도록 찾아야 하고, 다 찾은 다음 sort 한다

**[평가]**
- ✅ 본질 정확히 잡음. 등치 = 한 점, 범위 = 여러 그룹.

**[정리: 한 줄 룰]**
복합 인덱스 `(A, B, C)`에서, 앞쪽 컬럼이 **등치(=)** 조건이면 뒤 컬럼의 정렬이 살아있고, **범위(>, <, BETWEEN, LIKE 'abc%')** 조건이면 그 뒤 컬럼의 정렬이 깨진다. → "범위 조건 함정 (Range Condition Trap)"

[글감] "복합 인덱스에서 ORDER BY가 안 먹는 진짜 이유 — 등치는 한 점, 범위는 여러 그룹"

**[추가 질문 (c): filesort 제거하려면 인덱스를 어떻게 바꿔야 하나?]**
후보:
1. (brand_id, likes_count)
2. (brand_id, likes_count, price)
3. 그 외

**[답변]**
> 후보 2, brand_id, likes_count 순서로 인덱스 설정 후 나머지 price > 10000 조건에서 모든 데이터를 바로 가져올 수 있음

**[평가]**
- ✅ 후보 2 적절. ICP(Index Condition Pushdown) 효과 직관으로 잡음 — price 컬럼이 인덱스에 있으니 인덱스 단계에서 필터 가능
- ⚠ 단점은 명시 안 함 → 두 방향 흔들기 진행

**[흔들기 1: likes_count는 어떤 종류의 컬럼인가?]**
- brand_id: 거의 안 변함
- price: 가끔 변함
- likes_count: 매우 자주 변함
- 인덱스 컬럼 값이 변하면 인덱스에서 row 위치가 어떻게 되는가?
- Round 4의 핫 row 좋아요 동시성과 어떻게 연결되는가?

**[답변]**
> likes_count은 자주 변하는 데이터이기 때문에 인덱스 변경이 자주 발생할 것이다.
> likes_count가 1 증가하면 b+tree의 재정렬을 의미하고 1초 1000번이면 1초 1000번 재정렬이 발생한다

**[평가 + 보강]**
- ✅ 핵심 잡음
- 보강: "재정렬"의 정확한 의미 = **삭제 + 새 위치 삽입** (단순 값 변경이 아닌 물리적 이동)
- 1초 1000번 → 인덱스 페이지 **X-lock 경합** (Round 4 락 경합과 동일 메커니즘) + 페이지 split/merge → 디스크 I/O + buffer pool dirty 폭증 → fsync 부담
- **결론**: 자주 변하는 컬럼을 인덱스 컬럼으로 두는 건 위험. 비정규화/Materialized View가 필요한 이유로 자연 연결.

**[흔들기 2: LIMIT + 약한 필터의 함정]**
시나리오 비교:
- 시나리오 X: brand_id=1 상품 중 80%가 price > 10000 → 인덱스 몇 개 읽으면 LIMIT 20 채워지나?
- 시나리오 Y: 1%만 매칭 → ?

**[답변]**
> 시나리오 X : 20개
> 시나리오 Y : 조건에 맞는 모든 데이터를 읽게 된다. 매칭률이 1%일 때 (10개 데이터)... 20개 데이터가 될때까지 읽는다

**[평가 + 산수 명확화]**
- ✅ 본질 잡음. 시나리오 X는 정답 (20개)
- 시나리오 Y 산수: 매칭률 1% → 결과 20개 채우려면 평균 **2,000개의 인덱스 엔트리**를 거꾸로 스캔 (1/0.01 × 20)
- worst case: 매칭 row가 5개뿐이면 인덱스 거의 다 스캔해도 LIMIT 못 채움
- 본질: **"LIMIT N + ORDER BY + 약한 WHERE 필터" 함정** — filesort는 피했는데 인덱스 깊이 스캔 비용이 폭증

[글감] "ORDER BY ... LIMIT N"의 숨은 함정 — 정렬을 살려도 약한 필터를 만나면 인덱스를 끝까지 긁는다

**[Q3 종합 정리]**

| 함정 | 본질 |
|---|---|
| 범위 조건 함정 | 등치 = 한 점, 범위 = 여러 그룹 → 그 뒤 컬럼 정렬 깨짐 |
| LIMIT + 약한 필터 | 정렬은 살아도 인덱스 깊이 스캔, worst case는 인덱스 끝까지 |
| 자주 변하는 컬럼을 인덱스에 | 트리 위치 이동(삭제+삽입) + 락 경합 (Round 4 동시성과 직결) |

→ "likes_count를 인덱스 컬럼으로 두는 건 위험" 결론 = Q4의 비정규화/MV 논의로 자연 연결

---

---

## Q4. 좋아요 수 정렬 — JOIN+GROUP BY 비용 / 비정규화 vs Materialized View

**[질문 (a)/(b): JOIN+GROUP BY+ORDER BY+LIMIT 쿼리의 비용 분석]**
```sql
SELECT p.*, COUNT(l.id) AS like_count
FROM product p LEFT JOIN likes l ON p.id = l.product_id
GROUP BY p.id ORDER BY like_count DESC LIMIT 20;
```
- product 10만, likes 1000만 가정. PK + (product_id) 인덱스 있음

**[답변]**
> a : product, likes join을 통해 하나의 테이블을 만들고, GROUP BY → ORDER BY 순으로 처리. 모든 데이터에서 20개만 뽑는다.
> b : 모든 데이터를 생성한 후 20개를 뽑아야 하기 때문에 20개 뽑기 전 까지의 과정이 느리다

**[정리: TOP-N 최적화 실패]**
- ORDER BY 키가 "컬럼"이 아니라 "집계 결과값"이면, 모든 그룹의 집계가 끝나야 정렬이 가능 → LIMIT은 정렬 끝난 후에야 적용
- JOIN(1000만 row) → GROUP BY(10만 그룹) → ORDER BY filesort(10만) → LIMIT 20
- 인덱스 정렬이 살아있으면 N개 읽고 멈출 수 있는데(TOP-N), 집계 결과로 정렬할 땐 멈출 지점을 알 수가 없음

[글감] LIMIT 20이 거짓말이 되는 순간 — 정렬 키가 컬럼인가 집계값인가가 가른다

**[질문 (c-1)/(c-2)/(c-3): 비정규화 (product에 like_count 추가)로 가면?]**
```sql
ALTER TABLE product ADD COLUMN like_count INT;
CREATE INDEX idx_like_count ON product(like_count);
SELECT * FROM product ORDER BY like_count DESC LIMIT 20;
```

**[답변]**
> c-1 : product의 like_count만 정렬해서 20개만 가져오기 때문에 그전 JOIN+GROUP BY+filesort+full sort 과정이 필요없다
> c-2 : 인덱스의 비용은 product로 넘어갔다. like_count를 인덱스로 만든다면 +1을 한다면 인덱스의 b+tree는 자주 변경될 것이다
> c-3 : (질문 의도 명확화 필요)

**[흔들기 1: Lost Update가 정말 발생하나?]**
패턴 A (애플리케이션 read-modify-write 분리): SELECT → +1 → UPDATE → lost update 발생 ✓
패턴 B (DB 원자적): UPDATE x = x + 1 → X-lock으로 직렬화 → lost update 발생 안 함

**[답변]**
> 패턴 A 에서 발생하네

**[흔들기 2: TPS는 정말 1000인가?]**
- 1000개 UPDATE가 같은 row의 X-lock을 두고 줄 서서 직렬화
- 한 트랜잭션 평균 5ms → 같은 row TPS = 200
- 1000 RPS인데 200 TPS 한계 → 800개 대기 큐 → 응답 지연 폭증 → DB connection pool 고갈 → 도미노 장애

**[답변]**
> 200 tps

**[흔들기 3: 어떤 락 패턴인가?]**
- 낙관적 락 ❌ — 버전 충돌 시 재시도, 평소엔 락 안 잡음
- **비관적 락 (X-lock)** ✓ — 들어오는 순간 락 잡고 다른 트랜잭션은 대기 (Round 4 재고 차감과 동일)

**[답변]**
> 비관적 락

**[Q4 (c) 정리: 비정규화의 진짜 비용]**
| 갈래 | 메커니즘 | 영향 |
|---|---|---|
| 인덱스 변동 | like_count 변경 시 인덱스 트리에서 row 위치 이동 | 페이지 split/I/O |
| 핫 row 락 경합 | 같은 row에 X-lock 직렬화 | TPS 상한 → 응답 지연 → 타임아웃 도미노 |

[글감] 비정규화의 진짜 비용은 인덱스가 아니라 핫 row 비관적 락 경합 — 1000 RPS인데 200 TPS가 한계라면 무슨 일이?

**[질문 (d): 핫 row 락 경합을 어떻게 풀까?]**
방향 힌트: 시점 분리 / 저장소 분리 / 카운터 분산

**[답변]**
> spring event 또는 kafka를 사용하여 이벤트 처리, redis에 저장하고, 시간마다 db 업데이트

**[흔들기 1: 컨슈머에서 락 경합은 사라지나?]**
단순 이벤트 발행만으론 부족 — 컨슈머가 같은 row를 1000번 UPDATE하면 락 경합 그대로

**[답변]**
> 쌓인 이벤트 개수를 세워 한꺼번에 업데이트
✓ 이벤트 배치 집계 — product별로 그룹핑 후 +N 한 번에 UPDATE. 락 경합 1/N로 감소

**[흔들기 2: Redis에 카운터 두면 정렬은 어디서?]**

**[답변]**
> db에 있는 like_count를 가지고 정렬을 할거야. 트레이드오프 필요 — 정확성이냐, 정확성 떨어져도 성능을 잡냐
✓ DB의 stale 값으로 정렬. 정확성 vs 성능 트레이드오프 명확히 잡음

**[흔들기 3: UX 보완 (사용자가 좋아요 직후 새로고침)]**

**[답변]**
> 프론트에서 캐시로 좋아요한 표시. 비슷한 비동기 시간마다 캐시 지우고 백엔드 호출
✓ Optimistic UI 패턴. "전역 like_count는 stale, 본인 액션은 즉시 반영" — 일관성 경계를 사용자별로 분리

**[Q4 종합: 좋아요 수 정렬 4가지 패턴]**

| 패턴 | 조회 | 쓰기 | 실시간성 | 확장성 |
|---|---|---|---|---|
| ① 정규화 + JOIN/GROUP BY | ❌ | ✅ | ✅ | ✅ |
| ② 비정규화 + 동기 갱신 | ✅ | ❌ 핫 row 락 경합 | ✅ | ⚠ |
| ③ 비정규화 + 비동기 갱신 (이벤트 배치) | ✅ | ✅ | ⚠ stale | ⚠ |
| ④ Materialized View / 조회 전용 테이블 | ✅✅ | ⚠ 별도 동기화 | ⚠ stale | ✅ 여러 view |

**핵심 인사이트:**
- ②는 운영 위험 (1000 RPS vs 200 TPS)
- ③/④는 같은 발상 — "읽기 구조와 쓰기 구조의 동기화 시점을 분리하라" = **Pre-aggregation의 본질**
- ④의 진짜 가치: 같은 데이터를 **조회 패턴별로 여러 view**로 미리 만들 수 있음

[글감] 비정규화 vs Materialized View — 둘 다 Pre-aggregation이지만, 동기 vs 비동기, 컬럼 추가 vs 별도 테이블의 차이가 운영에서는 결정적

---

---

## Q5. Redis 캐시 — 본질 / TTL / 무효화 / Cache Stampede

### Q5 (a) 캐시의 본질

**[질문]**
- a-1. DB가 1ms로 빠른데 Redis 캐시 적중 시 추가로 사라지는 비용은?
- a-2. 캐시가 정확도(정합성)를 잃는 시점은 정확히 언제?

**[1차 답변]**
> a-1 : 모르겠다
> a-2 : redis는 휘발될 수 있지만 db는 디스크에 저장하여 비휘발 된다

**[흔들기 — 시나리오 제시]**
- a-1: "1ms는 한 명 기준. 1만 명 동시 조회 시 DB connection / CPU는?"
- a-2: "휘발은 가용성 문제. 정상 동작 캐시도 어긋나는 순간은? (DB 가격 변경 시점)"

**[2차 답변]**
> a-1 : DB에게 주던 부담을 redis에 옮겼다
> a-2 : db는 업데이트 되었지만 redis에 데이터 동기화가 안되었을 때

**[평가]**
- ✅ a-1: "캐시는 빠른 게 아니라 부담을 덜어준다" — 본질 정확
- ✅ a-2: stale의 정확한 정의

**[정리]**
- 캐시는 **빠른 게 핵심이 아니라 가벼운 게 핵심** — DB의 다양한 부담(파싱/MVCC/락/I/O)을 단순 KV GET으로 환원
- 휘발성(가용성) ≠ stale(정합성). 다른 축의 문제

[글감] "캐시는 빠른 게 아니라 가벼운 것이다 — DB의 다양한 부담을 단순 GET으로 환원한다"

---

### Q5 (b) TTL 트레이드오프

**[질문]**
- b-1. TTL 1초 / 1시간 / 24시간 각각의 고유 문제?
- b-2. TTL 결정의 본질적 기준 두 가지?

**[1차 답변]**
> b-1 : TTL 1초 캐시하는 의미가 없다. TTL 1시간, TTL 24시간 상황에 따라 문제가 있지만 고유한 문제는 잘 모르겟다
> b-2 : 데이터가 얼마나 자주 변경되는가, 얼마나 자주 읽는가?

**[흔들기]**
- b-1: 1시간 시나리오 (운영 변경 반영 지연), 24시간 시나리오 (Cache Stampede 예고)
- b-2: "잔액 vs 가격 — 변경/읽기 빈도 같은데 TTL 다른 이유는?" (정합성 요구도)

**[2차 답변]**
> TTL 1시간 고유 문제 : 언제 갱신될 지 모르는 문제 발생
> TTL 24시간 고유 문제 : 한번에 많은 데이터가 갱신을 시도하면서 부하 발생, 그리고 데이터 갱신 전까지 고객은 잘못된 데이터를 보게 된다
> b-2 : 데이터의 중요도

**[평가]**
- ✅ b-1: 1시간은 "예측 불가능", 24시간은 "stale 시간 + Stampede" 둘 다 잡음
- ✅ b-2: "중요도" = 정합성 요구도, 본질 잡음

**[정리: TTL 결정 3대 기준]**
1. 변경 빈도 (얼마나 자주 바뀌는가)
2. 읽기 빈도 (캐시 효용 / hit rate)
3. **stale 허용도 (비즈니스 정합성 요구)**

| TTL | 고유 문제 |
|---|---|
| 1초 | hit rate 낮아 캐시 의미 적음 |
| 1시간 | 운영 변경 반영 지연 / 예측 불가능 |
| 24시간 | stale 시간 길고, 동시 만료 시 **Cache Stampede** |

[글감] "TTL은 만능이 아니다 — 짧으면 예측 불가, 길면 Stampede"

---

### Q5 (c) 무효화 전략 — Write-Through vs Cache-Aside

**[질문 (c-1)]**
패턴 A (Write-Through `cache.set`) vs 패턴 B (Cache-Aside `cache.delete`) — A의 함정은?

**[답변]**
> 동시에 많은 트래픽이 몰리게 되면 db는 락으로 처리가능하지만 캐시는 자연스럽게 하나만 처리되지만 a, b 에서 동시에 요청을 할 때 db에서 순서가 a -> b 이더라도, 캐시는 a -> b 일지 b -> a 일지 보장이 안된다.

**[평가]**
- ✅ 본질 정확. **DB 락은 캐시까지 보호해주지 않는다**.
- 캐시는 두 set의 **순서가 어긋나면 영구 stale** (TTL 만료까지)
- 핵심 룰: **set은 순서 의존, delete는 멱등** → 캐시 동기화는 set이 아니라 delete

[글감] "캐시 동기화에 set을 쓰면 안 되는 이유 — DB의 락은 캐시까지 보호해주지 않는다"

**[질문 (c-2)]**
패턴 B(delete)도 race가 있다 (read-write race). 어떻게 막을까?

**[답변]**
> TTL 설정, db + 캐시 를 한꺼번에 락으로 묶음

**[흔들기 — "락으로 묶음"]**
- DB + 캐시 락 = 분산 트랜잭션(2PC) → 운영 복잡도 폭증
- read에도 락 잡으면 캐시의 본질(빠름/가벼움) 파괴
- "강한 일관성을 원하면 캐시를 쓰지 마세요" — eventual consistency가 캐시의 운명

**[평가 + 보강]**
- ✅ TTL = 정답 (안전망). race로 stale이 생겨도 결국 만료로 정리
- ⚠ "락으로 묶음" = 캐시 본질과 충돌
- 추가로 알아둘 두 패턴:
  - **Delayed Double-Delete**: write 후 즉시 + 일정 시간 뒤 한 번 더 삭제 → 늦게 도착한 read의 set 정리
  - **Singleflight**: 동시 미스 합치기 (다음 d-4에서)

[글감] "캐시에 락을 묶고 싶을 때 — 강한 일관성을 원하면 캐시를 쓰지 마세요"

**[Q5 (c) 정리: 캐시 동기화 룰]**
1. 동기화는 **set이 아니라 delete** (멱등성)
2. delete가 있어도 **TTL은 항상 함께** (race의 안전망)
3. 강한 일관성 vs 캐시 성능은 트레이드오프 — 락으로 묶지 말 것

---

### Q5 (d) Cache Stampede

**[질문]**
- d-1/d-2/d-3. 단일 인기 키 동시 미스 / 다수 키 동시 만료 시나리오 분석
- d-4. 단일 키 시나리오 해결책?
- d-5. 다수 키 시나리오 해결책?
- d-6. 만료 자체를 사용자에게 노출 안 시키는 방법?

**[답변 d-1~d-3]**
> d-1 : 캐시 미스로 인해 db에 많은 트래픽이 발생한다
> d-2 : 5000개, 순간 db 커넥션 생성, 연결 시도, cpu 증가 가능
> d-3 : 순식간에 DB 과부하로 cpu 과부하가 발생하여 다른 서비스에 영향을 미칠것이다

**[답변 d-4~d-6]**
> d-4 : 모두 한꺼번에 db에 접근하는게 아니라 하나만 접근하도록 락을 걸어 캐시에 넣는거 까지 하고, 나머지 작업은 다시 캐시를 읽는다.
> d-5 : 만료시간을 랜덤하게 준다. 1시간이라면 50 ~ 70분 사이 랜덤 값으로 준다
> d-6 : 스케줄러나, 배치로 캐시 갱신, ttl 시간에 임박하면 비동기 이벤트로 캐시 갱신

**[흔들기 (d-4): "그 락은 어떤 락?"]**
- JVM `synchronized`는 분산 환경에서 무력 (각 WAS 인스턴스마다 따로)
- DB 비관적 락 = 캐시의 본질(DB 부담 경감)과 충돌 (락 잡으려고 DB까지 가야 함)
- 정답: **Redis 분산 락** (SETNX, Redisson)

**[답변]**
> jvm 락을 걸면 분산환경이기 때문에 정확한 동작이 어렵다. db에 비관적 락으로 잡으면 될것이다 → (보강 후) redis 분산락을 잡는다

**[Q5 (d) 정리: Cache Stampede 3대 패턴]**

| 시나리오 | 본질 | 해결 패턴 | 표준 명칭 |
|---|---|---|---|
| 단일 인기 키 동시 미스 | 같은 쿼리 N건이 DB로 | Redis 분산 락으로 1건만 DB 가고 나머지 캐시 재조회 | **Singleflight** |
| 다수 키 동시 만료 | 만료 시점이 같음 | TTL ±랜덤 | **TTL Jitter** |
| 만료 자체 회피 | 사용자 미스 노출 | 스케줄러 미리 갱신 / TTL 임박 시 비동기 갱신 | **Cache Warming / Refresh-Ahead** |

핵심 인사이트:
- **락은 캐시 시스템(Redis)에 두는 것이 자연스럽다** — DB에 두면 캐시의 본질 파괴
- 세 패턴은 **상호 보완** — 큰 시스템은 셋 다 함께 씀

[글감] "Cache Stampede의 세 얼굴 — 단일 키 폭주, 다수 키 동시 만료, TTL 자체 노출"

---

---

## Q6. 코드 기반 분석 — 이 라운드의 캐시 코드는 안전한가?

### Q6 (a) 캐시 패턴 분석

**[질문]**
- a-1. `loadProductDetail`이 사용하는 캐시 패턴 이름?
- a-2. `cache.set()`에 c-1의 race 함정이 적용되는가?

**[답변]**
> a-1 : Read-Through
> a-2 : 발생한다, Db에 순식간에 수정이 발생하면 캐시 데이터와 불일치가 발생한다

**[평가 + 정정]**
- a-1 ⚠ → **Cache-Aside** (application 코드가 직접 미스 처리). Read-Through는 캐시 시스템이 자동 처리.
- a-2 ✅ read-write race로 stale 가능 (Q5 c-2 시나리오 재현)

[글감] "Cache-Aside의 read 경로에도 set이 숨어있다 — read-write race는 항상 잠재"

### Q6 (b) Stampede 보호 분석

**[답변]**
> b-1 : 아니요 (Singleflight 없음)
> b-2 : 응 위험해
> b-3 : TTL 이 위험을 상쇄 하고 있어 → (정정 후) 워밍 + 마진(2분 < 3분)이 LIST를 보호

**[흔들기 b-3]**
- TTL은 Stampede 방어가 아니라 race 안전망
- 진짜 보호 = Cache Warming + 1분 마진 (Refresh-Ahead)
- 단, **DETAIL은 워밍 안 됨** → 비대칭

**[γ 답변]**
> γ-1 : race 안전망 ✅
> γ-2 : 상세 정보는 비즈니스에 영향을 끼칠 여부가 크기 때문 ⚠ → 양적 차이 (DETAIL 키 카디널리티 = 상품 수)
> γ-3 : 모르겠어 → 5가지 패턴 제시

**[정리: DETAIL 보호 5종 세트]**
1. Singleflight (Redis 분산 락)
2. TTL Jitter
3. Selective Warming (인기 상품)
4. Refresh-Ahead with Probability (XFetch)
5. Stale-While-Revalidate

[글감] "Refresh-Ahead의 안전 마진 — 워밍 주기 < TTL 차이가 곧 워밍 실패 허용 시간"
[글감] "Detail 캐시는 왜 워밍 안 되는가 — 키의 카디널리티가 워밍의 한계를 정한다"
[글감] "Detail 캐시 보호 5종 세트 — Singleflight / TTL Jitter / Selective Warming / Refresh-Ahead / Stale-While-Revalidate"

### Q6 (c) 무효화 + Local 캐시 비대칭

**[사실 발견]**
- `evictProduct` 정의는 있으나 **외부 호출자 없음** → write 후 정합성은 TTL에 의존
- Local TTL(60s/5m) > Redis TTL(30s/3m) → **Local이 Redis 갱신을 가림**
- 다중 WAS 환경에서 Local 동기화 메커니즘 없음

**[답변]**
> c-1 : 캐시는 그대로 이다 ✅ (TTL 만료까지)
> c-2 : Local 캐시 만료 때까지 부정확한 데이터를 조회 한다 ✅
> c-3 : redis pub/sub 기능을 이용하면 로컬 캐시 갱신할 수 잇다 ✅

**[정리: 다층 캐시 stale 분석]**
| 시각 | Local | Redis | 사용자 응답 |
|---|---|---|---|
| t=0 | 10000 (60s) | 10000 (30s) | 10000 |
| t=10 | 10000 | 10000 | 10000 (DB는 12000으로 변경됨) |
| t=31 | 10000 (hit) | 만료 | 10000 (Local hit가 Redis를 가림) |
| t=61 | 만료 | 만료 → DB 12000 set | 12000 |

**핵심 통찰**: stale 길이는 **min이 아니라 max(Local, Redis) = 60초**

**[Q6 코드 안전성 종합 진단]**
| 항목 | 상태 | 위험 |
|---|---|---|
| evictProduct 외부 호출 | ❌ 없음 | TTL 의존만 |
| Local TTL > Redis TTL | ⚠ | Local이 Redis 갱신 가림 |
| WAS 간 Local 동기화 | ❌ 없음 | 인스턴스 간 stale |
| Singleflight | ❌ | DETAIL Stampede 노출 |
| TTL Jitter | ❌ | 동시 만료 위험 |
| Cache Warming | ✅ LIST만 | DETAIL 비보호 |

**개선 방향:**
1. write 시점 evictProduct 호출 추가
2. Local TTL ≤ Redis TTL로 정렬
3. Redis pub/sub 으로 Local 동기 무효화

[글감] "다층 캐시의 함정 — 짧은 TTL이 긴 TTL을 가린다. stale 길이는 가장 가까운 캐시가 결정"
[글감] "분산 환경 Local 캐시 동기화 — Redis pub/sub 또는 Kafka, 둘 중 무엇이 운영에 맞나?"

---

---

## Q7. Pre-aggregation 실제 구현 — Q4의 발상이 코드에서 어떻게 살아있는가 (진행 중)

### 도입부 — Q4 4가지 패턴 환기

Q4에서 좋아요 수 정렬을 위한 4가지 패턴을 비교했음:
- ① 정규화 + JOIN/GROUP BY (느림)
- ② 비정규화 + 동기 갱신 (핫 row 락 경합)
- ③ 비정규화 + 비동기 갱신 (이벤트 배치)
- ④ Materialized View / 조회 전용 테이블

이 라운드 코드는 이 중 **무엇을, 어떻게** 구현했는가? 코드를 보면서 분석.

---

### Q7 분석 대상 코드 발췌

**[코드 1] `RankingRedisRepository.getTopRankings` (Daily 랭킹 조회 — Redis ZSet)**

```kotlin
override fun getTopRankings(date: LocalDate, offset: Long, count: Long): List<RankingEntry> {
    val key = RedisKeys.rankingKey(date.format(DATE_FORMATTER))  // "ranking:20260430"
    val result = redisTemplate.opsForZSet()
        .reverseRangeWithScores(key, offset, offset + count - 1) ?: emptySet()
    return result.mapNotNull { typedTuple -> ... }
}
```
→ **Redis Sorted Set (ZSet)** 사용. 점수 기반으로 정렬된 자료구조.

**[코드 2] `getTopRankingsFromDb` (DB Fallback)**

```kotlin
override fun getTopRankingsFromDb(offset: Long, count: Long): List<RankingEntry> {
    val sql = """
        SELECT pm.product_id,
               (pm.view_count * 0.1 + pm.like_count * 0.2 + pm.sales_count * 0.7) AS score
        FROM product_metrics pm
        INNER JOIN products p ON pm.product_id = p.id
        WHERE p.deleted_at IS NULL
        ORDER BY score DESC
        LIMIT ? OFFSET ?
    """
    ...
}
```
→ **`product_metrics` 테이블** — 상품별로 view/like/sales 누적 수치를 미리 저장한 별도 테이블. JOIN/GROUP BY 없음.

**[코드 3] `DailyRankingStrategy` (Redis 우선, DB 폴백)**

```kotlin
override fun getRankings(date: LocalDate, page: Int, size: Int): RankingResult {
    val entries = runCatching {
        rankingService.getTopRankings(date, page, size)        // Redis ZSet
    }.getOrElse {
        log.warn("Redis 랭킹 조회 실패, DB fallback 수행", it)
        rankingService.getTopRankingsFromDb(page, size)        // DB product_metrics
    }
    ...
}
```

**[코드 4] 시간 단위별 전략 분기**

```
DailyRankingStrategy   → Redis ZSet (실시간)
WeeklyRankingStrategy  → 별도 JDBC 리포지토리 (배치 집계)
MonthlyRankingStrategy → 별도 JDBC 리포지토리 (배치 집계)
```

---

### Q7 (a) Sorted Set 선택의 본질

> Daily 랭킹 저장 자료구조 후보:
> - **Hash** (`HSET ranking:20260430 productId score`)
> - **List** (productId 순서대로 push)
> - **Sorted Set / ZSet** (점수 기반 자동 정렬)
> 코드는 ZSet을 선택했음. 이유는?

**[질문]**
- a-1. 좋아요 +1 동작을 각 자료구조로 구현한다면?
  - Hash: 어떻게? 시간 복잡도?
  - List: 어떻게? 시간 복잡도?
  - ZSet `ZINCRBY`: 어떻게? 시간 복잡도?
- a-2. TOP 100 조회를 각 자료구조로 한다면?
  - Hash: ?
  - List: ?
  - ZSet `ZREVRANGE`: ?
- a-3. ZSet이 선택된 본질은 — "**정렬 상태를 미리 유지하는 자료구조**"라는 발상이 Q4의 어떤 패턴과 직결되나?

힌트 (a-3): Q4의 ④ 패턴 — "읽기 구조와 쓰기 구조의 동기화 시점 분리". ZSet은 **쓰기 시점에 정렬 비용을 분산**하는 자료구조.

---

### Q7 (b) `product_metrics` 테이블의 정체

> 코드 2에서 `product_metrics` 테이블 등장. `view_count`, `like_count`, `sales_count` 컬럼이 미리 누적되어 있음.

**[질문]**
- b-1. Q4의 어떤 패턴? (① 정규화 / ② 비정규화 동기 / ③ 비정규화 비동기 / ④ Materialized View)
- b-2. 이 테이블 없으면 `getTopRankingsFromDb`의 SQL은 어떻게 생기나? (likes / products / orders 테이블에서 직접 집계한다면)
- b-3. Redis ZSet이 있는데도 굳이 DB에 또 `product_metrics`를 두는 이유 — 이중 저장의 의도는?

힌트 (b-3): 코드 3의 `getOrElse` — Redis 장애 시 폴백. 가용성 / 회복 탄력성 측면에서 본질은?

---

### Q7 (c) 시간 단위별 전략 분기의 본질

> Daily는 Redis ZSet (실시간), Weekly/Monthly는 별도 배치 집계. 같은 "랭킹"인데 왜 다른 전략?

**[시나리오 비교]**
- **Daily**: 좋아요 1개 → 즉시 점수 반영 필요? 1분 늦어도 됨? 1시간 늦어도 됨?
- **Monthly**: 한 달간 누적된 점수. 1분 단위 실시간성이 의미 있나?

**[질문]**
- c-1. Daily가 실시간성을 요구하는 이유는? (사용자 경험 / 비즈니스 측면)
- c-2. Monthly가 실시간성을 요구하지 않는 이유는?
- c-3. 그 결과 자료구조 선택이 어떻게 갈리는가? (실시간 누적 vs 배치 집계)

힌트: 시간 범위가 길수록 "한 건의 +1"이 전체 점수에 미치는 영향이 작음 → 실시간 누적의 가치 감소 → 배치 집계가 효율적.

---

**[답변 대기 중 — 다음 재개 시 (a)/(b)/(c)부터]**

---

## ⏸ 학습 일시 중단 (Q7 진행 중)

이 시점까지 진행 — 다음 재개 시 Q7 (a)/(b)/(c) 답변부터 이어갈 것.

남은 영역:
- Q7 (a)/(b)/(c) 답변 + 흔들기
- Q7 (d) — RankingReconciliation (Redis-DB drift 보정 배치)
- Q7 (e) — RankingSwap (무중단 갱신, Staging → Active swap 패턴)
- Q7 (f) — carry over (어제 점수 가중치 이월 Lua script)
- Q8 — 백지 설계 테스트

