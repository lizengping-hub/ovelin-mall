#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
ENV_FILE="$PROJECT_DIR/.env.dev"
COMPOSE_FILE="$PROJECT_DIR/docker-compose.test.yml"

if [ ! -f "$ENV_FILE" ]; then
    echo "ERROR: .env.dev not found"
    echo "Run: cp .env.dev.example .env.dev"
    exit 1
fi

echo "Compose: $COMPOSE_FILE"
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
