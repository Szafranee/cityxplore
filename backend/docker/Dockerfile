FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace/app

COPY build.gradle.kts settings.gradle.kts gradlew ./
COPY gradle gradle

RUN chmod +x gradlew

COPY src src

RUN ./gradlew build -x test --no-daemon

FROM eclipse-temurin:21-jre-jammy
VOLUME /tmp
COPY --from=build /workspace/app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app.jar"]
