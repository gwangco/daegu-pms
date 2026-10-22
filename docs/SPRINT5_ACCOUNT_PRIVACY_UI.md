# 5차 스프린트 — 계정 보안·개인정보 보호·화면 개선

작업일: 2026-10-22 · 브랜치: `feature/sprint5-account-privacy-ui` · 이전 문서: [4차 스프린트](SPRINT4_MESSAGES_SECURITY.md)

## 1. 범위

4차 문서에 후속 후보로 남긴 것 중 계정 잠금, 개인정보 암호화, 감사 컬럼, 공통 레이아웃을 구현했다. 이전 학습 기록에 실습 과제로 남겨 둔 정산 페이징·보완 재요청, 사업자등록번호 마스킹, 공통코드 사용 여부 확인, 새 DDL 코드 생성도 이번에 마무리했다.

회원가입, 신청 임시저장, MariaDB/Flyway, eGov 배치 전환은 이번 범위가 아니다.

## 2. 바뀐 것

| 영역 | 이전 | 이번 | 검증 |
| :--- | :--- | :--- | :--- |
| 계정 잠금 | 비밀번호를 제한 없이 시도 가능 | 5회 연속 실패 시 30분 잠금, 접속 기록, 관리자 잠금 해제 | `AccountLockIntegrationTest` |
| 개인정보 | 연락처·이메일 평문 저장 | eGov 암호화 서비스(ARIA)로 저장, 관리자 목록 마스킹 | `PersonalInfoCryptoIntegrationTest`, `MaskingUtilTest` |
| 감사 컬럼 | 공지사항만 서비스에서 수정자를 직접 넣음 | MyBatis 인터셉터가 모든 SQL에 수정자·수정일시 값을 제공 | `AuditColumnIntegrationTest` |
| 정산 | 반려되면 새로 등록, 목록 전체 조회 | 보완 후 재요청, 처리 이력, 페이징·상태 필터, 담당자 검수함 | `SettlementResubmitIntegrationTest` |
| 화면 | 화면마다 head·CDN 반복, 인라인 스크립트 | 공통 레이아웃, WebJars, CSP, 접근성 기본 요소 | `WebUiIntegrationTest`, 브라우저 점검 |
| 메인 | 전체 통계만 표시 | 역할별 처리할 일(대기 건수와 바로가기) | `DashboardAndCodeIntegrationTest` |
| 엑셀 | XSSF (모든 행을 메모리에 보관) | SXSSF 스트리밍, 고정 열 너비, 머리글 행 고정 | `ExcelExportIntegrationTest` |
| FAQ | 없음 | eGov 코드 생성기로 생성한 뒤 보완 | `FaqIntegrationTest` |
| 실행환경 | eGovFrame 5.0.2 (Spring Boot 3.5.6) | 5.1.0 (Spring Boot 3.5.16) | 전체 회귀 테스트 |

## 3. 계정 잠금 — 실패 횟수도 동시 요청에서 틀릴 수 있다

틀린 비밀번호 요청 두 건이 동시에 같은 실패 횟수(예: 3)를 읽고 각각 4로 저장하면 실제로는 두 번 틀렸는데 한 번만 늘어난다. 이를 반복하면 잠금 한도를 늦출 수 있다.

`LoginPolicyServiceImpl`은 사용자 행을 `SELECT … FOR UPDATE`로 잠근 뒤 Java에서 횟수를 계산해 저장한다. 같은 계정의 실패 요청은 순서대로 처리된다.

- 잠긴 계정은 비밀번호를 비교하기 전에 거부한다. 잠긴 동안 비밀번호가 맞는지 알려 주지 않는다.
- 없는 아이디는 접속 기록에 입력값 대신 NULL을 남긴다. 아이디 칸에 비밀번호를 잘못 넣는 경우가 있기 때문이다.
- 한도와 잠금 시간은 `app.login.max-failures`, `app.login.lock-duration` 설정값이다.

## 4. 개인정보 암호화 — eGov 구현의 특성부터 확인했다

`egovframe-boot-starter-crypto`의 `EgovARIACryptoService`로 `users.phone`·`email`을 `{ARIA}` + Base64로 저장한다(`cmm/crypto/PersonalInfoCipher`). 앞의 표식으로 평문과 암호문을 구분하므로 기존 평문을 단계적으로 전환할 수 있다.

- 키는 저장소에 두지 않는다. 운영은 `DPMS_CRYPTO_KEY`, `DPMS_CRYPTO_KEY_HASH` 환경변수로 넣고, 키가 32자가 아니거나 해시가 맞지 않으면 기동하지 않는다. eGov 기본 동작은 기본 키를 쓸 때 경고만 출력한다.
- 라이브러리를 직접 시험해 확인한 특성을 테스트로 남겼다.
  - 같은 평문은 항상 같은 암호문이 된다. 같은 값끼리 비교는 되지만 값의 빈도가 드러날 수 있다.
  - 평문이 16바이트 배수이고 끝 바이트가 패딩처럼 보이면 복호화 결과가 잘린다. 제어문자가 들어간 값은 암호화하지 않고 거부한다.
  - `org.egovframe.boot.crypto` DEBUG 로그는 평문과 키를 출력한다. 로그 설정에서 INFO로 고정했다.
- 마스킹은 서버에서 한다. 관리자 목록은 복호화 후 가린 값만 내려가고, 본인 정보(`/mypage`)만 전체를 보여 준다. 평가위원 화면의 사업자등록번호도 가린다.
- 조회 결과 객체를 그대로 마스킹하면 MyBatis 1차 캐시에 들어 있는 객체가 바뀐다. 같은 트랜잭션의 다음 조회가 가려진 값을 받으므로 복사본(`toBuilder`)을 만들어 고친다.

## 5. 감사 컬럼 — 서비스 코드에서 수정자를 넣지 않는다

`cmm/mybatis/AuditParameterInterceptor`는 MyBatis가 SQL에 값을 바인딩하기 직전(`StatementHandler.parameterize`)에 `audit`(로그인 사용자, 현재 시각)를 추가 파라미터로 넣는다.

```xml
UPDATE notice
SET title = #{title},
    updated_by = #{audit.userId},
    updated_at = #{audit.now}
```

파라미터 타입이 VO든 `@Param` Map이든 같은 방법으로 쓴다. 값은 SecurityContext와 Clock에서만 가져오므로 요청 파라미터로 바꿀 수 없다. 로그인 사용자가 없는 배치는 `SYSTEM`으로 기록한다.

## 6. 정산 보완 재요청 — 잠금 순서와 1차 캐시

처리 순서는 `과제 행 잠금 → 정산 건 최신 상태 재조회 → 금액 재계산·잔액 검증 → 반려 상태일 때만 UPDATE → 이력 INSERT`다. 등록·승인·반려·재요청은 `settlement_history`에 처리자·시각과 함께 남고 화면에서 건별 타임라인으로 보인다.

잠그기 전에 읽은 결과가 MyBatis 1차 캐시(기본 범위 SESSION)에 남아 있으면 잠근 뒤 같은 조회를 해도 DB 대신 캐시 객체를 돌려준다. 그러면 잠금 후 재검증이 의미가 없어서 `localCacheScope=STATEMENT`로 바꿨다.

목록에 보이는 정산 건의 이력은 `IN` 조회 한 번으로 가져온다(건마다 조회하는 N+1 방지).

## 7. 화면

- Thymeleaf Layout Dialect로 공통 레이아웃(`layout/default`, 오류 화면용 `layout/simple`)을 만들었다.
- Bootstrap·아이콘·Chart.js를 WebJars로 내장하고 CSP(`script-src 'self'`)를 적용했다. 인라인 `<script>`와 `onclick`은 `data-*` 속성과 공통 스크립트(`static/js/dpms.js`)로 옮겼다. eGov 페이지 렌더러도 `onclick` 대신 `data-page`를 출력한다.
- 본문 바로가기, `lang="ko"`, 현재 메뉴 `aria-current`, 표 `caption`·`scope`, 입력칸 `label` 연결을 공통 규칙으로 두고 `WebUiIntegrationTest`가 주요 화면 21개를 검사한다.
- 실행 JAR을 헤드리스 Chrome으로 열어 32개 화면을 1366px·390px 폭에서 점검했다. CSP 위반, 콘솔 오류, 가로 넘침, 라벨 없는 입력칸은 0건이었다.
- 화면 캡처 중 모든 목록 표 오른쪽에 세로 스크롤바가 보이는 것을 발견했다. 표 높이가 소수점(311.14px)이라 컨테이너가 세로로 1px 넘쳤고, 가로 스크롤용 `overflow-x: auto`가 세로도 `auto`로 만든 탓이다. 일반 스크롤바를 쓰는 Windows 브라우저에서만 보이고 자동 점검 항목에는 걸리지 않았다. `.table-responsive { overflow-y: hidden; }`으로 고쳤다.

## 8. FAQ — 코드 생성기를 두 번째로 쓰면서 확인한 것

공지사항 때 정한 10개 검토 기준이 FAQ 생성 결과에도 모두 그대로 나왔다. 기준 밖에서 다섯 가지를 더 찾았다.

1. 검색 조건을 숫자로 비교(`searchCondition == 0`)해서 `?searchCondition=title`만으로 500 오류가 난다.
2. 검색 대상이 컬럼 의미와 상관없이 PK와 두 번째 컬럼으로 정해진다.
3. 채번 Bean 이름이 `egovIdGnrService`로 고정되어 이 프로젝트에서는 기동되지 않는다.
4. 코드성 컬럼(분류)을 자유 입력으로 받는다.
5. 인라인 스크립트 화면이 CSP에 막혀 버튼이 동작하지 않는다.

자세한 비교는 [코드 생성기 기록 3절](egov-codegen/README.md#3-두-번째-생성-자주-묻는-질문faq)에 있다.

## 9. 규칙을 일부러 깨 보고 확인한 것

이전 문서의 학습 과제를 실제로 실행했다. 모두 실행 후 원래대로 되돌렸다.

| 실습 | 결과 |
| :--- | :--- |
| `EgovConfigTransaction`에서 `select*` 읽기 전용 규칙 삭제 | `EgovRuntimeIntegrationTest.transactionRules` 실패 (`Expecting value to be true but was false`) |
| 타 기업 신청서 열람 테스트의 기대값을 403 → 200으로 변경 | `Status expected:<200> but was:<403>` — 서버는 여전히 막고 있다는 뜻 |
| 신청서 공통 잠금 SQL에서 `FOR UPDATE` 삭제 | 동시성 테스트 4건 모두 실패 (`두 번째 요청이 첫 요청의 신청서 행 잠금을 실제로 기다림`) |
| 메시지 번들 키 오타(`faq.created`) | **처음에는 통과했다.** FAQ 컨트롤러가 키를 변수로 넘겨 누락 키 검사가 찾지 못했다. 호출 위치에 키를 적도록 고치고 검사 테스트를 추가한 뒤에는 `[번들 누락 키: faq.created]`로 실패한다 |

마지막 항목처럼 테스트가 통과해도 테스트가 실제로 그 상황을 보고 있는지는 따로 확인해야 한다.

## 10. 검증

- `./mvnw -B -ntp clean verify` — 테스트 144건 성공 (4차 78건 → 66건 추가)
- 브라우저 점검 — 7절
- Pull Request의 GitHub Actions 결과로 리눅스 환경 빌드를 다시 확인한다

## 11. 남은 일

- 기업 회원가입(사업자등록번호 형식·중복 검증, 비밀번호 정책)
- 신청서 임시저장·수정·제출 취소
- 개인정보 조회 기록(누가 누구의 연락처를 열람했는지), 암호화 키 교체 절차
- MariaDB 운영 프로필 + Flyway, MariaDB에서 잠금·동시성 테스트 재확인
- 공고 마감 배치를 eGov 배치(`egovframe-rte-bat-core`)로 옮길지 검토
- Bean Validation 문구와 화면 정적 라벨 외부화
