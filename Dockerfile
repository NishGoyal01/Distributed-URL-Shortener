FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends curl \
	&& groupadd --system app \
	&& useradd --system --gid app --home-dir /app app \
	&& rm -rf /var/lib/apt/lists/*
COPY --from=builder --chown=app:app /workspace/target/url-shortener-*.jar /app/app.jar
EXPOSE 10000
USER app
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
