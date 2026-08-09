# Docker Hub Deployment Instructions

## 1) Build the JAR
From `c:\exammaster-pro\exammaster-pro` run:

```powershell
.\mvnw.cmd -DskipTests clean package
```

## 2) Build the Docker image locally
Replace `YOUR_DOCKERHUB_USERNAME` with your Docker Hub username.

```powershell
docker build -t sharukhmukhuram/exammaster-pro:latest .
```

## 3) Push the image to Docker Hub

```powershell
docker login
docker push sharukhmukhuram/exammaster-pro:latest
```

## 4) Run the app with Docker Compose
Use the existing `docker-compose.yml` file. This file now uses the image from Docker Hub.

```powershell
docker compose up -d
```

## 5) What this does
- `db` service starts a shared MySQL database
- `app` service pulls the Docker Hub image
- The app connects to the single shared DB at `db:3306/exammasterpro`

## 6) Notes
- If you want the user to only run with Docker Compose, make sure the image is already pushed to Docker Hub.
- Update `docker-compose.yml` with the final Docker Hub username before sharing.
