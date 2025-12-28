# Build stage
FROM maven:3.9-eclipse-temurin-17 as builder

WORKDIR /build

# Create Maven settings with retry configuration
RUN mkdir -p /root/.m2
RUN echo '<?xml version="1.0" encoding="UTF-8"?>\
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"\
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"\
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0\
                              http://maven.apache.org/xsd/settings-1.0.0.xsd">\
  <mirrors>\
    <mirror>\
      <id>maven-default-http-blocker</id>\
      <mirrorOf>dummy</mirrorOf>\
      <name>Dummy mirror to override default blocking mirror that blocks http</name>\
      <url>http://0.0.0.0/</url>\
    </mirror>\
  </mirrors>\
</settings>' > /root/.m2/settings.xml

# Copy only pom.xml first to cache dependencies
COPY pom.xml .

# Download dependencies separately for better caching
# Use multiple attempts with timeout
RUN --mount=type=cache,target=/root/.m2 \
    for i in 1 2 3 4 5; do \
        echo "Attempt $i to download dependencies..." && \
        timeout 300 mvn dependency:go-offline -B -Dmaven.wagon.http.retryHandler.count=5 \
            -Dmaven.wagon.http.retryHandler.requestSentEnabled=true \
            -Dmaven.wagon.http.pool=false \
            -Dmaven.wagon.httpconnectionManager.ttlSeconds=10 && break || \
        if [ $i -eq 5 ]; then echo "Failed to download dependencies after 5 attempts"; exit 1; fi; \
        echo "Retrying in 10 seconds..." && sleep 10; \
    done

# Now copy source code
COPY src ./src

# Build project with retries
RUN --mount=type=cache,target=/root/.m2 \
    for i in 1 2 3; do \
        echo "Attempt $i to build project..." && \
        mvn clean package -DskipTests -B \
            -Dmaven.wagon.http.retryHandler.count=5 \
            -Dmaven.wagon.http.retryHandler.requestSentEnabled=true && break || \
        if [ $i -eq 3 ]; then echo "Failed to build after 3 attempts"; exit 1; fi; \
        echo "Retrying in 5 seconds..." && sleep 5; \
    done

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

RUN apk add --no-cache curl bash netcat-openbsd

COPY --from=builder /build/target/*.jar app.jar
COPY entrypoint.sh .
RUN chmod +x entrypoint.sh

ENV JAVA_OPTS="-Xmx2g -Xms512m"
ENV SPRING_PROFILES_ACTIVE=prod
ENV DB_HOST=postgres
ENV DB_PORT=5432

EXPOSE 8080 8081

HEALTHCHECK --interval=10s --timeout=5s --retries=5 \
    CMD curl -f http://localhost:8080/api/actuator/health || exit 1

ENTRYPOINT ["./entrypoint.sh"]