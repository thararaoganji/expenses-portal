# Getting Started


### Testcontainers support

This project uses [Testcontainers at development time](https://docs.spring.io/spring-boot/4.0.2/reference/features/dev-services.html#features.dev-services.testcontainers).

Testcontainers has been configured to use the following Docker images:

* [`postgres:latest`](https://hub.docker.com/_/postgres)

Please review the tags of the used images and set them to the same as you're running in production.

## 1. Running the PostgreSQL Database with Docker

To get the PostgreSQL database up and running, use the following Docker command:

```bash
docker run -d --name expense-postgres -e POSTGRES_DB=expense_db -e POSTGRES_USER=expense_user -e POSTGRES_PASSWORD=expense_pass -p 5432:5432 postgres:alpine
```

### Command Explanation:
- `-d`: Runs the container in detached mode (in the background).
- `--name expense-postgres`: Assigns a name to your container, making it easier to reference.
- `-e POSTGRES_DB=expense_db`: Sets the name of the database to be created inside the container.
- `-e POSTGRES_USER=expense_user`: Sets the username for the database.
- `-e POSTGRES_PASSWORD=expense_pass`: Sets the password for the database user.
- `-p 5432:5432`: Maps port 5432 on your host machine to port 5432 inside the container, allowing your application to connect to it.
- `postgres:alpine`: Specifies the Docker image to use (a lightweight PostgreSQL image).

### Verification:

You can check if the container is running by executing:

```bash
docker ps
```

You should see `expense-postgres` listed among the running containers.

### POST - /auth/login bash
```
curl -X POST http://localhost:8080/auth/login \
-H "Content-Type: application/json" \
-d '{
"username": "user_employee",
"password": "password_employee"
}'
```

### windows powershell
```
Invoke-WebRequest -Uri http://localhost:8080/auth/login `
    -Method POST `
-Headers @{"Content-Type"="application/json"} `
-Body '{
"username": "user_employee",
"password": "password-employee"
}'
```

### docker-compose up
```
docker-compose up --build
```
