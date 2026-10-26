# Eclipse 설정

기본 IDE 는 Eclipse 다. 빌드 기준은 Maven Wrapper(`mvnw.cmd`)이고 Eclipse 는 편집, 실행, 디버깅에 쓴다. VS Code 설정도 남겨 두었다([VS_CODE_SETUP.md](VS_CODE_SETUP.md)).

확인한 환경: Eclipse IDE for Java Developers 2025-12, Spring Tools, m2e, Lombok 1.18.48 이상

## 가져오기

워크스페이스는 프로젝트 폴더 밖에 둔다. 프로젝트 폴더를 워크스페이스로 열면 가져오기 화면에서 `pom.xml` 이 체크되지 않는다.

```text
C:\Users\사용자\Desktop\pms        워크스페이스 (.metadata 만 생김)
C:\Users\사용자\Desktop\전자정부   프로젝트 (소스, Git)
```

1. `File > Switch Workspace > Other...` 에서 바깥 폴더를 고른다
2. `File > Import > Maven > Existing Maven Projects` 에서 프로젝트 폴더를 고르고 Finish
3. 오른쪽 아래 진행 표시가 끝날 때까지 기다린다 (의존성 다운로드)

가져오기는 복사가 아니라 참조라서 Eclipse 에서 고친 파일이 곧 Git 저장소의 파일이다.

## 처음 한 번 할 설정

- JDK 17: `Preferences > Java > Installed JREs` 에 Temurin 17 이 없으면 추가하고, `Execution Environments > JavaSE-17` 에 연결한다. 연결하지 않으면 Eclipse 내장 JRE 로 실행된다.
- 인코딩: `Preferences > General > Workspace` 에서 Text file encoding 을 UTF-8 로 한다. 한글 메시지와 SQL 이 깨지지 않는다.
- 자동 새로고침: 같은 화면에서 `Refresh using native hooks or polling` 을 체크한다. 밖에서 바뀐 파일을 바로 반영한다.
- Lombok: `eclipse.ini` 에 `-javaagent:...\lombok.jar` 가 있어야 한다. 없으면 `java -jar lombok.jar` 로 설치한다.

## MySQL 준비 (처음 한 번)

PowerShell 에서 프로젝트 폴더로 가서 실행한다.

```powershell
.\scripts\setup-local-mysql.ps1
```

MySQL root 비밀번호를 물어본다(저장하지 않는다). `dpms`, `dpms_test` DB 와 계정 두 개를 만들고, 무작위 비밀번호를 `config/dpms-secret.yml`(Git 제외)에 저장한다. 서버는 실행 위치의 `./config/` 에서 이 파일을 읽기 때문에 작업 폴더가 프로젝트 폴더여야 한다. 비밀번호를 바꾸려면 `-Rotate` 를 붙여 다시 실행한다.

## 실행

`eclipse/` 폴더의 실행 설정 두 개가 Run, Debug 메뉴에 자동으로 나온다.

- `D-PMS 로컬 실행`: local 프로필로 서버 실행. 디버그도 같은 설정을 쓴다
- `D-PMS 전체 재빌드 (clean verify)`: 테스트 전체와 패키징. PR 전에 돌린다

테스트 하나만 돌릴 때는 테스트 클래스를 우클릭해서 `Run As > JUnit Test`. MySQL 이 켜져 있어야 한다.

## 자주 만나는 문제

- `Build path specifies execution environment JavaSE-17...`: JDK 17 을 연결하지 않은 경우다.
- 편집기에 getter 가 없다는 빨간 줄(Maven 빌드는 성공): Lombok 이 연결되지 않았다. 설정 후 재시작하고 `Project > Clean`.
- Eclipse 에서는 되는데 Maven 테스트만 실패: 둘이 같은 `target/classes` 를 쓴다. 검증은 항상 `clean verify` 로 한다.
- `Could not resolve placeholder 'DPMS_DB_PASSWORD'`: `config/dpms-secret.yml` 이 없거나 작업 폴더가 프로젝트 폴더가 아니다.
- `Migrations have failed validation`: 이미 적용한 `V*.sql` 을 고친 경우다. 되돌리고 새 버전 파일을 추가한다.
- `pom.xml` 을 고쳤는데 반영되지 않음: 프로젝트 우클릭 > `Maven > Update Project` (Alt+F5).
