# 화면 목록과 요구사항 추적표

> 기준: 0.11.1 (2026-10-26). 화면 ID 는 업무 영역 약어와 순번이다. 접근 권한은 `SecurityConfig` 의 URL 규칙과 컨트롤러의 `@PreAuthorize` 를 함께 본 결과다.
> 화면 목록의 모든 주요 화면은 `WebUiIntegrationTest` 가 CSP, 라벨 없는 입력칸, 정적 리소스 주소 규칙을 공통으로 확인한다.

- 접근: 전체 = 비로그인 포함, 로그인 = 로그인한 모든 역할, 기업·평가위원·담당자·관리자 = 그 역할만 (관리자는 총괄관리자)
- 테스트 이름의 `IntegrationTest` 는 줄여 적었다 (예: `Workflow` = `WorkflowIntegrationTest`)

## 1. 화면 목록

### 공통 (COM)

| 화면 ID | 화면명 | URL | 템플릿 | 접근 | 주요 기능 | 검증 테스트 |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| COM-01 | 메인 | `/` | `index` | 전체 | 비로그인은 소개, 로그인하면 역할별 처리할 일과 건수 | DashboardAndCode, Security |
| COM-02 | 로그인 | `/login` | `login` | 전체 | 로그인, 5회 실패 잠금 안내, 승인 대기 안내 | AccountLock, CompanySignup, Security |
| COM-03 | 개인정보처리방침 | `/privacy` | `privacy` | 전체 | 수집 항목, 보관 기간, 처리 기록 안내 | WebUi |
| COM-04 | 오류 화면 | (오류 시) | `error/403`, `404`, `4xx`, `5xx`, `business` | 전체 | 권한 없음, 없는 주소, 업무 안내 메시지 | Security, Message |

### 회원 (USR)

| 화면 ID | 화면명 | URL | 템플릿 | 접근 | 주요 기능 | 검증 테스트 |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| USR-01 | 기업회원 가입 | `/signup` | `user/signup` | 전체 (로그인 상태면 메인으로) | 약관·개인정보 동의, 사업자등록번호 검증번호 확인, 승인 대기로 가입 | CompanySignup |
| USR-02 | 내 정보 | `/mypage` | `user/mypage` | 로그인 | 본인 연락처·이메일 복호화 표시, 가입 승인 정보 | PersonalInfoCrypto, CompanySignup |

### 공고 (ANN)

| 화면 ID | 화면명 | URL | 템플릿 | 접근 | 주요 기능 | 검증 테스트 |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| ANN-01 | 사업공고 목록 | `/announcements` | `announcement/list` | 전체 | 분야(공통코드)·상태·검색어 조회, 마감 D-day | Workflow, DashboardAndCode |
| ANN-02 | 사업공고 상세 | `/announcements/{번호}` | `announcement/detail` | 전체 | 예산·최대지원금·접수기간, 기업은 신청 버튼 | Workflow, EgovRuntime |
| ANN-03 | 공고 등록 | `/announcements/new` | `announcement/form` | 담당자·관리자 | 분야 코드 검증, 작성자는 서버가 지정 | Workflow, ServiceAuthorization |

### 신청·심사 (APP)

| 화면 ID | 화면명 | URL | 템플릿 | 접근 | 주요 기능 | 검증 테스트 |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| APP-01 | 신청·심사 목록 | `/applications` | `application/list` | 로그인 (기업은 자기 신청, 평가위원은 배정 과제, 담당자·관리자는 전체) | 상태 검색, 임시저장은 본인만 | Workflow, ApplicationDraft |
| APP-02 | 사업 신청서 작성 | `/applications/apply?announcementId=` | `application/form` | 기업 | 접수기간·최대지원금·중복신청 검증, 접수번호 채번, 사업계획서 첨부, 임시저장 | Workflow, WorkflowConcurrency, ApplicationDraft |
| APP-03 | 임시저장 이어서 작성 | `/applications/{번호}/edit` | `application/form` | 기업 (본인) | 이어서 작성·제출, 임시저장 삭제 | ApplicationDraft |
| APP-04 | 신청서 상세 | `/applications/{번호}` | `application/detail` | 기업(본인)·배정된 평가위원·담당자·관리자 | 서류 검토, 평가위원 배정, 최종 선정, 제출 취소, 처리 기록 남김 | Workflow, PrivacyAccessLog, Security |
| APP-05 | 신청 현황 엑셀 | `/excel/applications` | (다운로드) | 담당자·관리자 | SXSSF 엑셀, 처리 기록 남김 | ExcelExport, PrivacyAccessLog |

### 평가 (EVL)

| 화면 ID | 화면명 | URL | 템플릿 | 접근 | 주요 기능 | 검증 테스트 |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| EVL-01 | 내 평가 목록 | `/evaluator/evaluations` | `evaluation/list` | 평가위원 | 배정된 과제와 채점 상태 | Workflow |
| EVL-02 | 채점 | `/evaluator/evaluations/{번호}` | `evaluation/form` | 평가위원 (배정된 과제) | 정량 40 + 정성 60, 제출 후 수정 불가 | Workflow, AuditColumn |

### 정산 (STL)

| 화면 ID | 화면명 | URL | 템플릿 | 접근 | 주요 기능 | 검증 테스트 |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| STL-01 | 과제별 집행·증빙 | `/settlements/application/{신청서 번호}` | `settlement/list` | 기업(본인 선정 과제)·담당자·관리자 | 비목별 집행 등록, 부가세 서버 계산, 잔액 초과 차단, 보완 재요청 | SettlementAmountSecurity, SettlementResubmit, Workflow |
| STL-02 | 정산 검수함 | `/settlements/review` | `settlement/review` | 담당자·관리자 | 승인·반려(사유 필수), 처리 이력 | SettlementResubmit |

### 공지·FAQ (BRD)

| 화면 ID | 화면명 | URL | 템플릿 | 접근 | 주요 기능 | 검증 테스트 |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| BRD-01 | 공지사항 목록 | `/notices` | `notice/list` | 전체 | 중요 공지 고정, 제목·내용·작성자 검색 | Notice |
| BRD-02 | 공지사항 상세 | `/notices/{번호}` | `notice/detail` | 전체 | 조회수, 첨부파일 내려받기(비로그인 가능) | Notice, NoticeAttachment |
| BRD-03 | 공지 등록·수정 | `/notices/new`, `/notices/{번호}/edit` | `notice/form` | 담당자·관리자 | 첨부 개수·크기 제한(게시판 속성), 논리 삭제 | Notice, NoticeAttachment |
| BRD-04 | 자주 묻는 질문 | `/faqs` | `faq/list` | 전체 | 분류 탭, 검색, 펼쳐 보기 | Faq |
| BRD-05 | FAQ 등록·수정 | `/faqs/new`, `/faqs/{번호}/edit` | `faq/form` | 담당자·관리자 | 분류·표시 순서, 논리 삭제 | Faq |

### 시스템 관리 (ADM)

| 화면 ID | 화면명 | URL | 템플릿 | 접근 | 주요 기능 | 검증 테스트 |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| ADM-01 | 사용자 관리 | `/admin/users` | `admin/user-list` | 관리자 | 가입 승인, 잠금 해제, 연락처 마스킹, 처리 기록 남김 | CompanySignup, AccountLock, PrivacyAccessLog |
| ADM-02 | 접속 기록 | `/admin/login-history` | `admin/login-history` | 관리자 | 로그인·실패·잠금·로그아웃 기록 검색 | AccountLock |
| ADM-03 | 개인정보 처리 기록 | `/admin/privacy-log` | `admin/privacy-log` | 관리자 | 처리자·업무·기간 검색, 다운로드 구분 | PrivacyAccessLog |
| ADM-04 | 배치 실행 이력 | `/admin/batch` | `admin/batch` | 관리자 | 공고 마감 배치 이력, 지금 실행 | AnnouncementCloseJob |

화면이 아닌 기능 주소: 첨부파일 내려받기 `/files/{첨부파일ID}/{순번}` (로그인, 서비스가 신청서·정산 권한 확인), 공지 첨부 `/notices/{번호}/files/{순번}` (전체).

## 2. 요구사항 추적표

요구사항 → 화면 → 구현 위치 → 검증 테스트를 한 줄로 잇는다. 테스트가 없는 요구사항은 두지 않는다.

| 요구사항 ID | 요구사항 | 화면 | 구현 위치 | 검증 테스트 |
| :--- | :--- | :--- | :--- | :--- |
| REQ-01 | 기업회원은 약관에 동의하고 가입하며, 총괄관리자가 승인해야 로그인할 수 있다 | USR-01, ADM-01, COM-02 | `CompanySignupController`, `UserManageServiceImpl`, V4 | CompanySignup |
| REQ-02 | 비밀번호를 5회 연속 틀리면 30분 잠그고, 접속 기록을 남기며 관리자가 해제할 수 있다 | COM-02, ADM-01, ADM-02 | `LoginFailureHandler`, `LoginPolicyServiceImpl` | AccountLock, Security |
| REQ-03 | 담당자는 공고를 등록하고, 누구나 공고를 조회한다. 분야는 공통코드로 관리한다 | ANN-01~03 | `AnnouncementServiceImpl`, 공통코드 | Workflow, ServiceAuthorization, DashboardAndCode |
| REQ-04 | 기업은 접수기간 안에 공고당 한 번 신청하고, 접수번호를 받는다. 신청 금액은 최대지원금을 넘지 못한다 | APP-02 | `ApplicationServiceImpl`, ID Generation | Workflow, WorkflowConcurrency |
| REQ-05 | 신청서를 임시저장해 이어서 쓰고, 서류 검토 전에는 제출을 취소할 수 있다 | APP-02~04 | `ApplicationServiceImpl`, V5 CHECK | ApplicationDraft |
| REQ-06 | 담당자가 서류를 검토하고 평가위원을 배정하면, 평가위원은 배정된 과제만 채점한다. 모두 채점해야 최종 선정한다 | APP-04, EVL-01~02 | `ApplicationStatus`, `EvaluationServiceImpl` | Workflow, Security |
| REQ-07 | 선정 기업은 비목별로 집행을 등록하고, 담당자가 검수한다. 잔액을 넘으면 등록할 수 없고, 반려되면 보완해 다시 요청한다 | STL-01~02 | `SettlementServiceImpl` | SettlementAmountSecurity, SettlementResubmit |
| REQ-08 | 공지사항과 FAQ 를 공통컴포넌트 게시판·FAQ 로 운영하고, 공지에 파일을 첨부한다 | BRD-01~05 | `egovframework/com`, `NoticeController` | Notice, NoticeAttachment, Faq, CommonComponent |
| REQ-09 | 연락처·이메일은 암호화해 저장하고 화면에서 가리며, 남의 개인정보 조회·다운로드는 기록한다 | USR-02, APP-04~05, ADM-01, ADM-03 | `cmm/crypto`, `PrivacyAccessLogServiceImpl` | PersonalInfoCrypto, PrivacyAccessLog |
| REQ-10 | 담당자는 신청 현황을 엑셀로 내려받는다 | APP-05 | `ExcelController` (SXSSF) | ExcelExport |
| REQ-11 | 접수 마감일이 지난 공고는 매일 자정 마감되고, 실행 이력을 확인할 수 있다 | ADM-04 | `announcement/batch`, V7 | AnnouncementCloseJob |
| REQ-12 | 모든 변경 요청은 CSRF 를 확인하고, 화면은 CSP 와 접근성 규칙을 지킨다. SQL 로그에 바인딩 값을 남기지 않는다 | 전 화면 | `SecurityConfig`, 공통 레이아웃 | Security, WebUi, SqlLoggingSecurity |
| REQ-13 | 수정자·수정일시는 서버가 기록하고, 안내 문구는 메시지 번들로 관리한다 | 전 화면 | `AuditParameterInterceptor`, `AppMessages` | AuditColumn, Message |
