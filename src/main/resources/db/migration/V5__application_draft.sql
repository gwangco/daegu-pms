-- =====================================================================
-- V5: 신청서 임시저장 (10차)
--
-- 임시저장(TEMP_SAVED)은 작성 중인 신청서라 접수번호·제출 일시가 없고, 사업비·요약도 비어 있을 수 있다.
-- 그래서 해당 컬럼의 NOT NULL 을 풀고, 대신 "임시저장이 아니면 필수 항목이 모두 있어야 한다"는 CHECK 제약을 건다.
-- 화면·서비스 검증을 거치지 않은 UPDATE 가 실수로 빈 신청서를 접수 상태로 만들어도 DB 가 거부한다.
-- (MySQL 은 8.0.16 부터 CHECK 를 실제로 검사한다. 로컬 8.0.46, CI 8.4)
--
-- 제출 일시의 기본값(CURRENT_TIMESTAMP)은 그대로 둔다. 기존 INSERT 가 이 기본값에 기대고 있어서(테스트 데이터 등),
-- 임시저장 INSERT 와 제출 취소 UPDATE 가 NULL 을 직접 넣는다.
-- 사업계획서 파일은 샘플 데이터에 없어서 CHECK 에 넣지 않고, 제출할 때 서비스가 확인한다.
-- 한 기업이 같은 공고에 신청서 하나(uk_application_announcement_company)는 임시저장에도 그대로 적용된다.
-- =====================================================================
ALTER TABLE application
    MODIFY receipt_no    VARCHAR(20) NULL COMMENT '접수번호 (eGov IdGnr: DPMS-00000001). 처음 제출할 때 발급, 임시저장은 없음',
    MODIFY total_budget  BIGINT      NULL COMMENT '총 사업비',
    MODIFY request_grant BIGINT      NULL COMMENT '신청 지원금',
    MODIFY self_funding  BIGINT      NULL COMMENT '자부담',
    MODIFY summary       TEXT        NULL COMMENT '사업계획 요약',
    MODIFY submitted_at  DATETIME    NULL DEFAULT CURRENT_TIMESTAMP COMMENT '제출 일시 (임시저장·제출 취소 상태는 NULL)',
    ADD CONSTRAINT ck_application_submitted_complete CHECK (
        status = 'TEMP_SAVED'
        OR (receipt_no IS NOT NULL
            AND total_budget IS NOT NULL
            AND request_grant IS NOT NULL
            AND self_funding IS NOT NULL
            AND summary IS NOT NULL
            AND submitted_at IS NOT NULL)
    );
