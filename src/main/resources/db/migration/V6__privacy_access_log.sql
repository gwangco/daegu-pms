-- =====================================================================
-- V6: 개인정보 처리 기록 (11차)
--
-- 「개인정보의 안전성 확보조치 기준」은 개인정보처리시스템에 접속해 개인정보를 처리한 기록
-- (처리자 계정, 일시, 접속지, 처리한 정보주체, 수행 업무)을 남기고 정기적으로 점검하도록 한다.
-- 로그인·로그아웃은 login_history 에 이미 남기므로, 여기에는 담당자·평가위원·관리자가
-- "남의 개인정보가 담긴 화면·파일"을 조회하거나 내려받은 기록을 남긴다. 본인 정보를 본 것은 남기지 않는다.
-- 운영에서는 1년 이상(대상에 따라 2년) 보관하고 월 1회 이상 점검한다. 데모는 보관 기간을 따로 두지 않는다.
-- =====================================================================
CREATE TABLE privacy_access_log (
    log_id       BIGINT      NOT NULL AUTO_INCREMENT COMMENT '기록 번호',
    user_id      VARCHAR(50) NOT NULL COMMENT '처리자 (로그인 사용자)',
    action       VARCHAR(20) NOT NULL COMMENT '수행 업무: VIEW 조회, DOWNLOAD 다운로드',
    target_type  VARCHAR(30) NOT NULL COMMENT '대상: APPLICATION 신청서, USER_LIST 사용자 목록, EXCEL 신청 현황 엑셀, FILE 첨부파일',
    target_id    VARCHAR(50) NULL     COMMENT '대상 식별자 (신청서 번호, 첨부파일 ID·순번)',
    subject_id   VARCHAR(50) NULL     COMMENT '정보주체 (신청 기업 아이디. 목록·엑셀처럼 여러 명이면 NULL)',
    record_count INT         NOT NULL DEFAULT 1 COMMENT '조회·다운로드한 건수',
    client_ip    VARCHAR(45) NOT NULL COMMENT '접속지 IP (IPv6 최대 길이)',
    accessed_at  DATETIME    NOT NULL COMMENT '처리 일시',
    PRIMARY KEY (log_id),
    INDEX idx_privacy_access_at (accessed_at),
    INDEX idx_privacy_access_user (user_id, accessed_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '개인정보 처리 기록';
