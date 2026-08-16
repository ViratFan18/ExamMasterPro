# ExamMaster Pro

ExamMaster Pro is a Spring Boot-based exam administration system designed to simplify exam seat allocation, data import/export, and admin workflows for educational institutions.

## Overview

The application helps manage:
- students
- invigilators
- exam halls and buildings
- exam allocation logic
- CSV upload and export workflows
- role-based access for college administration

This project is suitable for a Java backend / Spring Boot portfolio and demonstrates real-world domain logic beyond a basic CRUD app.

## Features

- Student, hall, building, and invigilator management
- CSV import validation for bulk data entry
- CSV export for exam and allocation reports
- Seat allocation logic for exam scheduling
- MySQL persistence with Flyway migrations
- Spring Security and JWT-style authentication setup
- Docker support for local environment setup
- Unit and service-level test coverage for validation and core logic

## Tech Stack

- Java 17
- Spring Boot 3
- Spring Data JPA
- Spring Security
- MySQL
- Flyway
- Maven
- Docker / Docker Compose
- OpenCSV
- JUnit 5 + Mockito

## Project Structure

```text
ExamMasterPro/
├── src/
│   ├── main/
│   │   ├── java/
│   │   └── resources/
│   └── test/
├── docker-compose.yml
├── Dockerfile
├── pom.xml
├── mvnw
├── mvnw.cmd
├── .env.example
├── .gitignore
├── README.md
└── .env
```

## Prerequisites

Before running the app locally, install:
- Java 17+
- Maven or use the included Maven wrapper
- Docker Desktop / Docker Engine
- MySQL (or use the provided Docker Compose setup)

## Local Setup

1. Copy the example environment file:

```bash
cp .env.example .env
```

2. Update the values in `.env` with your local credentials.

3. Start MySQL using Docker Compose:

```bash
docker compose up -d db
```

4. Run the application:

```bash
./mvnw clean spring-boot:run
```

The app will run on:

```text
http://localhost:8080
```

## Environment Variables

The project uses environment-based configuration for database and app secrets. See `.env.example` for the expected values.

Example:

```env
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/exammasterpro?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
SPRING_DATASOURCE_USERNAME=examuser
SPRING_DATASOURCE_PASSWORD=your_password
```

## Docker

You can also run the application with Docker Compose for a quick local setup:

```bash
docker compose up --build
```

## Build

```bash
./mvnw clean package
```

## Run JAR

```bash
java -jar target/exammaster-pro-0.0.1-SNAPSHOT.jar
```

## Notes

- `.env` is intentionally ignored by Git and should remain local.
- This project is meant for local development and portfolio/demo use.
- The app is designed to be simple to run and easy to extend.

## Future Improvements

- better dashboard analytics
- student and invigilator search filters
- improved allocation optimization logic
- reporting and printable exam summaries
- admin audit improvements

## License

This project is for educational and portfolio use.
