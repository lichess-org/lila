# Multi-stage Dockerfile for Randolila (Lila)
# Builds UI with Node/PNPM and the Scala Play backend with sbt, produces a lightweight runtime image.

# 1) UI builder (Node 24 + pnpm)
FROM docker.io/library/node:24-bullseye-slim AS ui-builder
WORKDIR /workspace
COPY package.json pnpm-lock.yaml pnpm-workspace.yaml ./
RUN npm install -g pnpm@10.4.1
COPY . .
RUN pnpm install --frozen-lockfile
RUN chmod +x ./ui/build || true
# Build all UI packages; try with --no-install first for caching, fallback to plain build
RUN ./ui/build --no-install || ./ui/build


# 2) sbt builder (Temurin 21 + sbt)
FROM docker.io/eclipse-temurin:21-jdk-jammy AS sbt-builder
WORKDIR /workspace
ENV DEBIAN_FRONTEND=noninteractive
RUN apt-get update && \
    apt-get install -y --no-install-recommends curl gnupg ca-certificates dirmngr && \
    curl -sL https://repo.scala-sbt.org/scalasbt/debian/sbt.gpg | gpg --dearmor -o /usr/share/keyrings/sbt-archive-keyring.gpg && \
    echo "deb [signed-by=/usr/share/keyrings/sbt-archive-keyring.gpg] https://repo.scala-sbt.org/scalasbt/debian all main" > /etc/apt/sources.list.d/sbt.list && \
    apt-get update && apt-get install -y --no-install-recommends sbt && \
    apt-get purge -y --auto-remove dirmngr && apt-get clean && rm -rf /var/lib/apt/lists/* /var/cache/apt/*

# Copy full workspace (includes UI artifacts from ui-builder)
COPY --from=ui-builder /workspace /workspace
ENV JAVA_TOOL_OPTIONS="-Xms512m -Xmx2g"
RUN sbt -batch -no-colors clean stage


# 3) Runtime image (lightweight Temurin 21 JRE)
FROM docker.io/eclipse-temurin:21-jre-jammy
WORKDIR /opt/lila
ENV PORT=9000
# Copy staged distribution produced by `sbt stage`
COPY --from=sbt-builder /workspace/target/universal/stage /opt/lila
EXPOSE 9000
# Run as non-root user where possible (UID/GID 1000 is a common default)
USER 1000:1000
# Entrypoint: start Play app on $PORT
CMD ["bin/lila", "-Dhttp.port=${PORT}", "-J-XX:MaxRAMPercentage=75.0"]
