# VS Code 개발 환경

이 프로젝트는 Java 17과 Maven Wrapper를 사용합니다. Antigravity IDE에서 사용하던 소스와 Git 기록을 같은 폴더에서 이어서 사용할 수 있습니다.

> 6차 스프린트부터 기본 IDE 는 Eclipse 입니다([ECLIPSE_SETUP.md](ECLIPSE_SETUP.md)). 서버와 테스트는 MySQL 이 필요하므로 먼저 `scripts/setup-local-mysql.ps1` 을 한 번 실행합니다(Eclipse 안내 3절).

## 1. Java 버전 구분

| 용도 | 기준 |
| --- | --- |
| 프로젝트 컴파일·서버 실행·테스트·CI | JDK 17 |
| Red Hat Java 확장의 코드 분석 서버 | 설치된 확장이 요구하는 JDK. 1.57.0부터 JDK 25 이상 (현재 1.58.0) |
| Maven | `mvnw.cmd` / `mvnw`가 선택하는 3.9.11 |

코드 분석 서버가 JDK 25로 실행되어도 프로젝트가 Java 25로 변경되는 것은 아닙니다. `pom.xml`의 `java.version=17`과 `release` 설정을 유지합니다.

## 2. 확장과 초기 설정

1. `pom.xml`이 있는 프로젝트 루트 폴더를 VS Code에서 엽니다.
2. 확장 화면에서 이 작업 영역의 추천 확장을 확인합니다. Microsoft Java 확장 팩, Spring Boot 확장 팩, eGovFrame 확장을 사용합니다.
3. **Java by Oracle (`oracle.oracle-java`)가 활성화되어 있다면 해당 확장 톱니바퀴 → `Disable (Workspace)` / `사용 안 함(작업 영역)`을 선택합니다.** Red Hat Java 확장과 두 언어 서버를 함께 실행하지 않도록 합니다. `unwantedRecommendations`는 추천 제외 기능이며 이미 설치된 확장을 비활성화하지는 않습니다.
4. `Developer: Reload Window`를 실행합니다. 기존 터미널은 종료하고 새 터미널을 엽니다.
5. 이전 IDE의 진단이 남아 있을 때만 `Java: Clean Java Language Server Workspace`를 실행하고 재시작합니다. 일반적인 작업 때마다 실행할 필요는 없습니다.

현재 PC의 설정은 `.vscode/settings.json`에 적용되어 있습니다. JDK 절대 경로가 있으므로 이 파일은 Git에서 제외합니다. 다른 PC에서는 `docs/vscode-settings.example.json`을 `.vscode/settings.json`으로 복사하고 다음 경로를 실제 설치 위치로 변경합니다.

- `C:/Java/jdk-17`: 프로젝트 런타임, Maven, 터미널, 테스트 실행기 경로
- `C:/Java/jdk-25`: 코드 분석 서버 경로
- `C:/Users/사용자/.m2/.../lombok-1.18.48.jar`: 본인 Maven 저장소의 Lombok 경로 (한 번 빌드하면 생김, 아래 5절 참고)

확장 업데이트 또는 JDK 설치 경로 변경 후에는 `Java: Configure Java Runtime`에서 Java 17 연결을 다시 확인합니다. VS Code 전체 사용자 설정이나 다른 프로젝트의 JDK는 변경하지 않습니다.

## 3. 실행과 검증

- **F5**: `D-PMS: 로컬 디버깅 (Java 17)` 실행
- **Ctrl+Shift+B**: Maven `verify` 실행(테스트와 패키징)
- **Terminal → Run Task**: `D-PMS: 전체 재빌드 (clean verify)` 또는 `D-PMS: 서버 실행`
- 서버를 중복 실행하지 않습니다. 실행 중인 서버를 종료한 뒤 F5 또는 서버 작업을 시작합니다.

Windows 터미널에서도 같은 명령을 사용할 수 있습니다.

```powershell
java -version
.\mvnw.cmd -v
.\mvnw.cmd -B -ntp verify
.\mvnw.cmd spring-boot:run
```

`java -version`과 `mvnw.cmd -v`에 Java 17이 표시되는지 확인합니다. macOS/Linux에서는 `./mvnw`를 사용합니다. Windows 작업은 로컬 설정의 `terminal.integrated.env.windows.JAVA_HOME`을 참조하므로 예제 설정을 먼저 복사해야 합니다.

## 4. 설정으로 예방하는 문제

- Maven Wrapper 우선 사용: Antigravity 내부 Maven 설치 경로에 의존하지 않습니다.
- Gradle 자동 가져오기 해제: 이전 빌드 산출물과 Maven 프로젝트를 혼동하지 않도록 합니다.
- POM 변경 자동 반영: 의존성 변경 뒤 오래된 클래스 경로가 남는 경우를 줄입니다.
- Lombok은 프로젝트와 같은 버전을 코드 분석 서버에 연결: 확장 내장 Lombok과 버전이 달라 생기는 오탐을 막습니다(5절).
- Java 전용 포매터 지정: 기존 전역 Prettier 설정이 Java 편집을 방해하지 않도록 합니다.
- HTML 저장 시 자동 포맷 해제: 기존 Thymeleaf 템플릿을 저장할 때 전역 Prettier가 의도치 않게 재정렬하지 않도록 합니다. 수동 포맷은 가능합니다.
- 파일·Maven JVM·테스트·디버깅 UTF-8 지정: Windows 기본 MS949와 한글 리소스 간 인코딩 차이를 줄입니다.
- 실행 위치를 프로젝트 루트로 고정: 업로드 상대 경로가 실행 방법에 따라 바뀌지 않도록 합니다.
- `.vscode/tasks.json`, `launch.json`, `extensions.json`은 Git으로 공유하고 PC별 `settings.json`은 제외합니다.

Windows Wrapper의 `.m2` 경로 처리에는 일반 폴더의 `Target` 값이 null인 환경도 지원하도록 방어 코드를 추가했습니다. Maven 버전과 다운로드 SHA-256 검증은 유지합니다.

## 5. 자주 만나는 문제

### `class file version 69.0` 오류로 테스트가 실행되지 않음

```text
ApplicationStatusTest has been compiled by a more recent version of the Java Runtime (class file version 69.0),
this version of the Java Runtime only recognizes class file versions up to 61.0
```

- 의미: `target/` 안의 클래스가 Java 25(69)로 컴파일되어 있는데 Maven은 Java 17(61)로 실행했다.
- 원인: VS Code Java 언어 서버도 Maven과 같은 `target/classes`에 컴파일 결과를 쓴다. 프로젝트를 처음 가져올 때 저장된 컴파일 수준(이 PC에서는 언어 서버 JDK와 같은 25)이 남아 있으면 그 값으로 계속 컴파일한다. Maven은 클래스가 최신이라고 판단해 다시 컴파일하지 않는다.
- 확인: 언어 서버 작업 영역의 프로젝트 설정(`redhat.java/jdt_ws/.metadata/.plugins/org.eclipse.core.resources/.projects/daegu-pms/.settings/org.eclipse.jdt.core.prefs`)에 `compiler.compliance=25`가 있으면 이 경우다.
- 해결: 명령 팔레트에서 `Java: Clean Java Language Server Workspace` → 재시작. 다시 가져오면 `pom.xml`의 `release 17`이 적용된다. 그 전까지는 `clean verify`(작업: `D-PMS: 전체 재빌드`)로 빌드한다.

### Maven 빌드는 성공하는데 편집기에만 Lombok 오류(빨간 줄)가 보임

```text
The Java feature 'Flexible Constructor Bodies' is only available with source level 25 and above
```

`@RequiredArgsConstructor`를 붙인 클래스 선언 줄에 위 오류가 표시된다. Java 17 프로젝트인데 Java 25 문법을 쓴 것처럼 보이지만 코드 문제가 아니다.

- 원인: Red Hat Java 확장은 자체 내장 Lombok(1.58.0 기준 `lombok-1.18.39-4050.jar`)으로 코드를 분석한다. 이 프로젝트는 eGovFrame 5.1.0 부모 POM이 정한 Lombok 1.18.48로 컴파일하므로, 내장본이 만든 생성자를 JDK 25로 실행 중인 분석 서버가 다르게 해석해 편집기에만 오류가 나타난다.
- 해결: 내장 Lombok을 끄고(`java.jdt.ls.lombokSupport.enabled: false`) 프로젝트와 같은 Lombok jar를 `java.jdt.ls.vmargs`의 `-javaagent:`로 지정한다. 예제 설정에 반영되어 있다.
- 확인: 설정 변경 후 `Developer: Reload Window`. 작업 관리자나 `Get-CimInstance Win32_Process`로 `jdt_ws`를 쓰는 java 프로세스 명령줄에 `lombok-1.18.48.jar`가 보이면 적용된 것이다.
- 주의: eGovFrame 부모 POM을 올려 Lombok 버전이 바뀌면 이 경로도 함께 바꾼다.

## 6. 참고한 공식 문서

- [VS Code Java 프로젝트와 런타임 설정](https://code.visualstudio.com/docs/java/java-project)
- [Red Hat Java 확장](https://github.com/redhat-developer/vscode-java)
- [Maven for Java](https://github.com/microsoft/vscode-maven)
- [VS Code 확장 비활성화](https://code.visualstudio.com/docs/configure/extensions/extension-marketplace#_disable-an-extension)
