# 6차 스프린트 — MySQL 전환과 전자정부 공통컴포넌트 도입

작업일: 2026-10-22 · 브랜치: `feature/sprint6-common-components-mysql` · 이전 문서: [5차 스프린트](SPRINT5_ACCOUNT_PRIVACY_UI.md)

## 1. 범위

실무 eGov 프로젝트와 가장 차이가 컸던 세 가지를 바꿨다.

1. 개발 도구: VS Code → Eclipse ([설정 안내](ECLIPSE_SETUP.md))
2. DB: H2 인메모리 → MySQL 8, 스키마 변경은 Flyway 로 관리
3. 공통코드·첨부파일·공지·FAQ·채번: 직접 만든 기능 → 전자정부 공통컴포넌트 v5.0.7 ([사용 내역](COMMON_COMPONENTS.md))

로그인·권한 공통컴포넌트, 공지 첨부파일, 회원가입은 이번 범위가 아니다.

## 2. 바뀐 것

| 영역 | 이전 | 이번 | 검증 |
| :--- | :--- | :--- | :--- |
| DB | H2 인메모리 (`schema.sql`, `data.sql`) | MySQL 8, Flyway `V1`(기본 스키마) → `V2`(공통컴포넌트 전환), 데모 데이터는 local·test 에만 | 전체 테스트, 개발 DB 실제 기동 |
| DB 계정 | `sa` (모든 권한) | 테이블 변경 `dpms_owner` / 실행 `dpms_app`(조회·등록·수정·삭제만) | 실행 계정의 `CREATE TABLE` 거부 확인 |
| 비밀번호 | 없음 | 저장소에 두지 않음 (로컬 파일·환경변수, CI 는 매번 무작위) | `git diff` 비밀값 검사 |
| 테스트 DB | 기동마다 새 H2 | `dpms_test` 를 실행마다 비우고 다시 생성 (이름이 `_test` 로 끝날 때만) | `TestDatabaseConfig` |
| 공통코드 | `com_code_group/detail` + 자체 Mapper | `EgovCmmUseService` + `COMTCCMMN*` (코드 ID `DPM001~005`) | `CommonComponentIntegrationTest` |
| 첨부파일 | `com_file` + 숫자 ID, `/files/{번호}` | `EgovFileMngService` + `COMTNFILE/DETAIL`, `/files/FILE_…/{순번}` | `WorkflowIntegrationTest`, `SecurityIntegrationTest` |
| 공지사항 | 코드 생성기로 만든 `notice` | 게시판 `EgovArticleService` + `COMTNBBS` | `NoticeIntegrationTest` |
| FAQ | 코드 생성기로 만든 `faq` | `EgovFaqService` + `COMTNFAQINFO` (분류·순서·사용여부 추가) | `FaqIntegrationTest` |
| 채번 | `ids` | `COMTECOPSEQ` (공통컴포넌트와 같은 Bean 이름) | `CommonComponentIntegrationTest` |
| 보안 헤더 | H2 콘솔 때문에 CSRF·iframe·CSP 예외 | 콘솔 제거와 함께 예외 제거, `frame-ancestors 'none'` | `WebUiIntegrationTest` |
| CI | JDK 17 | + MySQL 8.4 LTS 컨테이너 (이미지 digest 고정) | GitHub Actions |
| 개발 도구 | VS Code | Eclipse 공유 실행 설정, Eclipse 컴파일러 경고 0 | ECJ 컴파일 |

테스트 144건 → 153건.

## 3. MySQL 전환

### Flyway — 스키마도 코드처럼 버전을 남긴다

```text
db/migration/V1__init_schema.sql                  기존 테이블을 MySQL 문법으로 (기준 데이터 포함)
db/seed/V1_1__demo_data.sql                       데모 계정·공고·신청서 (local·test 프로필만)
db/migration/V2__adopt_egov_common_components.sql 옛 테이블 → 공통컴포넌트 테이블로 데이터를 옮기고 삭제
```

서버가 뜰 때 아직 적용하지 않은 버전만 순서대로 실행하고 `flyway_schema_history` 에 기록한다. 적용한 파일을 고치면 체크섬이 달라져 기동을 멈춘다. 구조를 바꿀 때는 V3 를 추가한다. 운영 DB 를 통째로 지우는 `clean` 은 테스트 프로필에서만 허용했다.

V1 과 V2 를 나눈 이유: 이미 데이터가 쌓인 DB 를 공통컴포넌트 구조로 바꾸는 상황을 그대로 재현하려고 했다. V2 는 옛 테이블 데이터를 새 테이블로 옮긴 뒤 옛 테이블을 지운다 (공지 ID `NTC_0000000001` → 게시물 ID `1` 처럼 값도 바꾼다).

### H2 문법에서 바꾼 것

| H2 (MODE=MySQL) | MySQL | 이유 |
| :--- | :--- | :--- |
| `TIMESTAMP` | `DATETIME` | TIMESTAMP 는 2038년 한계와 세션 시간대 변환이 있다. 업무 일시는 입력한 값 그대로 저장 |
| `CLOB` | `TEXT` | MySQL 에는 CLOB 이 없다. 입력 한도(4000자)를 담을 수 있는 크기 |
| `CREATE INDEX IF NOT EXISTS` | `CREATE TABLE` 안의 `INDEX` | MySQL 8.0 은 인덱스에 IF NOT EXISTS 가 없다 |
| 이름 없는 FK | `fk_…` 이름 지정, 컬럼마다 `COMMENT` | 오류 메시지·테이블정의서에서 바로 알아볼 수 있게 |

### DB 계정과 비밀번호

| 계정 | 권한 | 쓰는 곳 |
| :--- | :--- | :--- |
| `dpms_owner` | `dpms`, `dpms_test` 의 모든 권한 | Flyway 마이그레이션만 |
| `dpms_app` | 같은 두 DB 의 SELECT·INSERT·UPDATE·DELETE, 잠금 대기 목록(`performance_schema.data_lock_waits`) 조회 | 서버 실행, 테스트 |

- 로컬: `scripts/setup-local-mysql.ps1` 이 DB·계정을 만들고 무작위 비밀번호를 `config/dpms-secret.yml`(Git 제외)에 저장한다. root 비밀번호는 mysql 클라이언트가 직접 묻는다.
- `application.yml` 에는 비밀번호 기본값을 두지 않았다. 값이 없으면 기동하지 않는다 (잘못된 계정으로 조용히 뜨는 것보다 낫다).
- CI: 실행할 때마다 `openssl rand` 로 비밀번호를 만들고 로그에서 가린다. 컨테이너 포트는 127.0.0.1 에만 연다.
- 운영이라면 실행 계정과 마이그레이션 계정을 다른 서버·다른 비밀 저장소로 분리하고, 실행 계정에서 `performance_schema` 권한은 뺀다.

### H2 에서는 안 보이던 문제

| # | 증상 | 원인 | 해결 |
| :-: | :--- | :--- | :--- |
| 1 | 테스트 3건 `ClassCastException: LocalDateTime → Timestamp` | MySQL 드라이버는 DATETIME 을 `LocalDateTime` 으로 준다 (H2 는 `Timestamp`) | 형변환 없이 값 비교 |
| 2 | 공통컴포넌트 게시판 상세 조회가 MySQL 에서만 실패할 구조 | `selectArticleDetail` 이 조회수 UPDATE 를 포함하는데, D-PMS 규칙상 `select*` 는 읽기 전용 트랜잭션 → 드라이버가 `Connection is read-only` 로 거부 | 조회와 조회수 증가를 나눠 호출. 거부 동작 자체를 테스트로 남김 |
| 3 | 동시성 테스트의 "잠금 대기 확인" 쿼리가 없음 | H2 전용 `INFORMATION_SCHEMA.SESSIONS.BLOCKER_ID` | `performance_schema.data_lock_waits` 조회 (그 표에만 SELECT 권한 부여) |
| 4 | 공통컴포넌트 추가 후 전체 통합 테스트 기동 실패 | 지운 `CommonCode_SQL.xml` 이 `target/classes` 에 남아 없는 별칭을 찾음 | `clean verify` (기존 규칙 재확인) |
| 5 | 공통컴포넌트 SQL 별칭 `FileVO` 와 업무 별칭 `fileVO` 충돌 | MyBatis 별칭은 대소문자를 구분하지 않는다 | 업무 FileVO 를 지우고 공통컴포넌트 FileVO 사용 |
| 6 | 기동 시 `CglibAopProxy … final` 경고 4건 | Spring Boot 의 `@Repository` 예외 변환이 공통컴포넌트 DAO 마다 CGLIB 프록시를 만듦 | 예외 변환은 MyBatis 가 이미 하므로 `spring.dao.exceptiontranslation.enabled=false` |
| 7 | 로컬은 통과, CI(리눅스)에서만 계정 잠금 테스트 2건 실패 (`/login?error=locked` 기대값과 다름) | CI 의 JVM 시간대는 UTC. 테스트가 `Timestamp.valueOf(…)` 로 넣은 잠금 시각을 드라이버가 UTC → KST(접속 시간대)로 9시간 옮겨 저장 | 시간대 변환이 없는 `LocalDateTime` 으로 바인딩. 같은 원인으로 앱 Clock(서버 기본 시간대)과 마감 배치(자정)도 `Asia/Seoul` 로 고정 |

CI 는 리눅스라 MySQL 테이블 이름이 대소문자를 구분한다(`lower_case_table_names=0`). 업무 테이블은 소문자, 공통컴포넌트 테이블은 대문자로 SQL 과 DDL 을 맞췄고 CI 에서 확인한다.

### MySQL 버전

이 PC 의 MySQL 은 8.0.46 이다. 8.0 은 2026년 4월 지원이 끝난 마지막 버전이라 보안 패치가 더 나오지 않는다. CI 는 장기 지원판 8.4 를 쓴다. 코드는 두 버전에서 같게 동작하지만 로컬도 8.4 로 올리는 것을 권한다. 또 이 PC 의 MySQL(3306)·MariaDB(3307)는 모든 네트워크 주소(0.0.0.0)에서 접속을 받는다. 개발 PC 라면 `my.ini` 에 `bind-address=127.0.0.1` 을 두는 편이 안전하다.

## 4. 공통컴포넌트 도입

자세한 목록과 원본 대비 변경점은 [COMMON_COMPONENTS.md](COMMON_COMPONENTS.md) 에 있다. 요약하면:

```text
NoticeController ─▶ NoticeServiceImpl ─▶ EgovArticleService ─▶ EgovArticleDAO ─▶ EgovArticle_SQL_mysql.xml
  (화면·URL)          (권한·게시판 고정·404)   (공통컴포넌트, 원본 패키지 그대로)
```

- 공통컴포넌트는 `egovframework.com` 패키지 그대로 두고, 업무 서비스가 호출한다. 화면(JSP)·컨트롤러는 가져오지 않았다.
- 원본을 그대로 쓰면 안 되는 곳이 있었다. 대표적인 세 가지:
  1. **트랜잭션 규칙 충돌** — 원본 설정은 모든 메서드가 쓰기 트랜잭션이다. D-PMS 규칙(`select*` 읽기 전용)을 함께 적용하면 "조회하면서 조회수를 올리는" 메서드가 MySQL 에서 거부된다.
  2. **검색 조건 500 오류** — 게시판·FAQ SQL 이 `searchCnd == 0` 처럼 숫자와 비교한다. 코드 생성기 원본에서 찾았던 것과 같은 문제가 공통컴포넌트에도 있었다.
  3. **화면 값을 그대로 저장** — 작성자 ID, 첨부파일 ID 를 VO 값 그대로 INSERT 한다. 감사 값(`#{audit.*}`)과 바인딩 차단 목록으로 막았다.
- 고친 파일마다 머리말 개정이력에 `2026.10.22 gwangco D-PMS 적용: …` 을 남겼다. eGov 소스의 관례이고, 원본을 업데이트할 때 체크리스트가 된다.

## 5. Eclipse

- 워크스페이스는 프로젝트 밖(`Desktop\pms`)에 두고 프로젝트는 참조로 가져온다.
- JavaSE-17 실행 환경을 Temurin 17 에 연결, 외부 변경 자동 새로고침, Quick Diff 기준 Git.
- 공유 실행 설정 `eclipse/D-PMS 로컬 실행.launch`, `eclipse/D-PMS 전체 재빌드 (clean verify).launch`.
- Eclipse 컴파일러(ECJ)로 전체를 컴파일해 경고 3건(직렬화 ID 누락, 미사용 import)을 정리했다.

## 6. 검증

- `./mvnw clean verify` — 153건 성공 (로컬 MySQL 8.0.46, JVM 시간대 KST·UTC 둘 다), GitHub Actions(MySQL 8.4 LTS, 리눅스)
- 개발 DB `dpms` 에서 실제 기동 — Flyway 3개 버전 적용 1.0초, 기동 8.4초
- 내장 브라우저 점검 — 메인, 공지 목록·작성자 검색·상세·등록·수정, FAQ 분류 탭·등록, 신청 상세. 콘솔 오류 0건, 공지 본문의 HTML 태그는 글자로 표시
- 매퍼 전체에서 `${}` 문자열 치환 0건, 비밀번호 파일 Git 제외 확인
- Eclipse 컴파일러 기본 설정 오류·경고 0건

## 7. 남은 일

- [ ] 공지 첨부파일 (게시판 + 첨부파일 연동, 비로그인 사용자의 공지 첨부 다운로드 권한 설계)
- [ ] 로컬 MySQL 8.4 LTS 업그레이드, 로컬 DB 접속을 127.0.0.1 로 제한
- [ ] 공통컴포넌트 로그인·권한(`uat`, `sec`)과 현재 Spring Security 구성 비교
- [ ] 기업 회원가입, 신청서 임시저장 (기존 로드맵)
