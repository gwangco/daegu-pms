# 공통컴포넌트 로그인·권한과 비교

공통컴포넌트의 로그인(`uat`)과 권한(`sec`)은 가져오지 않고 Spring Security 로 직접 구성했다. 왜 그렇게 했는지, 실무에서 공통컴포넌트 방식을 따라야 한다면 어떻게 옮길지 정리했다. 확인한 원본은 egovframe-common-components v5.0.7 이다.

## 같은 점

실패 5회 잠금, CSRF 검사, 로그인할 때 세션 ID 교체는 둘 다 한다. 방식만 다르다.

## 다른 점

| | 공통컴포넌트 | D-PMS |
| :--- | :--- | :--- |
| 회원 저장 | 일반, 기업, 업무 사용자 테이블 3개. 로그인 SQL 이 회원 유형으로 테이블을 고른다 | `users` 하나와 `role` 컬럼 |
| 비밀번호 | 아이디를 솔트로 한 SHA-256 값을 SQL 에서 바로 비교 | BCrypt. 저장할 때마다 솔트가 달라서 애플리케이션에서 비교 |
| 권한 규칙 | DB(롤 테이블의 URL 패턴). 관리 화면에서 바꾼다 | 코드(`SecurityConfig`, `@PreAuthorize`, 서비스 검사) |
| 비밀번호 유효기간 | 180일 | 없음 |
| 개인정보 조회 기록 | 반환값에 개인정보 필드가 있으면 AOP 가 자동으로 남김 | 컨트롤러가 직접 남김. 본인 열람은 제외 |

## 직접 구성한 이유

- 역할 4개의 컬럼 차이가 작다. 테이블을 나누면 로그인, 잠금, 접속 기록, 사용자 관리 SQL 이 모두 세 갈래가 된다.
- 비밀번호는 BCrypt 로 저장하고 싶었다. 대신 SQL 로는 비교할 수 없다.
- 권한 대부분이 "자기 신청서만", "배정된 과제만" 같은 데이터 기준이라 URL 패턴으로는 표현할 수 없다. 어차피 서비스에서 검사해야 한다. URL 규칙을 DB 에 두면 재배포 없이 바꿀 수 있지만, 코드 리뷰와 테스트 밖에서 권한이 바뀐다. 역할이 4개로 고정이라 코드에 두고 테스트로 지키는 쪽을 택했다.
- 잠금, 접속 기록, 연락처 암호화, 가입 승인은 이미 만들고 테스트까지 마친 상태였다.

## 공통컴포넌트 방식을 따라야 한다면

발주처 표준이 공통컴포넌트 로그인이라면 이 순서로 옮긴다. 검토만 했고 구현하지는 않았다.

1. 회원을 옮긴다. 담당자, 평가위원, 관리자는 업무 사용자 테이블로, 기업회원은 기업회원 테이블로 옮긴다.
2. BCrypt 값은 원본 방식으로 바꿀 수 없다. 첫 로그인 때 비밀번호를 다시 정하게 하거나, 로그인 서비스가 `PasswordEncoder` 로 비교하도록 고친다.
3. `SecurityConfig` 의 URL 규칙을 롤 테이블로 옮긴다. 서비스의 데이터 기준 검사는 그대로 둔다.
4. 잠금 컬럼을 원본 컬럼으로 옮기고, 개인정보 기록은 원본 AOP 와 지금 방식 중 하나만 쓴다. 둘 다 쓰면 같은 조회가 두 번 남는다.
5. 지금의 인가, 잠금, CSRF 테스트를 그대로 돌려 결과가 같은지 본다.

## 확인한 원본 파일

- `uat/uia/web/EgovLoginController.java`, `uat/uia/service/impl/EgovLoginServiceImpl.java`
- `mapper/com/uat/uia/EgovLoginUsr_SQL_mysql.xml`
- `egovProps/globals.properties`
- `sec/security/common/EgovSessionMapping.java`, `sec/security/filter/EgovCsrfSecurityConfig.java`
- `mapper/com/sec/rmt/EgovRoleManage_SQL_mysql.xml`
