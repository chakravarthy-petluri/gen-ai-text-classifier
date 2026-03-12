# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /usr/src/app
COPY . ./
RUN ./mvnw package -DskipTests -q

# Stage 2: Runtime - Use distroless Java 17 (minimal, no shell, no package manager)
# Distroless images contain ONLY the application and runtime dependencies
FROM gcr.io/distroless/java17-debian12:nonroot

# Copy JAR from builder stage
COPY --from=builder /usr/src/app/target/gen-ai-text-classifier-0.0.1-SNAPSHOT.jar /app/app.jar

# Copy security policy
COPY --from=builder /usr/src/app/java.security /app/java.security

# Set working directory
WORKDIR /app

# User is already 'nonroot' (UID 65532) in distroless image
# No need to create users or set permissions - distroless handles this

EXPOSE 8080

# Security: Run with hardened Java options
# - G1GC: Optimized garbage collector
# - Memory limits: Use 75% of container memory
# - Exit on OOM: Prevent zombie processes
# - Secure random: Use /dev/urandom
# - Security policy: Restrict dangerous operations
# - Remove confidential data: Don't expose Java version
ENTRYPOINT ["java", \
  "-XX:+UseG1GC", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:+ExitOnOutOfMemoryError", \
  "-XX:+DisableExplicitGC", \
  "-Djava.security.egd=file:/dev/urandom", \
  "-Djava.security.properties=/app/java.security", \
  "-XX:-PrintCommandLineFlags", \
  "-XX:+ParallelRefProcEnabled", \
  "-Djdk.httpclient.connectionPoolSize=20", \
  "-Dcom.sun.jndi.rmiRegistry.object.trustURLCodebase=false", \
  "-Dcom.sun.jndi.ldap.object.trustURLCodebase=false", \
  "-Dspring.xml.ignore=true", \
  "-jar", "/app/app.jar"]