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

## 3. 두 번째 생성: 자주 묻는 질문(FAQ)

공지사항에서 정한 10개 기준이 다른 업무에도 통하는지 보려고 새 DDL(`faq.ddl.sql`)로 한 번 더 생성했다.
생성기 원본은 `faq-generated/`, 보완한 실제 코드는 `src/main/java/kr/go/daegu/pms/faq/`에 있다.

```bash
EGOV_EXT_DIR=~/.vscode/extensions/egovframework.egovframe-vscode-initializr-5.0.7 \
  node egov-codegen.mjs faq.ddl.sql kr.go.daegu.pms.faq out
```

### 3-1. 10개 기준 적용

10개 문제가 FAQ 원본에도 모두 그대로 나왔다. 보완 방법은 공지사항과 같고, FAQ에 맞게 달라진 부분만 적는다.

| # | FAQ에서 달라진 보완 |
| :-: | :--- |
| 1 | `@NotBlank`, `@Size`, 표시 순서는 `@Min(1)`·`@Max(999)` |
| 2 | UPDATE는 분류·질문·답변·표시 순서만. INSERT도 원본은 작성자·작성일·사용여부를 화면 값으로 저장했다 → `#{audit.userId}`, `#{audit.now}`, `'Y'` |
| 4 | 목록은 분류 순서 → 표시 순서. 질문을 눌러 답을 보는 화면이라 목록에서 답변까지 읽으므로 답변 컬럼은 CLOB 대신 `VARCHAR(4000)` |
| 6 | 수정·삭제는 UPDATE 건수가 0이면(그 사이 삭제됨) 404 |
| 10 | `/faqs`, `/faqs/new`, `/faqs/{faqId}/edit`, `/faqs/{faqId}/delete` |

### 3-2. 10개 기준 밖에서 추가로 확인한 것

| # | 생성기 원본 | 문제 | 보완 | 공지 원본에도 있었나 |
| :-: | :--- | :--- | :--- | :--- |
| 11 | 검색 조건을 `<when test="searchCondition == 0">`처럼 숫자로 비교 | MyBatis OGNL이 문자열을 숫자로 바꾸려다 실패한다. `?searchCondition=title`만 보내도 `NumberFormatException` → 500. 빈 문자열은 `0`과 같다고 판정 | 분류는 정확히 일치, 검색어는 질문·답변에서 찾는다 | 있었다. 그때는 검색 대상만 바꾸고 원인은 기록하지 않았다 |
| 12 | 검색 대상이 PK와 두 번째 컬럼 | 템플릿이 컬럼 의미와 관계없이 고른다. 공지는 두 번째가 제목이라 쓸 만했지만 FAQ는 분류코드 | 질문·답변 검색 | 같은 규칙 (공지는 우연히 맞음) |
| 13 | `@Resource(name = "egovIdGnrService")` | 채번 Bean이 하나라고 가정한다. 이 프로젝트에는 그 이름의 Bean이 없어 기동되지 않는다 | `faqIdGnrService`(FAQ_ + 10자리)를 이름으로 주입 | 있었다 (`noticeIdGnrService`로 고침) |
| 14 | 분류를 텍스트 입력칸으로 받음 | 아무 값이나 저장된다 | 공통코드 `FAQ_CATEGORY` 선택, 서비스에서 사용 중인 코드인지 다시 확인 | 해당 없음 |
| 15 | 화면이 인라인 `<script>`와 `th:onclick` 사용 | CSP(`script-src 'self'`)가 막아서 버튼이 동작하지 않는다 | 스크립트 없는 화면. 펼침은 `<details>`/`<summary>` | 있었다. 그때는 CSP 적용 전이라 드러나지 않았다 |

11번은 MyBatis의 `OgnlCache`로 같은 식을 직접 평가해 확인했다(`"0"` → true, `"1"` → false, `"title"` → NumberFormatException, `""` → true).
보완한 동작은 `FaqIntegrationTest`(9건)와 `WebUiIntegrationTest`의 FAQ 화면 검사로 고정했다.

> **정리**: 생성기 결과는 테이블이 바뀌면 다른 모양으로 깨진다. 10개 기준은 매번 그대로 보고,
> 생성할 때마다 검색 조건 템플릿·채번 Bean 이름·코드성 컬럼·화면 스크립트를 추가로 확인한다.
