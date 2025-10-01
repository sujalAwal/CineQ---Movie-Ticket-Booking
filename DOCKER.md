# 🐳 CineQ Docker Deployment Guide

This guide will help you deploy the CineQ Movie Booking System using Docker.

## 📋 Prerequisites

### Required Software:
- **Docker Desktop** (Windows/Mac) or **Docker Engine** (Linux)
- **Docker Compose** (usually included with Docker Desktop)

### Minimum System Requirements:
- **RAM**: 4GB available
- **Storage**: 2GB free space
- **CPU**: 2 cores recommended

## 🚀 Quick Start (Development)

### 1. Clone and Navigate
```bash
git clone https://github.com/sujalAwal/CineQ---Movie-Ticket-Booking.git
cd CineQ---Movie-Ticket-Booking
```

### 2. Setup Environment
```bash
# Copy environment template
cp .env.docker.example .env

# Edit .env file with your preferences (optional)
# Default settings work for development
```

### 3. Deploy with Script
**Windows:**
```cmd
deploy-dev.bat
```

**Linux/Mac:**
```bash
chmod +x deploy-dev.sh
./deploy-dev.sh
```

### 4. Access Services
- **CineQ API**: http://localhost:8080/api
- **Database**: localhost:5432 (cineqdb_dev)
- **PgAdmin**: http://localhost:5050 (admin@cineq.com / dev123)

## 🔧 Manual Deployment

### Development Environment
```bash
# Create environment file
cp .env.docker.example .env

# Start all services
docker-compose up -d

# View logs
docker-compose logs -f cineq-app

# Stop services
docker-compose down
```

### Production Environment
```bash
# Set production environment variables
export DB_URL="your-production-database-url"
export DB_USERNAME="your-db-username"
export DB_PASSWORD="your-secure-password"
export JWT_SECRET="your-secure-jwt-secret"

# Deploy production setup
docker-compose -f docker-compose.prod.yml up -d
```

## 🏗️ Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   CineQ App     │    │   PostgreSQL    │    │     Redis       │
│   (Port 8080)   │◄──►│   (Port 5432)   │    │   (Port 6379)   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                                              │
         └──────────────┐  ┌────────────────────────────┘
                        │  │
                 ┌─────────────────┐
                 │     PgAdmin     │
                 │   (Port 5050)   │
                 └─────────────────┘
```

## 📁 Container Volumes

- **postgres_data**: Database files persistence
- **redis_data**: Redis cache persistence  
- **app_logs**: Application logs
- **pgadmin_data**: PgAdmin configuration

## 🔐 Environment Configuration

### Development (.env)
```bash
# Database
DB_NAME=cineqdb_dev
DB_USERNAME=cineq_dev_user
DB_PASSWORD=dev_password_123

# Security
JWT_SECRET=dev-secret-key
JWT_EXPIRATION=86400

# Logging
LOGGING_LEVEL=DEBUG
```

### Production (Environment Variables)
```bash
# Database (External - Aiven)
DB_URL=jdbc:postgresql://prod-host:port/database?sslmode=require
DB_USERNAME=prod_user
DB_PASSWORD=secure_prod_password

# Security
JWT_SECRET=very-secure-production-secret
JWT_EXPIRATION=86400
```

## 🛠️ Common Commands

### Service Management
```bash
# Start services
docker-compose up -d

# Stop services
docker-compose down

# Restart specific service
docker-compose restart cineq-app

# View logs
docker-compose logs -f cineq-app

# Check service status
docker-compose ps
```

### Database Operations
```bash
# Access PostgreSQL container
docker-compose exec postgres psql -U cineq_dev_user -d cineqdb_dev

# Backup database
docker-compose exec postgres pg_dump -U cineq_dev_user cineqdb_dev > backup.sql

# Restore database
docker-compose exec -T postgres psql -U cineq_dev_user -d cineqdb_dev < backup.sql
```

### Application Management
```bash
# Rebuild application
docker-compose build --no-cache cineq-app
docker-compose up -d cineq-app

# Access application container
docker-compose exec cineq-app bash

# View application logs
docker-compose logs --tail=100 -f cineq-app
```

## 🐛 Troubleshooting

### Common Issues

**1. Port Already in Use**
```bash
# Check what's using the port
netstat -tulpn | grep :8080

# Stop the conflicting service or change ports in docker-compose.yml
```

**2. Database Connection Issues**
```bash
# Check PostgreSQL health
docker-compose exec postgres pg_isready -U cineq_dev_user

# Restart database
docker-compose restart postgres
```

**3. Out of Memory**
```bash
# Check Docker resources
docker system df

# Clean up unused containers/images
docker system prune -a
```

**4. Application Won't Start**
```bash
# Check logs for errors
docker-compose logs cineq-app

# Rebuild without cache
docker-compose build --no-cache
```

### Health Checks
```bash
# Application health
curl http://localhost:8080/api/actuator/health

# Database health  
docker-compose exec postgres pg_isready

# Redis health
docker-compose exec redis redis-cli ping
```

## 🔄 Updates and Maintenance

### Update Application
```bash
# Pull latest code
git pull origin develop

# Rebuild and restart
docker-compose down
docker-compose build --no-cache
docker-compose up -d
```

### Database Migrations
```bash
# Migrations run automatically with Flyway
# Check migration status in application logs
docker-compose logs cineq-app | grep flyway
```

## 📊 Monitoring

### Application Metrics
- **Health**: http://localhost:8080/api/actuator/health
- **Info**: http://localhost:8080/api/actuator/info  
- **Metrics**: http://localhost:8080/api/actuator/metrics

### Database Administration
- **PgAdmin**: http://localhost:5050
- **Username**: admin@cineq.com
- **Password**: dev123 (development)

## 🚢 Production Deployment

### Cloud Deployment Options

**1. AWS ECS/Fargate**
```bash
# Build and push to ECR
docker build -t cineq-app .
docker tag cineq-app:latest [ECR-URL]/cineq-app:latest
docker push [ECR-URL]/cineq-app:latest
```

**2. Google Cloud Run**
```bash
# Build and deploy
gcloud builds submit --tag gcr.io/PROJECT-ID/cineq-app
gcloud run deploy --image gcr.io/PROJECT-ID/cineq-app --platform managed
```

**3. Azure Container Instances**
```bash
# Create container group
az container create --resource-group myResourceGroup --name cineq-app \
  --image cineq-app:latest --ports 8080
```

## 📝 Notes

- **Development**: Uses local PostgreSQL container
- **Production**: Connects to external database (Aiven)
- **Security**: Never commit .env files with real credentials
- **Scaling**: Use Docker Swarm or Kubernetes for horizontal scaling
- **Backups**: Set up automated database backups for production

## 🆘 Support

For issues and questions:
1. Check logs: `docker-compose logs cineq-app`
2. Verify configuration: Review .env file
3. Check system resources: `docker system df`
4. Restart services: `docker-compose restart`

## 📄 License

This project is licensed under the MIT License.