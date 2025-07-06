# Lichess Docker Setup

This guide will help you deploy Lichess using Docker and Docker Compose.

## Prerequisites

- Docker Engine 20.10+
- Docker Compose 2.0+
- At least 4GB RAM available
- 20GB disk space for databases and assets

## Quick Start

1. **Clone the repository and navigate to the project directory:**
   ```bash
   git clone <your-lichess-fork>
   cd lichess
   ```

2. **Build and start all services:**
   ```bash
   docker-compose up --build
   ```

3. **Access the application:**
   - Main application: http://localhost:9663
   - Nginx proxy: http://localhost:80
   - MongoDB Express: http://localhost:8081 (admin/admin)
   - Redis Commander: http://localhost:8082
   - Kibana: http://localhost:5601
   - Mailhog: http://localhost:8025

## Services Overview

### Core Services

- **lichess**: Main Scala application (port 9663)
- **mongodb**: MongoDB database (port 27017)
- **redis**: Redis cache/pub-sub (port 6379)
- **elasticsearch**: Search engine (port 9200)
- **nginx**: Reverse proxy and static file server (port 80)

### Development Services (override file)

- **mongo-express**: MongoDB web interface
- **redis-commander**: Redis web interface
- **kibana**: Elasticsearch web interface
- **mailhog**: Email testing service
- **adminer**: Database management tool

## Configuration

### Environment Variables

Create a `.env` file in the root directory:

```env
# Database
MONGODB_URI=mongodb://lichess:password@mongodb:27017/lichess?authSource=admin
REDIS_URI=redis://:password@redis:6379

# Application
NET_DOMAIN=localhost:9663
NET_BASE_URL=http://localhost:9663
ELASTICSEARCH_URL=http://elasticsearch:9200

# Java
JAVA_OPTS=-Xmx2g -Xms1g -XX:+UseG1GC
```

### Custom Configuration

The application uses `conf/docker.conf` for Docker-specific settings. You can override any configuration by:

1. Creating a custom config file
2. Mounting it as a volume in `docker-compose.yml`
3. Setting the `CONFIG_FILE` environment variable

## Building Assets

### UI Assets

The UI assets need to be built before running the application:

```bash
# Build UI assets
docker run --rm -v $(pwd):/app -w /app/ui node:18-alpine sh -c "npm install && npm run build"

# Or use the build script
./docker/scripts/build-ui.sh
```

### CSS Assets

CSS assets are built as part of the UI build process. If you need to build them separately:

```bash
# Install sass compiler
npm install -g sass

# Build CSS
find ui -name "*.scss" -exec sass {} {}.css \;
```

## Data Persistence

All data is persisted in Docker volumes:

- `mongodb_data`: MongoDB database files
- `redis_data`: Redis persistence files
- `elasticsearch_data`: Elasticsearch indices

### Backup and Restore

```bash
# Backup MongoDB
docker exec lichess-mongodb mongodump --uri="mongodb://lichess:password@localhost:27017/lichess?authSource=admin" --out=/backup

# Restore MongoDB
docker exec lichess-mongodb mongorestore --uri="mongodb://lichess:password@localhost:27017/lichess?authSource=admin" /backup/lichess
```

## Development Setup

### Hot Reload Development

For development with hot reload:

```bash
# Use override file for development
docker-compose -f docker-compose.yml -f docker-compose.override.yml up

# Or modify the lichess service to use sbt run
# Uncomment the command line in docker-compose.override.yml
```

### Debug Mode

Enable Java debug mode:

```bash
# Set environment variable
JAVA_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"

# Connect debugger to port 5005
```

## Production Deployment

### Security Considerations

1. **Change default passwords:**
   ```bash
   # Generate secure passwords
   openssl rand -base64 32  # For MongoDB
   openssl rand -base64 32  # For Redis
   ```

2. **Use secrets management:**
   ```yaml
   # docker-compose.yml
   secrets:
     mongodb_password:
       file: ./secrets/mongodb_password.txt
   ```

3. **Enable SSL/TLS:**
   - Configure nginx with SSL certificates
   - Update configuration for HTTPS

### Resource Limits

```yaml
# docker-compose.yml
services:
  lichess:
    deploy:
      resources:
        limits:
          cpus: '2.0'
          memory: 4G
        reservations:
          cpus: '1.0'
          memory: 2G
```

### Monitoring

Add monitoring services:

```yaml
# Add to docker-compose.yml
services:
  prometheus:
    image: prom/prometheus:latest
    ports:
      - "9090:9090"
    volumes:
      - ./monitoring/prometheus.yml:/etc/prometheus/prometheus.yml

  grafana:
    image: grafana/grafana:latest
    ports:
      - "3000:3000"
    environment:
      GF_SECURITY_ADMIN_PASSWORD: admin
```

## Troubleshooting

### Common Issues

1. **Out of memory errors:**
   ```bash
   # Increase Java heap size
   JAVA_OPTS="-Xmx4g -Xms2g"
   ```

2. **Database connection issues:**
   ```bash
   # Check MongoDB logs
   docker logs lichess-mongodb
   
   # Test connection
   docker exec lichess-mongodb mongo --eval "db.runCommand('ping')"
   ```

3. **Asset loading issues:**
   ```bash
   # Rebuild assets
   ./docker/scripts/build-ui.sh
   
   # Check nginx logs
   docker logs lichess-nginx
   ```

### Health Checks

All services include health checks. Check status:

```bash
# Check all services
docker-compose ps

# Check specific service
docker-compose exec lichess curl -f http://localhost:9663/api/status
```

### Logs

View logs for debugging:

```bash
# All services
docker-compose logs

# Specific service
docker-compose logs lichess

# Follow logs
docker-compose logs -f lichess
```

## Scaling

### Horizontal Scaling

Scale the application:

```bash
# Scale lichess service
docker-compose up --scale lichess=3

# Use load balancer
# Update nginx configuration for multiple backends
```

### Database Scaling

For production, consider:

- MongoDB replica sets
- Redis cluster
- Elasticsearch cluster

## Advanced Configuration

### Custom Nginx Configuration

Create custom nginx configuration:

```nginx
# docker/nginx/custom.conf
server {
    listen 443 ssl;
    server_name yourdomain.com;
    
    ssl_certificate /etc/ssl/certs/lichess.pem;
    ssl_certificate_key /etc/ssl/private/lichess.key;
    
    # Your custom configuration
}
```

### External Services

Configure external services:

```conf
# conf/production.conf
include "docker"

# External services
explorer.endpoint = "https://your-explorer.com"
tablebase.endpoint = "https://your-tablebase.com"
```

## Support

For issues and questions:

1. Check the logs first
2. Review the configuration
3. Consult the original Lichess documentation
4. Open an issue with detailed error information

## Contributing

When contributing to the Docker setup:

1. Test changes with both development and production configurations
2. Update documentation
3. Ensure backward compatibility
4. Add appropriate health checks 