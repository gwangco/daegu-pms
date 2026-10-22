# 변경 이력 (CHANGELOG)

형식: [Keep a Changelog](https://keepachangelog.com/ko/1.1.0/) 를 참고해 스프린트 단위로 기록한다.

---

## [0.6.0] 6차 스프린트 — MySQL 전환·전자정부 공통컴포넌트 도입 (2026-10-22)
브랜치: `feature/sprint6-common-components-mysql` · 상세: [docs/SPRINT6_MYSQL_COMMON_COMPONENTS.md](docs/SPRINT6_MYSQL_COMMON_COMPONENTS.md)

### 추가 (Added)
- 전자정부 공통컴포넌트 v5.0.7: 공통코드(`EgovCmmUseService`), 첨부파일(`EgovFileMngService`), 게시판(`EgovArticleService`), FAQ(`EgovFaqService`) — [사용 내역과 원본 대비 변경점](docs/COMMON_COMPONENTS.md)
- Flyway 스키마 이력 관리: `V1` 기본 스키마, `V2` 공통컴포넌트 전환(기존 데이터 이전), 데모 데이터는 local·test 프로필에서만
- 로컬 MySQL 준비 스크립트 `scripts/setup-local-mysql.ps1` (DB·계정 생성, 무작위 비밀번호 파일)
- Eclipse 공유 실행 설정 `eclipse/` (로컬 실행·디버깅, Maven `clean verify`)와 [설정 안내](docs/ECLIPSE_SETUP.md)
- 테스트 153건 (+9): 공통컴포넌트 구성·트랜잭션 규칙·V2 마이그레이션 결과, 첨부파일 주소 형식, 공지 검색 조건, 업무 시간대

### 변경 (Changed)
- DB: H2 인메모리 → **MySQL 8** (CI 는 8.4 LTS 컨테이너), 시간 컬럼 DATETIME, 컬럼 COMMENT
- 공지사항·FAQ: 코드 생성기로 만든 테이블 → 공통컴포넌트 `COMTNBBS`, `COMTNFAQINFO` (공지 주소 `/notices/{번호}`)
- 첨부파일: `com_file` → `COMTNFILE`/`COMTNFILEDETAIL`, 다운로드 주소 `/files/{FILE_ID}/{순번}`
- 공통코드 그룹 ID → 공통컴포넌트 코드 ID `DPM001`~`DPM005`, 채번 테이블 `ids` → `COMTECOPSEQ`
- 테스트는 MySQL 테스트 전용 DB(`dpms_test`)를 실행마다 다시 만들고, 동시성 테스트는 `performance_schema.data_lock_waits` 로 잠금 대기 확인
- 개발 도구 VS Code → Eclipse, Eclipse 컴파일러 경고 3건 정리

### 보안 (Security)
- DB 계정 분리: 테이블 변경(`dpms_owner`)과 실행(`dpms_app`, 조회·등록·수정·삭제만)
- DB 비밀번호를 저장소에 두지 않음: 로컬은 Git 제외 파일, CI 는 실행마다 무작위 생성 후 로그 마스킹, 기본값 없음(없으면 기동 실패)
- H2 웹 콘솔 제거와 함께 콘솔용 CSRF 예외·iframe 허용·CSP 예외 제거, `frame-ancestors 'none'`
- 공통컴포넌트 원본 보완: 작성자·첨부파일 ID 를 화면 값으로 저장하지 않음, 게시물 비밀번호 컬럼 미조회, 파일 저장 경로를 업로드 루트 기준으로 검증
- CI MySQL 이미지 digest 고정

### 수정 (Fixed)
- 공통컴포넌트 게시판·FAQ 의 "조회 + 조회수 증가" 메서드가 읽기 전용 트랜잭션에서 MySQL 에 거부되는 구조 → 나눠 호출
- 공통컴포넌트 검색 SQL 의 숫자 비교(`searchCnd == 0`)로 문자 값이 오면 500 이 나던 문제
- 조회수 동시 증가 누락(읽고 쓰기 2단계 → SQL 한 번), 삭제 시 제목 덮어쓰기
- 공통컴포넌트 DAO 에 `@Repository` 예외 변환 CGLIB 프록시가 붙어 생기던 기동 경고
- 업무 Clock·공고 마감 배치가 서버 기본 시간대를 따르던 문제 → 한국 시간(Asia/Seoul) 고정 (UTC 서버에서 배치가 오전 9시에 돌 수 있었음)

---

## [0.5.0] 5차 스프린트 — 계정 보안·개인정보 보호·화면 개선 (2026-10-22)
브랜치: `feature/sprint5-account-privacy-ui` · 상세: [docs/SPRINT5_ACCOUNT_PRIVACY_UI.md](docs/SPRINT5_ACCOUNT_PRIVACY_UI.md)

### 추가 (Added)
- 로그인 5회 연속 실패 시 30분 계정 잠금, 접속 기록(성공·실패·잠김·로그아웃·IP), 총괄관리자 사용자 관리·잠금 해제·접속 기록 화면
- eGov 암호화 서비스(ARIA)로 연락처·이메일 암호화 저장, 관리자 목록 마스킹, 본인 정보 화면(`/mypage`)
- MyBatis 인터셉터가 모든 SQL 에 감사 값(`#{audit.userId}`, `#{audit.now}`)을 넣어 수정자·수정일시 자동 기록
- 정산 반려 건 보완 후 재요청, 처리 이력 타임라인, 과제별 정산 목록 페이징·상태 필터, 담당자 정산 검수함
- 메인 화면 역할별 "처리할 일" (대기 건수와 바로가기)
- 자주 묻는 질문(FAQ): eGov 코드 생성기로 두 번째 생성 → 10개 기준과 추가 5개 항목 보완, 공통코드 분류 탭 ([기록](docs/egov-codegen/README.md#3-두-번째-생성-자주-묻는-질문faq))
- 개인정보처리방침 화면, VS Code 공유 설정(디버깅·빌드 작업·추천 확장)과 [개발 환경 문서](docs/VS_CODE_SETUP.md)

### 변경 (Changed)
- eGovFrame 5.0.2 → **5.1.0** (Spring Boot 3.5.16, Spring 6.2.19, Spring Security 6.5.11-egov.1, JUnit 6)
- Thymeleaf Layout Dialect 공통 레이아웃, Bootstrap·아이콘·Chart.js 를 외부 CDN 대신 WebJars 로 내장, 웹 접근성 기본 요소 보완
- 엑셀 다운로드를 SXSSF 스트리밍으로 바꾸고 고정 열 너비·머리글 행 고정
- 사용자 조회 컬럼 최소화(인증 조회만 비밀번호 해시), 미사용 Mapper 제거, 정산 상태 상수 위치 정리

### 보안 (Security)
- CSP `script-src 'self'`: 인라인 스크립트·이벤트를 공통 스크립트로 이동, Referrer-Policy, 세션 쿠키 HttpOnly·SameSite=Lax, URL 세션 ID 미사용
- 잠긴 계정은 비밀번호 비교 전에 거부, 없는 아이디는 접속 기록에 입력값 대신 NULL 저장
- 암호화 키는 운영 환경변수로 분리, 키 길이·해시가 맞지 않으면 기동 실패
- MyBatis 1차 캐시 범위를 STATEMENT 로 바꿔 행 잠금 뒤 재조회가 실제 DB 값을 읽도록 함

### 수정 (Fixed)
- Windows Maven Wrapper 가 일반 `.m2` 폴더에서 `Cannot index into a null array`로 실패하던 문제
- 조회 결과를 그대로 마스킹하면 같은 트랜잭션의 다음 조회가 가려진 값을 받던 문제 → 복사본 반환
- VS Code 내장 Lombok 과 프로젝트 Lombok 버전 차이로 생기던 편집기 오탐 → 프로젝트 Lombok 지정
- 메시지 키를 변수로 넘기면 누락 키 검사가 찾지 못하던 문제(FAQ 안내) → 호출 위치에 키를 적고 검사 테스트 추가
- 표 높이가 소수점일 때 Windows 브라우저의 목록 표마다 불필요한 세로 스크롤바가 생기던 문제

### 테스트
- 78건 → **144건**: 화면 공통 규칙 6, 감사 컬럼 4, 계정 잠금 8, 마스킹 17, 암호화 8, 정산 재요청 6, 대시보드·공통코드 5, 엑셀 2, FAQ 9, 메시지 키 호출 규칙 1
- 실행 JAR 을 헤드리스 Chrome 으로 32개 화면 × 2개 폭(1366·390px) 점검: CSP 위반·콘솔 오류·가로 넘침·라벨 없는 입력칸 0건

---

## [0.4.0] 4차 스프린트 — 메시지 외부화와 보안·무결성 보완 (2026-10-21~22)
브랜치: `codex/sprint4-messages-security` · 상세: [docs/SPRINT4_MESSAGES_SECURITY.md](docs/SPRINT4_MESSAGES_SECURITY.md)

### 변경 (Changed)
- 서비스의 사용자 예외, 컨트롤러 성공/오류 안내를 UTF-8 메시지 번들 + `AppMessages`로 통합
- eGov 공통 `messageSource`를 재사용하고 `BusinessException`/`NotFoundException`/`AccessDeniedException` 분류 유지
- 한국 로케일의 금액·날짜·상태 인자 처리, 파일명 정제 함수의 메시지 의존성 명시

### 보안 및 수정 (Security / Fixed)
- 신청 상태 변경·평가위원 배정·채점의 신청서 행 잠금을 통일해 종료 상태 덮어쓰기와 미완료 평가가 있는 선정 차단
- 공고 등록 서비스에 담당자/관리자 권한 재검사 추가
- 정산 공급가액 양수 검사 및 `Math.addExact`로 부가세 합계 오버플로에 의한 예산 검사 우회 차단
- 업무/Mapper 기본 로그를 DEBUG → INFO로 변경하여 SQL 바인딩 값의 기본 로그 노출 방지

### 추가 (Added)
- 메시지 키 누락·인자·로케일·HTTP 예외 분류 회귀 테스트
- 별도 스레드·트랜잭션과 실제 DB 잠금 대기로 검증하는 심사 경쟁 상태 테스트
- 서비스 직접 인가, 정산 경계값, 민감 입력의 로그 비노출 테스트
- 3부 학습용 공개 문서와 로컬 작업 일지 갱신
- 전체 테스트 78건 성공(20건 추가), 실행 JAR의 역할별 실제 HTTP·오류 안내·인가·CSRF 검증

---

## [0.3.0] 3차 스프린트 — 전자정부 표준프레임워크 5.0 전환 (2026-10-21)
브랜치: `feature/egovframe5-migration` · 상세: [docs/EGOVFRAME_MIGRATION.md](docs/EGOVFRAME_MIGRATION.md)

### 변경 (Changed)
- 빌드: Gradle → **Maven** + `org.egovframe.boot:egovframe-boot-starter-parent:5.0.2` (Spring Boot 3.3.5 → 3.5.6)
- 패키지: 계층별 구조 → **eGov 표준 업무별 구조** (`업무/service`, `service/impl`, `web`), VO 명명 통일(`XxxVO`)
- 서비스: 모든 ServiceImpl 이 `EgovAbstractServiceImpl` 상속, Mapper 는 `@EgovMapper`
- 트랜잭션: `@Transactional` → eGov 방식 **AOP 선언적 트랜잭션**(포인트컷 일괄 적용)
- 페이징: 자체 구현 → eGov `PaginationInfo` + `AbstractPaginationRenderer` 확장 렌더러
- 설정값: 페이징 값을 `EgovPropertyService` 로 조회
- 로깅: Logback → **Log4j2** (eGov 실행환경 표준)
- MyBatis 설정: 자동 구성 → eGov 방식 Java Config + `sql-mapper-config.xml`, SQL 파일 `egovframework/sqlmap/mappers/*_SQL.xml`

### 추가 (Added)
- eGov **예외 후처리**(`ExceptionTransfer`) — 업무 예외는 장애 로그에서 제외하도록 커스터마이징
- eGov **ID Generation** — 신청 접수번호 `DPMS-00000001`, 공지 ID `NTC_0000000001`
- **공지사항** 게시판 — eGovFrame VS Code 확장의 CRUD 코드 생성기로 뼈대 생성 후 10개 항목 보완 ([기록](docs/egov-codegen/README.md))
- GitHub Actions CI (PR 마다 `./mvnw verify`), PR 템플릿, Maven Wrapper(SHA-256 검증)
- 테스트 18건 추가 (총 58건): eGov 실행환경 구성, 페이지 렌더러, 접수번호, 공지사항

### 수정 (Fixed)
- `select*` 이름의 메서드 안에서 조회수를 UPDATE 하던 문제 (eGov 트랜잭션 규칙상 읽기 전용) → `updateViewCount` 분리
- 엑셀 생성의 체크 예외가 AOP 후처리에서 선언되지 않은 예외로 바뀌던 문제 → 런타임 예외로 감쌈
- `@EgovMapper("이름")` 의 Spring 폐기 예정 경고 제거
- 미사용 라이브러리(iBATIS 2, JPA API, commons-logging) 제외

---

## [0.2.0] 2차 스프린트 — 보안 강화 및 핵심 기능 구현 (2026-10-21)
브랜치: `feature/sprint2-security-evaluation-paging`

### 수정 (Fixed) — 1차 뼈대의 결함
- 공고 등록 화면 템플릿 누락으로 인한 500 오류
- Thymeleaf 예약어 `${application}` 사용으로 신청서 상세·정산 화면 값이 비어 보이던 문제
- JDK 17 + 한글 Windows(MS949)에서 소스·`data.sql` 한글 깨짐 → UTF-8 명시
- 평가 LEFT JOIN 으로 평가위원 2명 이상일 때 상세 조회 오류(TooManyResultsException)
- seed 데이터 PK 직접 지정으로 인한 신규 등록 PK 충돌 가능성
- `X-Frame-Options` 중복 헤더, 불필요한 `/api/**` CSRF 예외

### 보안 (Security)
- 비밀번호 `{noop}` 평문 → `{bcrypt}` 해시
- 서비스 계층 소유자 검사로 신청서·정산·첨부파일 **IDOR 차단**
- `@InitBinder` 대량 할당 차단, 파일 업로드 화이트리스트 + 시그니처 검사, 경로 조작 차단
- H2 콘솔 local 프로필 + 관리자 전용, 오류 화면 내부 정보 비노출

### 추가 (Added)
- 서비스 인터페이스/Impl 분리, 신청서 상태머신, 평가위원 배정·채점
- 사업비 정산 예산 초과 검증(`SELECT … FOR UPDATE`), 반려 사유 필수
- 파일 업로드/다운로드, 표준 페이징, 공고 자동 마감 배치, 공통코드 select, 차트 DB 집계
- 테스트 40건

---

## [0.1.0] 1차 — 프로젝트 뼈대 (2026-10-20)
- Spring Boot 3.3.5 + MyBatis + Spring Security + Thymeleaf 기반 초기 구성
- 공고·신청·정산 기본 화면, 4단계 권한, Apache POI 엑셀, H2 스키마·샘플 데이터
