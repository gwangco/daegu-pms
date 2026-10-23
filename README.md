# D-PMS — 대구 지원사업 통합관리·e-정산 시스템

전자정부 표준프레임워크(eGovFrame) 5.1 실행환경과 공통컴포넌트로 만든 공공 SI 포트폴리오 프로젝트입니다.
지원사업 공고부터 온라인 신청, 서류 검토, 평가위원 채점, 최종 선정, 사업비 집행·증빙 정산까지 한 시스템에서 처리합니다.

[![CI](https://github.com/gwangco/daegu-pms/actions/workflows/ci.yml/badge.svg)](https://github.com/gwangco/daegu-pms/actions/workflows/ci.yml)
[![eGovFrame](https://img.shields.io/badge/eGovFrame-5.1.0-134F8C)](https://www.egovframe.go.kr)
[![Java](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.16-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![MyBatis](https://img.shields.io/badge/MyBatis-3.5.19-C70D2C)](https://mybatis.org/mybatis-3/)
[![MySQL](https://img.shields.io/badge/MySQL-8.4%20LTS-4479A1?logo=mysql&logoColor=white)](https://dev.mysql.com/doc/relnotes/mysql/8.4/en/)
[![Tests](https://img.shields.io/badge/Tests-233%20passed-brightgreen)](#5-실행과-테스트)

> 취업 포트폴리오용 데모입니다. 기관·사업·기업명은 설명을 위한 가상의 예시이며 실제 기관과 관련이 없습니다.

화면은 KRDS(대한민국 정부 디자인 시스템) 디자인 토큰과 Pretendard GOV 글꼴을 기준으로 만들었습니다 ([7차 스프린트](docs/SPRINT7_DESIGN_RENEWAL.md)).

| 메인 — 공고 검색과 역할별 처리할 일 (사업담당자) | 사업비 e-정산 — 집행률과 증빙 검수 (기업회원) |
| :---: | :---: |
| ![메인 화면](docs/images/main-manager.png) | ![정산 화면](docs/images/settlement-company.png) |
| **자주 묻는 질문 — 전자정부 공통컴포넌트 FAQ** | **사용자 관리 — 연락처·이메일 암호화 저장 후 서버 마스킹** |
| ![FAQ 화면](docs/images/faq-manager.png) | ![사용자 관리 화면](docs/images/admin-users.png) |

| 모바일 (390px) — 메인, 카드형 신청 목록, e-정산 |
| :---: |
| ![모바일 화면](docs/images/mobile-company.png) |

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
| ID Generation | 테이블 채번 (`COMTECOPSEQ`) | 접수번호 `DPMS-00000001`, 첨부파일 `FILE_…`, 게시물 번호, FAQ `FAQ_…` | `config/EgovConfigIdGeneration` |
| `PaginationInfo` + 렌더러 | 페이지 계산 + Bootstrap 렌더러(Thymeleaf Dialect) | 목록 하단 페이지 번호 바 | `cmm/pagination` |
| 암호화 서비스 (ARIA) | 연락처·이메일 암호화 저장, 키는 환경변수 | DB 에는 `{ARIA}…`, 관리자 화면에는 가린 값 | `cmm/crypto` |
| `MessageSource` | UTF-8 업무 메시지 번들 + `AppMessages` | 모든 안내 문구, 누락 키는 테스트에서 실패 | `egovframework/message` |
| **공통컴포넌트** v5.0.7 | 공통코드·첨부파일·게시판·FAQ 의 서비스·DAO·SQL 을 가져와 업무 서비스가 호출. 원본의 트랜잭션 충돌·검색 오류·화면 값 저장·첨부 삭제 순서를 고쳐 씀 | 공지사항 = 게시판, FAQ = FAQ 관리, 모든 첨부 = 첨부파일 관리, 선택 상자 = 공통코드 | `egovframework/com`, [사용 내역](docs/COMMON_COMPONENTS.md) |
| VS Code 확장 코드 생성기 | 공지사항·FAQ CRUD 를 DDL 로 생성한 뒤 보안·업무 규칙 보완 (6차에 공통컴포넌트로 교체) | 원본과 보완본, 검토 항목 15개 기록 | [`docs/egov-codegen`](docs/egov-codegen/README.md) |
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
| 인증·계정 | 4개 역할, BCrypt, 5회 연속 실패 시 30분 잠금, 접속 기록, 관리자 잠금 해제, 기업회원 가입(약관 동의·승인 대기·총괄관리자 승인) |
| 공지·FAQ | 공통컴포넌트 게시판·FAQ 기반, 중요 공지 고정, 작성자 검색, FAQ 분류 탭·검색, 논리 삭제, 공지 첨부파일(개수·크기는 게시판 속성, 비로그인 다운로드) |
| 공고·신청 | 공통코드 분야 필터, 접수기간·최대지원금·중복신청 검증, 접수번호 채번, 임시저장·이어서 작성·제출 취소(서류 검토 전) |
| 심사·평가 | 평가위원 배정, 전용 채점 화면, 제출 후 수정 불가 |
| e-정산 | 비목별 집행·증빙, 부가세 서버 계산, 잔액 초과 차단, 반려 건 보완 재요청과 처리 이력, 담당자 검수함 |
| 개인정보 | 연락처·이메일 ARIA 암호화, 목록·평가위원 화면 마스킹, 본인 정보 화면 |
| 공통 | 수정자·수정일시 자동 기록(MyBatis 인터셉터), 엑셀 다운로드(SXSSF), 공통 레이아웃 |
| 화면 | KRDS 디자인 토큰, Pretendard GOV 내장, 모바일 카드형 목록, 금액 한글 단위·한도 안내, 공고 마감 D-day, 정적 리소스 해시 주소·1년 캐시 |

## 4. 보안

| 항목 | 대응 |
| :--- | :--- |
| SQL 삽입 | MyBatis `#{}` 바인딩만 사용 |
| XSS | Thymeleaf `th:text` 이스케이프, CSP `script-src 'self'` (인라인 스크립트 없음) |
| 인가 | URL · `@PreAuthorize` · 서비스 계층에서 소유자·배정 여부 재검사 |
| 대량 할당 | 소유자·금액·채번 ID·감사 컬럼 등 서버가 정하는 필드는 바인딩 차단, 가입 입력에는 권한·승인 상태 필드 자체가 없음 |
| CSRF | Spring Security 토큰, 상태 변경은 모두 POST |
| 파일 | 확장자 화이트리스트 + 시그니처 검사, 웹 루트 밖 UUID 저장, 다운로드 주소 형식 고정 + 권한 재검사, 첨부 ID 는 화면 값을 쓰지 않음, 공지 첨부는 삭제되지 않은 공지의 것만 공개 |
| DB 계정 | 테이블 변경 계정과 실행 계정 분리 (실행 계정은 조회·등록·수정·삭제만), 비밀번호는 저장소 밖 |
| 계정 | 로그인 실패 잠금(사용자 행 잠금으로 횟수 계산), 잠긴 계정은 비밀번호 비교 전 거부, 비밀번호 규칙(KISA 조합·길이 기준), 가입 후 관리자 승인 전 로그인 불가, 사업자등록번호당 기업회원 하나(DB UNIQUE) |
| 개인정보 | 암호화 저장, 서버 마스킹, 암호화 라이브러리 DEBUG 로그(평문·키 출력) 차단 |
| 동시성 | 심사·평가·정산이 같은 신청서 행을 `SELECT … FOR UPDATE`로 잠근 뒤 검사, 공지 첨부 수정은 게시물 행을 잠근 뒤 파일 순번 계산 |
| 금액 | 양수 검사, `Math.addExact`로 합계 오버플로 차단 |
| 세션·화면 | 쿠키 HttpOnly·SameSite=Lax, URL 세션 ID 미사용, 30분 만료, 다른 사이트의 iframe 포함 차단 |
| 공급망 | Maven Wrapper SHA-256 검증, GitHub Actions 커밋 SHA·MySQL 이미지 digest 고정, 외부 CDN 대신 WebJars, 글꼴도 서버에 내장(배포본 무결성 값 확인) |

## 5. 실행과 테스트

JDK 17 과 MySQL 8.x(8.4 LTS 권장)가 필요합니다. Maven Wrapper 와 내장 Tomcat 을 씁니다.

```bash
# 1) 처음 한 번: DB(dpms, dpms_test)와 계정을 만들고 비밀번호 파일(config/dpms-secret.yml)을 생성 (Windows PowerShell)
.\scripts\setup-local-mysql.ps1

# 2) 실행 (서버가 뜰 때 Flyway 가 스키마와 데모 데이터를 적용)
.\mvnw.cmd spring-boot:run

# 3) 테스트 (dpms_test 를 비우고 다시 만든 뒤 실행)
.\mvnw.cmd clean verify
```

- 웹: http://localhost:8080
- 스크립트를 쓰지 않을 때는 `config/dpms-secret.example.yml` 을 참고해 같은 계정을 만들거나 환경변수 `DPMS_DB_*` 를 지정합니다.
- 개발 환경: [Eclipse](docs/ECLIPSE_SETUP.md) (기본) · [VS Code](docs/VS_CODE_SETUP.md)

테스트 224건: 상태머신·마스킹·업무 시간대·비밀번호 규칙·사업자등록번호 검증번호 단위 테스트, MockMvc 통합 테스트(인가·CSRF·파일 위조·예산 초과·계정 잠금·암호화·공지·공지 첨부·FAQ·기업회원 가입과 승인·신청서 임시저장과 제출 취소), 별도 스레드와 MySQL 행 잠금 대기로 확인하는 동시성 테스트, eGov 실행환경·공통컴포넌트 구성과 V2~V5 마이그레이션 결과(CHECK 제약 포함) 테스트, 모든 주요 화면의 공통 규칙(CSP·접근성)과 정적 리소스 캐시 테스트. Pull Request 마다 GitHub Actions 가 MySQL 8.4 컨테이너로 실행합니다.

공개 샘플 계정을 쓰는 로컬 데모입니다. 기업회원은 가입 화면(`/signup`)에서 직접 만들 수 있고, 총괄관리자(`admin`)가 사용자 관리에서 승인하면 로그인됩니다. 개인정보 조회 기록은 아직 구현하지 않았습니다.

| 권한 | 아이디 | 비밀번호 |
| :--- | :--- | :--- |
| 총괄관리자 | `admin` | `admin1234` |
| 사업담당자 | `manager1` | `manager1234` |
| 평가위원 | `evaluator1`, `evaluator2` | `eval1234` |
| 기업회원 | `company1`, `company2` | `company1234` |

## 6. 구조

```text
egovframework.com    전자정부 공통컴포넌트 v5.0.7 (원본 패키지 그대로, 고친 파일은 개정이력에 기록)
├── cmm/             공통 기반, 공통코드(EgovCmmUseService), 첨부파일(EgovFileMngService)
├── cop/bbs/         게시판(EgovArticleService) → 공지사항
└── uss/olh/faq/     FAQ(EgovFaqService)
kr.go.daegu.pms
├── config/          Security, EgovConfig*(Common·Mapper·Transaction·Aspect·Properties·IdGeneration·Web)
├── cmm/             공통: crypto, exception, message, mybatis(감사 인터셉터), pagination, util, web, service
├── notice/          공지사항 (공통컴포넌트 게시판·첨부파일 호출) ─┐
├── faq/             자주 묻는 질문 (공통컴포넌트 FAQ 호출) │   업무 패키지마다
├── announcement/    공고         │   ├── service/        인터페이스, VO
├── application/     신청         │   ├── service/impl/   ServiceImpl, @EgovMapper
├── evaluation/      평가         │   └── web/            Controller
├── settlement/      사업비 정산   │
└── user/            사용자·로그인·기업회원 가입 ─┘
resources/
├── db/migration/             Flyway V1(기본 스키마), V2(공통컴포넌트 전환), V3(공지 첨부 허용), V4(가입 승인·동의), V5(임시저장·CHECK)  · db/seed/ 데모 데이터
├── egovframework/mapper/com/ 공통컴포넌트 SQL (*_mysql.xml)
├── egovframework/sqlmap/     sql-mapper-config.xml, mappers/*_SQL.xml
├── egovframework/message/    메시지 번들
├── templates/                Thymeleaf (layout/, fragments/, 업무별 화면)
├── static/                   css/dpms.css(KRDS 토큰), js/dpms.js, fonts/pretendard-gov, images/logo.svg
└── application.yml, log4j2-spring.xml
```

## 7. 문서

| 문서 | 내용 |
| :--- | :--- |
| [EGOVFRAME_MIGRATION.md](docs/EGOVFRAME_MIGRATION.md) | Spring Boot 구성 → eGovFrame 5.0 전환 과정과 트러블슈팅 |
| [SPRINT4_MESSAGES_SECURITY.md](docs/SPRINT4_MESSAGES_SECURITY.md) | 메시지 외부화, 경쟁 상태·금액 오버플로·로그 노출 수정 |
| [SPRINT5_ACCOUNT_PRIVACY_UI.md](docs/SPRINT5_ACCOUNT_PRIVACY_UI.md) | 계정 잠금, 개인정보 암호화, 감사 컬럼, 정산 재요청, 화면 개선 |
| [SPRINT6_MYSQL_COMMON_COMPONENTS.md](docs/SPRINT6_MYSQL_COMMON_COMPONENTS.md) | MySQL·Flyway 전환, DB 계정 분리, 공통컴포넌트 도입, Eclipse 전환 |
| [SPRINT7_DESIGN_RENEWAL.md](docs/SPRINT7_DESIGN_RENEWAL.md) | KRDS 기준 화면 개편, 글꼴 내장, 모바일 카드형 목록, 정적 리소스 캐시, 명도 대비 수치 |
| [SPRINT8_NOTICE_ATTACHMENTS.md](docs/SPRINT8_NOTICE_ATTACHMENTS.md) | 공지사항 첨부파일, 게시판 속성으로 개수·크기 관리, 첨부 ID 위조 차단, 공통컴포넌트 첨부 삭제 순서 수정 |
| [SPRINT9_COMPANY_SIGNUP.md](docs/SPRINT9_COMPANY_SIGNUP.md) | 기업회원 가입, 약관 동의 기록, 승인 대기와 관리자 승인, 비밀번호 규칙, 사업자등록번호 검증·중복 방지 |
| [SPRINT10_APPLICATION_DRAFT.md](docs/SPRINT10_APPLICATION_DRAFT.md) | 신청서 임시저장·이어서 작성·제출 취소, 임시저장 공개 범위, CHECK 제약, 버튼 두 개 폼과 중복 제출 방지 |
| [COMMON_COMPONENTS.md](docs/COMMON_COMPONENTS.md) | 가져온 공통컴포넌트 목록과 원본 대비 변경점 |
| [ECLIPSE_SETUP.md](docs/ECLIPSE_SETUP.md) | Eclipse 워크스페이스·JDK·실행 설정 |
| [egov-codegen/README.md](docs/egov-codegen/README.md) | 코드 생성기 원본과 보완 내용 비교 (공지사항, FAQ) |
| [CHANGELOG.md](CHANGELOG.md) | 스프린트별 변경 이력 |

화면 기준: [KRDS](https://www.krds.go.kr) 디자인 토큰(krds-uiux 1.1.0)을 참고했고, 글꼴은 KRDS 배포본의 Pretendard GOV(SIL OFL 1.1)를 씁니다. 출처와 라이선스는 [`licenses/`](licenses)에 있습니다.

교육 및 포트폴리오 목적으로 만들었습니다. © 2026 gwangco
