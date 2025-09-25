FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -q -e -B -DskipTests dependency:go-offline
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -q -e -B -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
ENV JAVA_OPTS="-Xms256m -Xmx512m"
COPY --from=build /app/target/rag-chat-backend-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -jar app.jar" ]
