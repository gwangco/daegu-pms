-- D-PMS (Daegu Project Management System) Database Schema
-- 공공 R&D 및 중소기업 지원사업 관리 시스템 표준 스키마

-- 1. 공통코드 그룹 (전자정부 표준 공통컴포넌트 준수)
CREATE TABLE IF NOT EXISTS com_code_group (
    group_id VARCHAR(20) PRIMARY KEY,
    group_name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    use_yn CHAR(1) DEFAULT 'Y' NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- 2. 공통코드 상세
CREATE TABLE IF NOT EXISTS com_code_detail (
    code_id VARCHAR(20) NOT NULL,
    group_id VARCHAR(20) NOT NULL,
    code_name VARCHAR(100) NOT NULL,
    sort_order INT DEFAULT 1 NOT NULL,
    use_yn CHAR(1) DEFAULT 'Y' NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    PRIMARY KEY (group_id, code_id),
    FOREIGN KEY (group_id) REFERENCES com_code_group(group_id)
);

-- 3. 사용자 및 기관 회원 (RBAC 4단계 권한)
CREATE TABLE IF NOT EXISTS users (
    user_id VARCHAR(50) PRIMARY KEY,
    password VARCHAR(255) NOT NULL,
    user_name VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    company_name VARCHAR(100),
    business_no VARCHAR(20),
    role VARCHAR(30) NOT NULL, -- ROLE_COMPANY, ROLE_EVALUATOR, ROLE_MANAGER, ROLE_ADMIN
    enabled BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- 4. 지원사업 공고
CREATE TABLE IF NOT EXISTS announcement (
    announcement_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    field_code VARCHAR(20) NOT NULL, -- AI/모빌리티/로봇/헬스케어 등
    total_budget BIGINT NOT NULL,     -- 총 지원 예산 (원)
    max_grant BIGINT NOT NULL,        -- 기업당 최대 지원금 (원)
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    content CLOB NOT NULL,
    status VARCHAR(20) DEFAULT 'OPEN' NOT NULL, -- DRAFT, OPEN, CLOSED
    view_count INT DEFAULT 0 NOT NULL,
    writer_id VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    FOREIGN KEY (writer_id) REFERENCES users(user_id)
);

-- 5. 기업 사업신청서 (상태머신 라이프사이클)
CREATE TABLE IF NOT EXISTS application (
    application_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    announcement_id BIGINT NOT NULL,
    company_user_id VARCHAR(50) NOT NULL,
    project_name VARCHAR(200) NOT NULL,
    total_budget BIGINT NOT NULL,
    request_grant BIGINT NOT NULL,
    self_funding BIGINT NOT NULL,
    summary CLOB NOT NULL,
    plan_file_name VARCHAR(255),
    status VARCHAR(30) DEFAULT 'SUBMITTED' NOT NULL, -- TEMP_SAVED, SUBMITTED, DOCUMENT_PASSED, EVALUATING, SELECTED, REJECTED
    submitted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    FOREIGN KEY (announcement_id) REFERENCES announcement(announcement_id),
    FOREIGN KEY (company_user_id) REFERENCES users(user_id)
);

-- 6. 외부 심사위원 평가표
CREATE TABLE IF NOT EXISTS evaluation (
    evaluation_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    application_id BIGINT NOT NULL,
    evaluator_user_id VARCHAR(50) NOT NULL,
    quantitative_score INT DEFAULT 0 NOT NULL, -- 정량점수 (40점 만점)
    qualitative_score INT DEFAULT 0 NOT NULL,  -- 정성점수 (60점 만점)
    total_score INT DEFAULT 0 NOT NULL,        -- 총점 (100점 만점)
    eval_comment CLOB,
    status VARCHAR(20) DEFAULT 'ASSIGNED' NOT NULL, -- ASSIGNED, COMPLETED
    evaluated_at TIMESTAMP,
    FOREIGN KEY (application_id) REFERENCES application(application_id),
    FOREIGN KEY (evaluator_user_id) REFERENCES users(user_id)
);

-- 7. 사업비(보조금) 비목별 집행 및 정산
CREATE TABLE IF NOT EXISTS settlement (
    settlement_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    application_id BIGINT NOT NULL,
    item_code VARCHAR(20) NOT NULL,   -- EXP01(인건비), EXP02(재료비), EXP03(시제품), EXP04(여비)
    expense_date DATE NOT NULL,
    vendor_name VARCHAR(100) NOT NULL,
    supply_amount BIGINT NOT NULL,
    vat_amount BIGINT NOT NULL,
    total_amount BIGINT NOT NULL,
    description VARCHAR(255) NOT NULL,
    receipt_file_name VARCHAR(255),
    status VARCHAR(20) DEFAULT 'REQUESTED' NOT NULL, -- REQUESTED, APPROVED, REJECTED
    reject_reason VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    FOREIGN KEY (application_id) REFERENCES application(application_id)
);
