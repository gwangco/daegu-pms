# eGov 코드 생성기로 공지사항, FAQ 만들기

공지사항과 FAQ 는 처음에 eGovFrame VS Code 확장(eGovFrame VSCode Initializr 5.0.7)의 CRUD 코드 생성 기능으로 뼈대를 만들고, 검토하면서 고쳐 완성했다. 6차에 두 기능 모두 공통컴포넌트로 바꿨기 때문에 지금 코드는 아니지만, 생성기를 써 본 기록으로 남겨 둔다.

| 파일 | 내용 |
| :--- | :--- |
| `notice.ddl.sql`, `faq.ddl.sql` | 생성기에 넣은 DDL |
| `notice-generated/`, `faq-generated/` | 생성기가 만든 원본 그대로 (비교용, 빌드하지 않음) |
| `egov-codegen.mjs` | 확장의 템플릿을 그대로 써서 같은 결과를 만드는 스크립트 |

## 생성하는 방법

확장에서는 왼쪽 eGovFrame 아이콘 > Code Generator 에 DDL 을 붙여 넣고, 패키지 이름을 적은 뒤 Generate Code 를 누른다.

스크립트로 만들 때는 이렇게 한다.

```bash
npm install handlebars@4.7.9
EGOV_EXT_DIR=~/.vscode/extensions/egovframework.egovframe-vscode-initializr-5.0.7-universal \
  node egov-codegen.mjs notice.ddl.sql kr.go.daegu.pms.notice out
```

VO, Service, ServiceImpl(`EgovAbstractServiceImpl` 상속), Mapper(`@EgovMapper`), Controller, SQL, 화면이 eGov 표준 구조로 나온다.

## 그대로 쓰면 안 되는 이유

공지사항을 만들면서 고친 것이 10가지다.

1. VO 가 프로젝트에 없는 모듈(`EgovNullCheck`)을 import 해서 컴파일이 안 된다. Bean Validation 으로 바꿨다.
2. UPDATE 가 작성자, 작성일, 조회수까지 바꾼다. 화면 값을 조작하면 위조할 수 있어서 바꿀 컬럼만 남겼다.
3. 삭제가 물리 삭제다. 공공 시스템은 이력을 남겨야 해서 논리 삭제로 바꿨다.
4. 목록에 삭제한 글이 나오고 본문까지 읽는다. 조건과 정렬을 고치고 본문은 뺐다.
5. 컨트롤러에 권한 검사가 없어서 누구나 등록, 수정, 삭제할 수 있다.
6. 상세 조회 결과가 없으면 null 을 돌려줘서 500 이 난다. 404 로 바꿨다.
7. 폼을 jQuery 로 보내서 CSRF 토큰이 빠지고 POST 가 403 이 된다. `th:action` 으로 바꿨다.
8. 검색, 페이징 VO 를 업무마다 새로 만든다. 공통 VO 를 상속하게 했다.
9. `java.util.Date`, 필드 주입, `throws Exception` 을 쓴다. `LocalDateTime`, 생성자 주입, 업무 예외로 바꿨다.
10. `*.do` 주소에 조회도 POST 라서 북마크나 뒤로 가기가 안 된다. `/notices/{id}` 처럼 바꾸고 조회는 GET 으로 했다.

## FAQ 로 한 번 더

같은 기준이 다른 기능에도 통하는지 보려고 FAQ DDL 로 다시 생성했다. 10가지가 모두 그대로 나왔고 다섯 가지를 더 찾았다.

- 검색 조건을 `searchCondition == 0` 처럼 숫자로 비교한다. `?searchCondition=title` 만 보내도 MyBatis OGNL 이 숫자로 바꾸려다 실패해서 500 이 난다.
- 검색 대상이 컬럼 의미와 상관없이 PK 와 두 번째 컬럼으로 정해진다.
- 채번 Bean 이름이 `egovIdGnrService` 로 고정돼 있어 이 프로젝트에서는 서버가 뜨지 않는다.
- 분류 같은 코드성 컬럼을 자유 입력으로 받는다.
- 화면이 인라인 스크립트를 써서 CSP 에 막히고 버튼이 동작하지 않는다.

생성기 결과는 테이블이 바뀌면 다른 모양으로 깨진다. 그래서 위 10가지는 매번 보고, 검색 조건, 채번 Bean 이름, 코드성 컬럼, 화면 스크립트는 생성할 때마다 따로 확인한다. 숫자 검색 조건과 작성자 위조 문제는 공통컴포넌트 원본에도 있어서 같은 방법으로 고쳤다.
