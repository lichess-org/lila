# Lichess Docker Makefile
# Convenient commands for managing Lichess Docker deployment

.PHONY: help setup build up down logs health clean rebuild restart

# Default target
help:
	@echo "Lichess Docker Commands"
	@echo ""
	@echo "Setup & Build:"
	@echo "  setup     - Initialize environment and create config files"
	@echo "  build     - Build Docker images"
	@echo "  assets    - Build UI assets"
	@echo ""
	@echo "Running:"
	@echo "  up        - Start all services"
	@echo "  up-dev    - Start with development services"
	@echo "  down      - Stop all services"
	@echo "  restart   - Restart all services"
	@echo "  rebuild   - Rebuild and restart all services"
	@echo ""
	@echo "Monitoring:"
	@echo "  logs      - View logs from all services"
	@echo "  logs-f    - Follow logs from all services"
	@echo "  health    - Check health of all services"
	@echo "  ps        - Show running containers"
	@echo ""
	@echo "Maintenance:"
	@echo "  clean     - Remove containers and images"
	@echo "  clean-all - Remove everything including data"
	@echo "  backup    - Backup database"
	@echo "  restore   - Restore database"
	@echo ""
	@echo "Development:"
	@echo "  shell     - Open shell in main container"
	@echo "  mongo     - Open MongoDB shell"
	@echo "  redis     - Open Redis CLI"

# Setup and initialization
setup:
	@echo "🏁 Setting up Lichess Docker environment..."
	@chmod +x docker/scripts/*.sh
	@./docker/scripts/setup.sh

# Build images
build:
	@echo "🔨 Building Docker images..."
	@docker-compose build

# Build UI assets
assets:
	@echo "📦 Building UI assets..."
	@./docker/scripts/build-ui.sh

# Start services
up:
	@echo "🚀 Starting Lichess services..."
	@docker-compose up -d

# Start with development services
up-dev:
	@echo "🚀 Starting Lichess with development services..."
	@docker-compose -f docker-compose.yml -f docker-compose.override.yml up -d

# Stop services
down:
	@echo "🛑 Stopping Lichess services..."
	@docker-compose down

# Restart services
restart:
	@echo "🔄 Restarting Lichess services..."
	@docker-compose restart

# Rebuild and restart
rebuild:
	@echo "🔄 Rebuilding and restarting Lichess services..."
	@docker-compose down
	@docker-compose build
	@docker-compose up -d

# View logs
logs:
	@./docker/scripts/logs.sh

# Follow logs
logs-f:
	@./docker/scripts/logs.sh -f

# Health check
health:
	@./docker/scripts/health-check.sh

# Show running containers
ps:
	@docker-compose ps

# Development shell access
shell:
	@echo "🐚 Opening shell in Lichess container..."
	@docker-compose exec lichess /bin/bash

# MongoDB shell
mongo:
	@echo "🐚 Opening MongoDB shell..."
	@docker-compose exec mongodb mongosh -u lichess -p password lichess --authenticationDatabase admin

# Redis CLI
redis:
	@echo "🐚 Opening Redis CLI..."
	@docker-compose exec redis redis-cli -a password

# Backup database
backup:
	@echo "💾 Backing up MongoDB database..."
	@mkdir -p backups
	@docker-compose exec mongodb mongodump --uri="mongodb://lichess:password@localhost:27017/lichess?authSource=admin" --out=/backup
	@docker cp lichess-mongodb:/backup/lichess ./backups/lichess-$(shell date +%Y%m%d_%H%M%S)
	@echo "✅ Backup completed in backups/ directory"

# Restore database
restore:
	@echo "📁 Available backups:"
	@ls -la backups/ | grep lichess || echo "No backups found"
	@echo "To restore, run: docker-compose exec mongodb mongorestore --uri=\"mongodb://lichess:password@localhost:27017/lichess?authSource=admin\" /backup/lichess-TIMESTAMP"

# Clean up
clean:
	@echo "🧹 Cleaning up Lichess Docker environment..."
	@./docker/scripts/cleanup.sh

# Clean everything
clean-all:
	@echo "🧹 Removing all Lichess Docker data..."
	@./docker/scripts/cleanup.sh

# Show service URLs
urls:
	@echo "🌐 Lichess Service URLs:"
	@echo "  Main application:    http://localhost:9663"
	@echo "  Nginx proxy:         http://localhost:80"
	@echo "  MongoDB Express:     http://localhost:8081 (admin/admin)"
	@echo "  Redis Commander:     http://localhost:8082"
	@echo "  Kibana:              http://localhost:5601"
	@echo "  Mailhog:             http://localhost:8025"

# Development helpers
dev-setup: setup assets up-dev
	@echo "🎉 Development environment ready!"
	@make urls

# Production setup
prod-setup: setup build up
	@echo "🎉 Production environment ready!"
	@make urls

# Update images
update:
	@echo "📱 Updating Docker images..."
	@docker-compose pull
	@docker-compose up -d

# Show resource usage
stats:
	@echo "📊 Resource Usage:"
	@docker stats --no-stream

# View configuration
config:
	@echo "⚙️  Docker Compose Configuration:"
	@docker-compose config 