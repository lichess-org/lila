# Multi-stage build for Lichess
FROM eclipse-temurin:21-jdk-jammy AS builder

# Install required packages and SBT
RUN apt-get update && apt-get install -y \
    curl \
    git \
    gnupg \
    unzip \
    && echo "deb https://repo.scala-sbt.org/scalasbt/debian all main" | tee /etc/apt/sources.list.d/sbt.list \
    && echo "deb https://repo.scala-sbt.org/scalasbt/debian /" | tee /etc/apt/sources.list.d/sbt_old.list \
    && curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | gpg --no-default-keyring --keyring gnupg-ring:/etc/apt/trusted.gpg.d/scalasbt-release.gpg --import \
    && chmod 644 /etc/apt/trusted.gpg.d/scalasbt-release.gpg \
    && apt-get update \
    && apt-get install -y sbt \
    && rm -rf /var/lib/apt/lists/*

# Install Node.js 22.x and pnpm for UI building
RUN curl -fsSL https://deb.nodesource.com/setup_22.x | bash - \
    && apt-get install -y nodejs \
    && npm install -g pnpm@latest

# Set working directory
WORKDIR /app

# Copy build files
COPY build.sbt .
COPY project/ project/
COPY translation/ translation/

# Download dependencies (cached layer)
RUN export SBT_OPTS="-Xmx4g -Xms2g -XX:+UseG1GC -XX:MaxGCPauseMillis=100" && sbt update

# Copy source code
COPY . .

# Build UI assets
RUN echo "Building UI assets..." && \
    pnpm install && \
    git init && \
    git config user.email "docker@lichess.org" && \
    git config user.name "Docker Build" && \
    git add . && \
    git commit -m "Docker build" && \
    ./ui/build -p && \
    echo "UI build completed, checking output..." && \
    ls -la public/ && \
    ls -la public/css/ || echo "No CSS directory" && \
    ls -la public/compiled/ || echo "No compiled directory" && \
    find public -name "*.css" -o -name "*.js" | head -20

# Build the application
RUN export SBT_OPTS="-Xmx4g -Xms2g -XX:+UseG1GC -XX:MaxGCPauseMillis=100" && sbt Universal/packageBin

# Extract the built application
RUN cd target/universal && \
    ls -la && \
    unzip -q lila-*.zip && \
    ls -la && \
    mkdir -p /app/lila-dist && \
    mv lila-*/* /app/lila-dist/

# Production stage
FROM eclipse-temurin:21-jre-jammy

# Install required packages
RUN apt-get update && apt-get install -y \
    curl \
    unzip \
    git \
    && rm -rf /var/lib/apt/lists/*

# Create app user
RUN useradd -m -u 1000 lichess

# Set working directory
WORKDIR /app

# Copy built application from builder stage
COPY --from=builder /app/lila-dist .
COPY --from=builder /app/conf ./conf
COPY --from=builder /app/public ./public
COPY --from=builder /app/translation ./translation

# Ensure compiled assets directories exist and are copied if available
RUN mkdir -p ./public/css ./public/compiled ./public/hashed

# Fix permissions
RUN chown -R lichess:lichess /app

# Create logs directory
RUN mkdir -p /app/logs && chown lichess:lichess /app/logs

# Switch to app user
USER lichess

# Expose port
EXPOSE 9663

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:9663/api/status || exit 1

# Start the application
CMD ["./bin/lila", "-Dconfig.file=conf/application.conf"] 