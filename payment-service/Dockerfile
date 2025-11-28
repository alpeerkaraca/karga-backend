FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY karga-common/pom.xml karga-common/pom.xml
COPY payment-service/pom.xml payment-service/pom.xml

RUN --mount=type=cache,target=/root/.m2 \
    mvn -f karga-common/pom.xml dependency:go-offline

COPY karga-common/src/ karga-common/src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -f karga-common/pom.xml install -DskipTests

COPY payment-service/src payment-service/src

RUN --mount=type=cache,target=/root/.m2 \
    mvn -f payment-service/pom.xml package -DskipTests



FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/payment-service/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]