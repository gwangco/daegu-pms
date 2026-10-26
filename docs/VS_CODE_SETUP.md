# VS Code 설정

6차부터 기본 IDE 는 Eclipse 다([ECLIPSE_SETUP.md](ECLIPSE_SETUP.md)). VS Code 를 쓸 때를 위해 설정을 남겨 둔다. 서버와 테스트는 MySQL 이 필요하니 먼저 `scripts/setup-local-mysql.ps1` 을 한 번 실행한다.

## Java 버전

- 컴파일, 서버 실행, 테스트, CI: JDK 17
- Red Hat Java 확장의 코드 분석 서버: 확장이 요구하는 JDK (1.57.0 부터 25 이상)
- Maven: Wrapper(`mvnw.cmd`)가 고르는 3.9.11

분석 서버가 JDK 25 로 돌아도 프로젝트가 Java 25 가 되는 것은 아니다. `pom.xml` 의 `java.version=17` 은 그대로다.

## 처음 설정

1. `pom.xml` 이 있는 폴더를 연다.
2. 추천 확장(Java 확장 팩, Spring Boot 확장 팩, eGovFrame 확장)을 설치한다.
3. Java by Oracle 확장이 켜져 있으면 이 작업 영역에서 끈다. Red Hat 확장과 언어 서버 두 개가 같이 돌면 안 된다.
4. `Developer: Reload Window` 를 실행한다.

`.vscode/settings.json` 은 JDK 절대 경로가 들어 있어서 Git 에서 뺐다. 다른 PC 에서는 `docs/vscode-settings.example.json` 을 복사한 뒤 JDK 17, JDK 25, Lombok jar 경로를 바꾼다.

## 실행

- F5: 로컬 디버깅 (Java 17)
- Ctrl+Shift+B: Maven `verify`
- Terminal > Run Task: 전체 재빌드(clean verify), 서버 실행

터미널에서도 같다.

```powershell
.\mvnw.cmd -B -ntp verify
.\mvnw.cmd spring-boot:run
```

## 자주 만나는 문제

### `class file version 69.0` 으로 테스트가 돌지 않는다

`target/` 의 클래스가 Java 25 로 컴파일되어 있는데 Maven 은 Java 17 로 실행한 경우다. VS Code 언어 서버도 Maven 과 같은 `target/classes` 에 컴파일하는데, 처음 가져올 때 저장된 컴파일 수준(25)이 남아 있으면 계속 그 값으로 컴파일한다. `Java: Clean Java Language Server Workspace` 를 실행하고 다시 시작하면 `pom.xml` 의 `release 17` 이 적용된다.

### Maven 빌드는 되는데 편집기에만 Lombok 오류가 보인다

확장에 들어 있는 Lombok 과 프로젝트 Lombok(1.18.48)의 버전이 달라서 생기는 오탐이다. 내장 Lombok 을 끄고(`java.jdt.ls.lombokSupport.enabled: false`) 프로젝트와 같은 Lombok jar 를 `java.jdt.ls.vmargs` 의 `-javaagent:` 로 지정한다. 예제 설정에 들어 있다.
