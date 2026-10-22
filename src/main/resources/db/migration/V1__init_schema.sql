-- =====================================================================
-- V1 기본 스키마 (MySQL 8.x, InnoDB, utf8mb4)
-- H2 인메모리(schema.sql)에서 쓰던 구조를 MySQL 문법으로 옮긴 첫 버전.
--
-- Flyway 규칙: 한 번 적용된 마이그레이션 파일은 고치지 않는다. 구조를 바꿀 때는 V2, V3 ... 파일을 추가한다.
-- 시간 컬럼은 DATETIME 을 쓴다 (TIMESTAMP 는 2038년 한계와 세션 시간대 변환이 있어서 업무 일시에는 부적합).
-- 기준 데이터(공통코드, 채번 시작값)는 운영에도 필요하므로 여기에, 데모 데이터는 db/seed 에 둔다.
-- =====================================================================

-- 1. 공통코드 그룹
CREATE TABLE com_code_group (
    group_id    VARCHAR(20)  NOT NULL COMMENT '코드 그룹 ID',
    group_name  VARCHAR(100) NOT NULL COMMENT '코드 그룹명',
    description VARCHAR(255) NULL     COMMENT '설명',
    use_yn      CHAR(1)      NOT NULL DEFAULT 'Y' COMMENT '사용 여부',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록 일시',
    PRIMARY KEY (group_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '공통코드 그룹';

-- 2. 공통코드 상세
CREATE TABLE com_code_detail (
    code_id    VARCHAR(20)  NOT NULL COMMENT '코드',
    group_id   VARCHAR(20)  NOT NULL COMMENT '코드 그룹 ID',
    code_name  VARCHAR(100) NOT NULL COMMENT '코드명',
    sort_order INT          NOT NULL DEFAULT 1 COMMENT '표시 순서',
    use_yn     CHAR(1)      NOT NULL DEFAULT 'Y' COMMENT '사용 여부',
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록 일시',
    PRIMARY KEY (group_id, code_id),
    CONSTRAINT fk_code_detail_group FOREIGN KEY (group_id) REFERENCES com_code_group (group_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '공통코드 상세';

-- 3. 채번 테이블 (전자정부 ID Generation: EgovTableIdGnrServiceImpl 이 행을 잠그고 번호를 올린다)
CREATE TABLE ids (
    table_name VARCHAR(20)   NOT NULL COMMENT '채번 대상 이름',
    next_id    DECIMAL(30,0) NOT NULL COMMENT '다음에 발급할 번호',
    PRIMARY KEY (table_name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '채번 관리';

-- 4. 사용자 (역할 4종: 기업, 평가위원, 사업담당자, 총괄관리자)
CREATE TABLE users (
    user_id          VARCHAR(50)  NOT NULL COMMENT '사용자 ID',
    password         VARCHAR(255) NOT NULL COMMENT '비밀번호 해시 ({bcrypt} 단방향)',
    user_name        VARCHAR(50)  NOT NULL COMMENT '이름',
    email            VARCHAR(255) NOT NULL COMMENT '이메일 (ARIA 암호문 {ARIA}Base64)',
    phone            VARCHAR(100) NULL     COMMENT '연락처 (ARIA 암호문)',
    company_name     VARCHAR(100) NULL     COMMENT '소속 기관·기업명',
    business_no      VARCHAR(20)  NULL     COMMENT '사업자등록번호',
    role             VARCHAR(30)  NOT NULL COMMENT 'ROLE_COMPANY, ROLE_EVALUATOR, ROLE_MANAGER, ROLE_ADMIN',
    enabled          BOOLEAN      NOT NULL DEFAULT TRUE COMMENT '사용 여부',
    login_fail_count INT          NOT NULL DEFAULT 0 COMMENT '연속 로그인 실패 횟수 (성공하면 0)',
    locked_at        DATETIME     NULL     COMMENT '잠긴 시각 (잠금 해제 시 NULL)',
    last_login_at    DATETIME     NULL     COMMENT '마지막 로그인 일시',
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '가입 일시',
    updated_by       VARCHAR(50)  NULL     COMMENT '수정자',
    updated_at       DATETIME     NULL     COMMENT '수정 일시',
    PRIMARY KEY (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '사용자';

-- 5. 접속 기록 (개인정보 처리시스템 접속기록: 운영에서는 1년 이상 보관·점검)
CREATE TABLE login_history (
    history_id  BIGINT      NOT NULL AUTO_INCREMENT COMMENT '기록 번호',
    user_id     VARCHAR(50) NULL     COMMENT '사용자 ID (없는 아이디는 입력값 대신 NULL)',
    result_code VARCHAR(20) NOT NULL COMMENT 'SUCCESS, FAIL_PASSWORD, FAIL_UNKNOWN_ID, LOCKED, DISABLED, LOGOUT',
    client_ip   VARCHAR(45) NOT NULL COMMENT '접속 IP (IPv6 최대 길이)',
    login_at    DATETIME    NOT NULL COMMENT '발생 일시',
    PRIMARY KEY (history_id),
    INDEX idx_login_history_user (user_id, login_at),
    INDEX idx_login_history_at (login_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '접속 기록';

-- 6. 첨부파일 (물리 파일은 업로드 경로에, 원본명·저장명·크기는 DB 에 분리 저장)
CREATE TABLE com_file (
    file_id       BIGINT       NOT NULL AUTO_INCREMENT COMMENT '파일 번호',
    original_name VARCHAR(255) NOT NULL COMMENT '원본 파일명 (다운로드 표시용)',
    stored_name   VARCHAR(100) NOT NULL COMMENT '서버 저장명 (UUID, 확장자 없음)',
    file_path     VARCHAR(255) NOT NULL COMMENT '저장 하위 경로 (yyyy/MM)',
    file_ext      VARCHAR(10)  NOT NULL COMMENT '확장자',
    file_size     BIGINT       NOT NULL COMMENT '크기 (byte)',
    created_by    VARCHAR(50)  NOT NULL COMMENT '등록자',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록 일시',
    PRIMARY KEY (file_id),
    CONSTRAINT fk_file_created_by FOREIGN KEY (created_by) REFERENCES users (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '첨부파일';

-- 7. 지원사업 공고
CREATE TABLE announcement (
    announcement_id BIGINT       NOT NULL AUTO_INCREMENT COMMENT '공고 번호',
    title           VARCHAR(200) NOT NULL COMMENT '공고명',
    field_code      VARCHAR(20)  NOT NULL COMMENT '지원 분야 (공통코드 FIELD)',
    total_budget    BIGINT       NOT NULL COMMENT '총 지원 예산 (원)',
    max_grant       BIGINT       NOT NULL COMMENT '기업당 최대 지원금 (원)',
    start_date      DATE         NOT NULL COMMENT '접수 시작일',
    end_date        DATE         NOT NULL COMMENT '접수 마감일',
    content         TEXT         NOT NULL COMMENT '공고 내용',
    status          VARCHAR(20)  NOT NULL DEFAULT 'OPEN' COMMENT 'DRAFT, OPEN, CLOSED',
    view_count      INT          NOT NULL DEFAULT 0 COMMENT '조회수',
    writer_id       VARCHAR(50)  NOT NULL COMMENT '작성자',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록 일시',
    updated_by      VARCHAR(50)  NULL     COMMENT '수정자 (배치는 SYSTEM)',
    updated_at      DATETIME     NULL     COMMENT '수정 일시',
    PRIMARY KEY (announcement_id),
    INDEX idx_announcement_status_end (status, end_date),
    CONSTRAINT fk_announcement_writer FOREIGN KEY (writer_id) REFERENCES users (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '지원사업 공고';

-- 8. 사업 신청서 (상태: TEMP_SAVED, SUBMITTED, DOCUMENT_PASSED, EVALUATING, SELECTED, REJECTED)
CREATE TABLE application (
    application_id  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '신청서 번호',
    receipt_no      VARCHAR(20)  NOT NULL COMMENT '접수번호 (eGov IdGnr: DPMS-00000001)',
    announcement_id BIGINT       NOT NULL COMMENT '공고 번호',
    company_user_id VARCHAR(50)  NOT NULL COMMENT '신청 기업 사용자',
    project_name    VARCHAR(200) NOT NULL COMMENT '과제명',
    total_budget    BIGINT       NOT NULL COMMENT '총 사업비',
    request_grant   BIGINT       NOT NULL COMMENT '신청 지원금',
    self_funding    BIGINT       NOT NULL COMMENT '자부담',
    summary         TEXT         NOT NULL COMMENT '사업계획 요약',
    plan_file_id    BIGINT       NULL     COMMENT '사업계획서 첨부파일',
    status          VARCHAR(30)  NOT NULL DEFAULT 'SUBMITTED' COMMENT '진행 상태',
    submitted_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '제출 일시',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록 일시',
    updated_by      VARCHAR(50)  NULL     COMMENT '수정자',
    updated_at      DATETIME     NULL     COMMENT '수정 일시',
    PRIMARY KEY (application_id),
    -- 한 기업은 같은 공고에 1회만 신청 (중복 신청을 DB 에서도 차단)
    CONSTRAINT uk_application_announcement_company UNIQUE (announcement_id, company_user_id),
    CONSTRAINT uk_application_receipt_no UNIQUE (receipt_no),
    INDEX idx_application_company (company_user_id),
    CONSTRAINT fk_application_announcement FOREIGN KEY (announcement_id) REFERENCES announcement (announcement_id),
    CONSTRAINT fk_application_company FOREIGN KEY (company_user_id) REFERENCES users (user_id),
    CONSTRAINT fk_application_plan_file FOREIGN KEY (plan_file_id) REFERENCES com_file (file_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '사업 신청서';

-- 9. 평가표 (정량 40 + 정성 60 = 100점)
CREATE TABLE evaluation (
    evaluation_id      BIGINT      NOT NULL AUTO_INCREMENT COMMENT '평가 번호',
    application_id     BIGINT      NOT NULL COMMENT '신청서 번호',
    evaluator_user_id  VARCHAR(50) NOT NULL COMMENT '평가위원',
    quantitative_score INT         NOT NULL DEFAULT 0 COMMENT '정량 점수 (40점 만점)',
    qualitative_score  INT         NOT NULL DEFAULT 0 COMMENT '정성 점수 (60점 만점)',
    total_score        INT         NOT NULL DEFAULT 0 COMMENT '총점',
    eval_comment       TEXT        NULL     COMMENT '평가 의견',
    status             VARCHAR(20) NOT NULL DEFAULT 'ASSIGNED' COMMENT 'ASSIGNED, COMPLETED',
    assigned_at        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '배정 일시',
    evaluated_at       DATETIME    NULL     COMMENT '평가 일시',
    created_by         VARCHAR(50) NULL     COMMENT '배정한 담당자',
    updated_by         VARCHAR(50) NULL     COMMENT '수정자',
    updated_at         DATETIME    NULL     COMMENT '수정 일시',
    PRIMARY KEY (evaluation_id),
    -- 한 과제에 같은 평가위원 중복 배정 금지
    CONSTRAINT uk_evaluation_app_evaluator UNIQUE (application_id, evaluator_user_id),
    CONSTRAINT ck_evaluation_quantitative CHECK (quantitative_score BETWEEN 0 AND 40),
    CONSTRAINT ck_evaluation_qualitative CHECK (qualitative_score BETWEEN 0 AND 60),
    INDEX idx_evaluation_evaluator (evaluator_user_id),
    CONSTRAINT fk_evaluation_application FOREIGN KEY (application_id) REFERENCES application (application_id),
    CONSTRAINT fk_evaluation_evaluator FOREIGN KEY (evaluator_user_id) REFERENCES users (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '평가표';

-- 10. 사업비 집행·정산
CREATE TABLE settlement (
    settlement_id   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '정산 번호',
    application_id  BIGINT       NOT NULL COMMENT '신청서 번호',
    item_code       VARCHAR(20)  NOT NULL COMMENT '비목 (공통코드 EXP_ITEM)',
    expense_date    DATE         NOT NULL COMMENT '집행일',
    vendor_name     VARCHAR(100) NOT NULL COMMENT '거래처',
    supply_amount   BIGINT       NOT NULL COMMENT '공급가액',
    vat_amount      BIGINT       NOT NULL COMMENT '부가세',
    total_amount    BIGINT       NOT NULL COMMENT '합계',
    description     VARCHAR(255) NOT NULL COMMENT '집행 내용',
    receipt_file_id BIGINT       NULL     COMMENT '증빙 첨부파일',
    status          VARCHAR(20)  NOT NULL DEFAULT 'REQUESTED' COMMENT 'REQUESTED, APPROVED, REJECTED',
    reject_reason   VARCHAR(255) NULL     COMMENT '보완 요청 사유',
    resubmit_count  INT          NOT NULL DEFAULT 0 COMMENT '보완 후 재요청 횟수',
    reviewed_by     VARCHAR(50)  NULL     COMMENT '검수 처리자',
    reviewed_at     DATETIME     NULL     COMMENT '검수 일시',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록 일시',
    created_by      VARCHAR(50)  NULL     COMMENT '등록한 기업 사용자',
    updated_by      VARCHAR(50)  NULL     COMMENT '수정자',
    updated_at      DATETIME     NULL     COMMENT '수정 일시',
    PRIMARY KEY (settlement_id),
    INDEX idx_settlement_application (application_id, status),
    INDEX idx_settlement_status (status, created_at),
    CONSTRAINT fk_settlement_application FOREIGN KEY (application_id) REFERENCES application (application_id),
    CONSTRAINT fk_settlement_receipt_file FOREIGN KEY (receipt_file_id) REFERENCES com_file (file_id),
    CONSTRAINT fk_settlement_reviewed_by FOREIGN KEY (reviewed_by) REFERENCES users (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '사업비 정산';

-- 11. 정산 처리 이력 (등록 → 승인/반려 → 보완 재요청). 지난 반려 사유는 여기에 남는다
CREATE TABLE settlement_history (
    history_id    BIGINT       NOT NULL AUTO_INCREMENT COMMENT '이력 번호',
    settlement_id BIGINT       NOT NULL COMMENT '정산 번호',
    action_code   VARCHAR(20)  NOT NULL COMMENT 'REQUESTED, APPROVED, REJECTED, RESUBMITTED',
    total_amount  BIGINT       NOT NULL COMMENT '처리 시점의 집행 금액',
    memo          VARCHAR(255) NULL     COMMENT '보완 요청 사유',
    actor_id      VARCHAR(50)  NOT NULL COMMENT '처리자',
    acted_at      DATETIME     NOT NULL COMMENT '처리 일시',
    PRIMARY KEY (history_id),
    INDEX idx_settlement_history (settlement_id, acted_at),
    CONSTRAINT fk_settlement_history_settlement FOREIGN KEY (settlement_id) REFERENCES settlement (settlement_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '정산 처리 이력';

-- 12. 공지사항
CREATE TABLE notice (
    notice_id    VARCHAR(20)  NOT NULL COMMENT '공지 ID (eGov IdGnr: NTC_0000000001)',
    title        VARCHAR(200) NOT NULL COMMENT '제목',
    content      TEXT         NOT NULL COMMENT '내용',
    important_yn CHAR(1)      NOT NULL DEFAULT 'N' COMMENT '중요 공지 여부',
    view_count   INT          NOT NULL DEFAULT 0 COMMENT '조회수',
    use_yn       CHAR(1)      NOT NULL DEFAULT 'Y' COMMENT '사용 여부 (N: 삭제)',
    writer_id    VARCHAR(50)  NOT NULL COMMENT '작성자',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록 일시',
    updated_by   VARCHAR(50)  NULL     COMMENT '수정자',
    updated_at   DATETIME     NULL     COMMENT '수정 일시',
    PRIMARY KEY (notice_id),
    INDEX idx_notice_list (use_yn, important_yn, created_at),
    CONSTRAINT fk_notice_writer FOREIGN KEY (writer_id) REFERENCES users (user_id),
    CONSTRAINT fk_notice_updated_by FOREIGN KEY (updated_by) REFERENCES users (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '공지사항';

-- 13. 자주 묻는 질문
CREATE TABLE faq (
    faq_id        VARCHAR(20)   NOT NULL COMMENT 'FAQ ID (eGov IdGnr: FAQ_0000000001)',
    category_code VARCHAR(20)   NOT NULL COMMENT '분류 (공통코드 FAQ_CATEGORY)',
    question      VARCHAR(300)  NOT NULL COMMENT '질문',
    answer        VARCHAR(4000) NOT NULL COMMENT '답변',
    sort_order    INT           NOT NULL DEFAULT 1 COMMENT '분류 안의 표시 순서',
    use_yn        CHAR(1)       NOT NULL DEFAULT 'Y' COMMENT '사용 여부 (N: 삭제)',
    writer_id     VARCHAR(50)   NOT NULL COMMENT '작성자',
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록 일시',
    updated_by    VARCHAR(50)   NULL     COMMENT '수정자',
    updated_at    DATETIME      NULL     COMMENT '수정 일시',
    PRIMARY KEY (faq_id),
    INDEX idx_faq_list (use_yn, category_code, sort_order),
    CONSTRAINT fk_faq_writer FOREIGN KEY (writer_id) REFERENCES users (user_id),
    CONSTRAINT fk_faq_updated_by FOREIGN KEY (updated_by) REFERENCES users (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '자주 묻는 질문';

-- ---------------------------------------------------------------------
-- 기준 데이터 (운영에도 필요)
-- ---------------------------------------------------------------------
INSERT INTO com_code_group (group_id, group_name, description, use_yn) VALUES
('FIELD', '지원 분야', '대구 신산업 R&D 및 기술지원 5대 전략 분야', 'Y'),
('APP_STATUS', '신청서 진행상태', '지원사업 신청부터 협약까지의 상태머신', 'Y'),
('EXP_ITEM', '사업비 비목분류', '보조금 관리규정에 따른 사업비 집행 비목', 'Y'),
('SETTLE_STATUS', '정산 검수상태', '사업비 영수증 검수 상태', 'Y'),
('FAQ_CATEGORY', 'FAQ 분류', '자주 묻는 질문 분류 탭', 'Y');

INSERT INTO com_code_detail (group_id, code_id, code_name, sort_order, use_yn) VALUES
('FIELD', 'MOBILITY', '미래 모빌리티/부품', 1, 'Y'),
('FIELD', 'ROBOT', '첨단 로봇/자동화', 2, 'Y'),
('FIELD', 'AI_SW', '인공지능(AI)·빅데이터', 3, 'Y'),
('FIELD', 'HEALTH', '디지털 헬스케어', 4, 'Y'),
('FIELD', 'SMART_FAB', '스마트 제조/공구금형', 5, 'Y'),
('APP_STATUS', 'TEMP_SAVED', '임시저장', 1, 'Y'),
('APP_STATUS', 'SUBMITTED', '접수완료', 2, 'Y'),
('APP_STATUS', 'DOCUMENT_PASSED', '서류적격통과', 3, 'Y'),
('APP_STATUS', 'EVALUATING', '전문가심사중', 4, 'Y'),
('APP_STATUS', 'SELECTED', '최종선정(협약)', 5, 'Y'),
('APP_STATUS', 'REJECTED', '심사탈락', 6, 'Y'),
('EXP_ITEM', 'EXP01', '인건비 (연구원/개발자)', 1, 'Y'),
('EXP_ITEM', 'EXP02', '직접재료비 및 시약구입비', 2, 'Y'),
('EXP_ITEM', 'EXP03', '시제품 제작 및 가공비', 3, 'Y'),
('EXP_ITEM', 'EXP04', '연구활동비 (출장/여비)', 4, 'Y'),
('EXP_ITEM', 'EXP05', '위탁정산 및 수수료', 5, 'Y'),
('SETTLE_STATUS', 'REQUESTED', '검수요청', 1, 'Y'),
('SETTLE_STATUS', 'APPROVED', '검수승인', 2, 'Y'),
('SETTLE_STATUS', 'REJECTED', '보완(반려)', 3, 'Y'),
('FAQ_CATEGORY', 'APPLY', '신청·접수', 1, 'Y'),
('FAQ_CATEGORY', 'REVIEW', '심사·선정', 2, 'Y'),
('FAQ_CATEGORY', 'SETTLE', '사업비 정산', 3, 'Y'),
('FAQ_CATEGORY', 'ACCOUNT', '회원·계정', 4, 'Y');

-- 채번 시작값 (데모 데이터를 넣는 환경은 db/seed 에서 이어지는 번호로 올린다)
INSERT INTO ids (table_name, next_id) VALUES
('APPLICATION_RECEIPT', 1),
('NOTICE', 1),
('FAQ', 1);
