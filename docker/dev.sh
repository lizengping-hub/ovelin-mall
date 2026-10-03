#!/usr/bin/env bash
set -euo pipefail
echo "Directory: $(dirname "$0")"
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
echo "Project directory: $PROJECT_DIR"
ENV_FILE="$PROJECT_DIR/.env.dev"
echo "Environment file: $ENV_FILE"
COMPOSE_FILE="$PROJECT_DIR/docker-compose.dev.yml"
echo "Compose file: $COMPOSE_FILE"

if [ ! -f "$ENV_FILE" ]; then
    echo "ERROR: .env.dev not found"
    echo "Run: cp .env.dev.example .env.dev"
    exit 1
fi

echo "Clearing containers, networks, and volumes..."
docker compose \
    --project-directory "$PROJECT_DIR" \
    -f "$COMPOSE_FILE" \
    --env-file "$ENV_FILE" \
    down --volumes --remove-orphans

echo ""
echo "Starting a clean development environment..."
docker compose \
    --project-directory "$PROJECT_DIR" \
    -f "$COMPOSE_FILE" \
    --env-file "$ENV_FILE" \
    up -d \
    --wait
