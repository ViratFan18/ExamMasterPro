# Docker build and push guide

## Build the image locally

```bash
docker build -t <your-dockerhub-username>/exammaster-pro:latest .
```

## Push to Docker Hub

```bash
docker login
docker push <your-dockerhub-username>/exammaster-pro:latest
```

## Run everything with Docker Compose

```bash
docker compose up -d
```

## Useful commands

```bash
docker compose ps
docker compose logs -f app
docker compose down
```

## Health check

```bash
curl http://localhost:8080/actuator/health
```
