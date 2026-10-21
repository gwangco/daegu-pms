# 변경 이력 (CHANGELOG)

형식: [Keep a Changelog](https://keepachangelog.com/ko/1.1.0/) 를 참고해 스프린트 단위로 기록한다.

---

## [0.3.0] 3차 스프린트 — 전자정부 표준프레임워크 5.0 전환 (2026-10-21)
브랜치: `feature/egovframe5-migration` · 상세: [docs/EGOVFRAME_MIGRATION.md](docs/EGOVFRAME_MIGRATION.md)

### 변경 (Changed)
- 빌드: Gradle → **Maven** + `org.egovframe.boot:egovframe-boot-starter-parent:5.0.2` (Spring Boot 3.3.5 → 3.5.6)
- 패키지: 계층별 구조 → **eGov 표준 업무별 구조** (`업무/service`, `service/impl`, `web`), VO 명명 통일(`XxxVO`)
- 서비스: 모든 ServiceImpl 이 `EgovAbstractServiceImpl` 상속, Mapper 는 `@EgovMapper`
- 트랜잭션: `@Transactional` → eGov 방식 **AOP 선언적 트랜잭션**(포인트컷 일괄 적용)
- 페이징: 자체 구현 → eGov `PaginationInfo` + `AbstractPaginationRenderer` 확장 렌더러
- 설정값: 페이징 값을 `EgovPropertyService` 로 조회
- 로깅: Logback → **Log4j2** (eGov 실행환경 표준)
- MyBatis 설정: 자동 구성 → eGov 방식 Java Config + `sql-mapper-config.xml`, SQL 파일 `egovframework/sqlmap/mappers/*_SQL.xml`

### 추가 (Added)
- eGov **예외 후처리**(`ExceptionTransfer`) — 업무 예외는 장애 로그에서 제외하도록 커스터마이징
- eGov **ID Generation** — 신청 접수번호 `DPMS-00000001`, 공지 ID `NTC_0000000001`
- **공지사항** 게시판 — eGovFrame VS Code 확장의 CRUD 코드 생성기로 뼈대 생성 후 10개 항목 보완 ([기록](docs/egov-codegen/README.md))
- GitHub Actions CI (PR 마다 `./mvnw verify`), PR 템플릿, Maven Wrapper(SHA-256 검증)
- 테스트 18건 추가 (총 58건): eGov 실행환경 구성, 페이지 렌더러, 접수번호, 공지사항

### 수정 (Fixed)
- `select*` 이름의 메서드 안에서 조회수를 UPDATE 하던 문제 (eGov 트랜잭션 규칙상 읽기 전용) → `updateViewCount` 분리
- 엑셀 생성의 체크 예외가 AOP 후처리에서 선언되지 않은 예외로 바뀌던 문제 → 런타임 예외로 감쌈
- `@EgovMapper("이름")` 의 Spring 폐기 예정 경고 제거
- 미사용 라이브러리(iBATIS 2, JPA API, commons-logging) 제외

---

## [0.2.0] 2차 스프린트 — 보안 강화 및 핵심 기능 구현 (2026-10-21)
브랜치: `feature/sprint2-security-evaluation-paging`

### 수정 (Fixed) — 1차 뼈대의 결함
- 공고 등록 화면 템플릿 누락으로 인한 500 오류
- Thymeleaf 예약어 `${application}` 사용으로 신청서 상세·정산 화면 값이 비어 보이던 문제
- JDK 17 + 한글 Windows(MS949)에서 소스·`data.sql` 한글 깨짐 → UTF-8 명시
- 평가 LEFT JOIN 으로 평가위원 2명 이상일 때 상세 조회 오류(TooManyResultsException)
- seed 데이터 PK 직접 지정으로 인한 신규 등록 PK 충돌 가능성
- `X-Frame-Options` 중복 헤더, 불필요한 `/api/**` CSRF 예외

### 보안 (Security)
- 비밀번호 `{noop}` 평문 → `{bcrypt}` 해시
- 서비스 계층 소유자 검사로 신청서·정산·첨부파일 **IDOR 차단**
- `@InitBinder` 대량 할당 차단, 파일 업로드 화이트리스트 + 시그니처 검사, 경로 조작 차단
- H2 콘솔 local 프로필 + 관리자 전용, 오류 화면 내부 정보 비노출

### 추가 (Added)
- 서비스 인터페이스/Impl 분리, 신청서 상태머신, 평가위원 배정·채점
- 사업비 정산 예산 초과 검증(`SELECT … FOR UPDATE`), 반려 사유 필수
- 파일 업로드/다운로드, 표준 페이징, 공고 자동 마감 배치, 공통코드 select, 차트 DB 집계
- 테스트 40건

---

## [0.1.0] 1차 — 프로젝트 뼈대 (2026-10-20)
- Spring Boot 3.3.5 + MyBatis + Spring Security + Thymeleaf 기반 초기 구성
- 공고·신청·정산 기본 화면, 4단계 권한, Apache POI 엑셀, H2 스키마·샘플 데이터
