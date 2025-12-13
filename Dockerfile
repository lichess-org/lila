# Multi-stage Dockerfile for Lila (lichess) suitable for Fly.io
# Stage 1: build UI and backend
FROM ubuntu:24.04 AS builder
ENV DEBIAN_FRONTEND=noninteractive
RUN apt-get update \
  && apt-get install -y --no-install-recommends \
    curl ca-certificates gnupg git unzip build-essential wget ca-certificates \
    apt-transport-https \
  && rm -rf /var/lib/apt/lists/*

# Install Java 21
RUN apt-get update && apt-get install -y --no-install-recommends openjdk-21-jdk && rm -rf /var/lib/apt/lists/*

# Install Node 24
RUN curl -fsSL https://deb.nodesource.com/setup_24.x | bash - \
  && apt-get install -y --no-install-recommends nodejs \
  && npm --version

# Install pnpm (locked to workspace version used by this repo)
RUN npm i -g pnpm@10.4.1

# Install sbt (Scala build tool)
RUN curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x99E82A75642AC823" | gpg --dearmor -o /usr/share/keyrings/sbt.gpg || true
RUN echo "deb [signed-by=/usr/share/keyrings/sbt.gpg] https://repo.scala-sbt.org/scalasbt/debian all main" > /etc/apt/sources.list.d/sbt.list
RUN apt-get update && apt-get install -y --no-install-recommends sbt && rm -rf /var/lib/apt/lists/*

WORKDIR /src
COPY . /src

# Install JS deps and build UI assets
RUN pnpm install --frozen-lockfile || pnpm install
RUN ./ui/build -p || true

# Build Scala/Play application (stage creates runnable script under target/universal/stage)
ENV SBT_OPTS "-Dsbt.log.noformat=true -Dcoursier.progress=false -Dfile.encoding=UTF-8"
RUN set -eux; \
  for i in 1 2 3 4 5; do \
    sbt -batch -DskipTests=true stage && break || { echo "sbt failed, retrying ($i/5)"; sleep $((i*5)); }; \
  done

# Stage 2: runtime image
FROM eclipse-temurin:21-jre-jammy AS runtime
ENV PORT=8080
WORKDIR /app
COPY --from=builder /src/target/universal/stage/ /app
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s CMD curl -f http://localhost:${PORT}/ || exit 1

ENV JAVA_OPTS="-Xms256m -Xmx1g"

# Use shell form so $PORT and other env vars can be expanded at runtime
CMD /app/bin/lila -Dhttp.port=${PORT} $JAVA_OPTS
