.PHONY: help build clean run docker-up docker-down docker-build docker-logs

# Default target
help:
	@echo "Andesis - Reactive Statistics Service"
	@echo "========================================"
	@echo ""
	@echo "Available targets:"
	@echo "  make build          - Build Maven project"
	@echo "  make clean          - Clean build artifacts"
	@echo "  make run            - Run application locally (requires PostgreSQL on localhost:5432)"
	@echo "  make docker-build   - Build Docker images"
	@echo "  make docker-up      - Start Docker Compose (PostgreSQL + services)"
	@echo "  make docker-down    - Stop Docker Compose"
	@echo "  make docker-logs    - Show Docker logs"
	@echo "  make help           - Show this help message"
	@echo ""
	@echo "Quick start:"
	@echo "  1. make docker-up   - Start services"
	@echo "  2. make docker-logs - View logs"
	@echo "  3. Access at http://localhost:8080/api/client/random-stats?count=1000"

# Build Maven project
build:
	@echo "Building Maven project..."
	mvn clean package -DskipTests
	@echo "Build completed!"

# Clean build artifacts
clean:
	@echo "Cleaning build artifacts..."
	mvn clean
	docker-compose down -v 2>/dev/null || true
	@echo "Cleanup completed!"

# Run application locally (requires PostgreSQL)
run: build
	@echo "Starting application..."
	@echo "Ensure PostgreSQL is running on localhost:5432"
	java -jar target/andesis-*.jar

# Build Docker images
docker-build:
	@echo "Building Docker images..."
	docker-compose build --no-cache
	@echo "Docker build completed!"

# Start Docker Compose
docker-up: docker-build
	@echo "Starting Docker Compose..."
	docker-compose up -d
	@echo "Waiting for services to be healthy..."
	@sleep 5
	@echo ""
	@echo "Services are running!"
	@echo "Service A (Client): http://localhost:8080"
	@echo "Service B (Server): http://localhost:8081"
	@echo "PostgreSQL: localhost:5432"
	@echo ""
	@echo "Try this command:"
	@echo "  curl 'http://localhost:8080/api/client/random-stats?count=10000&min=-100000&max=100000'"

# Stop Docker Compose
docker-down:
	@echo "Stopping Docker Compose..."
	docker-compose down
	@echo "Stopped!"

# Show Docker logs
docker-logs:
	docker-compose logs -f

# Restart services
restart: docker-down docker-up

# Run tests
test:
	@echo "Running tests..."
	mvn test

# Format code
format:
	@echo "Code formatting is handled by IDE/editor"

# Show status
status:
	@echo "Docker Compose Status:"
	docker-compose ps

# Install dependencies
install:
	mvn install -DskipTests

.DEFAULT_GOAL := help
