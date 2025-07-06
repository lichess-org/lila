#!/bin/bash

# Lichess Docker startup script

set -e

echo "Starting Lichess application..."

# Function to check if a service is ready
wait_for_service() {
    local host=$1
    local port=$2
    local service_name=$3
    
    echo "Waiting for $service_name to be ready..."
    while true; do
        if command -v nc >/dev/null 2>&1 && nc -z "$host" "$port" 2>/dev/null; then
            break
        elif command -v telnet >/dev/null 2>&1 && timeout 2 telnet "$host" "$port" </dev/null >/dev/null 2>&1; then
            break
        elif command -v curl >/dev/null 2>&1 && curl -s --connect-timeout 1 "$host:$port" >/dev/null 2>&1; then
            break
        fi
        sleep 1
    done
    echo "$service_name is ready!"
}

# Wait for MongoDB to be ready
wait_for_service mongodb 27017 "MongoDB"

# Wait for Redis to be ready
wait_for_service redis 6379 "Redis"

# Wait for Elasticsearch to be ready (if enabled)
if [ "$ELASTICSEARCH_ENABLED" = "true" ]; then
    echo "Waiting for Elasticsearch to be ready..."
    while ! curl -f http://elasticsearch:9200/_cluster/health > /dev/null 2>&1; do
        sleep 1
    done
    echo "Elasticsearch is ready!"
fi

# Create necessary directories
mkdir -p /app/logs
mkdir -p /app/tmp

# Check if we need to compile assets
if [ "$COMPILE_ASSETS" = "true" ]; then
    echo "Compiling assets..."
    cd /app
    # This would require UI build tools to be installed
    # For now, we assume assets are pre-compiled
    echo "Assets compilation skipped in this container build"
fi

# Set JVM options
JAVA_OPTS="${JAVA_OPTS:-"-Xmx2g -Xms1g -XX:+UseG1GC"}"

# Set configuration file
CONFIG_FILE="${CONFIG_FILE:-"conf/application.conf"}"

# Start the application
echo "Starting Lichess with configuration: $CONFIG_FILE"
echo "JVM Options: $JAVA_OPTS"

exec java $JAVA_OPTS \
    -Dconfig.file=$CONFIG_FILE \
    -Dlogger.file=conf/logger.dev.xml \
    -Duser.dir=/app \
    -cp "/app/lib/*" \
    lila.app.Lila 