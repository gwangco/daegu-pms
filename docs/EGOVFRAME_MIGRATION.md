# eGovFrame 5.0 전환 기록

2026-10-21, 브랜치 `feature/egovframe5-migration`

1, 2차 때는 Spring Boot 3 위에 eGovFrame 의 패키지 구조와 이름만 흉내 낸 상태였다. eGovFrame 4.x 는 Spring 5(javax) 기반이라 Spring Boot 3(Spring 6, jakarta)과 같이 쓸 수 없었기 때문이다. eGovFrame 5.0 이 Spring Boot 3.5 기반으로 나오면서 실제 실행환경 라이브러리(`org.egovframe.rte`)를 쓸 수 있게 되어 옮겼다.

## 무엇이 바뀌었나

| | 전 | 후 |
| :--- | :--- | :--- |
| 빌드 | Gradle | Maven, `egovframe-boot-starter-parent` 5.0.2 |
| 패키지 | `controller/`, `service/`, `mapper/` | 업무별 `service`, `service/impl`, `web` |
| 서비스 | `@Service` | `EgovAbstractServiceImpl` 상속 |
| Mapper | `@Mapper` | `@EgovMapper` |
| 트랜잭션 | 메서드마다 `@Transactional` | AOP 로 `*Impl` 에 일괄 적용 |
| 페이징 | 직접 만든 것 | eGov `PaginationInfo` 와 렌더러 |
| 번호 채번 | AUTO_INCREMENT | eGov ID Generation (접수번호 `DPMS-00000001`) |
| 로깅 | Logback | Log4j2 |

단계마다 전체 테스트를 통과시킨 뒤 커밋했다. 빌드 전환, 패키지 이동(`git mv` 로 이력 유지), eGov 클래스 적용, 채번, 공지사항 생성, CI 순서였다.

## 옮기면서 생긴 문제

- eGov 트랜잭션은 메서드 이름으로 규칙을 정한다. `select*` 는 읽기 전용이라 상세 조회 안에서 조회수를 UPDATE 하던 코드는 MySQL 같은 DB 에서 거부될 수 있다. `updateViewCount` 로 나눴다.
- eGov 예외 후처리(`ExceptionTransfer`)는 모든 RuntimeException 을 ERROR 로 남긴다. "잔액 초과" 같은 안내까지 장애 로그에 쌓여서 업무 예외는 후처리에서 뺐다.
- 같은 후처리가 체크 예외를 eGov 의 체크 예외로 바꿔 던져서, 엑셀 다운로드에서 `UndeclaredThrowableException` 이 날 수 있었다. 서비스에서 런타임 예외로 감쌌다.
- 페이지 렌더러는 싱글톤인데 요청마다 공유 필드를 바꾸고 있어서 동시 요청에서 페이지 라벨이 섞일 수 있었다. 지역 변수로 바꿨다.
- eGov 모듈이 iBATIS 2 와 JPA API 를 같이 가져와서 쓰지 않는 라이브러리는 뺐다. 안 쓰는 라이브러리도 공격 대상이 될 수 있다.
- 접수번호 채번은 별도 트랜잭션에서 번호를 확정하기 때문에, 신청이 롤백돼도 번호는 소진된다. 결번이 생길 수 있다는 점을 남겨 둔다.

## 확인

- 테스트 58건 통과. eGov 실행환경 테스트는 ServiceImpl 상속과 AOP 적용, `@EgovMapper` 등록, 트랜잭션 규칙(`select*` 읽기 전용, 체크 예외 롤백), 페이지 렌더러 출력을 본다.
- 실제 서버를 띄워 신청부터 정산 승인까지 업무 흐름 전체와 공지 등록, 수정, 삭제를 손으로 확인했다.

## 참고

- 전자정부 표준프레임워크 포털: https://www.egovframe.go.kr
- eGovFrame GitHub: https://github.com/eGovFramework
