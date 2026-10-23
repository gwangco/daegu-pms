-- =====================================================================
-- V4: 기업회원 가입 (9차)
--
-- 1) 가입 승인 상태
--    가입 화면으로 들어온 기업회원은 승인 대기(REQUESTED)로 저장하고, 총괄관리자가 승인(APPROVED)해야 로그인할 수 있다.
--    공통컴포넌트 기업회원(uss.umt)도 가입을 신청 상태(ENTRPRS_MBER_STTUS = 'A')로 받고 관리자가 승인하는 같은 흐름이다.
--    본인인증·국세청 사업자 상태 조회를 붙이지 않았으므로, 실제 기업인지는 담당자가 승인할 때 확인한다.
--    이미 있는 계정(관리자가 만든 계정·데모 계정)은 기본값(APPROVED)으로 승인된 상태가 된다.
--
-- 2) 이용약관·개인정보 수집·이용 동의 일시 (동의 기록)
--
-- 3) 기업회원의 사업자등록번호 중복 방지: 한 기업은 계정 하나
--    공고 중복 신청 검사가 계정 단위라서, 같은 기업이 계정을 둘 만들면 중복 신청 검사를 피할 수 있다.
--    담당자·평가위원은 같은 기관 소속이면 번호가 같을 수 있으므로 기업회원(ROLE_COMPANY)만 대상으로 한다.
--    MySQL 에는 조건부(부분) 인덱스가 없어서, 기업회원일 때만 값이 생기는 생성 컬럼에 UNIQUE 인덱스를 건다.
--    UNIQUE 인덱스는 NULL 을 여러 개 허용하므로 기업회원이 아닌 계정은 서로 겹쳐도 된다.
-- =====================================================================
ALTER TABLE users
    ADD COLUMN approval_status     VARCHAR(20) NOT NULL DEFAULT 'APPROVED'
        COMMENT '가입 승인 상태 (REQUESTED 승인 대기, APPROVED 승인)' AFTER enabled,
    ADD COLUMN approved_by         VARCHAR(50) NULL COMMENT '가입 승인자' AFTER approval_status,
    ADD COLUMN approved_at         DATETIME    NULL COMMENT '가입 승인 일시' AFTER approved_by,
    ADD COLUMN terms_agreed_at     DATETIME    NULL COMMENT '이용약관 동의 일시' AFTER approved_at,
    ADD COLUMN privacy_agreed_at   DATETIME    NULL COMMENT '개인정보 수집·이용 동의 일시' AFTER terms_agreed_at,
    ADD COLUMN company_business_no VARCHAR(20)
        GENERATED ALWAYS AS (CASE WHEN role = 'ROLE_COMPANY' THEN business_no END) STORED
        COMMENT '기업회원 사업자등록번호 (중복 가입 방지용 생성 컬럼, 직접 넣지 않음)',
    ADD UNIQUE INDEX uk_users_company_business_no (company_business_no);
