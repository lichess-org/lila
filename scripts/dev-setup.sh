#!/usr/bin/env bash
set -euo pipefail

# Lichess Development Setup Script for NixOS
# Automates MongoDB setup, index loading, and server startup

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Configuration
MONGO_DATA_DIR="${MONGO_DATA_DIR:-$HOME/.local/share/lichess-mongo}"
MONGO_PORT="${MONGO_PORT:-27017}"
MONGO_PID_FILE="/tmp/lichess-mongod.pid"
REDIS_PORT="${REDIS_PORT:-6379}"
REDIS_PID_FILE="/tmp/lichess-redis.pid"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Helper functions
log_info() {
  echo -e "${BLUE}[INFO]${NC} $*"
}

log_success() {
  echo -e "${GREEN}[SUCCESS]${NC} $*"
}

log_warn() {
  echo -e "${YELLOW}[WARN]${NC} $*"
}

log_error() {
  echo -e "${RED}[ERROR]${NC} $*"
}

cleanup() {
  # MongoDB is managed separately (Docker/system), so we don't stop it
  # But stop Redis if we started it
  if [ -f "$REDIS_PID_FILE" ]; then
    local pid
    pid=$(cat "$REDIS_PID_FILE")
    if kill -0 "$pid" 2>/dev/null; then
      log_info "Stopping Redis (PID: $pid)..."
      kill "$pid" 2>/dev/null || true
    fi
    rm -f "$REDIS_PID_FILE"
  fi
  rm -f "$MONGO_PID_FILE"
}

trap cleanup EXIT

# Check if we're in nix develop environment
check_nix_env() {
  if [ -z "${IN_NIX_SHELL:-}" ]; then
    log_warn "Not in nix develop environment"
    log_info "Run: nix develop"
    return 1
  fi
  return 0
}

# Start MongoDB (or wait for it if already running via Docker/system)
start_mongodb() {
  log_info "Checking MongoDB..."

  # Try to connect to MongoDB
  log_info "Waiting for MongoDB to be ready on port $MONGO_PORT..."
  local attempts=0
  local max_attempts=30
  while [ $attempts -lt $max_attempts ]; do
    if mongosh --port "$MONGO_PORT" --eval "db.version()" &>/dev/null 2>&1; then
      log_success "MongoDB is ready"
      return 0
    fi
    ((attempts++))
    sleep 1
  done

  log_error "MongoDB not responding on port $MONGO_PORT after ${max_attempts}s"
  echo ""
  log_info "MongoDB must be installed separately (SSPL license)."
  log_info "Options:"
  echo "  1. Docker: docker run -d -p 27017:27017 mongo"
  echo "  2. System package: sudo apt install mongodb (or your package manager)"
  echo "  3. Download: https://www.mongodb.com/try/download/community"
  return 1
}

# Start Redis
start_redis() {
  log_info "Checking Redis..."

  # Try to connect to Redis
  log_info "Waiting for Redis to be ready on port $REDIS_PORT..."
  local attempts=0
  local max_attempts=30
  while [ $attempts -lt $max_attempts ]; do
    if redis-cli -p "$REDIS_PORT" ping &>/dev/null 2>&1; then
      log_success "Redis is ready"
      return 0
    fi
    ((attempts++))
    sleep 1
  done

  # Redis not running, try to start it
  log_info "Starting Redis on port $REDIS_PORT..."
  if ! command -v redis-server &>/dev/null; then
    log_error "redis-server not found. Make sure you're in the nix develop environment."
    return 1
  fi

  redis-server --port "$REDIS_PORT" --daemonize yes --pidfile "$REDIS_PID_FILE"

  # Wait for Redis to be ready
  local attempts=0
  while [ $attempts -lt $max_attempts ]; do
    if redis-cli -p "$REDIS_PORT" ping &>/dev/null 2>&1; then
      log_success "Redis is ready"
      return 0
    fi
    ((attempts++))
    sleep 1
  done

  log_error "Redis failed to start"
  return 1
}

# Load MongoDB indexes
load_indexes() {
  log_info "Loading MongoDB indexes..."

  if [ ! -f "$PROJECT_ROOT/bin/mongodb/indexes.js" ]; then
    log_error "indexes.js not found at $PROJECT_ROOT/bin/mongodb/indexes.js"
    return 1
  fi

  if mongosh --port "$MONGO_PORT" lichess < "$PROJECT_ROOT/bin/mongodb/indexes.js"; then
    log_success "MongoDB indexes loaded"
    return 0
  else
    log_error "Failed to load MongoDB indexes"
    return 1
  fi
}

# Install frontend dependencies
install_frontend() {
  log_info "Installing frontend dependencies..."

  cd "$PROJECT_ROOT"

  if ! command -v pnpm &>/dev/null; then
    log_error "pnpm not found. Make sure you're in the nix develop environment."
    return 1
  fi

  # Use --ignore-scripts on NixOS to avoid esbuild postinstall issues
  # (esbuild is provided by the nix flake)
  if pnpm install --ignore-scripts; then
    log_success "Frontend dependencies installed"
    return 0
  else
    log_error "Failed to install frontend dependencies"
    return 1
  fi
}

# Build frontend assets
build_frontend() {
  log_info "Building frontend assets..."

  cd "$PROJECT_ROOT"

  # Lichess uses a custom ui/build script for frontend compilation
  if [ ! -x "ui/build" ]; then
    log_error "ui/build script not found or not executable"
    return 1
  fi

  if ui/build; then
    log_success "Frontend assets built"
    return 0
  else
    log_error "Failed to build frontend assets"
    return 1
  fi
}

# Run the development server
run_server() {
  log_info "Starting Lichess development server..."
  log_info "Server will be available at http://localhost:9663"
  log_info "Press Ctrl+C to stop"

  cd "$PROJECT_ROOT"

  if [ ! -x "./lila.sh" ]; then
    log_error "lila.sh not found or not executable"
    return 1
  fi

  ./lila.sh
  # After lila.sh exits, the cleanup trap will handle MongoDB shutdown
}

# Main flow
main() {
  log_info "Lichess Development Setup"
  log_info "========================="

  # Check nix environment
  if ! check_nix_env; then
    log_error "Please run 'nix develop' first"
    exit 1
  fi

  log_success "Nix environment detected"

  # MongoDB setup
  if ! start_mongodb; then
    log_error "Failed to start MongoDB"
    exit 1
  fi

  if ! load_indexes; then
    log_error "Failed to load indexes"
    exit 1
  fi

  # Redis setup
  if ! start_redis; then
    log_error "Failed to start Redis"
    exit 1
  fi

  # Frontend setup
  if ! install_frontend; then
    log_error "Failed to install frontend dependencies"
    exit 1
  fi

  if ! build_frontend; then
    log_error "Failed to build frontend assets"
    exit 1
  fi

  log_success "Setup complete!"
  echo ""

  # Start server
  run_server
}

main "$@"
