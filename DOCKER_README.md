# Docker Setup for Video Metadata Service

This document explains how to run the Video Metadata Service using Docker Compose with Redis caching.

## Prerequisites

- **Docker Desktop** installed and running
- **Docker Compose** (usually included with Docker Desktop)
- **PowerShell** (for Windows users)

## Quick Start

### Option 1: Using PowerShell Script (Recommended)

```powershell
# Start all services
.\start-docker.ps1

# View logs
.\start-docker.ps1 logs

# Check status
.\start-docker.ps1 status

# Stop services
.\start-docker.ps1 stop

# Restart services
.\start-docker.ps1 restart
```

### Option 2: Using Docker Compose Directly

```bash
# Start all services
docker-compose up --build -d

# View logs
docker-compose logs -f

# Stop services
docker-compose down

# Restart services
docker-compose restart
```

## Services

### 1. Redis (Cache)
- **Image**: `redis:7-alpine`
- **Port**: `6379`
- **Purpose**: Caching layer for application data
- **Persistence**: Data persisted in Docker volume

### 2. Application (Spring Boot)
- **Image**: Built from local Dockerfile
- **Port**: `8080`
- **Purpose**: Main application server
- **Dependencies**: Requires Redis to be healthy

## Access Points

Once services are running:

- **Application**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **H2 Database Console**: http://localhost:8080/h2-console
- **Redis**: localhost:6379

## Authentication

Default users are configured:

| Username | Password | Roles |
|----------|----------|-------|
| `admin`  | `admin123` | ADMIN, USER |
| `user`   | `user123`  | USER |

## Testing Cache Functionality

### 1. Get JWT Token
```bash
curl -X POST "http://localhost:8080/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

### 2. Test Cache Health
```bash
curl -X GET "http://localhost:8080/cache/health" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### 3. Test Video Statistics Caching
```bash
# First call (hits database)
curl -X GET "http://localhost:8080/videos/stats" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# Second call (hits cache)
curl -X GET "http://localhost:8080/videos/stats" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### 4. Check Cache Statistics
```bash
curl -X GET "http://localhost:8080/cache/stats" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

## Configuration

### Environment Variables

The application uses these environment variables:

| Variable | Default | Description |
|----------|---------|-------------|
| `SPRING_DATA_REDIS_HOST` | `redis` | Redis hostname |
| `SPRING_DATA_REDIS_PORT` | `6379` | Redis port |
| `SPRING_PROFILES_ACTIVE` | `docker` | Spring profile |
| `JWT_SECRET` | `mySecretKeyForVideoMetadataService2025` | JWT signing key |

### Cache Configuration

Cache TTL settings (in seconds):

| Cache | TTL | Description |
|-------|-----|-------------|
| `video-stats` | 600 (10 min) | Video statistics |
| `job-stats` | 300 (5 min) | Job statistics |
| `videos` | 3600 (1 hour) | Video data |
| `jobs` | 900 (15 min) | Job data |

## Troubleshooting

### Common Issues

#### 1. Redis Connection Failed
```bash
# Check if Redis container is running
docker-compose ps

# Check Redis logs
docker-compose logs redis

# Restart Redis
docker-compose restart redis
```

#### 2. Application Won't Start
```bash
# Check application logs
docker-compose logs app

# Check if Redis is healthy
docker-compose exec redis redis-cli ping
```

#### 3. Port Already in Use
```bash
# Check what's using the port
netstat -ano | findstr :8080

# Stop conflicting service or change port in docker-compose.yml
```

### Debugging

#### View Real-time Logs
```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f app
docker-compose logs -f redis
```

#### Access Redis CLI
```bash
docker-compose exec redis redis-cli

# Test Redis
127.0.0.1:6379> ping
127.0.0.1:6379> keys *
127.0.0.1:6379> exit
```

#### Access Application Container
```bash
docker-compose exec app sh

# Check application status
curl http://localhost:8080/actuator/health
```

## Development

### Building Only
```bash
# Build application image
.\start-docker.ps1 build

# Or directly
docker-compose build
```

### Cleanup
```bash
# Remove all containers and images
.\start-docker.ps1 clean

# Or directly
docker-compose down -v --rmi all
docker system prune -f
```

### Development Mode

For development without Docker Compose:

1. **Start Redis manually**:
   ```bash
   docker run -d --name video-metadata-redis -p 6379:6379 redis:7-alpine
   ```

2. **Run application locally**:
   ```bash
   mvn spring-boot:run
   ```

3. **Stop Redis when done**:
   ```bash
   docker stop video-metadata-redis
   docker rm video-metadata-redis
   ```

## Production Considerations

For production deployment:

1. **Use external Redis**: Configure Redis host/port via environment variables
2. **Use external database**: Replace H2 with PostgreSQL/MySQL
3. **Add monitoring**: Configure health checks and metrics
4. **Security**: Use proper JWT secrets and database credentials
5. **Scaling**: Consider Redis clustering for high availability

## File Structure

```
.
├── docker-compose.yml          # Service definitions
├── Dockerfile                  # Application image
├── .dockerignore              # Build exclusions
├── start-docker.ps1          # Management script
├── DOCKER_README.md          # This file
└── src/main/resources/
    └── application-docker.yml # Docker-specific config
```

## Health Checks

Both services include health checks:

- **Redis**: Pings Redis server every 5 seconds
- **Application**: Checks `/actuator/health` endpoint every 30 seconds

Services will restart automatically if health checks fail. 