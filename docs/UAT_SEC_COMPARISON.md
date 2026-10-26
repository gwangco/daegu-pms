# 공통컴포넌트 로그인·권한(uat, sec)과 D-PMS 비교

확인한 원본: [egovframe-common-components](https://github.com/eGovFramework/egovframe-common-components) `v5.0.7` (2026-10-26 확인, 파일 목록은 4절)

## 한눈에 보기

- D-PMS 는 공통컴포넌트의 로그인(`uat`)과 권한(`sec`)을 가져오지 않았다. 대신 Spring Security 로 직접 구성했다.
- 실패 잠금 5회, CSRF 검사, 로그인할 때 세션 ID 교체처럼 같은 일을 다른 방식으로 하는 곳이 많다. 크게 다른 곳은 세 군데다. 회원을 어떻게 저장하는지(테이블 3개와 1개), 비밀번호를 어떻게 비교하는지(SQL 비교와 BCrypt), 권한 규칙을 어디에 두는지(DB 와 코드)다.
- 공통컴포넌트 방식으로 바꾸려면 인증 전체와 회원 데이터를 옮겨야 한다. 이 프로젝트 범위에서는 바꿔서 얻는 것이 적다고 판단했다. 그래도 실무에서 따라야 할 때의 절차는 3절에 정리했다.

## 1. 항목별 비교

| 항목 | 공통컴포넌트 v5.0.7 | D-PMS |
| :--- | :--- | :--- |
| 인증 방식 선택 | `globals.properties` 의 `Globals.Auth` 로 dummy, session, security 중 하나를 고른다(기본 security). 방식마다 `egovUserDetailsHelper` 구현이 다르다 (`context-egovuserdetailshelper.xml`) | Spring Security 폼 로그인 하나 |
| 로그인 처리 | `EgovLoginController.actionLogin` (`/uat/uia/actionLogin.do`) → `EgovLoginServiceImpl.actionLogin` | `/login-process` + `CustomUserDetailsService`, 성공·실패 핸들러 |
| 회원 저장 | 회원 유형(`userSe`)마다 테이블이 다르다. 일반 `COMTNGNRLMBER`(GNR), 기업 `COMTNENTRPRSMBER`(ENT), 업무 `COMTNEMPLYRINFO`(USR). 로그인 SQL 이 유형 값으로 테이블을 고른다 | `users` 하나와 `role` 컬럼(기업·평가위원·담당자·총괄관리자). 사업자등록번호 중복은 기업회원끼리만 막는다(생성 컬럼 + UNIQUE) |
| 비밀번호 비교 | `EgovFileScrty.encryptPassword(비밀번호, 아이디)` 로 만든 값을 SQL 에서 `password = #{password}` 로 비교한다 | BCrypt(`{bcrypt}`). 저장값마다 솔트가 달라 SQL 로 비교할 수 없고 `PasswordEncoder.matches` 로 비교한다 |
| 로그인 뒤 사용자 정보 | 세션 속성 `loginVO`. 보안 모드에서는 `EgovSessionMapping` 이 DB 행을 `EgovUserDetails` 와 `LoginVO` 로 만든다 | SecurityContext 의 인증 정보. 서비스는 `SecurityUtil` 로 아이디와 역할만 읽는다 |
| 실패 잠금 | `Globals.login.Lock = true`, `Globals.login.LockCount = 5`. 회원 테이블마다 `LOCK_AT`, `LOCK_CNT`, `LOCK_LAST_PNTTM` 이 있고, 결과 코드는 L(잠금), C(실패 횟수 증가), E(정상, 횟수 초기화)다 | `login_fail_count`, `locked_at`. 5회 실패하면 잠그고, 설정한 시간(30분)이 지나면 풀린다. 총괄관리자도 풀 수 있다. 시도마다 `login_history` 에 남긴다 |
| 비밀번호 유효기간 | `Globals.ExpirePwdDay = 180`, 만료 안내 화면(`noticeExpirePwd.do`) | 없음 (남은 일) |
| 세션 고정 공격 방지 | 로그인에 성공했을 때 세션이 이미 있으면 `request.changeSessionId()` | Spring Security 기본값(로그인할 때 세션 ID 교체) |
| CSRF | `EgovCsrfSecurityConfig`, `EgovCsrfControllerAdvice` | Spring Security CSRF. 모든 변경 요청을 검사하고 테스트로 확인 |
| 권한 규칙 위치 | DB 다. 롤 `COMTNROLEINFO` 에 URL 패턴(`ROLE_PTTRN`)과 유형(`ROLE_TY`, 공통코드 COM029)을 두고, 권한·권한그룹·그룹·부서권한(`sec.ram`, `rgm`, `gmt`, `drm`)을 관리 화면에서 바꾼다 | 코드다. `SecurityConfig` 의 URL 규칙, 컨트롤러 `@PreAuthorize`, 서비스의 데이터 기준 검사("이 신청서의 기업인가", "배정된 평가위원인가") |
| 개인정보 조회 기록 | `EgovPrivacyLogAspect`(`sym.log.plg`, `context-privacylogaop.xml`). `egovframework.com..impl.*Impl.*` 메서드의 반환값에 회원명·휴대폰·이메일 같은 필드가 있으면 AOP 가 남긴다 | 컨트롤러가 직접 남긴다. 대상·정보주체·건수를 적고 본인 열람은 남기지 않는다 ([11차 1부](SPRINT11_PRIVACY_ACCESS_LOG.md)) |

## 2. D-PMS 가 직접 구성한 이유

**① 회원 구조가 단순하다.** D-PMS 의 역할 4개는 컬럼 차이가 작다(기업만 회사명·사업자등록번호를 쓴다). 테이블을 나누면 로그인·잠금·접속기록·사용자 관리 SQL 이 유형마다 세 갈래로 나뉜다. 로그인 화면에서도 회원 유형을 먼저 골라야 한다.

**② 비밀번호를 SQL 로 비교하지 않는다.** 원본은 같은 아이디와 비밀번호면 늘 같은 값이 나오는 방식이라 SQL 에서 바로 비교할 수 있다. BCrypt 는 저장할 때마다 솔트를 새로 만들고 반복 비용을 조절할 수 있다. 대신 비교는 애플리케이션이 한다.

**③ 권한의 대부분이 데이터 기준이다.** "기업은 자기 신청서만, 평가위원은 배정된 과제만" 같은 규칙은 URL 패턴으로 표현할 수 없어서 어차피 서비스 계층에서 검사해야 한다. URL 규칙을 DB 에 두면 운영 중 재배포 없이 바꿀 수 있다는 장점이 있다. 하지만 코드 리뷰와 테스트 밖에서 권한이 바뀐다. D-PMS 는 역할이 4개로 고정이라 코드에 두고 테스트(`SecurityIntegrationTest`, `ServiceAuthorizationIntegrationTest`)로 지키는 쪽을 골랐다.

**④ 이미 구현과 테스트가 있다.** 잠금과 해제, 접속기록, 연락처 암호화, 가입 승인 대기는 5·9차에 만들고 테스트했다. 공통컴포넌트로 바꾸면 이것들을 원본 구조에 맞춰 다시 만들어야 한다.

## 3. 실무에서 공통컴포넌트 방식을 따라야 한다면

발주처 표준이 공통컴포넌트 로그인·권한이라면 다음 순서로 옮긴다 (검토만 했고 구현하지 않았다).

1. **회원 이전**: 담당자·평가위원·총괄관리자는 업무사용자(`COMTNEMPLYRINFO`), 기업회원은 기업회원(`COMTNENTRPRSMBER`)으로 옮긴다. 승인 대기·동의 일시처럼 원본에 없는 컬럼은 따로 둔다.
2. **비밀번호**: BCrypt 값은 원본 방식으로 바꿀 수 없다(단방향). 첫 로그인 때 비밀번호를 다시 정하게 하거나, 로그인 서비스가 `PasswordEncoder` 로 비교하도록 고친다.
3. **권한**: `SecurityConfig` 의 URL 규칙을 롤(`COMTNROLEINFO` 의 URL 패턴)로 옮긴다. 서비스 계층의 데이터 기준 검사는 그대로 둔다.
4. **잠금·기록**: `login_fail_count`·`locked_at` 을 `LOCK_CNT`·`LOCK_AT` 으로 옮긴다. 시간이 지나면 자동으로 푸는 규칙은 원본에 맞출지 정한다. 개인정보 처리 기록은 원본 AOP 와 D-PMS 의 명시적 기록 중 하나로 정한다. 둘을 같이 쓰면 같은 조회가 두 번 남는다.
5. **회귀 테스트**: 지금의 인가·잠금·CSRF 테스트를 그대로 돌려 같은 결과가 나오는지 확인한다.

## 4. 확인한 원본 파일

- `src/main/java/egovframework/com/uat/uia/web/EgovLoginController.java`
- `src/main/java/egovframework/com/uat/uia/service/impl/EgovLoginServiceImpl.java`
- `src/main/resources/egovframework/mapper/com/uat/uia/EgovLoginUsr_SQL_mysql.xml`
- `src/main/resources/egovframework/egovProps/globals.properties`
- `src/main/resources/egovframework/spring/com/context-egovuserdetailshelper.xml`, `context-privacylogaop.xml`
- `src/main/java/egovframework/com/sec/security/common/EgovSessionMapping.java`, `filter/EgovCsrfSecurityConfig.java`
- `src/main/resources/egovframework/mapper/com/sec/rmt/EgovRoleManage_SQL_mysql.xml`
