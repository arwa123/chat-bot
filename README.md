# RAG Chat Storage Microservice (Spring Boot + Postgres + pgvector)

Production-ready backend skeleton that stores chat sessions/messages and includes a minimal RAG pipeline with embeddings + retrieval over `pgvector`.

## Features
- Sessions & messages CRUD (rename, favorite, delete)
- API key auth via `x-api-key`
- Rate limiting (Bucket4j) per API key
- Centralized error handling + JSON errors
- Health endpoints (`/actuator/health`)
- OpenAPI/Swagger (`/swagger-ui.html`)
- CORS allowlist
- Pagination for listing sessions & messages
- RAG:
  - Knowledge chunk upsert (auto-embeds)
  - Query-time retrieval (top-k via pgvector)
  - Prompt augmentation + LLM call (stub by default)
- Dockerized: Postgres (with pgvector), API, pgAdmin
- Flyway migrations
- `.env.example`

> NOTE: The LLM & embedding providers are **stubbed** by default so you can run locally without external keys. Flip to OpenAI by setting env vars.

---

## Quickstart

### 1) Prereqs
- Docker & Docker Compose
- Java 17 + Maven (for local run without Docker build, optional)

### 2) Configure env
```bash
cp .env.example .env
# optionally edit .env (API_KEYS, CORS_ORIGINS, etc.)
```

### 3) Run with Docker
```bash
docker compose up --build
```
- API on http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- Postgres on localhost:5432 (db=chat_bot, user=asaify, pass=)
- pgAdmin on http://localhost:5050 (admin@example.com / admin)

### 4) Smoke test
```bash
API_KEY=dev-key-123
curl -H "x-api-key: $API_KEY" -H "Content-Type: application/json"   -d '{"title":"My first chat","userId":"user-1"}'   http://localhost:8080/v1/sessions
```

Upsert knowledge:
```bash
curl -H "x-api-key: $API_KEY" -H "Content-Type: application/json"   -d '{"content":"Quarterly revenue grew 12% YoY.","source":"Q2-Report.pdf"}'   http://localhost:8080/v1/knowledge/upsert
```

Send a message (auto-RAG + generate assistant using stub LLM):
```bash
SESSION_ID=<copy from create session response>
curl -H "x-api-key: $API_KEY" -H "Content-Type: application/json"   -d '{"sender":"user","content":"What was our revenue growth?","generate":true,"userId":"user-1"}'   http://localhost:8080/v1/sessions/$SESSION_ID/messages
```

List messages:
```bash
curl -H "x-api-key: $API_KEY"   'http://localhost:8080/v1/sessions/$SESSION_ID/messages?page=0&size=50'
```

---

## Configure Providers

### Embeddings
- Default: `stub` (deterministic hash → vector)
- OpenAI: set `EMBEDDING_PROVIDER=openai` and `OPENAI_API_KEY`, `EMBEDDING_MODEL`
  - You must implement the HTTP call in `OpenAIEmbeddingClient` (left as TODO).

### Generation
- Default: `stub` (echoes prompt head)
- To use OpenAI/Anthropic: set `LLM_PROVIDER=openai|anthropic` and API keys
  - You must implement client calls in `LlmClient` implementations.

> This starter focuses on storage + retrieval. Model client code is stubbed for security and simplicity.

---

## API Overview

### Sessions
- `POST /v1/sessions` → `{ title?, userId }`
- `GET /v1/sessions?userId=...&page=0&size=20`
- `GET /v1/sessions/{id}`
- `PATCH /v1/sessions/{id}` → `{ title?, isFavorite? }`
- `DELETE /v1/sessions/{id}`

### Messages
- `POST /v1/sessions/{id}/messages` → `{ sender, content, context?, generate?, userId }`
  - If `generate=true` and `sender=user`, runs RAG + stores assistant reply
- `GET /v1/sessions/{id}/messages?page=0&size=50`

### Knowledge (RAG corpus)
- `POST /v1/knowledge/upsert` → `{ id?, content, source?, metadata? }`

### Health
- `GET /actuator/health`

### Auth
- Header `x-api-key` required (except docs/health). Keys from `API_KEYS` env var.

---

## VS Code + GitHub Copilot Agent (Enterprise)

1. **Open the folder** in VS Code:
   - `File → Open Folder… → rag-chat-backend/`

2. **Install recommended extensions** (when prompted):
   - “Extension Pack for Java”
   - “Language Support for Java by Red Hat”
   - “Spring Boot Extension Pack”
   - “Docker”
   - “GitHub Copilot Chat”

3. **Talk to the Copilot Agent** (Command Palette → “GitHub Copilot Chat: Open in Side Bar”):
   - *“Add OpenAI embeddings call in `OpenAIEmbeddingClient` using `text-embedding-3-small` and env `OPENAI_API_KEY`.”*
   - *“Generate unit tests for `RagService.retrieve()` using testcontainers for Postgres.”*
   - *“Create `OpenAiLlmClient` class calling Chat Completions and wire by `LLM_PROVIDER=openai`.”*
   - *“Refactor `ApiKeyFilter` to support hashed keys using BCrypt.”*

4. **Common Commands**:
   - Run app: `./mvnw spring-boot:run` or `mvn spring-boot:run`
   - Build image: `docker compose build api`
   - Tail logs: `docker compose logs -f api`

---

## Notes & Next Steps
- Consider Row Level Security if you centralize multiple tenants.
- Swap stub providers with real clients.
- Add JWT user auth if you move beyond API keys.
- Add cursor-based pagination.
- Add integration tests (Testcontainers: Postgres).
- Add Prometheus/Grafana for metrics & dashboards.

---