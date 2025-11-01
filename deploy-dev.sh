#!/bin/bash

# CineQ Docker Deployment Script for Development

echo "🚀 Starting CineQ Development Environment..."

# Check if Docker is installed
if ! command -v docker &> /dev/null; then
    echo "❌ Docker is not installed. Please install Docker first."
    exit 1
fi

if ! command -v docker-compose &> /dev/null; then
    echo "❌ Docker Compose is not installed. Please install Docker Compose first."
    exit 1
fi

# Create .env file if it doesn't exist
if [ ! -f .env ]; then
    echo "📋 Creating .env file from template..."
    cp .env.docker.example .env
    echo "✅ .env file created. Please review and modify if needed."
fi

# Build and start the services
echo "🔨 Building and starting CineQ services..."
docker-compose down --remove-orphans
docker-compose build --no-cache
docker-compose up -d

# Wait for services to be healthy
echo "⏳ Waiting for services to start..."
sleep 30

# Check service status
echo "📊 Service Status:"
docker-compose ps

# Show logs
echo "📝 Recent logs:"
docker-compose logs --tail=50 cineq-app

echo "✅ CineQ Development Environment is ready!"
echo "🌐 Application: http://localhost:8080/api"
echo "🗄️  Database: localhost:5432 (cineqdb_dev)"
echo "🔧 PgAdmin: http://localhost:5050"
echo ""
echo "To view logs: docker-compose logs -f cineq-app"
echo "To stop: docker-compose down"