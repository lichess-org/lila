#!/bin/bash

# Lichess Docker Cleanup Script
# This script removes all Docker containers, images, and volumes related to Lichess

set -e

echo "🧹 Starting Lichess Docker Cleanup..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to ask for confirmation
confirm() {
    read -r -p "${1:-Are you sure?} [y/N] " response
    case "$response" in
        [yY][eE][sS]|[yY]) 
            true
            ;;
        *)
            false
            ;;
    esac
}

# Stop all running containers
echo "🛑 Stopping all containers..."
docker-compose down

# Remove containers
if confirm "❓ Remove all Lichess containers?"; then
    echo "🗑️  Removing containers..."
    docker-compose down --remove-orphans
    docker container prune -f
    echo -e "${GREEN}✅ Containers removed${NC}"
fi

# Remove images
if confirm "❓ Remove all Lichess images?"; then
    echo "🗑️  Removing images..."
    
    # Remove compose images
    docker-compose down --rmi all 2>/dev/null || true
    
    # Remove any remaining lichess images
    docker images | grep -E "(lichess|lila)" | awk '{print $3}' | xargs -r docker rmi -f
    
    # Remove dangling images
    docker image prune -f
    
    echo -e "${GREEN}✅ Images removed${NC}"
fi

# Remove volumes
if confirm "❓ Remove all data volumes? (This will delete all database data!)"; then
    echo -e "${RED}⚠️  WARNING: This will permanently delete all database data!${NC}"
    if confirm "❓ Are you absolutely sure?"; then
        echo "🗑️  Removing volumes..."
        docker-compose down --volumes
        docker volume prune -f
        echo -e "${GREEN}✅ Volumes removed${NC}"
    else
        echo -e "${YELLOW}⏭️  Volumes cleanup skipped${NC}"
    fi
fi

# Remove networks
if confirm "❓ Remove custom networks?"; then
    echo "🗑️  Removing networks..."
    docker network prune -f
    echo -e "${GREEN}✅ Networks removed${NC}"
fi

# Remove logs
if confirm "❓ Remove local log files?"; then
    echo "🗑️  Removing log files..."
    rm -rf logs/*
    echo -e "${GREEN}✅ Log files removed${NC}"
fi

# Clean up build cache
if confirm "❓ Remove Docker build cache?"; then
    echo "🗑️  Removing build cache..."
    docker builder prune -f
    echo -e "${GREEN}✅ Build cache removed${NC}"
fi

echo ""
echo -e "${GREEN}🎉 Cleanup completed!${NC}"
echo ""
echo "📊 Current Docker usage:"
docker system df

echo ""
echo "🔄 To start fresh, run:"
echo "  ./docker/scripts/setup.sh"
echo "  docker-compose up --build" 