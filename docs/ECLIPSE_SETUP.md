# Eclipse 개발 환경

이 프로젝트의 기본 IDE는 Eclipse입니다. 빌드는 그대로 Maven Wrapper(`mvnw.cmd`)가 기준이고, Eclipse는 편집·실행·디버깅에 씁니다. VS Code 설정(`.vscode/`, [VS_CODE_SETUP.md](VS_CODE_SETUP.md))도 남겨 두었으니 변경 내역 비교 등에 함께 써도 됩니다.

확인한 환경: Eclipse IDE for Java Developers 2025-12 + Spring Tools, m2e, Lombok 1.18.48 이상.

## 1. 워크스페이스와 가져오기

워크스페이스는 **프로젝트 폴더 바깥**에 둡니다. 프로젝트 폴더 자체를 워크스페이스로 열면 가져오기 화면에서 `pom.xml`이 빨갛게 표시되고 체크되지 않습니다(프로젝트와 워크스페이스 위치가 겹침).

```text
C:\Users\사용자\Desktop\pms        ← 워크스페이스 (Eclipse 설정 .metadata 만 생김)
C:\Users\사용자\Desktop\전자정부   ← 프로젝트 (소스, Git)
```

1. `File → Switch Workspace → Other...` 에서 바깥 폴더 선택
2. `File → Import → Maven → Existing Maven Projects` → Root Directory에 프로젝트 폴더 → Finish
3. 오른쪽 아래 진행 표시가 끝날 때까지 기다림 (의존성 다운로드)

가져오기는 복사가 아니라 참조입니다. Eclipse에서 고친 파일이 곧 Git 저장소의 파일입니다. 프로젝트 폴더에 생기는 `.project`, `.classpath`, `.settings/`, `.factorypath`는 `.gitignore` 대상입니다.

## 2. 처음 한 번 할 설정

| 설정 | 위치 | 이유 |
| :--- | :--- | :--- |
| JDK 17 등록 | `Preferences → Java → Installed JREs → Add → Standard VM` 에 Temurin 17 경로 추가 | 등록하지 않으면 Eclipse 내장 JRE 21로 실행된다. 빌드·CI 기준은 17 |
| 실행 환경 연결 | `Installed JREs → Execution Environments → JavaSE-17` 에서 위 JDK 체크 | 실행 설정이 JavaSE-17을 찾으므로 여기 연결된 JDK가 쓰인다 |
| 파일 인코딩 | `Preferences → General → Workspace → Text file encoding = UTF-8` | 한글 메시지·SQL 파일이 MS949로 깨지지 않게 |
| 자동 새로고침 | 같은 화면의 `Refresh using native hooks or polling` 체크 | Git·터미널·AI 도구가 바꾼 파일을 바로 반영 |
| Quick Diff | `Preferences → General → Editors → Text Editors → Quick Diff` 기준을 `Git Revision` | 편집기 왼쪽에 커밋 이후 바뀐 줄 표시 |

Eclipse 2025-12 는 새 워크스페이스를 처음 열 때 설치된 JDK 를 찾아 자동으로 등록한다 (이 PC 에서는 11·17·20·24). `Installed JREs` 목록에 17 이 이미 있으면 Add 는 건너뛰고 `Execution Environments` 연결만 하면 된다.

Lombok은 Eclipse 설치 폴더의 `eclipse.ini`에 `-javaagent:...\lombok.jar`로 연결되어 있어야 합니다. 없으면 `java -jar lombok.jar`로 설치 화면을 띄워 Eclipse를 지정합니다. 버전은 프로젝트(1.18.48)와 같거나 높게 맞춥니다.

## 3. MySQL 준비 (처음 한 번)

서버와 테스트 모두 MySQL 이 필요하다. PowerShell 에서 프로젝트 폴더로 이동해 실행한다.

```powershell
.\scripts\setup-local-mysql.ps1
```

- MySQL root 비밀번호를 묻는다. 입력값은 화면에 보이지 않고 어디에도 저장되지 않는다.
- `dpms`(개발), `dpms_test`(테스트) DB 와 `dpms_owner`(테이블 변경), `dpms_app`(서버 실행) 계정을 만든다.
- 두 계정의 무작위 비밀번호를 `config/dpms-secret.yml` 에 저장한다 (Git 제외). 서버는 실행 위치 기준 `./config/` 에서 이 파일을 읽으므로, 실행 설정의 작업 폴더가 프로젝트 폴더여야 한다 (공유 실행 설정은 그렇게 되어 있다).
- 비밀번호를 바꾸려면 `-Rotate` 를 붙여 다시 실행한다.

## 4. 실행과 검증

`eclipse/` 폴더의 실행 설정 두 개가 자동으로 `Run`/`Debug` 메뉴에 등록됩니다.

| 실행 설정 | 하는 일 |
| :--- | :--- |
| `D-PMS 로컬 실행` | `DaeguPmsApplication`을 local 프로필, UTF-8, JavaSE-17로 실행. 디버그(`F11`)도 같은 설정 사용 |
| `D-PMS 전체 재빌드 (clean verify)` | Maven `clean verify` (테스트 전체 + 패키징). PR 전에 실행 |

- 테스트 하나만 돌릴 때: 테스트 클래스 우클릭 → `Run As → JUnit Test` (MySQL 이 켜져 있어야 한다. 테스트는 `dpms_test` 를 비우고 다시 만든다)
- 서버는 하나만 띄웁니다. 콘솔의 빨간 정지 버튼으로 끈 뒤 다시 실행합니다.
- 터미널에서도 같은 결과를 냅니다: `.\mvnw.cmd -B -ntp clean verify`

## 5. 변경 내역 보기

- `Window → Show View → Other → Git → Git Staging`: 바뀐 파일 목록, 더블클릭하면 좌우 비교
- 파일 우클릭 → `Compare With → HEAD Revision`: 마지막 커밋과 비교
- 커밋 전에는 Git Staging 화면에서 파일 하나씩 확인한 뒤 Stage → 메시지 작성 → Commit

## 6. 자주 만나는 문제

**`Build path specifies execution environment JavaSE-17. There are no JREs installed in the workspace that are strictly compatible`**
2절의 JDK 17 등록을 하지 않은 경우입니다. 실행은 되지만 JRE 21로 돌아가므로 등록합니다.

**편집기에서 getter·생성자가 없다는 빨간 줄 (Maven 빌드는 성공)**
Lombok이 Eclipse에 연결되지 않은 경우입니다. 2절 Lombok 설정 후 Eclipse를 재시작하고 `Project → Clean`.

**Eclipse에서 실행하면 되는데 Maven 테스트만 실패 (또는 반대)**
Eclipse와 Maven이 같은 `target/classes`에 컴파일 결과를 씁니다. 검증은 항상 `clean verify`로 해서 남은 클래스의 영향을 없앱니다.

**기동 시 `Could not resolve placeholder 'DPMS_DB_PASSWORD'`**
`config/dpms-secret.yml` 이 없거나 실행 위치가 프로젝트 폴더가 아닌 경우다. 3절 스크립트를 실행하고, 직접 만든 실행 설정이라면 `Arguments → Working directory` 를 프로젝트 폴더로 지정한다.

**기동 시 `Validate failed: Migrations have failed validation` (checksum mismatch)**
이미 적용한 `db/migration/V*.sql` 파일을 고친 경우다. 적용한 파일은 되돌리고 새 버전(V3 …)을 추가한다. 개발 DB 를 처음부터 다시 만들고 싶으면 `dpms` 를 지웠다가 다시 만든 뒤(MySQL root 로 `DROP DATABASE dpms; CREATE DATABASE dpms CHARACTER SET utf8mb4;`) 서버를 띄운다. 데모 데이터와 화면에서 입력한 데이터는 모두 지워진다.

**`pom.xml` 수정 후 의존성이 반영되지 않음**
프로젝트 우클릭 → `Maven → Update Project` (`Alt+F5`).
