# 🚀 Lichess Docker - Quick Start Guide

Get Lichess running with Docker in 5 minutes!

## Prerequisites

- Docker Engine 20.10+
- Docker Compose 2.0+
- 4GB RAM minimum
- 20GB disk space

## Option 1: Super Quick Start (Recommended)

```bash
# 1. Run setup script
make setup

# 2. Start all services  
make up

# 3. Access Lichess
open http://localhost:9663
```

## Option 2: Development Mode

```bash
# 1. Setup with development tools
make dev-setup

# 2. Access all services
make urls
```

## Option 3: Manual Steps

```bash
# 1. Initialize environment
./docker/scripts/setup.sh

# 2. Build and start services
docker-compose up --build -d

# 3. Check health
./docker/scripts/health-check.sh
```

## 🌐 Service URLs

| Service | URL | Credentials |
|---------|-----|-------------|
| **Lichess** | http://localhost:9663 | - |
| **Nginx Proxy** | http://localhost:80 | - |
| **MongoDB Express** | http://localhost:8081 | admin/admin |
| **Redis Commander** | http://localhost:8082 | - |
| **Kibana** | http://localhost:5601 | - |
| **Mailhog** | http://localhost:8025 | - |

## 🔧 Common Commands

```bash
# View logs
make logs-f

# Check status
make health

# Stop services
make down

# Restart everything
make restart

# Clean up
make clean
```

## 🆘 Troubleshooting

### Service won't start?
```bash
# Check logs
docker-compose logs [service-name]

# Restart specific service
docker-compose restart [service-name]
```

### Out of memory?
```bash
# Reduce Java heap size in .env
JAVA_OPTS=-Xmx1g -Xms512m
```

### Can't connect to database?
```bash
# Check MongoDB status
docker-compose exec mongodb mongo --eval "db.runCommand('ping')"
```

## 📚 Next Steps

- Read `README.Docker.md` for detailed documentation
- Configure external services in `conf/docker.conf`
- Set up SSL certificates for production
- Scale services with `docker-compose up --scale`

## 🎯 Production Deployment

1. Change default passwords in `.env`
2. Configure SSL certificates
3. Set up monitoring
4. Configure backups
5. Use external databases for scale

---

**🎉 That's it! Your Lichess instance should now be running.**

For support, check the logs first, then consult the full documentation. 