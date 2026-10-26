# ERD

> 이 문서는 `scripts/generate-db-docs.py` 가 실제 DB 의 외래키와 컬럼 설명으로 만든다. 실선은 외래키, 점선은 외래키 없이 값으로 잇는 관계다.

- 기준: Flyway V7 적용 DB, 생성일 2026-10-26
- 칸에는 키 컬럼과 상태 같은 핵심 컬럼만 그렸다. 전체 컬럼은 [테이블 정의서](TABLE_DEFINITION.md) 에 있다

## 1. 업무와 공통컴포넌트

```mermaid
erDiagram
    users ||--o{ announcement : "작성"
    announcement ||--o{ application : "접수"
    users ||--o{ application : "신청"
    COMTNFILE |o--o{ application : "사업계획서"
    COMTCCMMNCLCODE |o--o{ COMTCCMMNCODE : "분류"
    COMTCCMMNCODE ||--o{ COMTCCMMNDETAILCODE : "상세 코드"
    COMTNBBSMASTER ||--o{ COMTNBBS : "게시물"
    COMTNFILE |o--o{ COMTNFAQINFO : "첨부"
    COMTNFILE ||--o{ COMTNFILEDETAIL : "파일"
    application ||--o{ evaluation : "평가"
    users ||--o{ evaluation : "채점"
    application ||--o{ settlement : "집행"
    COMTNFILE |o--o{ settlement : "증빙"
    users |o--o{ settlement : "검수"
    settlement ||--o{ settlement_history : "처리 이력"
    users |o..o{ login_history : "접속"
    users ||..o{ privacy_access_log : "처리자"
    users |o..o{ privacy_access_log : "정보주체"
    users ||..o{ settlement_history : "처리자"
    users ||..o{ COMTNBBS : "게시자"
    COMTCCMMNDETAILCODE ||..o{ announcement : "분야 FIELD"
    COMTCCMMNDETAILCODE ||..o{ settlement : "비목 EXP_ITEM"
    COMTNFILE |o..o{ COMTNBBS : "공지 첨부"
    users {
        varchar user_id PK "사용자 ID"
        varchar role "ROLE_COMPANY, ROLE_EVALUATOR 등"
        varchar company_business_no UK "기업회원 사업자등록번호"
    }
    login_history {
        bigint history_id PK "기록 번호"
        varchar user_id "사용자 ID"
    }
    privacy_access_log {
        bigint log_id PK "기록 번호"
        varchar user_id "처리자"
        varchar subject_id "정보주체"
    }
    announcement {
        bigint announcement_id PK "공고 번호"
        varchar field_code "지원 분야"
        varchar status "DRAFT, OPEN, CLOSED"
        varchar writer_id FK "작성자"
    }
    application {
        bigint application_id PK "신청서 번호"
        varchar receipt_no UK "접수번호"
        bigint announcement_id UK, FK "공고 번호"
        varchar company_user_id UK, FK "신청 기업 사용자"
        char plan_atch_file_id FK "사업계획서 첨부파일"
        varchar status "진행 상태"
    }
    evaluation {
        bigint evaluation_id PK "평가 번호"
        bigint application_id UK, FK "신청서 번호"
        varchar evaluator_user_id UK, FK "평가위원"
        int total_score "총점"
        varchar status "ASSIGNED, COMPLETED"
    }
    settlement {
        bigint settlement_id PK "정산 번호"
        bigint application_id FK "신청서 번호"
        varchar item_code "비목"
        bigint total_amount "합계"
        char receipt_atch_file_id FK "증빙 첨부파일"
        varchar status "REQUESTED, APPROVED, REJECTED"
        varchar reviewed_by FK "검수 처리자"
    }
    settlement_history {
        bigint history_id PK "이력 번호"
        bigint settlement_id FK "정산 번호"
        bigint total_amount "처리 시점의 집행 금액"
        varchar actor_id "처리자"
    }
    COMTCCMMNCLCODE {
        char CL_CODE PK "분류코드"
    }
    COMTCCMMNCODE {
        varchar CODE_ID PK "코드ID"
        char CL_CODE FK "분류코드"
    }
    COMTCCMMNDETAILCODE {
        varchar CODE_ID PK, FK "코드ID"
        varchar CODE PK "코드"
    }
    COMTECOPSEQ {
        varchar TABLE_NAME PK "채번 대상"
    }
    COMTNBBSMASTER {
        char BBS_ID PK "게시판ID"
    }
    COMTNBBS {
        decimal NTT_ID PK "게시물ID"
        char BBS_ID PK, FK "게시판ID"
        varchar NTCR_ID "게시자ID"
        char ATCH_FILE_ID "첨부파일ID"
    }
    COMTNFAQINFO {
        char FAQ_ID PK "FAQID"
        char ATCH_FILE_ID FK "첨부파일ID"
    }
    COMTNFILE {
        char ATCH_FILE_ID PK "첨부파일ID"
    }
    COMTNFILEDETAIL {
        char ATCH_FILE_ID PK, FK "첨부파일ID"
        decimal FILE_SN PK "파일순번"
    }
```

## 2. 배치 메타데이터

```mermaid
erDiagram
    BATCH_JOB_INSTANCE ||--o{ BATCH_JOB_EXECUTION : "실행"
    BATCH_JOB_EXECUTION ||--o{ BATCH_JOB_EXECUTION_CONTEXT : "상태"
    BATCH_JOB_EXECUTION ||--o{ BATCH_JOB_EXECUTION_PARAMS : "파라미터"
    BATCH_JOB_EXECUTION ||--o{ BATCH_STEP_EXECUTION : "스텝"
    BATCH_STEP_EXECUTION ||--o{ BATCH_STEP_EXECUTION_CONTEXT : "상태"
    BATCH_JOB_INSTANCE {
        bigint JOB_INSTANCE_ID PK
        varchar JOB_NAME UK
        varchar JOB_KEY UK
    }
    BATCH_JOB_EXECUTION {
        bigint JOB_EXECUTION_ID PK
        bigint JOB_INSTANCE_ID FK
    }
    BATCH_JOB_EXECUTION_PARAMS {
        bigint JOB_EXECUTION_ID FK
    }
    BATCH_JOB_EXECUTION_CONTEXT {
        bigint JOB_EXECUTION_ID PK, FK
    }
    BATCH_STEP_EXECUTION {
        bigint STEP_EXECUTION_ID PK
        bigint JOB_EXECUTION_ID FK
    }
    BATCH_STEP_EXECUTION_CONTEXT {
        bigint STEP_EXECUTION_ID PK, FK
    }
```

## 3. 외래키 없이 잇는 관계

| 관계 | 컬럼 | 외래키를 두지 않은 이유 |
| :--- | :--- | :--- |
| users → login_history (접속) | `login_history.user_id` | 접속기록은 계정과 묶지 않아 계정을 정리해도 남는다. 없는 아이디로 실패하면 비운다 |
| users → privacy_access_log (처리자) | `privacy_access_log.user_id` | 처리 기록은 계정과 묶지 않아 계정을 정리해도 남는다 |
| users → privacy_access_log (정보주체) | `privacy_access_log.subject_id` | 같은 이유. 여러 기업이 담긴 엑셀·목록은 비운다 |
| users → settlement_history (처리자) | `settlement_history.actor_id` | 처리 이력은 계정과 묶지 않아 계정을 정리해도 남는다 |
| users → COMTNBBS (게시자) | `COMTNBBS.NTCR_ID` | 공통컴포넌트 원본 DDL 에 외래키가 없다 |
| COMTCCMMNDETAILCODE → announcement (분야 FIELD) | `announcement.field_code` | 공통코드 키가 (코드ID, 코드) 두 컬럼이라 외래키 대신 서비스가 공통코드 목록으로 검증한다 |
| COMTCCMMNDETAILCODE → settlement (비목 EXP_ITEM) | `settlement.item_code` | 공통코드 키가 (코드ID, 코드) 두 컬럼이라 외래키 대신 서비스가 공통코드 목록으로 검증한다 |
| COMTNFILE → COMTNBBS (공지 첨부) | `COMTNBBS.ATCH_FILE_ID` | 공통컴포넌트 원본 DDL 에 외래키가 없다 |
