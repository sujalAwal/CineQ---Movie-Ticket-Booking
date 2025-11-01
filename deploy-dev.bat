@echo off
REM CineQ Docker Deployment Script for Development (Windows)

echo 🚀 Starting CineQ Development Environment...

REM Check if Docker is installed
docker --version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Docker is not installed. Please install Docker Desktop first.
    pause
    exit /b 1
)

docker-compose --version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Docker Compose is not installed. Please install Docker Compose first.
    pause
    exit /b 1
)

REM Create .env file if it doesn't exist
if not exist .env (
    echo 📋 Creating .env file from template...
    copy .env.docker.example .env
    echo ✅ .env file created. Please review and modify if needed.
)

REM Build and start the services
echo 🔨 Building and starting CineQ services...
docker-compose down --remove-orphans
docker-compose build --no-cache
docker-compose up -d

REM Wait for services to be healthy
echo ⏳ Waiting for services to start...
timeout /t 30 /nobreak >nul

REM Check service status
echo 📊 Service Status:
docker-compose ps

REM Show logs
echo 📝 Recent logs:
docker-compose logs --tail=50 cineq-app

echo.
echo ✅ CineQ Development Environment is ready!
echo 🌐 Application: http://localhost:8080/api
echo 🗄️  Database: localhost:5432 (cineqdb_dev)
echo 🔧 PgAdmin: http://localhost:5050
echo.
echo To view logs: docker-compose logs -f cineq-app
echo To stop: docker-compose down
pause