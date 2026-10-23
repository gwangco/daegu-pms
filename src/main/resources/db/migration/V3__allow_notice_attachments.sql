-- 8차: 공지사항 게시판에 첨부파일을 허용한다.
--
-- 전자정부 공통컴포넌트는 게시판마다 첨부 규칙을 게시판 속성(COMTNBBSMASTER)에 둔다.
-- 코드를 고치지 않고 이 값만 바꿔 첨부 허용 여부·개수·크기를 조정할 수 있다 (D-PMS 는 서버에서 이 값으로 검사).
--   FILE_ATCH_POSBL_AT      첨부 가능 여부 (Y/N)
--   ATCH_POSBL_FILE_NUMBER  게시물 하나에 올릴 수 있는 파일 수
--   ATCH_POSBL_FILE_SIZE    파일 하나의 최대 크기 (바이트, 10MB = 10485760)
UPDATE COMTNBBSMASTER
   SET FILE_ATCH_POSBL_AT = 'Y',
       ATCH_POSBL_FILE_NUMBER = 3,
       ATCH_POSBL_FILE_SIZE = 10485760,
       LAST_UPDUSR_ID = 'SYSTEM',
       LAST_UPDT_PNTTM = CURRENT_TIMESTAMP
 WHERE BBS_ID = 'BBSMSTR_000000000001';
