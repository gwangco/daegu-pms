# eGovFrame CRUD 코드 생성기로 공지사항 만들기

공지사항(`kr.go.daegu.pms.notice`) 기능은 **전자정부 표준프레임워크 VS Code 확장(eGovFrame VSCode Initializr 5.0.7)**의
"CRUD 코드 생성" 기능으로 뼈대를 만든 뒤, 프로젝트 규칙과 보안 기준에 맞게 고쳐서 완성했다.

| 폴더/파일 | 내용 |
| :--- | :--- |
| `notice.ddl.sql` | 생성기에 입력한 DDL |
| `notice-generated/` | 생성기가 만든 **원본 그대로** (빌드 대상 아님, 비교용) |
| `egov-codegen.mjs` | 확장의 템플릿(`templates/code/*.hbs`)·헬퍼·타입 매핑을 그대로 써서 같은 결과를 만드는 스크립트 |
| `../../src/main/java/kr/go/daegu/pms/notice/` | 검토 후 보완한 **실제 코드** |

## 1. 직접 생성해 보기 (VS Code / Antigravity)

1. 확장 설치: 마켓플레이스에서 `eGovFrame VSCode Initializr` (게시자 `egovframework`)
2. 왼쪽 활동 표시줄의 **eGovFrame** 아이콘 → **Code Generator(코드 생성)**
3. `notice.ddl.sql` 내용을 붙여넣기 → **미리보기 생성** → 템플릿별 결과 확인
4. 패키지명 `kr.go.daegu.pms.notice` 입력 → **Generate Code** → 출력 폴더 선택

> 확장 화면 대신 스크립트로 같은 결과를 만들 때:
> ```bash
> npm install handlebars@4.7.9
> EGOV_EXT_DIR=~/.vscode/extensions/egovframework.egovframe-vscode-initializr-5.0.7-universal \
>   node egov-codegen.mjs notice.ddl.sql kr.go.daegu.pms.notice out
> ```

생성되는 파일 (eGov 표준 패키지 구조)

```text
notice/service/NoticeVO.java, NoticeDefaultVO.java, NoticeService.java
notice/service/impl/NoticeServiceImpl.java   (extends EgovAbstractServiceImpl, EgovIdGnrService 로 PK 발급)
notice/service/impl/NoticeMapper.java        (@EgovMapper)
notice/web/NoticeController.java             (EgovPropertyService + PaginationInfo)
mapper/Notice_SQL.xml
templates/thymeleaf/notice/noticeList.html, noticeRegister.html
```

## 2. 생성 결과 검토 포인트 (그대로 쓰면 안 되는 이유)

| # | 생성기 원본 | 문제 | 보완 |
| :-: | :--- | :--- | :--- |
| 1 | VO 가 `org.egovframe.rte.ptl.reactive.validation.EgovNullCheck` import | 프로젝트에 없는 모듈 → **컴파일 실패** | Bean Validation(`@NotBlank`, `@Size`, `@Pattern`) |
| 2 | UPDATE 가 `writer_id`, `created_at`, `view_count` 까지 SET | 화면 값 변조로 **작성자·작성일·조회수 위조** | 제목·내용·중요여부·수정자·수정일시만 UPDATE |
| 3 | DELETE 가 물리 삭제 | 공공 시스템은 **이력 보존** 필요 | `use_yn = 'N'` 논리 삭제 |
| 4 | 목록이 삭제글 포함, `notice_id` 순 정렬, 본문(CLOB) 조회 | 기능·성능 문제 | `use_yn='Y'` 조건, 중요 공지 우선, 본문 제외, 작성자명 조인 |
| 5 | 컨트롤러에 권한 검사 없음 | **누구나 등록/수정/삭제** | `@PreAuthorize` + URL 권한 + 서비스 재검사 |
| 6 | 상세 조회 결과가 없으면 `null` 반환 (예외 코드는 주석) | NPE → 500 오류 | `NotFoundException` → 404 |
| 7 | 화면의 폼에 `th:action` 없이 jQuery 로 submit | Spring Security **CSRF 토큰 누락 → POST 403** | `th:action` 사용 (토큰 자동 포함), jQuery 제거 |
| 8 | `NoticeDefaultVO` 를 업무마다 생성 | 검색/페이징 필드 중복 | 공통 `ComDefaultVO` 상속 |
| 9 | `java.util.Date`, 필드 주입(`@Resource`), `throws Exception` | 구식 API, 테스트 어려움 | `LocalDateTime`, 생성자 주입, 업무 예외 |
| 10 | `*.do` URL, 조회 화면도 POST 로 이동 | 북마크·뒤로가기 불가, 프로젝트 URL 규칙과 불일치 | `/notices`, `/notices/{id}` (조회는 GET) |

> **면접 포인트**: "코드 생성기로 표준 구조의 뼈대를 빠르게 만들고, 보안(권한·CSRF·대량 할당)과
> 공공 시스템 요건(논리 삭제·감사 컬럼) 관점에서 생성 결과를 검토해 10가지를 보완했습니다."
