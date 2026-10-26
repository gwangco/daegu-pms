# 변경 이력 (CHANGELOG)

형식: [Keep a Changelog](https://keepachangelog.com/ko/1.1.0/) 를 참고해 스프린트 단위로 기록한다.

---

## [1.0.0] 최종 마감 — Docker Compose 실행 (2026-10-26)
브랜치: `feature/docker-compose` · 상세: [docs/DOCKER.md](docs/DOCKER.md)

### 추가 (Added)
- `Dockerfile`(멀티 스테이지: Maven Wrapper 빌드 → JRE 17 실행, root 가 아닌 계정, 베이스 이미지 digest 고정)
- `compose.yaml`(앱 + MySQL 8.4, DB 준비 확인 뒤 앱 시작, 볼륨), DB 계정을 둘로 나누는 초기화 스크립트, `.env.example`, `.dockerignore`
- CI `compose` 잡: 무작위 비밀번호로 띄워 로그인·메인 응답, 공고 목록의 데모 데이터, 기동 직후 배치 실행 확인
- `docs/DOCKER.md`: 왜 쓰는지, 구성, 실행 방법, 자주 쓰는 명령, 알아 둘 점

### 변경 (Changed)
- README: Docker 실행 방법, "하지 않은 것"에서 Docker 항목 제거
- 버전 1.0.0 (로드맵 완료)

---

## [0.12.0] 12차 스프린트 — 마감: SI 산출물, 후속 테스트, uat·sec 비교 (2026-10-26)
브랜치: `feature/sprint12-final` · 상세: [docs/SPRINT12_FINAL.md](docs/SPRINT12_FINAL.md)

### 추가 (Added)
- SI 산출물: 테이블 정의서, ERD (`scripts/generate-db-docs.py` 가 실제 DB 의 information_schema 에서 생성), 화면 목록 27개와 요구사항 추적표 13개
- 공지 동시 첨부 테스트 (게시물 행 잠금, 같은 첨부 묶음에 다음 순번), 동시 가입 테스트 (DB 중복 오류를 안내 문구로)
- 공통컴포넌트 로그인·권한(uat, sec)과 D-PMS 비교 문서
- 테스트 249건 (+3)

### 변경 (Changed)
- 행 잠금 대기 확인과 다른 스레드 실행을 `DatabaseLockWait` 로 묶어 동시성 테스트 세 클래스가 같이 씀
- README: SI 산출물 링크, 문서 표, 하지 않은 것 절
- 버전 0.12.0-SNAPSHOT

### 하지 않음
- Docker Compose 한 번에 실행: 개발 PC 에 Docker 가 없어 검증할 수 없는 설정은 넣지 않음

---

## [0.11.1] 11차 스프린트 2부 — 공고 마감 eGov 배치 (2026-10-26)
브랜치: `feature/sprint11-egov-batch` · 상세: [docs/SPRINT11_EGOV_BATCH.md](docs/SPRINT11_EGOV_BATCH.md)

### 추가 (Added)
- eGov 배치(`egovframe-rte-bat-core`) 잡 `announcementCloseJob`: MyBatis 페이징 리더로 마감 대상을 읽고 일괄 쓰기로 마감, 요약 리스너
- Spring Batch 메타데이터 테이블 (Flyway V7)
- 시스템 관리 > 배치 실행 이력 (`/admin/batch`, 총괄관리자): 기준일, 실행 시각과 소요 시간, 결과, 읽은·처리 건수, 실행한 사람, 지금 실행
- 테스트 246건 (+8, 예전 마감 테스트 2건은 배치 테스트로 옮김)

### 변경 (Changed)
- 공고 마감을 서비스의 UPDATE 한 번에서 청크 처리(100건)로. 같은 기준일은 한 번만 실행하고, 지금 실행은 요청 시각을 붙여 새로 실행
- 마감 대상 SQL 은 OFFSET 없이 첫 쪽만 읽음 (읽은 공고가 조건에서 빠져 OFFSET 이 남은 공고를 건너뛰기 때문)
- `spring.batch.job.enabled=false`, `spring.batch.jdbc.initialize-schema=never`
- 버전 0.11.1-SNAPSHOT

---

## [0.11.0] 11차 스프린트 — 개인정보 처리 기록 (2026-10-23)
브랜치: `feature/sprint11-privacy-access-log` · 상세: [docs/SPRINT11_PRIVACY_ACCESS_LOG.md](docs/SPRINT11_PRIVACY_ACCESS_LOG.md)

### 추가 (Added)
- 개인정보 처리 기록 `privacy_access_log` (Flyway V6): 담당자·평가위원·관리자의 신청서 상세 조회, 첨부파일·엑셀 다운로드, 사용자 목록 조회
- 시스템 관리 > 개인정보 처리 기록 (`/admin/privacy-log`, 총괄관리자): 처리자·업무·기간 검색
- 테스트 238건 (+5)

### 변경 (Changed)
- README 미구현 목록에서 개인정보 조회 기록 제거. 공고 마감 배치의 eGov 배치 전환은 다음 작업으로
- 버전 0.11.0-SNAPSHOT

---

## [0.10.0] 10차 스프린트 — 신청서 임시저장·제출 취소 (2026-10-23)
브랜치: `feature/sprint10-application-draft` · 상세: [docs/SPRINT10_APPLICATION_DRAFT.md](docs/SPRINT10_APPLICATION_DRAFT.md)

### 추가 (Added)
- 신청서 임시저장(과제명만 필수), 이어서 작성(`/applications/{번호}/edit`)·제출, 임시저장 삭제
- 제출 취소: 서류 검토 전(접수완료)·접수 기간 안이면 임시저장으로 되돌림, 다시 제출해도 같은 접수번호
- 메인 처리할 일 "작성 중인 신청서", 상세 화면의 이어서 작성·삭제·제출 취소 안내
- Flyway V5: 임시저장용 NULL 허용 + CHECK 제약(임시저장이 아니면 필수 항목 모두 존재)
- 테스트 233건 (+9)

### 변경 (Changed)
- 임시저장은 신청한 기업만 봄: 담당자·평가위원 목록·상태 검색·메인 통계에서 제외, 다른 사람에게 상세는 404
- 같은 공고 신청 화면을 다시 열면 임시저장은 이어서 작성으로, 제출한 공고는 상세로 이동
- 공통 스크립트: 중복 제출 방지가 누른 버튼 값을 지우지 않도록 숨은 칸으로 옮긴 뒤 비활성화
- 버전 0.10.0-SNAPSHOT

---

## [0.9.0] 9차 스프린트 — 기업회원 가입 (2026-10-23)
브랜치: `feature/sprint9-company-signup` · 상세: [docs/SPRINT9_COMPANY_SIGNUP.md](docs/SPRINT9_COMPANY_SIGNUP.md)

### 추가 (Added)
- 기업회원 가입 `/signup`: 이용약관·개인정보 수집·이용 동의(필수, 동의 일시 저장), 계정·기업·담당자 정보
- 가입 승인: 가입은 승인 대기로 저장, 총괄관리자가 사용자 관리에서 승인(승인자·일시 기록). 메인 처리할 일 "가입 승인 대기", 목록 "승인 대기만" 필터
- 비밀번호 규칙(KISA 조합·길이 기준, 최대 64자, 아이디·반복·연속 문자 금지), 사업자등록번호 형식·검증번호 확인, 전화번호 형식 통일
- Flyway V4: 승인 상태·승인자·승인 일시·동의 일시, 기업회원 사업자등록번호 중복 방지(생성 컬럼 + UNIQUE)
- 테스트 224건 (+58): 단위 47건(비밀번호 규칙·사업자등록번호·전화번호), 가입·승인 통합 11건

### 변경 (Changed)
- 로그인: 승인 대기 계정은 맞는 비밀번호로도 로그인할 수 없고 "가입 승인을 기다리는 계정" 안내, 접속 기록에 `PENDING`
- 사용자 관리 목록에 가입일·사업자등록번호, 내 정보에 약관 동의·가입 승인 일시
- 개인정보처리방침에 기업회원 가입 수집 항목·동의 기록·승인 절차
- 데모 기업회원 사업자등록번호 2개를 검증번호 규칙에 맞춤 (db/seed V4_1, 마지막 자리만)
- 버전 0.9.0-SNAPSHOT

---

## [0.8.0] 8차 스프린트 — 공지사항 첨부파일 (2026-10-23)
브랜치: `feature/sprint8-notice-attachments` · 상세: [docs/SPRINT8_NOTICE_ATTACHMENTS.md](docs/SPRINT8_NOTICE_ATTACHMENTS.md)

### 추가 (Added)
- 공지사항 첨부파일: 공지 하나에 3개, 파일당 10MB. 허용 여부·개수·크기는 공통컴포넌트 게시판 속성(`COMTNBBSMASTER`)에서 읽음 (Flyway V3)
- 공지 첨부 다운로드 `/notices/{번호}/files/{순번}` (비로그인 허용, 삭제된 공지·없는 순번은 404)
- 수정 화면의 기존 첨부 삭제 표시, 선택한 파일 이름·크기와 더 올릴 수 있는 개수 안내
- 공지 상세 첨부 목록(형식 아이콘·크기), 목록·메인 화면의 첨부 표시
- 테스트 166건 (+10): 게시판 속성, 첨부 ID 위조 무시, 비로그인 다운로드·404, 개수·크기·형식 거부, 수정 시 순번, 커밋 뒤 파일 삭제, 첨부 금지 설정, 기업회원 거부

### 변경 (Changed)
- 공지 등록·수정과 첨부 저장을 한 트랜잭션으로 처리. 수정은 게시물 행을 잠그고, 첨부 ID 는 잠근 행의 값만 씀
- 첨부 저장(`FileService.storeFiles`)은 모든 파일을 먼저 검사한 뒤 저장하고, 기존 묶음에는 마지막 순번 다음부터 추가
- 공통컴포넌트 게시판 SQL: 등록 시 서버가 만든 첨부 ID 저장, 수정 시 비어 있을 때만 연결, 목록은 파일이 남아 있을 때만 첨부 ID 반환
- 버전 0.8.0-SNAPSHOT

### 수정 (Fixed)
- 공통컴포넌트 `EgovFileMngServiceImpl.deleteFileInf` 가 DB 삭제 전에 실제 파일부터 지워, 뒤에서 롤백되면 DB 에는 남고 파일만 사라지던 문제 (커밋 뒤에 지우도록 변경)

---

## [0.7.0] 7차 스프린트 — 화면 디자인 개편 (KRDS 기준) (2026-10-23)
브랜치: `feature/sprint7-design-renewal` · 상세: [docs/SPRINT7_DESIGN_RENEWAL.md](docs/SPRINT7_DESIGN_RENEWAL.md)

### 추가 (Added)
- KRDS(대한민국 정부 디자인 시스템) 디자인 토큰을 `dpms.css` 변수로 옮김: 색 단계, 본문 17px, 버튼 높이 32·40·48·56px, 입력칸 48px (출처 표기 `licenses/krds-NOTICE.txt`)
- Pretendard GOV 글꼴 내장 (KRDS 배포본 서브셋 400·500·700, SIL OFL 1.1)
- 목록 화면 모바일 카드형 (768px 미만), 검색 조건 초기화 버튼
- 메인: 공고 검색창, 역할별 처리할 일 카드, 진행 절차 4단계, 분야별 예산 막대
- 상세 화면 오른쪽 요약 카드, 신청 과제 진행 단계, 공고 마감 D-day(업무 날짜 기준)
- 금액 입력 한글 단위 안내(1억 5,000만원)와 최대 지원금·자부담금·정산 잔액 경고, 첨부 파일 크기 안내
- e-정산 집행률 막대, 정산 승인 확인 창
- 테스트 156건 (+3): 정적 리소스 해시 주소·캐시, 하위 메뉴 현재 위치, 공고 D-day, 정산 승인 확인 창

### 변경 (Changed)
- 헤더를 로고 줄 + 주 메뉴 줄로 나누고 지금 메뉴는 아래 4px 막대로 표시, 푸터를 화면 아래에 고정
- 상세 화면의 항목·값 4칸 표 → 정보 목록(dl), 입력 화면은 구역 제목과 작성 안내
- 신청 목록에서 공고명 칸을 과제명 아래로 합쳐 과제명이 좁아지던 문제 해결
- e-정산 반려 사유·처리 이력·증빙을 모바일에서도 보이게 거래처 칸 안으로 이동
- 정적 리소스(CSS·JS·글꼴·WebJars)에 내용 해시 주소와 1년 캐시. 화면(HTML)은 계속 캐시하지 않음
- 버전 0.7.0-SNAPSHOT

### 제거 (Removed)
- Chart.js WebJar (도넛 차트를 HTML 막대로 바꿔 쓰지 않게 됨), 메인 화면 데모 계정 표 (로그인 화면 빠른 입력으로 대신)

### 수정 (Fixed)
- CSS 에 적힌 글꼴 파일이 없어 맑은 고딕으로 보이던 문제
- 포커스 표시 대비 1.7:1 → 6.8:1, 입력칸 테두리 대비 1.4:1 → 6.3:1
- Bootstrap 이 입력칸·모달 바탕에 페이지 배경 변수를 써서 회색으로 보이던 문제
- 개인정보처리방침의 "메모리 DB 라 재시작하면 지워진다" 문구 (6차 MySQL 전환 뒤에도 남아 있었음), 푸터의 "표준프레임워크 5.0" 표기

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
- Flyway 11.7.2(Spring Boot 기본) → 11.20.3: 11.7.2 는 MySQL 8.1 까지만 검증 범위라 CI(8.4)에서 미검증 경고
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
- CI MySQL 이미지 digest 고정, GitHub Actions 를 Node.js 24 실행 버전으로 올림 (checkout v7.0.1, setup-java v6.0.1, upload-artifact v7.0.2, 커밋 SHA 고정)

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
