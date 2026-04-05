# 🎵 Music Player App — Monorepo

A full-stack music player powered by **JioSaavn Unofficial API** and **Spring Boot**.

```
music-player-app/
├── backend/            → Spring Boot REST API (Java 17)
├── jiosaavn-api/       → JioSaavn Node.js proxy (saavn-api)
├── docker-compose.yml  → Run everything with one command
└── docs/               → API documentation
```

---

## 🚀 Quick Start

### Prerequisites
- Java 17+
- Node.js 18+
- Docker & Docker Compose (optional but recommended)

### Run with Docker (Recommended)
```bash
docker-compose up --build
```

### Run manually
```bash
# Terminal 1 — JioSaavn API
cd jiosaavn-api && npm install && npm start

# Terminal 2 — Spring Boot Backend
cd backend && ./mvnw spring-boot:run
```

---

## 📡 Endpoints
| Service          | URL                          |
|-----------------|------------------------------|
| Spring Boot API  | http://localhost:8080/api    |
| JioSaavn Proxy   | http://localhost:3000        |
| Swagger UI       | http://localhost:8080/swagger-ui.html |

---

## 📱 Mobile APK Integration
The backend exposes REST APIs consumed by the Android APK.
See `docs/API_REFERENCE.md` for all endpoint details.
