# Docker 로 실행하기

JDK 나 MySQL 을 설치하지 않고 `docker compose up` 한 번으로 앱과 MySQL 8.4 를 띄운다. 데모 데이터와 데모 암호화 키를 쓰는 로컬 데모용이고 운영 구성은 아니다.

## 왜 넣었나

포트폴리오를 보는 사람이 MySQL 을 설치하고 계정까지 만들어 가며 실행해 보기는 어렵다. Docker Desktop 만 있으면 누구 PC 에서나 같은 버전, 같은 설정으로 뜬다. GitHub Actions 도 같은 파일로 실제로 띄워서 확인한다.

## 구성

- `db`: MySQL 8.4.10. 데이터는 `db-data` 볼륨에 남는다. 처음 한 번 `docker/mysql/init/01-users.sh` 가 계정 두 개(테이블 변경용, 실행용)를 만든다.
- `app`: `Dockerfile` 로 빌드한 D-PMS. db 가 준비됐다는 확인(healthcheck)이 끝난 뒤 시작하고, 뜰 때 Flyway 가 스키마와 데모 데이터를 넣는다. 업로드 파일은 `uploads` 볼륨에 남는다.

`Dockerfile` 은 두 단계다. JDK 17 이미지에서 jar 를 빌드하고, JRE 17 이미지에 jar 만 옮겨 root 가 아닌 계정으로 실행한다. 베이스 이미지는 digest 로 고정했다.

## 실행

Docker Desktop 이 필요하다. Windows 는 WSL 2 도 필요한데, 없으면 관리자 PowerShell 에서 `wsl --install --no-distribution` 을 실행한다. Docker Desktop 왼쪽 아래에 "Engine running" 이 보이면 준비된 것이다.

```bash
cp .env.example .env          # PowerShell: Copy-Item .env.example .env
# .env 의 비밀번호 세 개를 바꾼다
docker compose up -d --build
```

처음에는 이미지를 빌드하느라 몇 분 걸린다. 다 뜨면 http://localhost:8080 으로 들어간다. 계정은 README 에 있다.

## 자주 쓰는 명령

```bash
docker compose ps                  # 상태 확인. db 가 healthy 인지 본다
docker compose logs -f app         # 앱 로그 보기 (Ctrl+C 로 나온다)
docker compose up -d --build app   # 코드를 바꾼 뒤 앱만 다시 빌드
docker compose down                # 끄기 (데이터는 남는다)
docker compose down -v             # 볼륨까지 지우고 처음부터
```

## 알아 둘 것

- 컨테이너끼리는 서비스 이름으로 찾는다. 앱 컨테이너 안에서 `localhost` 는 앱 자신이라서 DB 주소는 `db:3306` 이다.
- DB 포트는 밖으로 열지 않았다. 이 PC 의 MySQL 이 3306 을 쓰고 있어도 겹치지 않는다. 8080 이 겹치면 `.env` 의 `DPMS_PORT` 를 바꾼다.
- 계정 초기화 스크립트는 데이터 볼륨이 비어 있을 때 한 번만 돈다. `.env` 비밀번호를 바꿨다면 `docker compose down -v` 로 볼륨을 지우고 다시 띄운다.
- 비밀번호는 영문, 숫자, 하이픈만 쓴다. 초기화 SQL 문자열에 그대로 들어간다.
- 운영에 쓰려면 암호화 키를 환경변수로 넣고 데모 데이터를 빼야 한다. TLS, 백업, 비밀값 관리도 따로 정해야 한다.

## CI 에서 확인하는 것

Pull Request 마다 GitHub Actions 가 무작위 비밀번호로 띄워서 로그인 화면과 메인 화면 응답, 공고 목록의 데모 데이터, 기동 직후 공고 마감 배치 실행 로그를 확인한다(`.github/workflows/ci.yml`).
