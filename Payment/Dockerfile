# ---------- Build stage ----------
FROM eclipse-temurin:21-jdk AS build

WORKDIR /workspace

COPY gradlew ./
COPY gradle ./gradle
COPY build.gradle settings.gradle ./

RUN chmod +x gradlew
RUN ./gradlew dependencies --no-daemon || true

COPY src ./src
RUN ./gradlew clean bootJar -x test --no-daemon

# ---------- Runtime stage ----------
FROM eclipse-temurin:21-jre

ENV TZ=Asia/Seoul \
    JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0"

WORKDIR /app

COPY --from=build /workspace/build/libs/*.jar /app/app.jar

RUN useradd -r -s /sbin/nologin spring && chown -R spring: /app
USER spring

EXPOSE 8082
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
  CMD curl -fsS http://localhost:8082/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS} -jar /app/app.jar"]
