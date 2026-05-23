# Docker Setup for BetterBlog

## Overview
This Docker setup containerizes the entire BetterBlog application with MongoDB using Docker Compose.

## Files Created
- **Dockerfile**: Multi-stage build for Spring Boot application (Java 17)
- **docker-compose.yml**: Orchestrates Spring Boot app + MongoDB service
- **.dockerignore**: Excludes unnecessary files from Docker build context
- **application-docker.yml**: Spring Boot configuration profile for Docker environment

## Prerequisites
- Docker installed (v20.10+)
- Docker Compose installed (v2.0+)

## Quick Start

### 1. Build and start the application
```bash
docker-compose up --build
```

This will:
- Build the Spring Boot application in a Docker image
- Start MongoDB container (port 27017)
- Start BetterBlog application container (port 8080)
- Create a shared network for inter-container communication

### 2. Access the application
- **API**: http://localhost:8080/api
- **MongoDB**: mongodb://localhost:27017

### 3. Stop the application
```bash
docker-compose down
```

To also remove volumes:
```bash
docker-compose down -v
```

## Services Overview

### MongoDB Service
- **Image**: mongo:7.0
- **Container**: betterblog-mongodb
- **Port**: 27017
- **Username**: admin
- **Password**: password123
- **Database**: betterblog
- **Credentials Env**: `?authSource=admin`
- **Health Check**: Enabled (waits for MongoDB to be ready)
- **Data Persistence**: Stored in `mongodb_data` volume

### Spring Boot Service
- **Image**: Built from Dockerfile
- **Container**: betterblog-app
- **Port**: 8080
- **Profile**: docker (uses `application-docker.yml`)
- **Depends On**: MongoDB (waits for health check)
- **Network**: betterblog-network (internal communication with MongoDB)

## Configuration Details

### MongoDB Connection
- **Docker Connection String**: `mongodb://admin:password123@mongodb:27017/betterblog?authSource=admin`
- **Local Connection String** (from host): `mongodb://localhost:27017`

### Spring Boot Environment Variables
The `docker-compose.yml` sets these environment variables:
- `SPRING_PROFILES_ACTIVE=docker` → activates `application-docker.yml`
- `SPRING_MONGODB_URI=...` → MongoDB connection string
- `JWT_SECRET=...` → JWT signing key
- `JWT_EXPIRATION=86400000` → 24 hours
- `JWT_REFRESH_EXPIRATION=604800000` → 7 days

### Application Context Path
- All API endpoints are prefixed with `/api`
- Example: http://localhost:8080/api/auth/login

## Build Details

### Multi-Stage Build
1. **Stage 1 (builder)**: Maven 3.9 + OpenJDK 17
   - Downloads dependencies
   - Compiles source code
   - Packages into JAR
   - Result: Smaller final image (builder stage not included)

2. **Stage 2 (runtime)**: Eclipse Temurin JRE 17 Alpine
   - Lightweight base image (~200MB)
   - Copies JAR from builder
   - Runs application

**Resulting Image Size**: ~500-600MB (optimized with Alpine JRE)

## Volume Management

### mongodb_data Volume
- Persists MongoDB data between container restarts
- Automatically created by Docker Compose
- Use `docker volume ls` to list
- Use `docker volume rm betterblog_mongodb_data` to delete

## Network

### betterblog-network
- Bridge network for inter-container communication
- MongoDB accessible as `mongodb:27017` from Spring Boot container
- Host can access containers via localhost

## Common Commands

### View logs
```bash
docker-compose logs -f betterblog
docker-compose logs -f mongodb
```

### Access MongoDB from host
```bash
mongosh "mongodb://localhost:27017" -u admin -p password123
```

### Rebuild after code changes
```bash
docker-compose up --build
```

### Remove everything (including volumes)
```bash
docker-compose down -v
rm -rf mongodb_data
```

### Run in background
```bash
docker-compose up -d
```

### Check service status
```bash
docker-compose ps
```

## Troubleshooting

### MongoDB connection failed
- Check if MongoDB container is running: `docker-compose ps`
- Check logs: `docker-compose logs mongodb`
- Wait 10-15 seconds for health check to pass

### Application won't start
- Check if JDK is compatible: `java -version`
- Check if port 8080 is available: `lsof -i :8080`
- Check logs: `docker-compose logs betterblog`

### Data persists after restart (not wanted)
- Delete the volume: `docker-compose down -v`

### Build fails on Windows
- Ensure line endings are LF (not CRLF) if Git is checked out
- Try: `git config core.autocrlf false`

## Architecture

```
┌─────────────────────────────────────────┐
│        Host Machine                      │
├─────────────────────────────────────────┤
│                                          │
│  ┌──────────────────────────────────┐  │
│  │   betterblog-network (bridge)   │  │
│  │                                 │  │
│  │  ┌──────────────┐  ┌──────────┐ │  │
│  │  │ betterblog   │  │ mongodb  │ │  │
│  │  │ (Spring      │  │ (Mongo   │ │  │
│  │  │  Boot)       │  │  7.0)    │ │  │
│  │  │              │  │          │ │  │
│  │  │ :8080        │  │ :27017   │ │  │
│  │  └──────────────┘  └──────────┘ │  │
│  │         │              │        │  │
│  └─────────┼──────────────┼────────┘  │
│            │              │           │
│  Host Port 8080  Host Port 27017      │
│  localhost:8080  localhost:27017      │
│                                        │
└─────────────────────────────────────────┘
```

## Security Considerations

⚠️ **For Production Deployment:**
- Change MongoDB credentials in `docker-compose.yml`
- Use environment variables or `.env` file (see below)
- Enable MongoDB authentication with stronger passwords
- Use Docker Secrets for sensitive data
- Do not commit credentials to Git

### Using .env file (recommended)
Create `.env` file in project root:
```env
MONGO_ROOT_USERNAME=admin
MONGO_ROOT_PASSWORD=your-strong-password
MONGO_DATABASE=betterblog
JWT_SECRET=your-secret-key
```

Then update `docker-compose.yml` to reference:
```yaml
  mongodb:
    environment:
      MONGO_INITDB_ROOT_USERNAME: ${MONGO_ROOT_USERNAME}
      MONGO_INITDB_ROOT_PASSWORD: ${MONGO_ROOT_PASSWORD}
      MONGO_INITDB_DATABASE: ${MONGO_DATABASE}
  betterblog:
    environment:
      JWT_SECRET: ${JWT_SECRET}
```

## Next Steps

1. **Run the application**: `docker-compose up --build`
2. **Test endpoints**: http://localhost:8080/api/auth/login
3. **Access MongoDB**: `mongosh "mongodb://localhost:27017" -u admin -p password123`
4. **View logs**: `docker-compose logs -f`
5. **Stop services**: `docker-compose down`

