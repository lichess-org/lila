#!/bin/bash

# Lichess Docker Setup Script
# This script prepares the environment for running Lichess in Docker

set -e

echo "🏁 Starting Lichess Docker Setup..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if Docker is installed
if ! command -v docker &> /dev/null; then
    echo -e "${RED}❌ Docker is not installed. Please install Docker first.${NC}"
    exit 1
fi

# Check if Docker Compose is installed
if ! command -v docker-compose &> /dev/null; then
    echo -e "${RED}❌ Docker Compose is not installed. Please install Docker Compose first.${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Docker and Docker Compose are installed${NC}"

# Create necessary directories
echo "📁 Creating necessary directories..."
mkdir -p logs/nginx
mkdir -p docker/mongodb/conf
mkdir -p docker/nginx
mkdir -p tmp

# Make scripts executable
echo "🔧 Making scripts executable..."
chmod +x docker/scripts/*.sh

# Create .env file if it doesn't exist
if [ ! -f .env ]; then
    echo "📄 Creating .env file..."
    cat > .env << EOF
# Lichess Docker Environment Configuration

# Database Configuration
MONGODB_URI=mongodb://lichess:password@mongodb:27017/lichess?authSource=admin
REDIS_URI=redis://:password@redis:6379

# Application Configuration
NET_DOMAIN=localhost:9663
NET_BASE_URL=http://localhost:9663
ELASTICSEARCH_URL=http://elasticsearch:9200

# Java Configuration
JAVA_OPTS=-Xmx2g -Xms1g -XX:+UseG1GC

# Development Options
COMPILE_ASSETS=false
ELASTICSEARCH_ENABLED=true
DEBUG_MODE=false
EOF
    echo -e "${GREEN}✅ Created .env file${NC}"
else
    echo -e "${YELLOW}⚠️  .env file already exists${NC}"
fi

# Check if we need to build UI assets
if [ -d "ui" ] && [ -f "ui/package.json" ]; then
    echo -e "${YELLOW}📦 UI assets detected. You may need to build them before starting.${NC}"
    echo "Run: ./docker/scripts/build-ui.sh"
fi

# Check available system resources
echo "🔍 Checking system resources..."

# Check OS type and use appropriate commands
if [[ "$OSTYPE" == "darwin"* ]]; then
    # macOS
    TOTAL_MEM=$(system_profiler SPHardwareDataType | grep "Memory:" | awk '{print $2}' | sed 's/GB//')
    AVAILABLE_DISK=$(df -h . | awk 'NR==2{print $4}' | sed 's/G.*//' | sed 's/\..*//')
elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
    # Linux
    TOTAL_MEM=$(free -g | awk 'NR==2{print $2}')
    AVAILABLE_DISK=$(df -BG . | awk 'NR==2{print $4}' | sed 's/G//')
else
    # Unknown OS, skip resource check
    TOTAL_MEM=8
    AVAILABLE_DISK=50
    echo -e "${YELLOW}⚠️  Unknown OS, skipping resource check${NC}"
fi

# Check memory (only if we got a valid value)
if [ -n "$TOTAL_MEM" ] && [ "$TOTAL_MEM" -gt 0 ] 2>/dev/null; then
    if [ "$TOTAL_MEM" -lt 4 ]; then
        echo -e "${YELLOW}⚠️  Warning: Less than 4GB RAM available ($TOTAL_MEM GB). Consider reducing Java heap size.${NC}"
    else
        echo -e "${GREEN}✅ Memory: $TOTAL_MEM GB${NC}"
    fi
else
    echo -e "${YELLOW}⚠️  Could not determine available memory${NC}"
fi

# Check disk space (only if we got a valid value)
if [ -n "$AVAILABLE_DISK" ] && [ "$AVAILABLE_DISK" -gt 0 ] 2>/dev/null; then
    if [ "$AVAILABLE_DISK" -lt 20 ]; then
        echo -e "${YELLOW}⚠️  Warning: Less than 20GB disk space available ($AVAILABLE_DISK GB).${NC}"
    else
        echo -e "${GREEN}✅ Disk space: $AVAILABLE_DISK GB available${NC}"
    fi
else
    echo -e "${YELLOW}⚠️  Could not determine available disk space${NC}"
fi

echo -e "${GREEN}✅ Setup completed successfully!${NC}"
echo ""
echo "🚀 Quick Start Commands:"
echo "  Start all services:     docker-compose up --build"
echo "  Start with dev tools:   docker-compose -f docker-compose.yml -f docker-compose.override.yml up"
echo "  Build UI assets:        ./docker/scripts/build-ui.sh"
echo "  View logs:              docker-compose logs -f"
echo "  Stop all services:      docker-compose down"
echo ""
echo "🌐 Access URLs:"
echo "  Main application:       http://localhost:9663"
echo "  Nginx proxy:            http://localhost:80"
echo "  MongoDB Express:        http://localhost:8081 (admin/admin)"
echo "  Redis Commander:        http://localhost:8082"
echo "  Kibana:                 http://localhost:5601"
echo "  Mailhog:                http://localhost:8025"
echo ""
echo "📚 For detailed documentation, see README.Docker.md" 