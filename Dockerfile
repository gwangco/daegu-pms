# syntax=docker/dockerfile:1
# D-PMS 실행 이미지 (docs/DOCKER.md).
# 1단계: JDK 17 과 Maven Wrapper 로 jar 를 만든다. 테스트는 CI 가 MySQL 로 따로 돌리므로 여기서는 건너뛴다.
# 2단계: JRE 17 에 jar 만 담고 root 가 아닌 계정으로 실행한다.
# 베이스 이미지는 태그와 digest 를 함께 적어 고정한다 (같은 태그에 다른 이미지가 올라와도 영향 없음).

FROM eclipse-temurin:17-jdk@sha256:fe947d993c32dd9cf651a4353a6ed723c2370b3a9d034ea4cab4775404ea531a AS build
WORKDIR /workspace
# Maven Wrapper 는 unzip 이 없으면 .tar.gz 배포본을 받는데, 체크섬(distributionSha256Sum)은 .zip 기준이라 검증에 실패한다.
# 그래서 unzip 을 먼저 설치해 .zip 을 받게 한다 (체크섬 검증은 그대로 유지)
RUN apt-get update \
    && apt-get install -y --no-install-recommends unzip \
    && rm -rf /var/lib/apt/lists/*
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY src/ src/
# Maven 저장소는 빌드 캐시에 두어 다시 빌드할 때 의존성을 새로 받지 않는다
RUN --mount=type=cache,target=/root/.m2 \
    chmod +x mvnw \
    && ./mvnw -B -ntp -DskipTests package \
    && cp target/daegu-pms-*.jar /workspace/app.jar

FROM eclipse-temurin:17-jre@sha256:e04fa72be13b257a59fdec6dc14af78c8f80f035d3c1ed4e03e45552a231d4c8
WORKDIR /app
RUN useradd --system --uid 10001 --no-create-home dpms \
    && mkdir -p /app/uploads \
    && chown dpms /app/uploads
COPY --from=build /workspace/app.jar /app/app.jar
USER dpms
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
