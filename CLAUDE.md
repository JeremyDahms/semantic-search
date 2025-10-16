# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Spring Boot 3 REST API that provides semantic search over industry codes (e.g., ICD-10 medical codes) using vector embeddings. It uses Ollama's `nomic-embed-text` model to generate 768-dimensional embeddings and PostgreSQL with the pgvector extension for vector similarity search.

**Core Technologies:**
- Java 21
- Spring Boot 3.5.6
- PostgreSQL 16 with pgvector extension
- Hibernate 6.6.4 with hibernate-vector
- Ollama (local LLM for embeddings)
- Maven

## Development Commands

### Running the Application

**Local development (requires PostgreSQL + Ollama running locally):**
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

**With Docker Compose (recommended):**
```bash
# Start services (PostgreSQL + Ollama)
docker-compose up -d

# Pull Ollama model (first time only)
./scripts/init-ollama.sh
# Or manually: docker exec semantic-search-ollama ollama pull nomic-embed-text

# Run Spring Boot app
./mvnw spring-boot:run
```

### Testing & Building

```bash
# Run tests
./mvnw test

# Build JAR
./mvnw clean package

# Run JAR
java -jar target/semantic-search-0.0.1-SNAPSHOT.jar
```

### Database Access

```bash
# Connect to PostgreSQL (Docker)
docker exec -it semantic-search-postgres psql -U postgres -d semantic_search

# Check if pgvector is installed
# Inside psql:
\dx vector

# View table schema
\d industry_codes

# Drop table to reset (WARNING: deletes all data)
DROP TABLE industry_codes;
```

## Architecture

### Request Flow

1. **Upload Flow** (POST /api/codes/upload or /upload-csv):
   - Controller receives code + description
   - OllamaService generates 768-dim embedding via HTTP to Ollama API
   - Entity saved to PostgreSQL with vector column
   - CSV uploads process each row sequentially (batch saved at end)

2. **Search Flow** (GET /api/codes/search):
   - Controller receives natural language query
   - OllamaService generates embedding for query
   - Repository uses pgvector's `<=>` operator (cosine distance)
   - Returns top N results sorted by similarity (1.0 = identical, 0.0 = unrelated)

### Key Components

**Entity Layer** (`entity/IndustryCode.java`):
- JPA entity with vector column using `@JdbcTypeCode(SqlTypes.VECTOR)`
- `@Array(length = 768)` specifies vector dimensions (must match Ollama model)
- Fluent setters return `this` for method chaining

**Repository Layer** (`repository/CodeRepository.java`):
- Native SQL query using pgvector's `<=>` operator for cosine distance
- `findSimilarCodes()` returns interface projection `CodeSimilarity`
- Similarity calculated as `1 - (embedding <=> queryEmbedding)`

**Service Layer**:
- `OllamaService.java`: Wraps Ollama API calls, generates embeddings
- `CodeService.java`: Handles CSV parsing and batch processing

**Controller Layer** (`controller/CodeController.java`):
- REST endpoints at `/api/codes`
- Coordinates between services and repository
- Manual mapping from `CodeSimilarity` projection to `SearchResult` DTO

### Vector Search Implementation

**Critical details:**
- pgvector cosine distance operator: `<=>` (0 = identical, 2 = opposite)
- Similarity score: `1 - distance` (converts to 0-1 scale, higher = more similar)
- Query embedding must be cast: `cast(:queryEmbedding as vector)`
- Embedding dimension is hardcoded to 768 (nomic-embed-text model)

**Repository query:**
```sql
SELECT id, code, description, embedding,
       (1 - (embedding <=> cast(:queryEmbedding as vector))) as similarity
FROM industry_codes
WHERE embedding IS NOT NULL
ORDER BY embedding <=> cast(:queryEmbedding as vector)
LIMIT :limit
```

## Configuration

### Application Profiles

**Default** (`application.properties`):
- Uses container hostnames: `postgres:5432`, `ollama:11434`
- For Docker Compose deployments

**Local** (`application-local.properties`):
- Uses localhost: `localhost:5432`, `http://localhost:11434`
- For running Spring Boot outside Docker

**To switch profiles:**
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

### Important Properties

```properties
# Batch processing (CSV uploads)
spring.jpa.properties.hibernate.jdbc.batch_size=50

# Schema management
spring.jpa.hibernate.ddl-auto=update  # Auto-creates tables

# Ollama API URL (injectable via @Value)
ollama.api.url=http://ollama:11434/api/embeddings  # Default
ollama.api.url=http://localhost:11434/api/embeddings  # Local
```

### Ollama Integration

**Model:** `nomic-embed-text` (768 dimensions)
**API endpoint:** `/api/embeddings`

**Request format:**
```json
{
  "model": "nomic-embed-text",
  "prompt": "Type 2 diabetes mellitus without complications"
}
```

**Response format:**
```json
{
  "embedding": [0.8643, 0.5875, ..., -0.1234]  // 768 floats
}
```

**If changing models:**
1. Update model name in `OllamaService.java:20`
2. Update vector dimension in `IndustryCode.java:29` (`@Array(length = ...)`)
3. Drop and recreate database table (dimension mismatch will cause errors)

## Common Development Tasks

### Adding a New Endpoint

1. Add method to `CodeController.java`
2. Inject required services via constructor
3. Use `repository.findSimilarCodes()` for vector search
4. Create DTO in `dto/` package if needed

### Modifying Vector Search

- Query is in `CodeRepository.java:15-23` (native SQL)
- pgvector operators: `<=>` (cosine), `<->` (L2), `<#>` (inner product)
- To change similarity metric, update operator and similarity calculation

### CSV Upload Format

Expected format (header required):
```csv
code,description
E11.9,Type 2 diabetes mellitus without complications
```

Parser: `CodeService.java:27-74`
- Splits on first comma (allows commas in description)
- Skips empty lines
- Processes sequentially (one Ollama call per row)

### Testing Vector Search

```bash
# Upload sample data
curl -X POST http://localhost:8080/api/codes/upload-csv \
  -F "file=@data/sample_codes.csv"

# Search (URL-encode query)
curl "http://localhost:8080/api/codes/search?query=high%20blood%20sugar&limit=5"
```

## Troubleshooting

### "Connection refused" to Ollama
- Verify Ollama is running: `curl http://localhost:11434/api/tags`
- Check correct profile is active (`local` vs default)
- Docker: `docker-compose logs ollama`

### "Model not found" error
- Pull model: `docker exec semantic-search-ollama ollama pull nomic-embed-text`
- Or: `ollama pull nomic-embed-text` (if running locally)

### Vector dimension mismatch
- Error: "expected 768 dimensions, got X"
- Solution: Drop table and let Hibernate recreate it
- Root cause: Changed model without updating `@Array(length = 768)`

### Slow CSV uploads
- Normal behavior: Each row requires an Ollama API call (~1-2 sec each)
- 100 codes = ~2-3 minutes
- Progress logged to console: "Processed: E11.9 (5 total)"

### Tables not created
- Check `spring.jpa.hibernate.ddl-auto=update` is set
- Check database connection (credentials in application.properties)
- View SQL: `spring.jpa.show-sql=true`
