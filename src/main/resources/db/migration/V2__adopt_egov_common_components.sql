-- =====================================================================
-- V2 전자정부 공통컴포넌트(v5.0.7) 테이블로 전환
--
-- 바꾸는 것 (왼쪽: V1 자체 테이블 → 오른쪽: 공통컴포넌트 표준 테이블)
--   com_code_group, com_code_detail → COMTCCMMNCLCODE, COMTCCMMNCODE, COMTCCMMNDETAILCODE (공통코드)
--   ids                             → COMTECOPSEQ (채번)
--   com_file                        → COMTNFILE, COMTNFILEDETAIL (첨부파일)
--   notice                          → COMTNBBSMASTER, COMTNBBS (게시판: 공지사항)
--   faq                             → COMTNFAQINFO (FAQ)
--
-- 테이블 정의는 공통컴포넌트 배포본 script/ddl/mysql/com_DDL_mysql.sql 을 따르고,
-- D-PMS 에서 추가한 컬럼에는 COMMENT 앞에 [D-PMS 추가] 를 붙였다.
-- 기존 데이터는 지우지 않고 새 테이블로 옮긴 뒤 옛 테이블을 삭제한다.
--
-- ※ MySQL 은 CREATE/ALTER/DROP 이 자동 커밋이라 중간에 실패하면 일부만 바뀐 상태로 남는다.
--   운영 반영 전에는 반드시 백업하고, 실패하면 flyway_schema_history 를 확인한 뒤 수동 복구한다.
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. 공통코드 (분류코드 → 코드 → 상세코드 3단계)
-- ---------------------------------------------------------------------
CREATE TABLE COMTCCMMNCLCODE
(
	CL_CODE               CHAR(3) NOT NULL COMMENT '분류코드',
	CL_CODE_NM            VARCHAR(60) NULL COMMENT '분류코드명',
	CL_CODE_DC            VARCHAR(200) NULL COMMENT '분류코드설명',
	USE_AT                CHAR(1) NULL COMMENT '사용여부',
	FRST_REGIST_PNTTM     DATETIME NULL COMMENT '최초등록시점',
	FRST_REGISTER_ID      VARCHAR(20) NULL COMMENT '최초등록자ID',
	LAST_UPDT_PNTTM       DATETIME NULL COMMENT '최종수정시점',
	LAST_UPDUSR_ID        VARCHAR(20) NULL COMMENT '최종수정자ID',
	PRIMARY KEY (CL_CODE)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '공통분류코드';

CREATE TABLE COMTCCMMNCODE
(
	CODE_ID               VARCHAR(6) NOT NULL COMMENT '코드ID',
	CODE_ID_NM            VARCHAR(60) NULL COMMENT '코드ID명',
	CODE_ID_DC            VARCHAR(200) NULL COMMENT '코드ID설명',
	USE_AT                CHAR(1) NULL COMMENT '사용여부',
	CL_CODE               CHAR(3) NULL COMMENT '분류코드',
	FRST_REGIST_PNTTM     DATETIME NULL COMMENT '최초등록시점',
	FRST_REGISTER_ID      VARCHAR(20) NULL COMMENT '최초등록자ID',
	LAST_UPDT_PNTTM       DATETIME NULL COMMENT '최종수정시점',
	LAST_UPDUSR_ID        VARCHAR(20) NULL COMMENT '최종수정자ID',
	PRIMARY KEY (CODE_ID),
	CONSTRAINT COMTCCMMNCODE_FK1 FOREIGN KEY (CL_CODE) REFERENCES COMTCCMMNCLCODE (CL_CODE)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '공통코드';

CREATE TABLE COMTCCMMNDETAILCODE
(
	CODE_ID               VARCHAR(6) NOT NULL COMMENT '코드ID',
	CODE                  VARCHAR(15) NOT NULL COMMENT '코드',
	CODE_NM               VARCHAR(60) NULL COMMENT '코드명',
	CODE_DC               VARCHAR(200) NULL COMMENT '코드설명',
	USE_AT                CHAR(1) NULL COMMENT '사용여부',
	SORT_ORDR             NUMERIC(5) NOT NULL DEFAULT 0 COMMENT '[D-PMS 추가] 화면 표시 순서',
	FRST_REGIST_PNTTM     DATETIME NULL COMMENT '최초등록시점',
	FRST_REGISTER_ID      VARCHAR(20) NULL COMMENT '최초등록자ID',
	LAST_UPDT_PNTTM       DATETIME NULL COMMENT '최종수정시점',
	LAST_UPDUSR_ID        VARCHAR(20) NULL COMMENT '최종수정자ID',
	PRIMARY KEY (CODE_ID, CODE),
	CONSTRAINT COMTCCMMNDETAILCODE_FK1 FOREIGN KEY (CODE_ID) REFERENCES COMTCCMMNCODE (CODE_ID)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '공통상세코드';

INSERT INTO COMTCCMMNCLCODE (CL_CODE, CL_CODE_NM, CL_CODE_DC, USE_AT, FRST_REGIST_PNTTM, FRST_REGISTER_ID)
VALUES ('DPM', 'D-PMS 업무', '지원사업 통합관리 업무 코드', 'Y', CURRENT_TIMESTAMP, 'SYSTEM');

-- 코드 그룹 ID 는 공통컴포넌트 규칙(6자리)에 맞춰 바꾼다. 아래에 없는 그룹이 있으면 NULL 이 되어 일부러 실패한다
INSERT INTO COMTCCMMNCODE (CODE_ID, CODE_ID_NM, CODE_ID_DC, USE_AT, CL_CODE, FRST_REGIST_PNTTM, FRST_REGISTER_ID)
SELECT CASE g.group_id
           WHEN 'FIELD' THEN 'DPM001'
           WHEN 'APP_STATUS' THEN 'DPM002'
           WHEN 'EXP_ITEM' THEN 'DPM003'
           WHEN 'SETTLE_STATUS' THEN 'DPM004'
           WHEN 'FAQ_CATEGORY' THEN 'DPM005'
       END,
       g.group_name, g.description, g.use_yn, 'DPM', g.created_at, 'SYSTEM'
FROM com_code_group g;

INSERT INTO COMTCCMMNDETAILCODE (CODE_ID, CODE, CODE_NM, CODE_DC, USE_AT, SORT_ORDR, FRST_REGIST_PNTTM, FRST_REGISTER_ID)
SELECT CASE d.group_id
           WHEN 'FIELD' THEN 'DPM001'
           WHEN 'APP_STATUS' THEN 'DPM002'
           WHEN 'EXP_ITEM' THEN 'DPM003'
           WHEN 'SETTLE_STATUS' THEN 'DPM004'
           WHEN 'FAQ_CATEGORY' THEN 'DPM005'
       END,
       d.code_id, d.code_name, NULL, d.use_yn, d.sort_order, d.created_at, 'SYSTEM'
FROM com_code_detail d;

-- ---------------------------------------------------------------------
-- 2. 채번 (공통컴포넌트의 모든 ID Generation 이 쓰는 표)
-- ---------------------------------------------------------------------
CREATE TABLE COMTECOPSEQ
(
	TABLE_NAME            VARCHAR(20) NOT NULL COMMENT '채번 대상',
	NEXT_ID               NUMERIC(30) NULL COMMENT '다음 번호',
	PRIMARY KEY (TABLE_NAME)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '채번';

INSERT INTO COMTECOPSEQ (TABLE_NAME, NEXT_ID)
SELECT table_name, next_id FROM ids WHERE table_name = 'APPLICATION_RECEIPT';

-- ---------------------------------------------------------------------
-- 3. 첨부파일 (첨부파일 묶음 COMTNFILE 1건 + 파일 COMTNFILEDETAIL N건)
--    FILE_STRE_COURS 에는 원본처럼 절대 경로가 아니라 업로드 루트 아래 하위 경로(yyyy/MM)를 저장한다
--    (서버를 옮겨도 DB 를 고칠 필요가 없고, 서버 디렉터리 구조가 DB 에 남지 않는다)
-- ---------------------------------------------------------------------
CREATE TABLE COMTNFILE
(
	ATCH_FILE_ID          CHAR(20) NOT NULL COMMENT '첨부파일ID',
	CREAT_DT              DATETIME NOT NULL COMMENT '생성일시',
	USE_AT                CHAR(1) NULL COMMENT '사용여부',
	PRIMARY KEY (ATCH_FILE_ID)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '파일속성';

CREATE TABLE COMTNFILEDETAIL
(
	ATCH_FILE_ID          CHAR(20) NOT NULL COMMENT '첨부파일ID',
	FILE_SN               NUMERIC(10) NOT NULL COMMENT '파일순번',
	FILE_STRE_COURS       VARCHAR(2000) NOT NULL COMMENT '파일저장경로 (업로드 루트 기준 하위 경로)',
	STRE_FILE_NM          VARCHAR(255) NOT NULL COMMENT '저장파일명 (UUID, 확장자 없음)',
	ORIGNL_FILE_NM        VARCHAR(255) NULL COMMENT '원파일명',
	FILE_EXTSN            VARCHAR(20) NOT NULL COMMENT '파일확장자',
	FILE_CN               MEDIUMTEXT NULL COMMENT '파일내용',
	FILE_SIZE             NUMERIC(8) NULL COMMENT '파일크기 (byte)',
	PRIMARY KEY (ATCH_FILE_ID, FILE_SN),
	CONSTRAINT COMTNFILEDETAIL_FK1 FOREIGN KEY (ATCH_FILE_ID) REFERENCES COMTNFILE (ATCH_FILE_ID)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '파일상세정보';

-- 기존 파일 번호 1 → FILE_000000000000001 (공통컴포넌트 첨부파일 ID 형식: FILE_ + 15자리)
INSERT INTO COMTNFILE (ATCH_FILE_ID, CREAT_DT, USE_AT)
SELECT CONCAT('FILE_', LPAD(file_id, 15, '0')), created_at, 'Y'
FROM com_file;

INSERT INTO COMTNFILEDETAIL (ATCH_FILE_ID, FILE_SN, FILE_STRE_COURS, STRE_FILE_NM, ORIGNL_FILE_NM, FILE_EXTSN, FILE_CN, FILE_SIZE)
SELECT CONCAT('FILE_', LPAD(file_id, 15, '0')), 0, file_path, stored_name, original_name, file_ext, NULL, file_size
FROM com_file;

INSERT INTO COMTECOPSEQ (TABLE_NAME, NEXT_ID)
SELECT 'FILE_ID', COALESCE(MAX(file_id), 0) + 1 FROM com_file;

-- 신청서의 사업계획서, 정산의 증빙: 숫자 파일 번호 → 첨부파일 ID
ALTER TABLE application
    ADD COLUMN plan_atch_file_id CHAR(20) NULL COMMENT '사업계획서 첨부파일 (COMTNFILE)' AFTER summary;
UPDATE application SET plan_atch_file_id = CONCAT('FILE_', LPAD(plan_file_id, 15, '0')) WHERE plan_file_id IS NOT NULL;
ALTER TABLE application DROP FOREIGN KEY fk_application_plan_file;
ALTER TABLE application DROP COLUMN plan_file_id;
ALTER TABLE application
    ADD CONSTRAINT fk_application_plan_atch_file FOREIGN KEY (plan_atch_file_id) REFERENCES COMTNFILE (ATCH_FILE_ID);

ALTER TABLE settlement
    ADD COLUMN receipt_atch_file_id CHAR(20) NULL COMMENT '증빙 첨부파일 (COMTNFILE)' AFTER description;
UPDATE settlement SET receipt_atch_file_id = CONCAT('FILE_', LPAD(receipt_file_id, 15, '0')) WHERE receipt_file_id IS NOT NULL;
ALTER TABLE settlement DROP FOREIGN KEY fk_settlement_receipt_file;
ALTER TABLE settlement DROP COLUMN receipt_file_id;
ALTER TABLE settlement
    ADD CONSTRAINT fk_settlement_receipt_atch_file FOREIGN KEY (receipt_atch_file_id) REFERENCES COMTNFILE (ATCH_FILE_ID);

-- ---------------------------------------------------------------------
-- 4. 게시판 - 공지사항 (게시판 마스터 1건 + 게시물)
-- ---------------------------------------------------------------------
CREATE TABLE COMTNBBSMASTER
(
	BBS_ID                CHAR(30) NOT NULL COMMENT '게시판ID',
	BBS_NM                VARCHAR(255) NOT NULL COMMENT '게시판명',
	BBS_INTRCN            VARCHAR(2400) NULL COMMENT '게시판소개',
	BBS_TY_CODE           CHAR(6) NOT NULL COMMENT '게시판유형코드 (BBST01 통합게시판)',
	REPLY_POSBL_AT        CHAR(1) NULL COMMENT '답장가능여부',
	FILE_ATCH_POSBL_AT    CHAR(1) NOT NULL COMMENT '파일첨부가능여부',
	ATCH_POSBL_FILE_NUMBER  NUMERIC(2) NOT NULL COMMENT '첨부가능파일숫자',
	ATCH_POSBL_FILE_SIZE  NUMERIC(8) NULL COMMENT '첨부가능파일사이즈',
	USE_AT                CHAR(1) NOT NULL COMMENT '사용여부',
	TMPLAT_ID             CHAR(20) NULL COMMENT '템플릿ID',
	CMMNTY_ID             CHAR(20) NULL COMMENT '커뮤니티ID',
	FRST_REGISTER_ID      VARCHAR(20) NOT NULL COMMENT '최초등록자ID',
	FRST_REGIST_PNTTM     DATETIME NOT NULL COMMENT '최초등록시점',
	LAST_UPDUSR_ID        VARCHAR(20) NULL COMMENT '최종수정자ID',
	LAST_UPDT_PNTTM       DATETIME NULL COMMENT '최종수정시점',
	BLOG_ID               CHAR(20) NULL COMMENT '블로그ID',
	BLOG_AT               CHAR(2) NULL COMMENT '블로그여부',
	PRIMARY KEY (BBS_ID)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '게시판마스터';

CREATE TABLE COMTNBBS
(
	NTT_ID                NUMERIC(20) NOT NULL COMMENT '게시물ID',
	BBS_ID                CHAR(30) NOT NULL COMMENT '게시판ID',
	NTT_NO                NUMERIC(20) NULL COMMENT '게시물번호',
	NTT_SJ                VARCHAR(2000) NULL COMMENT '게시물제목',
	NTT_CN                MEDIUMTEXT NULL COMMENT '게시물내용',
	ANSWER_AT             CHAR(1) NULL COMMENT '댓글여부',
	PARNTSCTT_NO          NUMERIC(10) NULL COMMENT '부모글번호',
	ANSWER_LC             NUMERIC(8) NULL COMMENT '댓글위치',
	SORT_ORDR             NUMERIC(8) NULL COMMENT '정렬순서',
	RDCNT                 NUMERIC(10) NULL COMMENT '조회수',
	USE_AT                CHAR(1) NOT NULL COMMENT '사용여부 (N: 삭제)',
	NTCE_BGNDE            CHAR(20) NULL COMMENT '게시시작일',
	NTCE_ENDDE            CHAR(20) NULL COMMENT '게시종료일',
	NTCR_ID               VARCHAR(20) NULL COMMENT '게시자ID',
	NTCR_NM               VARCHAR(20) NULL COMMENT '게시자명',
	PASSWORD              VARCHAR(200) NULL COMMENT '비밀번호 (익명 게시판용, D-PMS 미사용)',
	ATCH_FILE_ID          CHAR(20) NULL COMMENT '첨부파일ID',
	NOTICE_AT             CHAR(1) NULL COMMENT '공지사항여부 (Y: 목록 상단 고정)',
	SJ_BOLD_AT            CHAR(1) NULL COMMENT '제목볼드여부',
	SECRET_AT             CHAR(1) NULL COMMENT '비밀글여부',
	FRST_REGIST_PNTTM     DATETIME NOT NULL COMMENT '최초등록시점',
	FRST_REGISTER_ID      VARCHAR(20) NOT NULL COMMENT '최초등록자ID',
	LAST_UPDT_PNTTM       DATETIME NULL COMMENT '최종수정시점',
	LAST_UPDUSR_ID        VARCHAR(20) NULL COMMENT '최종수정자ID',
	BLOG_ID               CHAR(20) NULL COMMENT '블로그ID',
	PRIMARY KEY (NTT_ID, BBS_ID),
	-- [D-PMS 추가] 목록 조회 조건(게시판, 사용여부)과 정렬(공지 고정, 최신순)에 맞춘 인덱스
	INDEX COMTNBBS_i02 (BBS_ID, USE_AT, NOTICE_AT, SORT_ORDR),
	CONSTRAINT COMTNBBS_FK1 FOREIGN KEY (BBS_ID) REFERENCES COMTNBBSMASTER (BBS_ID)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '게시판';

INSERT INTO COMTNBBSMASTER (BBS_ID, BBS_NM, BBS_INTRCN, BBS_TY_CODE, REPLY_POSBL_AT, FILE_ATCH_POSBL_AT,
                            ATCH_POSBL_FILE_NUMBER, ATCH_POSBL_FILE_SIZE, USE_AT, FRST_REGISTER_ID, FRST_REGIST_PNTTM)
VALUES ('BBSMSTR_000000000001', '공지사항', 'D-PMS 이용 안내와 일정 공지', 'BBST01', 'N', 'N', 0, 0, 'Y', 'SYSTEM', CURRENT_TIMESTAMP);

INSERT INTO COMTECOPSEQ (TABLE_NAME, NEXT_ID) VALUES ('BBS_ID', 2);

-- 공지 ID NTC_0000000001 → 게시물 ID 1. 정렬 순서는 공통컴포넌트처럼 게시물 ID 로 둔다
INSERT INTO COMTNBBS (NTT_ID, BBS_ID, NTT_NO, NTT_SJ, NTT_CN, ANSWER_AT, PARNTSCTT_NO, ANSWER_LC, SORT_ORDR,
                      RDCNT, USE_AT, NTCR_ID, NTCR_NM, NOTICE_AT, SJ_BOLD_AT, SECRET_AT,
                      FRST_REGIST_PNTTM, FRST_REGISTER_ID, LAST_UPDT_PNTTM, LAST_UPDUSR_ID)
SELECT CAST(SUBSTRING(n.notice_id, 5) AS UNSIGNED), 'BBSMSTR_000000000001', 1, n.title, n.content, 'N', 0, 0,
       CAST(SUBSTRING(n.notice_id, 5) AS UNSIGNED),
       n.view_count, n.use_yn, n.writer_id, LEFT(u.user_name, 20), n.important_yn, 'N', 'N',
       n.created_at, n.writer_id, n.updated_at, n.updated_by
FROM notice n
LEFT JOIN users u ON u.user_id = n.writer_id;

INSERT INTO COMTECOPSEQ (TABLE_NAME, NEXT_ID)
SELECT 'NTT_ID', GREATEST(COALESCE((SELECT next_id FROM ids WHERE table_name = 'NOTICE'), 1),
                          COALESCE(MAX(CAST(SUBSTRING(notice_id, 5) AS UNSIGNED)), 0) + 1)
FROM notice;

-- ---------------------------------------------------------------------
-- 5. FAQ
-- ---------------------------------------------------------------------
CREATE TABLE COMTNFAQINFO
(
	FAQ_ID                CHAR(20) NOT NULL COMMENT 'FAQID',
	QESTN_SJ              VARCHAR(255) NULL COMMENT '질문제목',
	QESTN_CN              VARCHAR(2500) NULL COMMENT '질문내용',
	ANSWER_CN             VARCHAR(2500) NULL COMMENT '답변내용',
	RDCNT                 NUMERIC(10) NULL COMMENT '조회수',
	FRST_REGIST_PNTTM     DATETIME NOT NULL COMMENT '최초등록시점',
	FRST_REGISTER_ID      VARCHAR(20) NOT NULL COMMENT '최초등록자ID',
	LAST_UPDT_PNTTM       DATETIME NOT NULL COMMENT '최종수정시점',
	LAST_UPDUSR_ID        VARCHAR(20) NOT NULL COMMENT '최종수정자ID',
	ATCH_FILE_ID          CHAR(20) NULL COMMENT '첨부파일ID',
	QNA_PROCESS_STTUS_CODE  CHAR(1) NULL COMMENT '질의응답처리상태코드',
	FAQ_CL_CODE           VARCHAR(15) NOT NULL COMMENT '[D-PMS 추가] FAQ 분류 (공통코드 DPM005)',
	SORT_ORDR             NUMERIC(5) NOT NULL DEFAULT 1 COMMENT '[D-PMS 추가] 분류 안의 표시 순서',
	USE_AT                CHAR(1) NOT NULL DEFAULT 'Y' COMMENT '[D-PMS 추가] 사용여부 (N: 삭제)',
	PRIMARY KEY (FAQ_ID),
	INDEX COMTNFAQINFO_i02 (USE_AT, FAQ_CL_CODE, SORT_ORDR),
	CONSTRAINT COMTNFAQINFO_FK1 FOREIGN KEY (ATCH_FILE_ID) REFERENCES COMTNFILE (ATCH_FILE_ID)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'FAQ정보';

-- FAQ ID FAQ_0000000001 → FAQ_0000000000000001 (공통컴포넌트 형식: FAQ_ + 16자리)
INSERT INTO COMTNFAQINFO (FAQ_ID, QESTN_SJ, QESTN_CN, ANSWER_CN, RDCNT, FRST_REGIST_PNTTM, FRST_REGISTER_ID,
                          LAST_UPDT_PNTTM, LAST_UPDUSR_ID, FAQ_CL_CODE, SORT_ORDR, USE_AT)
SELECT CONCAT('FAQ_', LPAD(CAST(SUBSTRING(f.faq_id, 5) AS UNSIGNED), 16, '0')), f.question, NULL, f.answer, 0,
       f.created_at, f.writer_id, COALESCE(f.updated_at, f.created_at), COALESCE(f.updated_by, f.writer_id),
       f.category_code, f.sort_order, f.use_yn
FROM faq f;

INSERT INTO COMTECOPSEQ (TABLE_NAME, NEXT_ID)
SELECT 'FAQ_ID', GREATEST(COALESCE((SELECT next_id FROM ids WHERE table_name = 'FAQ'), 1),
                          COALESCE(MAX(CAST(SUBSTRING(faq_id, 5) AS UNSIGNED)), 0) + 1)
FROM faq;

-- ---------------------------------------------------------------------
-- 6. 옮긴 뒤 옛 테이블 삭제
-- ---------------------------------------------------------------------
DROP TABLE notice;
DROP TABLE faq;
DROP TABLE com_file;
DROP TABLE ids;
DROP TABLE com_code_detail;
DROP TABLE com_code_group;
