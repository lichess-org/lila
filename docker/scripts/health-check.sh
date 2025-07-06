#!/bin/bash

# Lichess Docker Health Check Script
# This script checks the health of all Lichess services

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to check service health
check_service() {
    local service=$1
    local url=$2
    local expected_status=${3:-200}
    
    echo -n "  $service: "
    
    if curl -s -o /dev/null -w "%{http_code}" "$url" | grep -q "$expected_status"; then
        echo -e "${GREEN}✅ Healthy${NC}"
        return 0
    else
        echo -e "${RED}❌ Unhealthy${NC}"
        return 1
    fi
}

# Function to check container status
check_container() {
    local container=$1
    local service_name=$2
    
    echo -n "  $service_name: "
    
    if docker inspect "$container" &>/dev/null; then
        local status=$(docker inspect --format='{{.State.Status}}' "$container" 2>/dev/null)
        local health=$(docker inspect --format='{{.State.Health.Status}}' "$container" 2>/dev/null)
        
        if [ "$status" = "running" ]; then
            if [ "$health" = "healthy" ] || [ "$health" = "<no value>" ]; then
                echo -e "${GREEN}✅ Running${NC}"
                return 0
            else
                echo -e "${YELLOW}⚠️  Running but unhealthy${NC}"
                return 1
            fi
        else
            echo -e "${RED}❌ Not running ($status)${NC}"
            return 1
        fi
    else
        echo -e "${RED}❌ Container not found${NC}"
        return 1
    fi
}

# Function to check port availability
check_port() {
    local host=$1
    local port=$2
    local service_name=$3
    
    echo -n "  $service_name (port $port): "
    
    # Try nc first, then fallback to other methods
    if command -v nc >/dev/null 2>&1; then
        if nc -z "$host" "$port" 2>/dev/null; then
            echo -e "${GREEN}✅ Available${NC}"
            return 0
        fi
    elif command -v telnet >/dev/null 2>&1; then
        # Fallback to telnet for systems without nc
        if timeout 2 telnet "$host" "$port" </dev/null >/dev/null 2>&1; then
            echo -e "${GREEN}✅ Available${NC}"
            return 0
        fi
    elif command -v curl >/dev/null 2>&1; then
        # Last resort: try curl
        if curl -s --connect-timeout 2 "$host:$port" >/dev/null 2>&1; then
            echo -e "${GREEN}✅ Available${NC}"
            return 0
        fi
    fi
    
    echo -e "${RED}❌ Unavailable${NC}"
    return 1
}

echo -e "${BLUE}🏥 Lichess Docker Health Check${NC}"
echo ""

# Check if docker-compose is available
if ! command -v docker-compose &> /dev/null; then
    echo -e "${RED}❌ Docker Compose is not installed${NC}"
    exit 1
fi

# Check overall docker-compose status
echo -e "${BLUE}📊 Service Status:${NC}"
docker-compose ps
echo ""

# Check individual containers
echo -e "${BLUE}🐳 Container Health:${NC}"
check_container "lichess-mongodb" "MongoDB"
check_container "lichess-redis" "Redis"
check_container "lichess-elasticsearch" "Elasticsearch"
check_container "lichess-app" "Lichess App"
check_container "lichess-nginx" "Nginx"

# Check development services if they exist
if docker inspect lichess-mongo-express &>/dev/null; then
    check_container "lichess-mongo-express" "MongoDB Express"
fi
if docker inspect lichess-redis-commander &>/dev/null; then
    check_container "lichess-redis-commander" "Redis Commander"
fi
if docker inspect lichess-kibana &>/dev/null; then
    check_container "lichess-kibana" "Kibana"
fi
if docker inspect lichess-mailhog &>/dev/null; then
    check_container "lichess-mailhog" "Mailhog"
fi

echo ""

# Check service endpoints
echo -e "${BLUE}🌐 Service Endpoints:${NC}"
check_service "Lichess App" "http://localhost:9663/api/status"
check_service "Nginx" "http://localhost:80/health"
check_service "MongoDB Express" "http://localhost:8081" "200"
check_service "Redis Commander" "http://localhost:8082" "200"
check_service "Elasticsearch" "http://localhost:9200/_cluster/health"
check_service "Kibana" "http://localhost:5601/api/status" "200"
check_service "Mailhog" "http://localhost:8025" "200"

echo ""

# Check port connectivity
echo -e "${BLUE}🔌 Port Connectivity:${NC}"
check_port "localhost" "9663" "Lichess App"
check_port "localhost" "80" "Nginx"
check_port "localhost" "27017" "MongoDB"
check_port "localhost" "6379" "Redis"
check_port "localhost" "9200" "Elasticsearch"

echo ""

# Check disk usage
echo -e "${BLUE}💾 Disk Usage:${NC}"
docker system df

echo ""

# Check logs for recent errors
echo -e "${BLUE}📋 Recent Errors (last 50 lines):${NC}"
echo "Checking for errors in logs..."

# Check Lichess app logs for errors
if docker logs lichess-app --tail=50 2>&1 | grep -i "error\|exception\|failed" | head -5; then
    echo -e "${YELLOW}⚠️  Found errors in Lichess app logs${NC}"
else
    echo -e "${GREEN}✅ No recent errors in Lichess app logs${NC}"
fi

# Check MongoDB logs for errors
if docker logs lichess-mongodb --tail=50 2>&1 | grep -i "error\|exception\|failed" | head -5; then
    echo -e "${YELLOW}⚠️  Found errors in MongoDB logs${NC}"
else
    echo -e "${GREEN}✅ No recent errors in MongoDB logs${NC}"
fi

echo ""

# Summary
echo -e "${BLUE}📈 Health Check Summary:${NC}"
echo "For detailed logs, run: ./docker/scripts/logs.sh"
echo "To restart services: docker-compose restart"
echo "To view service details: docker-compose ps"
echo ""
echo -e "${GREEN}🎉 Health check completed!${NC}" 