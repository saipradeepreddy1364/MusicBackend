FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app
COPY backend/pom.xml .
RUN mvn dependency:go-offline -q
COPY backend/src ./src
RUN mvn clean package -DskipTests -q

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN apt-get update && apt-get install -y python3 python3-pip python3-venv curl && \
    python3 -m venv /opt/venv && \
    /opt/venv/bin/pip install --upgrade pip yt-dlp && \
    rm -rf /var/lib/apt/lists/*
COPY --from=builder /app/target/music-player-backend-*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-8080} -jar app.jar"]