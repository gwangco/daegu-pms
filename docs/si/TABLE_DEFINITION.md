# 테이블 정의서

> 이 문서는 `scripts/generate-db-docs.py` 가 MySQL 의 `information_schema` 에서 만든다. 손으로 고치지 말고, 마이그레이션을 적용한 뒤 스크립트를 다시 실행한다.

- 기준: Flyway V7 적용 DB, 생성일 2026-10-26
- 테이블: 업무 8 · 공통컴포넌트 9 · 배치 메타데이터 9 · 관리 1
- 키: PK 기본키, UK 유일, FK 외래키, AI 자동 증가, 생성 = 다른 컬럼으로 계산하는 생성 컬럼. NULL 이 Y 면 비워 둘 수 있다
- 관계 그림은 [ERD](ERD.md) 에 있다

## 목차

| 구분 | 테이블 | 설명 | 컬럼 수 |
| :--- | :--- | :--- | ---: |
| 업무 | users | 사용자 | 21 |
| 업무 | login_history | 접속 기록 | 5 |
| 업무 | privacy_access_log | 개인정보 처리 기록 | 9 |
| 업무 | announcement | 지원사업 공고 | 14 |
| 업무 | application | 사업 신청서 | 15 |
| 업무 | evaluation | 평가표 | 13 |
| 업무 | settlement | 사업비 정산 | 19 |
| 업무 | settlement_history | 정산 처리 이력 | 7 |
| 공통컴포넌트 | COMTCCMMNCLCODE | 공통분류코드 | 8 |
| 공통컴포넌트 | COMTCCMMNCODE | 공통코드 | 9 |
| 공통컴포넌트 | COMTCCMMNDETAILCODE | 공통상세코드 | 10 |
| 공통컴포넌트 | COMTECOPSEQ | 채번 | 2 |
| 공통컴포넌트 | COMTNBBSMASTER | 게시판마스터 | 17 |
| 공통컴포넌트 | COMTNBBS | 게시판 | 25 |
| 공통컴포넌트 | COMTNFAQINFO | FAQ정보 | 14 |
| 공통컴포넌트 | COMTNFILE | 파일속성 | 3 |
| 공통컴포넌트 | COMTNFILEDETAIL | 파일상세정보 | 8 |

## 1. 업무 테이블

### 1.1 users · 사용자

| # | 컬럼 | 타입 | NULL | 기본값 | 키 | 설명 |
| ---: | :--- | :--- | :---: | :--- | :--- | :--- |
| 1 | `user_id` | VARCHAR(50) | N |  | PK | 사용자 ID |
| 2 | `password` | VARCHAR(255) | N |  |  | 비밀번호 해시 ({bcrypt} 단방향) |
| 3 | `user_name` | VARCHAR(50) | N |  |  | 이름 |
| 4 | `email` | VARCHAR(255) | N |  |  | 이메일 (ARIA 암호문 {ARIA}Base64) |
| 5 | `phone` | VARCHAR(100) | Y |  |  | 연락처 (ARIA 암호문) |
| 6 | `company_name` | VARCHAR(100) | Y |  |  | 소속 기관·기업명 |
| 7 | `business_no` | VARCHAR(20) | Y |  |  | 사업자등록번호 |
| 8 | `role` | VARCHAR(30) | N |  |  | ROLE_COMPANY, ROLE_EVALUATOR, ROLE_MANAGER, ROLE_ADMIN |
| 9 | `enabled` | TINYINT(1) | N | 1 |  | 사용 여부 |
| 10 | `approval_status` | VARCHAR(20) | N | APPROVED |  | 가입 승인 상태 (REQUESTED 승인 대기, APPROVED 승인) |
| 11 | `approved_by` | VARCHAR(50) | Y |  |  | 가입 승인자 |
| 12 | `approved_at` | DATETIME | Y |  |  | 가입 승인 일시 |
| 13 | `terms_agreed_at` | DATETIME | Y |  |  | 이용약관 동의 일시 |
| 14 | `privacy_agreed_at` | DATETIME | Y |  |  | 개인정보 수집·이용 동의 일시 |
| 15 | `login_fail_count` | INT | N | 0 |  | 연속 로그인 실패 횟수 (성공하면 0) |
| 16 | `locked_at` | DATETIME | Y |  |  | 잠긴 시각 (잠금 해제 시 NULL) |
| 17 | `last_login_at` | DATETIME | Y |  |  | 마지막 로그인 일시 |
| 18 | `created_at` | DATETIME | N | CURRENT_TIMESTAMP |  | 가입 일시 |
| 19 | `updated_by` | VARCHAR(50) | Y |  |  | 수정자 |
| 20 | `updated_at` | DATETIME | Y |  |  | 수정 일시 |
| 21 | `company_business_no` | VARCHAR(20) | Y |  | UK, 생성 | 기업회원 사업자등록번호 (중복 가입 방지용 생성 컬럼, 직접 넣지 않음) |

- 인덱스: `uk_users_company_business_no` (company_business_no, 유일)
- 생성 컬럼 `company_business_no` = `(case when (role = 'ROLE_COMPANY') then business_no end)`

### 1.2 login_history · 접속 기록

| # | 컬럼 | 타입 | NULL | 기본값 | 키 | 설명 |
| ---: | :--- | :--- | :---: | :--- | :--- | :--- |
| 1 | `history_id` | BIGINT | N |  | PK, AI | 기록 번호 |
| 2 | `user_id` | VARCHAR(50) | Y |  |  | 사용자 ID (없는 아이디는 입력값 대신 NULL) |
| 3 | `result_code` | VARCHAR(20) | N |  |  | SUCCESS, FAIL_PASSWORD, FAIL_UNKNOWN_ID, LOCKED, DISABLED, LOGOUT |
| 4 | `client_ip` | VARCHAR(45) | N |  |  | 접속 IP (IPv6 최대 길이) |
| 5 | `login_at` | DATETIME | N |  |  | 발생 일시 |

- 인덱스: `idx_login_history_at` (login_at), `idx_login_history_user` (user_id, login_at)

### 1.3 privacy_access_log · 개인정보 처리 기록

| # | 컬럼 | 타입 | NULL | 기본값 | 키 | 설명 |
| ---: | :--- | :--- | :---: | :--- | :--- | :--- |
| 1 | `log_id` | BIGINT | N |  | PK, AI | 기록 번호 |
| 2 | `user_id` | VARCHAR(50) | N |  |  | 처리자 (로그인 사용자) |
| 3 | `action` | VARCHAR(20) | N |  |  | 수행 업무: VIEW 조회, DOWNLOAD 다운로드 |
| 4 | `target_type` | VARCHAR(30) | N |  |  | 대상: APPLICATION 신청서, USER_LIST 사용자 목록, EXCEL 신청 현황 엑셀, FILE 첨부파일 |
| 5 | `target_id` | VARCHAR(50) | Y |  |  | 대상 식별자 (신청서 번호, 첨부파일 ID·순번) |
| 6 | `subject_id` | VARCHAR(50) | Y |  |  | 정보주체 (신청 기업 아이디. 목록·엑셀처럼 여러 명이면 NULL) |
| 7 | `record_count` | INT | N | 1 |  | 조회·다운로드한 건수 |
| 8 | `client_ip` | VARCHAR(45) | N |  |  | 접속지 IP (IPv6 최대 길이) |
| 9 | `accessed_at` | DATETIME | N |  |  | 처리 일시 |

- 인덱스: `idx_privacy_access_at` (accessed_at), `idx_privacy_access_user` (user_id, accessed_at)

### 1.4 announcement · 지원사업 공고

| # | 컬럼 | 타입 | NULL | 기본값 | 키 | 설명 |
| ---: | :--- | :--- | :---: | :--- | :--- | :--- |
| 1 | `announcement_id` | BIGINT | N |  | PK, AI | 공고 번호 |
| 2 | `title` | VARCHAR(200) | N |  |  | 공고명 |
| 3 | `field_code` | VARCHAR(20) | N |  |  | 지원 분야 (공통코드 FIELD) |
| 4 | `total_budget` | BIGINT | N |  |  | 총 지원 예산 (원) |
| 5 | `max_grant` | BIGINT | N |  |  | 기업당 최대 지원금 (원) |
| 6 | `start_date` | DATE | N |  |  | 접수 시작일 |
| 7 | `end_date` | DATE | N |  |  | 접수 마감일 |
| 8 | `content` | TEXT | N |  |  | 공고 내용 |
| 9 | `status` | VARCHAR(20) | N | OPEN |  | DRAFT, OPEN, CLOSED |
| 10 | `view_count` | INT | N | 0 |  | 조회수 |
| 11 | `writer_id` | VARCHAR(50) | N |  | FK | 작성자 |
| 12 | `created_at` | DATETIME | N | CURRENT_TIMESTAMP |  | 등록 일시 |
| 13 | `updated_by` | VARCHAR(50) | Y |  |  | 수정자 (배치는 SYSTEM) |
| 14 | `updated_at` | DATETIME | Y |  |  | 수정 일시 |

- 인덱스: `idx_announcement_status_end` (status, end_date)
- 외래키: `writer_id` → users.`user_id`

### 1.5 application · 사업 신청서

| # | 컬럼 | 타입 | NULL | 기본값 | 키 | 설명 |
| ---: | :--- | :--- | :---: | :--- | :--- | :--- |
| 1 | `application_id` | BIGINT | N |  | PK, AI | 신청서 번호 |
| 2 | `receipt_no` | VARCHAR(20) | Y |  | UK | 접수번호 (eGov IdGnr: DPMS-00000001). 처음 제출할 때 발급, 임시저장은 없음 |
| 3 | `announcement_id` | BIGINT | N |  | UK, FK | 공고 번호 |
| 4 | `company_user_id` | VARCHAR(50) | N |  | UK, FK | 신청 기업 사용자 |
| 5 | `project_name` | VARCHAR(200) | N |  |  | 과제명 |
| 6 | `total_budget` | BIGINT | Y |  |  | 총 사업비 |
| 7 | `request_grant` | BIGINT | Y |  |  | 신청 지원금 |
| 8 | `self_funding` | BIGINT | Y |  |  | 자부담 |
| 9 | `summary` | TEXT | Y |  |  | 사업계획 요약 |
| 10 | `plan_atch_file_id` | CHAR(20) | Y |  | FK | 사업계획서 첨부파일 (COMTNFILE) |
| 11 | `status` | VARCHAR(30) | N | SUBMITTED |  | 진행 상태 |
| 12 | `submitted_at` | DATETIME | Y | CURRENT_TIMESTAMP |  | 제출 일시 (임시저장·제출 취소 상태는 NULL) |
| 13 | `created_at` | DATETIME | N | CURRENT_TIMESTAMP |  | 등록 일시 |
| 14 | `updated_by` | VARCHAR(50) | Y |  |  | 수정자 |
| 15 | `updated_at` | DATETIME | Y |  |  | 수정 일시 |

- 인덱스: `idx_application_company` (company_user_id), `uk_application_announcement_company` (announcement_id, company_user_id, 유일), `uk_application_receipt_no` (receipt_no, 유일)
- 외래키: `announcement_id` → announcement.`announcement_id`, `company_user_id` → users.`user_id`, `plan_atch_file_id` → COMTNFILE.`ATCH_FILE_ID`
- CHECK `ck_application_submitted_complete`: `((status = 'TEMP_SAVED') or ((receipt_no is not null) and (total_budget is not null) and (request_grant is not null) and (self_funding is not null) and (summary is not null) and (submitted_at is not null)))`

### 1.6 evaluation · 평가표

| # | 컬럼 | 타입 | NULL | 기본값 | 키 | 설명 |
| ---: | :--- | :--- | :---: | :--- | :--- | :--- |
| 1 | `evaluation_id` | BIGINT | N |  | PK, AI | 평가 번호 |
| 2 | `application_id` | BIGINT | N |  | UK, FK | 신청서 번호 |
| 3 | `evaluator_user_id` | VARCHAR(50) | N |  | UK, FK | 평가위원 |
| 4 | `quantitative_score` | INT | N | 0 |  | 정량 점수 (40점 만점) |
| 5 | `qualitative_score` | INT | N | 0 |  | 정성 점수 (60점 만점) |
| 6 | `total_score` | INT | N | 0 |  | 총점 |
| 7 | `eval_comment` | TEXT | Y |  |  | 평가 의견 |
| 8 | `status` | VARCHAR(20) | N | ASSIGNED |  | ASSIGNED, COMPLETED |
| 9 | `assigned_at` | DATETIME | N | CURRENT_TIMESTAMP |  | 배정 일시 |
| 10 | `evaluated_at` | DATETIME | Y |  |  | 평가 일시 |
| 11 | `created_by` | VARCHAR(50) | Y |  |  | 배정한 담당자 |
| 12 | `updated_by` | VARCHAR(50) | Y |  |  | 수정자 |
| 13 | `updated_at` | DATETIME | Y |  |  | 수정 일시 |

- 인덱스: `idx_evaluation_evaluator` (evaluator_user_id), `uk_evaluation_app_evaluator` (application_id, evaluator_user_id, 유일)
- 외래키: `application_id` → application.`application_id`, `evaluator_user_id` → users.`user_id`
- CHECK `ck_evaluation_qualitative`: `(qualitative_score between 0 and 60)`
- CHECK `ck_evaluation_quantitative`: `(quantitative_score between 0 and 40)`

### 1.7 settlement · 사업비 정산

| # | 컬럼 | 타입 | NULL | 기본값 | 키 | 설명 |
| ---: | :--- | :--- | :---: | :--- | :--- | :--- |
| 1 | `settlement_id` | BIGINT | N |  | PK, AI | 정산 번호 |
| 2 | `application_id` | BIGINT | N |  | FK | 신청서 번호 |
| 3 | `item_code` | VARCHAR(20) | N |  |  | 비목 (공통코드 EXP_ITEM) |
| 4 | `expense_date` | DATE | N |  |  | 집행일 |
| 5 | `vendor_name` | VARCHAR(100) | N |  |  | 거래처 |
| 6 | `supply_amount` | BIGINT | N |  |  | 공급가액 |
| 7 | `vat_amount` | BIGINT | N |  |  | 부가세 |
| 8 | `total_amount` | BIGINT | N |  |  | 합계 |
| 9 | `description` | VARCHAR(255) | N |  |  | 집행 내용 |
| 10 | `receipt_atch_file_id` | CHAR(20) | Y |  | FK | 증빙 첨부파일 (COMTNFILE) |
| 11 | `status` | VARCHAR(20) | N | REQUESTED |  | REQUESTED, APPROVED, REJECTED |
| 12 | `reject_reason` | VARCHAR(255) | Y |  |  | 보완 요청 사유 |
| 13 | `resubmit_count` | INT | N | 0 |  | 보완 후 재요청 횟수 |
| 14 | `reviewed_by` | VARCHAR(50) | Y |  | FK | 검수 처리자 |
| 15 | `reviewed_at` | DATETIME | Y |  |  | 검수 일시 |
| 16 | `created_at` | DATETIME | N | CURRENT_TIMESTAMP |  | 등록 일시 |
| 17 | `created_by` | VARCHAR(50) | Y |  |  | 등록한 기업 사용자 |
| 18 | `updated_by` | VARCHAR(50) | Y |  |  | 수정자 |
| 19 | `updated_at` | DATETIME | Y |  |  | 수정 일시 |

- 인덱스: `idx_settlement_application` (application_id, status), `idx_settlement_status` (status, created_at)
- 외래키: `application_id` → application.`application_id`, `receipt_atch_file_id` → COMTNFILE.`ATCH_FILE_ID`, `reviewed_by` → users.`user_id`

### 1.8 settlement_history · 정산 처리 이력

| # | 컬럼 | 타입 | NULL | 기본값 | 키 | 설명 |
| ---: | :--- | :--- | :---: | :--- | :--- | :--- |
| 1 | `history_id` | BIGINT | N |  | PK, AI | 이력 번호 |
| 2 | `settlement_id` | BIGINT | N |  | FK | 정산 번호 |
| 3 | `action_code` | VARCHAR(20) | N |  |  | REQUESTED, APPROVED, REJECTED, RESUBMITTED |
| 4 | `total_amount` | BIGINT | N |  |  | 처리 시점의 집행 금액 |
| 5 | `memo` | VARCHAR(255) | Y |  |  | 보완 요청 사유 |
| 6 | `actor_id` | VARCHAR(50) | N |  |  | 처리자 |
| 7 | `acted_at` | DATETIME | N |  |  | 처리 일시 |

- 인덱스: `idx_settlement_history` (settlement_id, acted_at)
- 외래키: `settlement_id` → settlement.`settlement_id`

## 2. 공통컴포넌트 테이블

### 2.1 COMTCCMMNCLCODE · 공통분류코드

| # | 컬럼 | 타입 | NULL | 기본값 | 키 | 설명 |
| ---: | :--- | :--- | :---: | :--- | :--- | :--- |
| 1 | `CL_CODE` | CHAR(3) | N |  | PK | 분류코드 |
| 2 | `CL_CODE_NM` | VARCHAR(60) | Y |  |  | 분류코드명 |
| 3 | `CL_CODE_DC` | VARCHAR(200) | Y |  |  | 분류코드설명 |
| 4 | `USE_AT` | CHAR(1) | Y |  |  | 사용여부 |
| 5 | `FRST_REGIST_PNTTM` | DATETIME | Y |  |  | 최초등록시점 |
| 6 | `FRST_REGISTER_ID` | VARCHAR(20) | Y |  |  | 최초등록자ID |
| 7 | `LAST_UPDT_PNTTM` | DATETIME | Y |  |  | 최종수정시점 |
| 8 | `LAST_UPDUSR_ID` | VARCHAR(20) | Y |  |  | 최종수정자ID |

### 2.2 COMTCCMMNCODE · 공통코드

| # | 컬럼 | 타입 | NULL | 기본값 | 키 | 설명 |
| ---: | :--- | :--- | :---: | :--- | :--- | :--- |
| 1 | `CODE_ID` | VARCHAR(6) | N |  | PK | 코드ID |
| 2 | `CODE_ID_NM` | VARCHAR(60) | Y |  |  | 코드ID명 |
| 3 | `CODE_ID_DC` | VARCHAR(200) | Y |  |  | 코드ID설명 |
| 4 | `USE_AT` | CHAR(1) | Y |  |  | 사용여부 |
| 5 | `CL_CODE` | CHAR(3) | Y |  | FK | 분류코드 |
| 6 | `FRST_REGIST_PNTTM` | DATETIME | Y |  |  | 최초등록시점 |
| 7 | `FRST_REGISTER_ID` | VARCHAR(20) | Y |  |  | 최초등록자ID |
| 8 | `LAST_UPDT_PNTTM` | DATETIME | Y |  |  | 최종수정시점 |
| 9 | `LAST_UPDUSR_ID` | VARCHAR(20) | Y |  |  | 최종수정자ID |

- 외래키: `CL_CODE` → COMTCCMMNCLCODE.`CL_CODE`

### 2.3 COMTCCMMNDETAILCODE · 공통상세코드

| # | 컬럼 | 타입 | NULL | 기본값 | 키 | 설명 |
| ---: | :--- | :--- | :---: | :--- | :--- | :--- |
| 1 | `CODE_ID` | VARCHAR(6) | N |  | PK, FK | 코드ID |
| 2 | `CODE` | VARCHAR(15) | N |  | PK | 코드 |
| 3 | `CODE_NM` | VARCHAR(60) | Y |  |  | 코드명 |
| 4 | `CODE_DC` | VARCHAR(200) | Y |  |  | 코드설명 |
| 5 | `USE_AT` | CHAR(1) | Y |  |  | 사용여부 |
| 6 | `SORT_ORDR` | DECIMAL(5,0) | N | 0 |  | [D-PMS 추가] 화면 표시 순서 |
| 7 | `FRST_REGIST_PNTTM` | DATETIME | Y |  |  | 최초등록시점 |
| 8 | `FRST_REGISTER_ID` | VARCHAR(20) | Y |  |  | 최초등록자ID |
| 9 | `LAST_UPDT_PNTTM` | DATETIME | Y |  |  | 최종수정시점 |
| 10 | `LAST_UPDUSR_ID` | VARCHAR(20) | Y |  |  | 최종수정자ID |

- 외래키: `CODE_ID` → COMTCCMMNCODE.`CODE_ID`

### 2.4 COMTECOPSEQ · 채번

| # | 컬럼 | 타입 | NULL | 기본값 | 키 | 설명 |
| ---: | :--- | :--- | :---: | :--- | :--- | :--- |
| 1 | `TABLE_NAME` | VARCHAR(20) | N |  | PK | 채번 대상 |
| 2 | `NEXT_ID` | DECIMAL(30,0) | Y |  |  | 다음 번호 |

### 2.5 COMTNBBSMASTER · 게시판마스터

| # | 컬럼 | 타입 | NULL | 기본값 | 키 | 설명 |
| ---: | :--- | :--- | :---: | :--- | :--- | :--- |
| 1 | `BBS_ID` | CHAR(30) | N |  | PK | 게시판ID |
| 2 | `BBS_NM` | VARCHAR(255) | N |  |  | 게시판명 |
| 3 | `BBS_INTRCN` | VARCHAR(2400) | Y |  |  | 게시판소개 |
| 4 | `BBS_TY_CODE` | CHAR(6) | N |  |  | 게시판유형코드 (BBST01 통합게시판) |
| 5 | `REPLY_POSBL_AT` | CHAR(1) | Y |  |  | 답장가능여부 |
| 6 | `FILE_ATCH_POSBL_AT` | CHAR(1) | N |  |  | 파일첨부가능여부 |
| 7 | `ATCH_POSBL_FILE_NUMBER` | DECIMAL(2,0) | N |  |  | 첨부가능파일숫자 |
| 8 | `ATCH_POSBL_FILE_SIZE` | DECIMAL(8,0) | Y |  |  | 첨부가능파일사이즈 |
| 9 | `USE_AT` | CHAR(1) | N |  |  | 사용여부 |
| 10 | `TMPLAT_ID` | CHAR(20) | Y |  |  | 템플릿ID |
| 11 | `CMMNTY_ID` | CHAR(20) | Y |  |  | 커뮤니티ID |
| 12 | `FRST_REGISTER_ID` | VARCHAR(20) | N |  |  | 최초등록자ID |
| 13 | `FRST_REGIST_PNTTM` | DATETIME | N |  |  | 최초등록시점 |
| 14 | `LAST_UPDUSR_ID` | VARCHAR(20) | Y |  |  | 최종수정자ID |
| 15 | `LAST_UPDT_PNTTM` | DATETIME | Y |  |  | 최종수정시점 |
| 16 | `BLOG_ID` | CHAR(20) | Y |  |  | 블로그ID |
| 17 | `BLOG_AT` | CHAR(2) | Y |  |  | 블로그여부 |

### 2.6 COMTNBBS · 게시판

| # | 컬럼 | 타입 | NULL | 기본값 | 키 | 설명 |
| ---: | :--- | :--- | :---: | :--- | :--- | :--- |
| 1 | `NTT_ID` | DECIMAL(20,0) | N |  | PK | 게시물ID |
| 2 | `BBS_ID` | CHAR(30) | N |  | PK, FK | 게시판ID |
| 3 | `NTT_NO` | DECIMAL(20,0) | Y |  |  | 게시물번호 |
| 4 | `NTT_SJ` | VARCHAR(2000) | Y |  |  | 게시물제목 |
| 5 | `NTT_CN` | MEDIUMTEXT | Y |  |  | 게시물내용 |
| 6 | `ANSWER_AT` | CHAR(1) | Y |  |  | 댓글여부 |
| 7 | `PARNTSCTT_NO` | DECIMAL(10,0) | Y |  |  | 부모글번호 |
| 8 | `ANSWER_LC` | DECIMAL(8,0) | Y |  |  | 댓글위치 |
| 9 | `SORT_ORDR` | DECIMAL(8,0) | Y |  |  | 정렬순서 |
| 10 | `RDCNT` | DECIMAL(10,0) | Y |  |  | 조회수 |
| 11 | `USE_AT` | CHAR(1) | N |  |  | 사용여부 (N: 삭제) |
| 12 | `NTCE_BGNDE` | CHAR(20) | Y |  |  | 게시시작일 |
| 13 | `NTCE_ENDDE` | CHAR(20) | Y |  |  | 게시종료일 |
| 14 | `NTCR_ID` | VARCHAR(20) | Y |  |  | 게시자ID |
| 15 | `NTCR_NM` | VARCHAR(20) | Y |  |  | 게시자명 |
| 16 | `PASSWORD` | VARCHAR(200) | Y |  |  | 비밀번호 (익명 게시판용, D-PMS 미사용) |
| 17 | `ATCH_FILE_ID` | CHAR(20) | Y |  |  | 첨부파일ID |
| 18 | `NOTICE_AT` | CHAR(1) | Y |  |  | 공지사항여부 (Y: 목록 상단 고정) |
| 19 | `SJ_BOLD_AT` | CHAR(1) | Y |  |  | 제목볼드여부 |
| 20 | `SECRET_AT` | CHAR(1) | Y |  |  | 비밀글여부 |
| 21 | `FRST_REGIST_PNTTM` | DATETIME | N |  |  | 최초등록시점 |
| 22 | `FRST_REGISTER_ID` | VARCHAR(20) | N |  |  | 최초등록자ID |
| 23 | `LAST_UPDT_PNTTM` | DATETIME | Y |  |  | 최종수정시점 |
| 24 | `LAST_UPDUSR_ID` | VARCHAR(20) | Y |  |  | 최종수정자ID |
| 25 | `BLOG_ID` | CHAR(20) | Y |  |  | 블로그ID |

- 인덱스: `COMTNBBS_i02` (BBS_ID, USE_AT, NOTICE_AT, SORT_ORDR)
- 외래키: `BBS_ID` → COMTNBBSMASTER.`BBS_ID`

### 2.7 COMTNFAQINFO · FAQ정보

| # | 컬럼 | 타입 | NULL | 기본값 | 키 | 설명 |
| ---: | :--- | :--- | :---: | :--- | :--- | :--- |
| 1 | `FAQ_ID` | CHAR(20) | N |  | PK | FAQID |
| 2 | `QESTN_SJ` | VARCHAR(255) | Y |  |  | 질문제목 |
| 3 | `QESTN_CN` | VARCHAR(2500) | Y |  |  | 질문내용 |
| 4 | `ANSWER_CN` | VARCHAR(2500) | Y |  |  | 답변내용 |
| 5 | `RDCNT` | DECIMAL(10,0) | Y |  |  | 조회수 |
| 6 | `FRST_REGIST_PNTTM` | DATETIME | N |  |  | 최초등록시점 |
| 7 | `FRST_REGISTER_ID` | VARCHAR(20) | N |  |  | 최초등록자ID |
| 8 | `LAST_UPDT_PNTTM` | DATETIME | N |  |  | 최종수정시점 |
| 9 | `LAST_UPDUSR_ID` | VARCHAR(20) | N |  |  | 최종수정자ID |
| 10 | `ATCH_FILE_ID` | CHAR(20) | Y |  | FK | 첨부파일ID |
| 11 | `QNA_PROCESS_STTUS_CODE` | CHAR(1) | Y |  |  | 질의응답처리상태코드 |
| 12 | `FAQ_CL_CODE` | VARCHAR(15) | N |  |  | [D-PMS 추가] FAQ 분류 (공통코드 DPM005) |
| 13 | `SORT_ORDR` | DECIMAL(5,0) | N | 1 |  | [D-PMS 추가] 분류 안의 표시 순서 |
| 14 | `USE_AT` | CHAR(1) | N | Y |  | [D-PMS 추가] 사용여부 (N: 삭제) |

- 인덱스: `COMTNFAQINFO_i02` (USE_AT, FAQ_CL_CODE, SORT_ORDR)
- 외래키: `ATCH_FILE_ID` → COMTNFILE.`ATCH_FILE_ID`

### 2.8 COMTNFILE · 파일속성

| # | 컬럼 | 타입 | NULL | 기본값 | 키 | 설명 |
| ---: | :--- | :--- | :---: | :--- | :--- | :--- |
| 1 | `ATCH_FILE_ID` | CHAR(20) | N |  | PK | 첨부파일ID |
| 2 | `CREAT_DT` | DATETIME | N |  |  | 생성일시 |
| 3 | `USE_AT` | CHAR(1) | Y |  |  | 사용여부 |

### 2.9 COMTNFILEDETAIL · 파일상세정보

| # | 컬럼 | 타입 | NULL | 기본값 | 키 | 설명 |
| ---: | :--- | :--- | :---: | :--- | :--- | :--- |
| 1 | `ATCH_FILE_ID` | CHAR(20) | N |  | PK, FK | 첨부파일ID |
| 2 | `FILE_SN` | DECIMAL(10,0) | N |  | PK | 파일순번 |
| 3 | `FILE_STRE_COURS` | VARCHAR(2000) | N |  |  | 파일저장경로 (업로드 루트 기준 하위 경로) |
| 4 | `STRE_FILE_NM` | VARCHAR(255) | N |  |  | 저장파일명 (UUID, 확장자 없음) |
| 5 | `ORIGNL_FILE_NM` | VARCHAR(255) | Y |  |  | 원파일명 |
| 6 | `FILE_EXTSN` | VARCHAR(20) | N |  |  | 파일확장자 |
| 7 | `FILE_CN` | MEDIUMTEXT | Y |  |  | 파일내용 |
| 8 | `FILE_SIZE` | DECIMAL(8,0) | Y |  |  | 파일크기 (byte) |

- 외래키: `ATCH_FILE_ID` → COMTNFILE.`ATCH_FILE_ID`

## 3. 배치 메타데이터 (Spring Batch 5.2)

Flyway V7 이 spring-batch-core 의 `schema-mysql.sql` 을 그대로 만든다. 컬럼 정의는 그 스크립트와 같다.

| 테이블 | 용도 | 컬럼 수 |
| :--- | :--- | ---: |
| BATCH_JOB_INSTANCE | 잡 이름 + 식별 파라미터 = 실행 단위. 같은 기준일은 한 번만 성공 | 4 |
| BATCH_JOB_EXECUTION | 실행 1회의 시작·종료 시각, 상태, 종료 코드 | 10 |
| BATCH_JOB_EXECUTION_PARAMS | 실행 파라미터 (targetDate, requestedAt, requestedBy) | 5 |
| BATCH_JOB_EXECUTION_CONTEXT | 잡 재시작에 쓰는 상태 | 3 |
| BATCH_STEP_EXECUTION | 스텝 실행의 읽기·쓰기·커밋 건수 | 19 |
| BATCH_STEP_EXECUTION_CONTEXT | 스텝 재시작에 쓰는 상태 (리더 위치) | 3 |
| BATCH_JOB_SEQ | 잡 인스턴스 번호 채번 | 2 |
| BATCH_JOB_EXECUTION_SEQ | 잡 실행 번호 채번 | 2 |
| BATCH_STEP_EXECUTION_SEQ | 스텝 실행 번호 채번 | 2 |

## 4. 관리 테이블

| 테이블 | 용도 | 컬럼 수 |
| :--- | :--- | ---: |
| flyway_schema_history | Flyway 마이그레이션 적용 이력 (버전, 체크섬, 적용 일시) | 10 |
