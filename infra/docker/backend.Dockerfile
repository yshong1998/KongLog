# 백엔드(Spring Boot) 이미지. 저장소 루트에서 만든다. 마지막 인자 backend가 빌드 대상 폴더다.
#   docker build -f infra/docker/backend.Dockerfile -t konglog-backend:local backend
# 띄울 때 환경 변수 DB_URL, DB_USERNAME, DB_PASSWORD로 DB를 정한다.
# Java가 쓸 메모리 상한은 JAVA_TOOL_OPTIONS로 정한다. 방법은 infra/README.md에 있다.
# 이미지에 넣지 않을 파일은 옆의 backend.Dockerfile.dockerignore에 적는다.

# 1단계: 실행 파일(jar)을 만든다. 소스 코드와 Gradle은 이 단계에만 있고 최종 이미지에는 들어가지 않는다.
FROM eclipse-temurin:25-jdk-noble AS build
WORKDIR /workspace

# 빌드 설정만 먼저 복사해 의존성을 받는다. 코드만 바뀐 빌드에서는 이 단계를 다시 하지 않는다.
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon dependencies --configuration runtimeClasspath > /dev/null

COPY src src
# 테스트는 여기서 돌리지 않는다. DB가 필요해서 자동 검사(.github/workflows/ci.yml)가 맡는다.
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon bootJar

# jar를 자주 바뀌는 부분(우리 코드)과 거의 안 바뀌는 부분(라이브러리)으로 나눈다.
# 코드만 바뀐 배포에서는 서버가 작은 조각만 새로 받는다.
# 파일 이름에 버전이 붙어 나오므로(konglog-backend-0.0.1-SNAPSHOT.jar) 고정된 이름으로 바꾼 뒤에 나눈다.
RUN cp build/libs/*.jar application.jar \
    && java -Djarmode=tools -jar application.jar extract --layers --destination extracted

# 2단계: 실행에 필요한 것만 담는다. JDK가 아니라 실행 전용인 JRE를 쓴다.
FROM eclipse-temurin:25-jre-noble
WORKDIR /app

# root가 아닌 계정으로 실행한다.
RUN useradd --system --no-create-home --shell /usr/sbin/nologin konglog
USER konglog

# 거의 안 바뀌는 것부터 복사한다.
COPY --from=build /workspace/extracted/dependencies/ ./
COPY --from=build /workspace/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/extracted/application/ ./

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "application.jar"]
