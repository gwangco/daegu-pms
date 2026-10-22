# D-PMS — 대구 지원사업 통합관리·e-정산 시스템

전자정부 표준프레임워크(eGovFrame) 5.1 실행환경으로 만든 공공 SI 포트폴리오 프로젝트입니다.
지원사업 공고부터 온라인 신청, 서류 검토, 평가위원 채점, 최종 선정, 사업비 집행·증빙 정산까지 한 시스템에서 처리합니다.

[![CI](https://github.com/gwangco/daegu-pms/actions/workflows/ci.yml/badge.svg)](https://github.com/gwangco/daegu-pms/actions/workflows/ci.yml)
[![eGovFrame](https://img.shields.io/badge/eGovFrame-5.1.0-134F8C)](https://www.egovframe.go.kr)
[![Java](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.16-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![MyBatis](https://img.shields.io/badge/MyBatis-3.5.19-C70D2C)](https://mybatis.org/mybatis-3/)
[![Tests](https://img.shields.io/badge/Tests-144%20passed-brightgreen)](#5-실행과-테스트)

> 취업 포트폴리오용 데모입니다. 기관·사업·기업명은 설명을 위한 가상의 예시이며 실제 기관과 관련이 없습니다.

| 메인 — 역할별 처리할 일 (사업담당자) | 사업비 e-정산 (기업회원) |
| :---: | :---: |
| ![메인 화면](docs/images/main-manager.png) | ![정산 화면](docs/images/settlement-company.png) |
| **자주 묻는 질문 — eGov 코드 생성기로 생성 후 보완** | **사용자 관리 — 연락처·이메일 암호화 저장 후 서버 마스킹** |
| ![FAQ 화면](docs/images/faq-manager.png) | ![사용자 관리 화면](docs/images/admin-users.png) |

---

## 1. eGovFrame을 어디에 썼나

| 구성요소 | 이 프로젝트에서 한 일 | 화면·동작으로 보이는 결과 | 코드 |
| :--- | :--- | :--- | :--- |
| 부모 POM `egovframe-boot-starter-parent` | Spring Boot·Spring Security·eGov 모듈 버전을 한 곳에서 관리 | 5.0.2 → 5.1.0 업그레이드를 부모 버전 변경과 회귀 테스트로 끝냄 | `pom.xml` |
| `EgovAbstractServiceImpl` | 모든 ServiceImpl 이 상속 | — | `*/service/impl` |
| `@EgovMapper` + MyBatis | Mapper 인터페이스 스캔, SQL 은 `*_SQL.xml` | — | `config/EgovConfigMapper` |
| AOP 선언적 트랜잭션 | `@Transactional` 없이 메서드 이름으로 규칙 적용 (`select*` 읽기 전용, 체크 예외 롤백) | 저장 중 오류가 나면 같은 요청의 변경이 모두 취소됨 | `config/EgovConfigTransaction` |
| 예외 후처리 `ExceptionTransfer` | 장애 로그 후처리, 업무 안내 예외는 제외하도록 조정 | "잔액 초과" 같은 안내는 장애 로그로 남지 않음 | `config/EgovConfigAspect` |
| `EgovPropertyService` | 페이징 설정값(pageUnit·pageSize) | 모든 목록 10건씩 | `config/EgovConfigProperties` |
| ID Generation | 테이블 채번 | 접수번호 `DPMS-00000001`, 공지 `NTC_…`, FAQ `FAQ_…` | `config/EgovConfigIdGeneration` |
| `PaginationInfo` + 렌더러 | 페이지 계산 + Bootstrap 렌더러(Thymeleaf Dialect) | 목록 하단 페이지 번호 바 | `cmm/pagination` |
| 암호화 서비스 (ARIA) | 연락처·이메일 암호화 저장, 키는 환경변수 | DB 에는 `{ARIA}…`, 관리자 화면에는 가린 값 | `cmm/crypto` |
| `MessageSource` | UTF-8 업무 메시지 번들 + `AppMessages` | 모든 안내 문구, 누락 키는 테스트에서 실패 | `egovframework/message` |
| VS Code 확장 코드 생성기 | 공지사항·FAQ CRUD 를 DDL 로 생성한 뒤 보안·업무 규칙 보완 | 원본과 보완본, 검토 항목 15개 기록 | [`docs/egov-codegen`](docs/egov-codegen/README.md) |
| Log4j2 | 실행환경 표준 로깅, 업무·Mapper 기본 INFO | SQL 바인딩 값이 기본 로그에 남지 않음 | `log4j2-spring.xml` |

Spring Boot 단독 구성에서 eGovFrame으로 옮긴 과정은 [전환 기록](docs/EGOVFRAME_MIGRATION.md)에 있습니다.

## 2. 업무 흐름

```mermaid
flowchart LR
    A[담당자: 공고 등록] --> B[기업: 사업계획서 제출<br/>접수번호 발급]
    B --> C[담당자: 서류 검토]
    C --> D[담당자: 평가위원 배정]
    D --> E[평가위원: 정량40 + 정성60 채점]
    E --> F[담당자: 최종 선정]
    F --> G[기업: 비목별 집행 + 증빙 첨부]
    G --> H[담당자: 검수]
    H -- 반려 --> I[기업: 보완 후 재요청]
    I --> H
```

- 신청서는 허용된 다음 상태로만 바뀝니다(`ApplicationStatus`). 배정된 평가위원이 모두 채점해야 최종 선정할 수 있습니다.
- 로그인하면 메인 화면에 역할별 처리할 일(서류 검토 대기, 채점 대기, 정산 검수 대기 등)이 건수와 함께 나옵니다.
- 공고 접수 마감은 매일 자정 배치가 처리합니다.

## 3. 주요 기능

| 영역 | 내용 |
| :--- | :--- |
| 인증·계정 | 4개 역할, BCrypt, 5회 연속 실패 시 30분 잠금, 접속 기록, 관리자 잠금 해제 |
| 공지·FAQ | eGov 코드 생성기 기반 CRUD, 중요 공지 고정, FAQ 분류 탭·검색, 논리 삭제 |
| 공고·신청 | 공통코드 분야 필터, 접수기간·최대지원금·중복신청 검증, 접수번호 채번 |
| 심사·평가 | 평가위원 배정, 전용 채점 화면, 제출 후 수정 불가 |
| e-정산 | 비목별 집행·증빙, 부가세 서버 계산, 잔액 초과 차단, 반려 건 보완 재요청과 처리 이력, 담당자 검수함 |
| 개인정보 | 연락처·이메일 ARIA 암호화, 목록·평가위원 화면 마스킹, 본인 정보 화면 |
| 공통 | 수정자·수정일시 자동 기록(MyBatis 인터셉터), 엑셀 다운로드(SXSSF), 공통 레이아웃 |

## 4. 보안

| 항목 | 대응 |
| :--- | :--- |
| SQL 삽입 | MyBatis `#{}` 바인딩만 사용 |
| XSS | Thymeleaf `th:text` 이스케이프, CSP `script-src 'self'` (인라인 스크립트 없음) |
| 인가 | URL · `@PreAuthorize` · 서비스 계층에서 소유자·배정 여부 재검사 |
| 대량 할당 | 소유자·금액·채번 ID·감사 컬럼 등 서버가 정하는 필드는 바인딩 차단 |
| CSRF | Spring Security 토큰, 상태 변경은 모두 POST |
| 파일 | 확장자 화이트리스트 + 시그니처 검사, 웹 루트 밖 UUID 저장, 다운로드 권한 재검사 |
| 계정 | 로그인 실패 잠금(사용자 행 잠금으로 횟수 계산), 잠긴 계정은 비밀번호 비교 전 거부 |
| 개인정보 | 암호화 저장, 서버 마스킹, 암호화 라이브러리 DEBUG 로그(평문·키 출력) 차단 |
| 동시성 | 심사·평가·정산이 같은 신청서 행을 `SELECT … FOR UPDATE`로 잠근 뒤 검사 |
| 금액 | 양수 검사, `Math.addExact`로 합계 오버플로 차단 |
| 세션 | 쿠키 HttpOnly·SameSite=Lax, URL 세션 ID 미사용, 30분 만료 |
| 공급망 | Maven Wrapper SHA-256 검증, GitHub Actions 커밋 SHA 고정, 외부 CDN 대신 WebJars |

## 5. 실행과 테스트

JDK 17만 있으면 됩니다. Maven Wrapper, 내장 H2, 내장 Tomcat을 씁니다.

```bash
# 실행 (Windows)
.\mvnw.cmd spring-boot:run
# 실행 (Mac / Linux)
./mvnw spring-boot:run

# 테스트
./mvnw verify
```

- 웹: http://localhost:8080
- H2 콘솔: http://localhost:8080/h2-console (`admin` 로그인 후 `jdbc:h2:mem:daegupms` / `sa` / 비밀번호 없음)
- VS Code 설정: [개발 환경 안내](docs/VS_CODE_SETUP.md)

테스트 144건: 상태머신·마스킹 단위 테스트, MockMvc 통합 테스트(인가·CSRF·파일 위조·예산 초과·계정 잠금·암호화·FAQ), 별도 스레드와 DB 잠금 대기로 확인하는 동시성 테스트, eGov 실행환경 구성 테스트, 모든 주요 화면의 공통 규칙(CSP·접근성) 테스트. Pull Request마다 GitHub Actions가 실행합니다.

H2 인메모리 DB와 공개 샘플 계정을 쓰는 로컬 데모입니다. 운영 DB 전환(MariaDB·Flyway), 회원가입, 개인정보 조회 기록은 아직 구현하지 않았습니다.

| 권한 | 아이디 | 비밀번호 |
| :--- | :--- | :--- |
| 총괄관리자 | `admin` | `admin1234` |
| 사업담당자 | `manager1` | `manager1234` |
| 평가위원 | `evaluator1`, `evaluator2` | `eval1234` |
| 기업회원 | `company1`, `company2` | `company1234` |

## 6. 구조

```text
kr.go.daegu.pms
├── config/          Security, EgovConfig*(Common·Mapper·Transaction·Aspect·Properties·IdGeneration·Web)
├── cmm/             공통: crypto, exception, message, mybatis(감사 인터셉터), pagination, util, web, service
├── notice/          공지사항   ─┐
├── faq/             자주 묻는 질문 │   업무 패키지마다
├── announcement/    공고         │   ├── service/        인터페이스, VO
├── application/     신청         │   ├── service/impl/   ServiceImpl, @EgovMapper
├── evaluation/      평가         │   └── web/            Controller
├── settlement/      사업비 정산   │
└── user/            사용자·로그인 ─┘
resources/
├── egovframework/sqlmap/     sql-mapper-config.xml, mappers/*_SQL.xml
├── egovframework/message/    메시지 번들
├── templates/                Thymeleaf (layout/, fragments/, 업무별 화면)
├── static/                   dpms.css, dpms.js
└── schema.sql, data.sql, application.yml, log4j2-spring.xml
```

## 7. 문서

| 문서 | 내용 |
| :--- | :--- |
| [EGOVFRAME_MIGRATION.md](docs/EGOVFRAME_MIGRATION.md) | Spring Boot 구성 → eGovFrame 5.0 전환 과정과 트러블슈팅 |
| [SPRINT4_MESSAGES_SECURITY.md](docs/SPRINT4_MESSAGES_SECURITY.md) | 메시지 외부화, 경쟁 상태·금액 오버플로·로그 노출 수정 |
| [SPRINT5_ACCOUNT_PRIVACY_UI.md](docs/SPRINT5_ACCOUNT_PRIVACY_UI.md) | 계정 잠금, 개인정보 암호화, 감사 컬럼, 정산 재요청, 화면 개선 |
| [egov-codegen/README.md](docs/egov-codegen/README.md) | 코드 생성기 원본과 보완 내용 비교 (공지사항, FAQ) |
| [CHANGELOG.md](CHANGELOG.md) | 스프린트별 변경 이력 |

교육 및 포트폴리오 목적으로 만들었습니다. © 2026 gwangco
