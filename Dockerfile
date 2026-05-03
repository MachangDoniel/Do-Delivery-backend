# Multi-stage Dockerfile: build with Maven, run with a slim JRE

FROM maven:3.9-eclipse-temurin-21 as builder
WORKDIR /workspace

# Copy pom and source, then build the fat jar
COPY pom.xml ./
COPY src ./src

# Use non-interactive, skip tests to speed up CI builds
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy the built jar (pom.xml sets <finalName>app → target/app.jar)
COPY --from=builder /workspace/target/app.jar ./app.jar

EXPOSE 8080
ENV JAVA_OPTS="-Xms128m -Xmx512m"
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
