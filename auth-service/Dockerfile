FROM docker.io/library/maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY karga-common/pom.xml karga-common/pom.xml
COPY auth-service/pom.xml auth-service/pom.xml

RUN --mount=type=cache,target=/root/.m2 \
    mvn -f karga-common/pom.xml dependency:go-offline

COPY karga-common/src/ karga-common/src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -f karga-common/pom.xml install -DskipTests

COPY auth-service/src auth-service/src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -f auth-service/pom.xml package -DskipTests

ADD https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar /opentelemetry-javaagent.jar

# (LAYERING)
WORKDIR /app/auth-service/target
RUN java -Djarmode=tools -jar auth-service.jar extract --layers --launcher --destination extracted


FROM docker.io/library/eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /app/auth-service/target/extracted/dependencies/ /app/
COPY --from=build /app/auth-service/target/extracted/spring-boot-loader/ /app/
COPY --from=build /app/auth-service/target/extracted/snapshot-dependencies/ /app/
COPY --from=build /app/auth-service/target/extracted/application/ /app/
COPY --from=build /opentelemetry-javaagent.jar /app/agent.jar
ENTRYPOINT ["java", "-javaagent:/app/agent.jar", "org.springframework.boot.loader.launch.JarLauncher"]