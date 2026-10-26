# Docker 로 실행하기

> JDK·MySQL 을 설치하지 않고 `docker compose up` 한 번으로 D-PMS(앱 + MySQL 8.4)를 띄운다. 데모 데이터와 데모 암호화 키를 쓰는 로컬 데모용 구성이며 운영 구성이 아니다.

## 1. 왜 쓰나

| 직접 설치해서 실행 | Docker 로 실행 |
| :--- | :--- |
| JDK 17, MySQL 8 설치 | Docker Desktop 만 설치 |
| 스크립트로 DB 2개·계정 2개·비밀번호 파일 만들기 | `.env` 에 비밀번호 세 개 적기 |
| `mvnw spring-boot:run` | `docker compose up -d --build` |
| PC 마다 버전이 다를 수 있음 | MySQL 8.4.10·JDK 17 이 파일에 고정 (digest) |

- **처음 보는 사람도 바로 실행해 볼 수 있다.** 포트폴리오를 보는 사람이 MySQL 을 설치하고 계정까지 만들어 가며 따라 하기는 어렵다.
- **"내 PC 에서는 되는데"가 줄어든다.** 누구 PC 에서나 같은 이미지·같은 설정으로 뜬다.
- **CI 와 같은 방식이다.** Pull Request 마다 GitHub Actions 가 이 compose 파일로 실제로 띄워 응답을 확인한다 (6절).

## 2. 구성

```text
compose.yaml
 ├ db   MySQL 8.4.10   데이터는 볼륨 db-data. 처음 한 번 docker/mysql/init/01-users.sh 가 계정 둘을 만든다
 └ app  Dockerfile 로 빌드한 D-PMS   local 프로필(데모 데이터), 업로드 파일은 볼륨 uploads, 포트 8080
        db 가 준비됐다는 확인(healthcheck)이 통과한 뒤에 시작하고, 뜰 때 Flyway 가 스키마와 데모 데이터를 넣는다
```

| 파일 | 하는 일 |
| :--- | :--- |
| `Dockerfile` | 1단계: JDK 17 + Maven Wrapper 로 jar 빌드(테스트는 CI 가 따로 함). 2단계: JRE 17 에 jar 만 담고 root 가 아닌 `dpms` 계정으로 실행 |
| `compose.yaml` | 두 컨테이너, DB 준비 확인 뒤 앱 시작, 볼륨, 포트 |
| `docker/mysql/init/01-users.sh` | DB 계정 분리: `dpms_owner`(Flyway, 테이블 변경) / `dpms_app`(서버, 조회·등록·수정·삭제만) |
| `.env.example` | 비밀번호 견본. `.env` 로 복사해 쓴다 (`.env` 는 Git 제외) |
| `.dockerignore` | 빌드에 필요한 `.mvn`, `mvnw`, `pom.xml`, `src` 만 보낸다 |

## 3. 실행

준비: [Docker Desktop](https://www.docker.com/products/docker-desktop/). Windows 는 WSL 2 가 필요하다. 없으면 관리자 PowerShell 에서 `wsl --install --no-distribution` 을 실행하고(재부팅을 요구하면 재부팅) Docker Desktop 을 켠다. 왼쪽 아래가 "Engine running" 이면 준비가 끝난 것이다.

```bash
cp .env.example .env          # PowerShell: Copy-Item .env.example .env
# .env 의 비밀번호 세 개를 바꾼다
docker compose up -d --build
```

- 처음에는 이미지 빌드(Maven 의존성 내려받기) 때문에 몇 분 걸린다. 두 번째부터는 빌드 캐시를 쓴다.
- 웹: http://localhost:8080 (계정은 README 5절)
- 끄기: `docker compose down` (데이터 유지) · 처음부터 다시: `docker compose down -v` (DB·업로드 볼륨까지 지움)

## 4. 자주 쓰는 명령

| 명령 | 뜻 |
| :--- | :--- |
| `docker compose ps` | 컨테이너 상태. db 가 `healthy` 인지 본다 |
| `docker compose logs -f app` | 앱 로그 따라 보기 (Ctrl+C 로 빠져나옴) |
| `docker compose exec db mysql -uroot -p dpms` | 컨테이너 안의 MySQL 접속 (root 비밀번호는 `.env`) |
| `docker compose up -d --build app` | 코드를 바꾼 뒤 앱만 다시 빌드해 띄우기 |
| `docker compose down -v` | 컨테이너와 볼륨 삭제 |

## 5. 알아 둘 점

- **컨테이너끼리는 서비스 이름으로 찾는다.** 앱의 DB 주소는 `localhost` 가 아니라 `db:3306` 이다. 앱 컨테이너 안에서 `localhost` 는 앱 자신이다.
- **DB 포트는 밖으로 열지 않는다.** 이 PC 의 MySQL 이 3306 을 쓰고 있어도 겹치지 않는다. 8080 을 다른 프로그램이 쓰면 `.env` 의 `DPMS_PORT` 를 바꾼다.
- **초기화 스크립트는 데이터 볼륨이 비어 있을 때 한 번만 돈다.** `.env` 의 비밀번호를 바꿨다면 `docker compose down -v` 로 볼륨을 지우고 다시 띄운다.
- **비밀번호는 영문·숫자·하이픈으로.** 초기화 SQL 문자열에 그대로 들어간다.
- **데모용 구성이다.** local 프로필의 데모 암호화 키와 데모 데이터를 쓴다. 운영이라면 키를 환경변수로 넣고 seed 를 빼고, TLS·백업·비밀값 관리를 따로 설계해야 한다.

## 6. 검증

- GitHub Actions `compose` 잡: Pull Request 마다 무작위 비밀번호로 띄워 로그인·메인 화면 응답, 공고 목록의 데모 데이터, 기동 직후 공고 마감 배치 실행 로그를 확인한다 (`.github/workflows/ci.yml`).
