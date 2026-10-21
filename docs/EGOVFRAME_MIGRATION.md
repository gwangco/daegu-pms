# Spring Boot 단독 구성 → 전자정부 표준프레임워크 5.0 전환 기록

> 작업일: 2026-10-21 · 브랜치: `feature/egovframe5-migration`

## 1. 배경
1·2차 스프린트는 Spring Boot 3.3 위에 eGovFrame 의 **구조와 명명 규칙만 흉내 낸** 상태였다.
eGovFrame 4.x 는 Spring 5.3(javax) 기반이라 Spring Boot 3(Spring 6, jakarta)과 함께 쓸 수 없었기 때문이다.

eGovFrame **5.0** 이 Spring Boot 3.5 / Spring 6.2 / Jakarta EE 기반으로 공개되면서 같은 기술 기반에서
**실제 eGovFrame 실행환경 라이브러리(org.egovframe.rte)**를 쓸 수 있게 되어 전환했다.

| 항목 | 버전 |
| :--- | :--- |
| eGovFrame 실행환경 | 5.0.2 (`egovframe-boot-starter-parent:5.0.2`, `egovframe.rte.version=5.0.2`) |
| Spring Boot / Spring / Security | 3.5.6 / 6.2.11 / 6.5.5 (부모 POM 이 관리) |
| JDK / Maven | 17 / 3.9.11 (Maven Wrapper) |
| eGovFrame VS Code 확장 | eGovFrame VSCode Initializr 5.0.7 |
| eGov 저장소 | `https://maven.egovframe.go.kr/maven/` |

## 2. 전환 전후 비교

| 구분 | 전환 전 (Spring Boot 단독) | 전환 후 (eGovFrame 5.0) |
| :--- | :--- | :--- |
| 빌드 | Gradle, `spring-boot-starter-parent` | Maven, `egovframe-boot-starter-parent` |
| 패키지 | `controller/`, `service/`, `mapper/`, `domain/` | `업무/service`, `업무/service/impl`, `업무/web`, 공통 `cmm` |
| 서비스 구현 | `@Service` 클래스 | `extends EgovAbstractServiceImpl` |
| Mapper | `@org.apache.ibatis.annotations.Mapper` | `@EgovMapper` + `@MapperScan(annotationClass = EgovMapper.class)` |
| MyBatis 설정 | `mybatis-spring-boot-starter` 자동 구성 | `EgovConfigMapper`(Java Config) + `sql-mapper-config.xml` |
| 트랜잭션 | 메서드마다 `@Transactional` | AOP 포인트컷으로 `*Impl` 일괄 적용 (`EgovConfigTransaction`) |
| 예외 처리 | `@ControllerAdvice` | + eGov `ExceptionTransfer` 서비스 예외 후처리 (`EgovConfigAspect`) |
| 업무 예외 | `RuntimeException` | `BaseRuntimeException` (eGov 예외 계층) |
| 설정값 | `@ConfigurationProperties` | + `EgovPropertyService` (`propertiesService.getInt("pageUnit")`) |
| 페이징 | 자체 PaginationInfo | eGov `PaginationInfo` + `AbstractPaginationRenderer` 확장 |
| 번호 채번 | DB AUTO_INCREMENT | + `EgovTableIdGnrServiceImpl` (접수번호, 공지 ID) |
| 로깅 | Logback | Log4j2 |
| 메시지 | `messages.properties` | eGov 방식 `messageSource` (업무 + eGov 번들) |

## 3. 작업 순서 (커밋 단위)
각 단계마다 전체 테스트를 통과시킨 뒤 커밋했다.

1. **build** — Gradle → Maven, eGov 부모 POM, Log4j2, Maven Wrapper, MyBatis Java Config
2. **refactor** — eGov 표준 패키지로 재배치(`git mv` 로 이력 유지), VO 명명 통일
3. **feat** — `EgovAbstractServiceImpl`, `@EgovMapper`, AOP 트랜잭션·예외 후처리, PropertyService, PaginationInfo/Renderer
4. **feat** — ID Generation 접수번호
5. **feat** — VS Code 코드 생성기로 공지사항 ([기록](egov-codegen/README.md))
6. **fix** — `@EgovMapper` 폐기 예정 경고 제거
7. **ci** — GitHub Actions, PR 템플릿

## 4. 전환 중 발견한 문제와 해결 (트러블슈팅)

| # | 문제 | 원인 | 해결 |
| :-: | :--- | :--- | :--- |
| 1 | Logback·Log4j2 충돌 위험 | `egovframe-rte-ptl-mvc` 가 Log4j2 를 함께 가져옴 | 공유 `spring-boot-starter` 에서 `spring-boot-starter-logging` 한 번 제외 → 전역 제외 |
| 2 | 2010년 개발 종료된 iBATIS 2 포함 | `psl-dataaccess` 의 전이 의존성 | `ibatis-sqlmap` 제외 (불필요한 라이브러리 = 공격 표면) |
| 3 | 조회수 UPDATE 가 읽기 전용 트랜잭션에서 실행될 수 있음 | eGov 트랜잭션은 **메서드 이름**으로 규칙 결정 (`select*` = readOnly). H2 는 통과하지만 MariaDB 는 쓰기 거부 가능 | `updateViewCount` 로 분리 (eGov 게시판 공통컴포넌트와 같은 구성) |
| 4 | "잔액 초과" 같은 안내까지 ERROR 로그 | eGov `ExceptionTransfer` 는 모든 RuntimeException 을 ERROR 로 기록 | `@AfterThrowing` Aspect 에서 업무·권한·데이터없음 예외는 후처리 제외 |
| 5 | 엑셀 다운로드 시 `UndeclaredThrowableException` 가능성 | `ExceptionTransfer` 가 체크 예외를 eGov `BaseException`(체크 예외)으로 바꿔 던짐 | 서비스에서 `UncheckedIOException` 으로 감쌈 |
| 6 | 검색 결과 0건일 때 "1 / 0 페이지" | eGov `PaginationInfo` 는 0건이면 전체 페이지 수 0 | 렌더러·화면에서 0건 처리 |
| 7 | 페이지 라벨이 동시 요청 사이에 섞일 수 있음 | 싱글톤 렌더러에서 공유 필드를 요청마다 변경하는 구조 | 라벨을 지역 변수로 만들어 `doRender(...)` 에 전달 |
| 8 | 기동 시 `convention-based @Component names` 폐기 경고 | `@EgovMapper("이름")` 의 value 를 Bean 이름으로 쓰는 방식이 Spring 6.1 부터 폐기 예정 | 이름이 기본값과 같으므로 `@EgovMapper` 만 사용 |
| 9 | `jakarta.persistence.spi::No valid providers found` 경고 | eGov 모듈이 가져온 JPA API 를 Hibernate Validator 가 감지 | JPA 미사용 → `jakarta.persistence-api` 제외 |
| 10 | 코드 생성기 결과 컴파일 실패·보안 결함 | 생성기는 범용 뼈대 | 10개 항목 검토·보완 ([기록](egov-codegen/README.md)) |
| 11 | 접수번호 결번 | IdGnr 은 별도 트랜잭션(REQUIRES_NEW)으로 번호 확정 → 신청이 롤백돼도 번호 소진 | 블록 크기 1 로 재시작 결번 최소화, 결번 가능성은 문서화 |

## 5. 검증
- 단위·통합 테스트 58건 (`./mvnw verify`), 그중 eGov 실행환경 검증
  (`EgovRuntimeIntegrationTest`): ServiceImpl 의 eGov 상속·AOP 프록시, `@EgovMapper` Bean 등록,
  PropertyService 값, 메시지 번들, **트랜잭션 규칙**(`select*` 읽기 전용·체크 예외 롤백·`export*` 비트랜잭션), 페이지 렌더러 출력
- 실제 서버 기동 후 역할별 전 화면 응답 확인, 업무 전체 흐름(신청→심사→채점→선정→정산 승인)과 공지 CRUD 수동 확인
- 기동 로그 ERROR 0건

## 6. 참고 자료
- 전자정부 표준프레임워크 포털: https://www.egovframe.go.kr
- eGovFrame GitHub: https://github.com/eGovFramework (egovframe-boot-sample-java-config, egovframe-template-simple-backend, egovframe-vscode-initializr)
