# Multi-stage build for lila-ws
FROM eclipse-temurin:21-jdk-jammy as builder

# Install system dependencies
RUN apt-get update && apt-get install -y \
    curl \
    unzip \
    git \
    && rm -rf /var/lib/apt/lists/*

# Install SBT
ENV SBT_VERSION=1.9.8
RUN curl -L "https://github.com/sbt/sbt/releases/download/v${SBT_VERSION}/sbt-${SBT_VERSION}.tgz" | tar -xz -C /opt/ \
    && ln -s /opt/sbt/bin/sbt /usr/local/bin/sbt

# Set working directory
WORKDIR /app

# Clone lila-ws repository
RUN git clone https://github.com/lichess-org/lila-ws.git . \
    && git checkout master

# Set SBT options for better memory management
ENV SBT_OPTS="-Xmx2g -Xms1g -XX:+UseG1GC"

# Compile the application
RUN sbt compile stage

# Runtime stage
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Copy the staged application from builder
COPY --from=builder /app/target/universal/stage/ ./

# Create application configuration
COPY application.conf ./conf/

# Expose port
EXPOSE 9664

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:9664/ || exit 1

# Run the application
CMD ["./bin/lila-ws"] 