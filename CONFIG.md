# CineQ Configuration Guide

## Database Configuration

This project supports multiple database configurations for different environments.

### Development Setup

#### Option 1: Using application-local.properties (Recommended)
1. Create `src/main/resources/application-local.properties`
2. Add your database configuration:
```properties
# PostgreSQL Configuration
DB_URL=jdbc:postgresql://your-host:port/database?sslmode=require
DB_USERNAME=your_username
DB_PASSWORD=your_password
DB_DRIVER=org.postgresql.Driver
DB_DIALECT=org.hibernate.dialect.PostgreSQLDialect
DDL_AUTO=none
FLYWAY_ENABLED=true
```

3. Run with local profile:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

#### Option 2: Using Environment Variables
1. Copy `.env.example` to `.env`
2. Fill in your actual database credentials
3. The application will pick up these environment variables

### Production Setup

Set environment variables on your production server:
```bash
export DB_URL="jdbc:postgresql://prod-host:port/database"
export DB_USERNAME="prod_user"
export DB_PASSWORD="secure_password"
export DDL_AUTO="validate"
export FLYWAY_ENABLED="true"
```

## Security

- Never commit sensitive credentials to version control
- Use environment variables or local property files for secrets
- The `.gitignore` file is configured to exclude sensitive files

## Database Migration

This project uses Flyway for database migrations:
- Migration files are located in `src/main/resources/db/migration/postgres/`
- Migrations run automatically when `FLYWAY_ENABLED=true`
- For development, you can use `DDL_AUTO=create-drop` to auto-create tables