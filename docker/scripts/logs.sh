#!/bin/bash

# Lichess Docker Log Viewer Script
# This script provides convenient log viewing for Lichess services

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to display help
show_help() {
    echo "Lichess Docker Log Viewer"
    echo ""
    echo "Usage: $0 [OPTIONS] [SERVICE]"
    echo ""
    echo "Services:"
    echo "  all           - All services (default)"
    echo "  lichess       - Main Lichess application"
    echo "  mongodb       - MongoDB database"
    echo "  redis         - Redis cache"
    echo "  elasticsearch - Elasticsearch search"
    echo "  nginx         - Nginx web server"
    echo "  mongo-express - MongoDB web interface"
    echo "  redis-commander - Redis web interface"
    echo "  kibana        - Kibana web interface"
    echo "  mailhog       - Email testing service"
    echo ""
    echo "Options:"
    echo "  -f, --follow  - Follow log output"
    echo "  -t, --tail N  - Show last N lines (default: 100)"
    echo "  -s, --since   - Show logs since timestamp (e.g., 2023-01-01, 1h)"
    echo "  -h, --help    - Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0                    - Show last 100 lines from all services"
    echo "  $0 -f lichess         - Follow Lichess application logs"
    echo "  $0 -t 50 mongodb      - Show last 50 lines from MongoDB"
    echo "  $0 -s 1h nginx        - Show Nginx logs from last hour"
    echo "  $0 --since 2023-01-01 - Show logs since specific date"
}

# Default values
SERVICE="all"
FOLLOW=false
TAIL=100
SINCE=""

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -f|--follow)
            FOLLOW=true
            shift
            ;;
        -t|--tail)
            TAIL="$2"
            shift 2
            ;;
        -s|--since)
            SINCE="$2"
            shift 2
            ;;
        -h|--help)
            show_help
            exit 0
            ;;
        -*)
            echo "Unknown option: $1"
            show_help
            exit 1
            ;;
        *)
            SERVICE="$1"
            shift
            ;;
    esac
done

# Check if docker-compose is available
if ! command -v docker-compose &> /dev/null; then
    echo -e "${RED}❌ Docker Compose is not installed${NC}"
    exit 1
fi

# Check if services are running
if ! docker-compose ps | grep -q "Up"; then
    echo -e "${YELLOW}⚠️  No services appear to be running${NC}"
    echo "Start services with: docker-compose up"
    exit 1
fi

# Build docker-compose command
CMD="docker-compose logs"

# Add tail option
if [ "$TAIL" != "all" ]; then
    CMD="$CMD --tail=$TAIL"
fi

# Add since option
if [ -n "$SINCE" ]; then
    CMD="$CMD --since=$SINCE"
fi

# Add follow option
if [ "$FOLLOW" = true ]; then
    CMD="$CMD --follow"
fi

# Add service name
if [ "$SERVICE" != "all" ]; then
    CMD="$CMD $SERVICE"
fi

# Display header
echo -e "${BLUE}📋 Lichess Docker Logs${NC}"
echo -e "${BLUE}Service: ${SERVICE}${NC}"
if [ "$FOLLOW" = true ]; then
    echo -e "${BLUE}Mode: Following (Press Ctrl+C to stop)${NC}"
else
    echo -e "${BLUE}Mode: Static (last $TAIL lines)${NC}"
fi
if [ -n "$SINCE" ]; then
    echo -e "${BLUE}Since: $SINCE${NC}"
fi
echo ""

# Execute the command
echo -e "${GREEN}Executing: $CMD${NC}"
echo "----------------------------------------"
eval $CMD 