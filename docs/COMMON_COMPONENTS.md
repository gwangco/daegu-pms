# 전자정부 공통컴포넌트 사용 내역

원본: [eGovFramework/egovframe-common-components](https://github.com/eGovFramework/egovframe-common-components) 태그 `v5.0.7` (2026-10-07, 커밋 `7912e13`)
라이선스: Apache License 2.0 — 사본 [licenses/egovframe-common-components-LICENSE.txt](../licenses/egovframe-common-components-LICENSE.txt)

공통컴포넌트는 전자정부 사업에서 로그인·게시판·첨부파일·코드관리처럼 어느 시스템에나 있는 기능을 미리 만들어 둔 소스 묶음이다. 실무에서는 필요한 것만 가져와 프로젝트에 맞게 고쳐 쓴다. 이 문서는 그 "가져와서 고친 내역"을 남기는 곳이다. 원본을 업데이트할 때 여기와 각 파일의 개정이력을 보고 다시 반영한다.

## 1. 가져온 범위

| 컴포넌트 | 패키지 | 테이블 | D-PMS 에서 쓰는 곳 |
| :--- | :--- | :--- | :--- |
| 공통 기반 | `cmm` (`EgovComAbstractDAO`, `ComDefaultVO`, `ComDefaultCodeVO`) | — | 모든 공통컴포넌트 DAO 의 부모 |
| 공통코드 | `cmm.service.EgovCmmUseService` | `COMTCCMMNCLCODE`, `COMTCCMMNCODE`, `COMTCCMMNDETAILCODE` | 지원 분야·비목·FAQ 분류 선택 상자, 코드 검증 |
| 첨부파일 | `cmm.service.EgovFileMngService` | `COMTNFILE`, `COMTNFILEDETAIL` | 사업계획서, 정산 증빙, 공지 첨부(8차, 묶음 하나에 여러 파일) |
| 게시판 | `cop.bbs` (`EgovArticleService`) | `COMTNBBSMASTER`, `COMTNBBS` | 공지사항. 첨부 허용·개수·크기는 게시판 속성(`COMTNBBSMASTER`) 값으로 정함 |
| FAQ | `uss.olh.faq` (`EgovFaqService`) | `COMTNFAQINFO` | 자주 묻는 질문 |
| 채번 | (실행환경 ID Generation) | `COMTECOPSEQ` | 접수번호, 첨부파일·게시물·FAQ ID |

가져오지 않은 것: 로그인·권한(`uat`, `sec`), 회원 관리(`uss.umt`). D-PMS 는 역할 4개, 로그인 실패 잠금, 개인정보 암호화를 Spring Security 와 `users` 테이블로 이미 구현했고, 공통컴포넌트 로그인은 회원 테이블 3개와 세션 기반 `LoginVO` 를 전제로 해서 바꾸려면 인증 전체를 다시 만들어야 한다. 9차 기업회원 가입도 같은 이유로 `uss.umt` 의 기업회원 테이블(`COMTNENTRPRSMBER`)을 쓰지 않고 `users` 에 만들었으며, 가입을 신청 상태로 받고 관리자가 승인하는 흐름만 원본과 맞췄다([9차 문서](SPRINT9_COMPANY_SIGNUP.md) 5절). 항목별 비교와 실무에서 옮겨야 할 때의 절차는 [uat·sec 비교](UAT_SEC_COMPARISON.md)에 있다. 화면(JSP)과 컨트롤러(`*.do`)도 가져오지 않았다. 서비스·DAO·SQL 만 쓰고 화면은 D-PMS 의 Thymeleaf 공통 레이아웃으로 만들었다.

## 2. 연결 방식

```text
업무 Controller ──▶ 업무 Service (권한·검증·404) ──▶ 공통컴포넌트 Service ──▶ 공통컴포넌트 DAO ──▶ *_mysql.xml
NoticeController     NoticeServiceImpl                 EgovArticleService         EgovArticleDAO         EgovArticle_SQL_mysql.xml
```

- 공통컴포넌트는 원본 패키지(`egovframework.com.*`)를 그대로 둔다. 원본과 파일 단위로 비교하기 쉽다.
- `DaeguPmsApplication` 이 `kr.go.daegu.pms` 와 `egovframework.com` 을 함께 스캔한다.
- 공통컴포넌트 DAO 는 `egov.sqlSession` 이라는 이름의 SqlSessionFactory 를 주입받는다. 업무 Mapper 와 같은 팩토리에 이름만 하나 더 붙였다 (`EgovConfigMapper`). 그래서 트랜잭션과 감사 인터셉터를 같이 쓴다.
- SQL 은 원본처럼 DB 종류별 파일(`_mysql.xml`)을 쓰고, MySQL 파일만 읽는다.
- 트랜잭션·예외 후처리 포인트컷에 `egovframework.com..service.impl.*Impl` 을 추가했다.
- 공통컴포넌트가 이름으로 찾는 Bean(`egovFileIdGnrService`, `egovNttIdGnrService`, `egovFaqManageIdGnrService`)은 원본 `context-idgn-*.xml` 과 같은 설정으로 만들었다 (`EgovConfigIdGeneration`).
- 원본이 `globals.properties` 에서 읽던 `Globals.fileStorePath` 는 `application.yml` 에 두고 `app.file.upload-dir` 을 가리키게 했다.

## 3. 원본 대비 바꾼 점

### 공통 (여러 파일에 같은 이유로 적용)

| 바꾼 점 | 이유 |
| :--- | :--- |
| `@EgovNullCheck`(ptl-reactive 모듈) 제거, 화면 VO 에 Bean Validation 과 안내 문구 | 이 검증만 쓰려고 WebFlux 계열 모듈을 추가하지 않는다 |
| 작성자·수정자·일시를 `#{audit.userId}`, `#{audit.now}` 로 저장 | 원본은 화면·서비스가 넣은 값을 그대로 저장 → 위조 여지. 시간도 DB 시계 대신 애플리케이션 Clock 으로 통일 (테스트에서 고정 가능) |
| 수정·삭제가 처리 건수를 돌려줌 | 없는 글이나 이미 삭제된 글이면 업무 서비스가 404 로 응답하기 위함 |

### 게시판 (`cop.bbs` → 공지사항)

| 원본 | 바꾼 것 | 이유 |
| :--- | :--- | :--- |
| `selectArticleDetail` 이 조회수를 올린 뒤 조회 | 제거. `increaseInqireCo` + `selectArticleDetailNoCount` 로 나눠 호출 | D-PMS 는 `select*` 메서드를 읽기 전용 트랜잭션으로 실행한다. MySQL 드라이버는 읽기 전용 연결의 UPDATE 를 거부한다 (H2 는 허용해서 몰랐던 문제) |
| 조회수: 최대값을 읽어 +1 한 값을 저장 (2단계) | `RDCNT = RDCNT + 1` 한 번 | 두 사람이 동시에 열면 증가분 하나가 사라진다. 조회 시 수정자·수정일시를 바꾸던 부분도 제거 |
| `searchCnd == 0` 숫자 비교 | 문자열 비교 `searchCnd == "0"` | `?searchCnd=abc` 처럼 숫자가 아닌 값이 오면 OGNL 숫자 변환 오류로 500 |
| 작성자명: 회원 통합 뷰 `COMVNUSERMASTER`, 댓글 수: `COMTNCOMMENT` 조인 | D-PMS `users` 조인, 댓글 조인 제거 | 해당 뷰·테이블이 없다 |
| 상세 조회에 `PASSWORD` 컬럼 포함 | 조회하지 않음 | 필요 없는 민감 컬럼을 화면 객체에 싣지 않는다 |
| 삭제 시 제목을 "이 글은 작성자에 의해서 삭제되었습니다." 로 덮어씀 | `USE_AT = 'N'` 만 변경 | 원래 제목·내용을 감사 기록으로 남긴다 |
| 수정 시 게시 기간·비밀글·볼드·첨부 등 모든 컬럼 갱신 | 제목·내용·중요여부만, 삭제된 글은 수정 불가 | 화면에 없는 값이 빈 값으로 덮이지 않게 |
| 목록 정렬 `SORT_ORDR DESC` (공지는 별도 쿼리) | `NOTICE_AT DESC, SORT_ORDR DESC` | 중요 공지를 한 목록 위에 고정. 목록에서는 본문(`NTT_CN`)을 조회하지 않음 |
| 답글·방명록·블로그·첨부 업로드(`EgovFileMngUtil`) | 제거 | 공지 게시판에서 쓰지 않는 기능. 업로드는 검증이 더 강한 D-PMS `FileService` 를 쓴다 |
| 등록 시 `ATCH_FILE_ID` 저장 (원본은 컨트롤러가 정한 값) | 6차: 항상 비움 → 8차: 서버가 새로 만든 첨부 ID 만 저장 (`Board` 를 업무 서비스가 새로 만들어 채움) | 화면 값으로 남의 첨부 묶음을 연결하지 못하게. 원본 v5.0.7 컨트롤러도 수정 때 "원본 게시물의 첨부 ID 만 쓴다"는 IDOR 조치를 해 두었다 |
| 수정 시 `ATCH_FILE_ID` | 비어 있을 때만 연결(`IFNULL`), 이미 연결된 값은 바꾸지 않음 | 첫 첨부 때만 묶음을 연결하고, 다른 게시물의 묶음으로 바꿔치기할 수 없게 |
| 목록의 `ATCH_FILE_ID` | 파일이 하나라도 남아 있을 때만 반환 (`EXISTS`) | 파일을 모두 지운 빈 묶음에 첨부 표시(클립)가 붙지 않게 |

D-PMS 에서 더한 것: 공지 수정 전에 게시물 행을 `SELECT … FOR UPDATE` 로 잠그고(`NoticeMapper`), 게시판 속성의 첨부 가능 여부·개수·크기를 업무 서비스가 서버에서 검사한다. 원본은 게시판 속성을 게시판 속성관리(`EgovBBSMasterService`)로 읽지만, D-PMS 는 그 컴포넌트를 가져오지 않고 필요한 세 값만 조회한다.

### FAQ (`uss.olh.faq`)

| 원본 | 바꾼 것 | 이유 |
| :--- | :--- | :--- |
| 분류·표시 순서·사용 여부 없음 | `FAQ_CL_CODE`, `SORT_ORDR`, `USE_AT` 컬럼 추가 | 분류 탭, 분류 안 순서, 논리 삭제 |
| `DELETE` | `USE_AT = 'N'` | 이력 보존 |
| 등록·수정에 `#{atchFileId}` (화면 값) | 첨부파일 ID 를 받지 않음 | 다른 사람의 첨부파일 ID 를 FAQ 에 연결해 내려받는 경로를 만들 수 있었다 |
| 수정 SQL 에 `SET FAQ_ID = #{faqId}` | 제거 | PK 를 다시 쓸 이유가 없다 |
| `selectFaqDetail` 이 조회수 증가 + `throws Exception` | 조회수 증가 제거, 체크 예외 제거 | 게시판과 같은 읽기 전용 문제. D-PMS 는 서비스 밖으로 체크 예외를 던지지 않는다 |
| 검색: `searchCnd == 0` 숫자 비교, 질문 제목만 | 분류 필터 + 질문·답변 검색 | 500 오류 방지, 답변 내용으로도 찾기 |

### 첨부파일 (`cmm.fms`)

| 원본 | 바꾼 것 | 이유 |
| :--- | :--- | :--- |
| `FILE_STRE_COURS` 에 서버 절대 경로 저장 | 업로드 루트 아래 하위 경로(`yyyy/MM`)만 저장, 루트는 설정값 | 서버를 옮겨도 DB 를 고칠 필요가 없고 서버 디렉터리 구조가 DB 에 남지 않는다 |
| 물리 파일 삭제 `new File(경로, 이름).delete()` | 루트와 합친 뒤 루트 밖이면 삭제하지 않음 | 경로 조작(`../`)으로 다른 파일을 지우지 못하게 |
| `deleteFileInf`: DB 행을 지우기 전에 실제 파일부터 삭제 | DB 삭제 후 트랜잭션 **커밋 뒤에** 실제 파일 삭제 (8차) | 뒤에서 오류가 나 롤백되면 DB 에는 파일 정보가 남고 실제 파일만 사라졌다 |
| 업로드 처리 `EgovFileMngUtil.parseFileInf` | 가져오지 않음. D-PMS `FileServiceImpl` 이 확장자·시그니처 검사, UUID 저장명, 롤백 시 파일 삭제 후 `insertFileInf`(파일 1개)·`insertFileInfs`/`updateFileInfs`(여러 개, 순번은 `getMaxFileSN`) 호출 | 원본 유틸은 정적 `globals.properties` 와 commons-lang 2 에 의존하고, D-PMS 에 이미 더 엄격한 검증이 있다 |
| 다운로드 컨트롤러(`EgovFileDownloadController`) | 가져오지 않음. 업무 첨부는 `/files/{FILE_ID}/{순번}`(로그인 + 신청서·정산 열람 권한 재검사), 공지 첨부는 `/notices/{번호}/files/{순번}`(삭제되지 않은 공지의 파일만, 비로그인 허용) | 첨부파일 ID 는 순번이라 추측할 수 있다 (IDOR). 공개 범위를 "공개된 게시물의 첨부"로 좁혔다 |

### 공통코드 (`cmm.service.EgovCmmUseService`)

| 원본 | 바꾼 것 | 이유 |
| :--- | :--- | :--- |
| 상세코드 조회에 `ORDER BY` 없음 | `SORT_ORDR`(추가 컬럼), `CODE` 순 | 화면 선택 상자 순서를 업무 담당자가 정한 대로 |
| 조직정보·권한그룹 코드 조회 | 제거 | `COMTNORGNZTINFO`, `COMTNAUTHORGROUPINFO` 를 쓰지 않음 |

코드 ID 는 공통컴포넌트 규칙(분류코드 3자리 + 일련번호 3자리)으로 정했다.

| 코드 ID | 이름 | 이전 그룹 ID |
| :--- | :--- | :--- |
| `DPM001` | 지원 분야 | `FIELD` |
| `DPM002` | 신청서 진행상태 | `APP_STATUS` |
| `DPM003` | 사업비 비목 | `EXP_ITEM` |
| `DPM004` | 정산 검수상태 | `SETTLE_STATUS` |
| `DPM005` | FAQ 분류 | `FAQ_CATEGORY` |

## 4. 기존 데이터 옮기기 (Flyway V2)

`db/migration/V2__adopt_egov_common_components.sql` 이 옛 테이블 → 공통컴포넌트 테이블로 데이터를 옮기고 옛 테이블을 지운다.

| 옛 값 | 새 값 |
| :--- | :--- |
| 파일 번호 `1` | `FILE_000000000000001` (`FILE_` + 15자리) |
| 공지 ID `NTC_0000000001` | 게시물 ID `1` |
| FAQ ID `FAQ_0000000001` | `FAQ_0000000000000001` (`FAQ_` + 16자리) |
| `ids` 의 다음 번호 | `COMTECOPSEQ` 에 같은 값 (옮긴 데이터의 최대값 + 1 보다 작으면 큰 쪽) |

`CommonComponentIntegrationTest` 가 옛 테이블이 없어졌는지, 건수·ID 형식·채번 다음 번호가 맞는지 확인한다.

8차의 `V3__allow_notice_attachments.sql` 은 데이터를 옮기지 않고 게시판 속성 값만 바꾼다: 공지 게시판 첨부 가능(`FILE_ATCH_POSBL_AT='Y'`), 공지 하나에 3개(`ATCH_POSBL_FILE_NUMBER`), 파일당 10MB(`ATCH_POSBL_FILE_SIZE`, 바이트). 규칙을 바꿀 때 코드가 아니라 이 값을 고치면 된다 (`NoticeAttachmentIntegrationTest` 가 속성을 N 으로 바꾸면 업로드를 거부하는지 확인).

## 5. 원본을 업데이트할 때

1. 새 태그를 받아 이 문서의 파일 목록과 비교한다 (`git diff v5.0.7 v5.0.x -- <파일>`).
2. 원본 변경을 반영한 뒤 위 "바꾼 점"을 다시 적용한다. 각 파일 머리말 개정이력의 `D-PMS 적용` 줄이 체크리스트다.
3. 테이블 구조가 바뀌었으면 새 Flyway 버전(V4 …)으로 추가한다. 이미 적용한 V1~V3 파일은 고치지 않는다.
4. `./mvnw clean verify` 로 전체 테스트를 확인한다.
