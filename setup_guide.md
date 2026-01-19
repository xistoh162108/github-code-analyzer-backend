# 🚀 Backend Setup & Execution Guide

This guide will help you set up and run the GitHub Analyzer backend server.

## 📋 Prerequisites
- **JDK 17** or higher
- **MySQL 8.0+**
- **Redis 6.0+**
- **GitHub App** (for Webhooks and API access)
- **OpenAI API Key** (for AI Analysis)

## ⚙️ Environment Configuration

### 1. External Services
Ensure your `MySQL` and `Redis` are running.
- **MySQL**: Create a database named `springstudy`.
- **Redis**: Default port `6379`.

### 2. Environment Variables
You need to set the following environment variables (or update them in `application.yaml`):

```bash
# Required for AI Analysis
export OPENAI_API_KEY='your_openai_key'

# Required for GitHub App Integration
export GITHUB_APP_ID='your_app_id'
export GITHUB_WEBHOOK_SECRET='your_webhook_secret'
# Path to your github-app.pem file
export GITHUB_APP_PRIVATE_KEY_PATH='/path/to/your/github-app.pem'
```

## 🏃 Building & Running

### Using Gradle Wrapper
Navigate to the project root and run:

```bash
# 1. Clean and build (skipping tests for quick start)
./gradlew clean build -x test

# 2. Run the application
./gradlew bootRun
```

The server will start on `http://localhost:8080`.

## 🛠️ Troubleshooting

### 1. Dependency Issues
If you see errors related to `WebClient` or missing imports, try:
```bash
./gradlew clean build --refresh-dependencies
```

### 2. Database Connection
Ensure the `springuser` has permissions to the `springstudy` database. You can change these in `application.yaml`:
```yaml
spring:
  datasource:
    username: your_username
    password: your_password
```

### 3. Port Conflict
If `8080` is in use, kill the process:
```bash
lsof -i :8080 -t | xargs kill -9
```
