# Semantic Code Search Application

A Spring Boot REST API that enables semantic search over industry codes using vector embeddings. Upload medical codes (ICD-10) or any code/description pairs, and search using natural language queries powered by Ollama embeddings and PostgreSQL pgvector.

## Features

- 🔍 **Semantic Search** - Find codes by meaning, not just keywords
- 📤 **Batch Upload** - Import codes via CSV files
- 🐳 **Docker Ready** - One-command setup with Docker Compose
- 🚀 **Fast Vector Search** - Powered by pgvector cosine similarity
- 🤖 **Local AI** - Uses Ollama for embeddings (no API keys needed)

## Tech Stack

- **Java 21** - Modern Java with records and pattern matching
- **Spring Boot 3.3.5** - REST API framework
- **PostgreSQL 16** - Database with pgvector extension
- **Ollama** - Local LLM for generating embeddings (nomic-embed-text model)
- **Hibernate 6.x** - ORM with vector type support
- **Maven** - Dependency management and build tool

---

## Quick Start (with Docker Compose)

### Prerequisites

- [Docker Desktop](https://www.docker.com/products/docker-desktop) (or Podman) installed and running
- Java 21 (check with `java -version`)
- Maven 3.6+ (or use included `./mvnw`)

### Steps

1. **Clone and navigate to project:**
```bash
   cd semantic-search
```

2. **Start PostgreSQL and Ollama:**
```bash
   docker-compose up -d
```

3. **Pull the Ollama embedding model** (first time only, ~1-2 minutes):
```bash
   ./scripts/init-ollama.sh
```

Or manually:
```bash
   docker exec semantic-search-ollama ollama pull nomic-embed-text
```

4. **Run the Spring Boot application:**
```bash
   ./mvnw spring-boot:run
```

5. **Verify it's running:**
```bash
   curl http://localhost:8080/api/codes
```

---

## Alternative Setup (without Docker Compose)

If you prefer to run PostgreSQL and Ollama natively on your machine:

### Prerequisites

- Java 21
- Maven 3.6+
- **PostgreSQL 16+** with pgvector extension installed
- **Ollama** installed locally

### PostgreSQL Setup

**macOS (Homebrew):**
```bash
brew install postgresql@16 pgvector
brew services start postgresql@16

# Create database
createdb semantic_search
psql semantic_search -c "CREATE EXTENSION vector;"
```

**Ubuntu/Debian:**
```bash
sudo apt install postgresql-16 postgresql-16-pgvector
sudo systemctl start postgresql

# Create database
sudo -u postgres createdb semantic_search
sudo -u postgres psql semantic_search -c "CREATE EXTENSION vector;"
```

**Update credentials if needed:**  
Edit `src/main/resources/application.properties` if your PostgreSQL uses different credentials.

### Ollama Setup

**macOS/Linux:**
```bash
# Install Ollama
curl -fsSL https://ollama.com/install.sh | sh

# Start Ollama service
ollama serve

# In another terminal, pull the embedding model
ollama pull nomic-embed-text
```

**Verify Ollama is running:**
```bash
curl http://localhost:11434/api/tags
```

### Run the Application
```bash
./mvnw spring-boot:run
```

---

## API Usage

### 1. Upload a Single Code
```bash
curl -X POST http://localhost:8080/api/codes/upload \
  -H "Content-Type: application/json" \
  -d '{
    "code": "E11.9",
    "description": "Type 2 diabetes mellitus without complications"
  }'
```

**Response:**
```json
{
  "id": 1,
  "code": "E11.9",
  "description": "Type 2 diabetes mellitus without complications",
  "embedding": [0.8643, 0.5875, ...]
}
```

### 2. Upload Codes from CSV
```bash
curl -X POST http://localhost:8080/api/codes/upload-csv \
  -F "file=@data/sample_codes.csv"
```

**CSV Format:**
```csv
code,description
E11.9,Type 2 diabetes mellitus without complications
I10,Essential (primary) hypertension
E78.5,Hyperlipidemia, unspecified
```

**Response:**
```json
{
  "totalProcessed": 100,
  "successful": 100,
  "failed": 0,
  "message": "Successfully uploaded 100 codes"
}
```

### 3. Semantic Search
```bash
curl "http://localhost:8080/api/codes/search?query=high%20blood%20sugar&limit=5"
```

**Response:**
```json
[
  {
    "code": {
      "id": 1,
      "code": "E11.9",
      "description": "Type 2 diabetes mellitus without complications",
      "embedding": null
    },
    "similarity": 0.8234
  },
  ...
]
```

**Query Parameters:**
- `query` (required) - Natural language search query
- `limit` (optional, default=5) - Number of results to return

### 4. Get All Codes
```bash
curl http://localhost:8080/api/codes
```

---

## Example Searches

**Diabetes-related:**
```bash
curl "http://localhost:8080/api/codes/search?query=high%20blood%20sugar"
curl "http://localhost:8080/api/codes/search?query=insulin%20problems"
```

**Heart conditions:**
```bash
curl "http://localhost:8080/api/codes/search?query=chest%20pain%20heart%20attack"
curl "http://localhost:8080/api/codes/search?query=irregular%20heartbeat"
```

**Respiratory issues:**
```bash
curl "http://localhost:8080/api/codes/search?query=difficulty%20breathing"
curl "http://localhost:8080/api/codes/search?query=chronic%20cough"
```

---

## Project Structure
```
semantic-search/
├── src/main/java/com/jdahms/semantic_search/
│   ├── SemanticSearchApplication.java    # Main entry point
│   ├── config/
│   │   └── AppConfig.java                # RestTemplate bean
│   ├── controller/
│   │   └── CodeController.java           # REST endpoints
│   ├── dto/
│   │   ├── CodeSimilarity.java           # Projection interface
│   │   ├── SearchResult.java             # Search response
│   │   └── UploadResponse.java           # Upload response
│   ├── entity/
│   │   └── IndustryCode.java             # JPA entity
│   ├── repository/
│   │   └── CodeRepository.java           # Data access
│   └── service/
│       ├── CodeService.java              # Business logic
│       └── OllamaService.java            # Embedding generation
├── src/main/resources/
│   └── application.properties            # Configuration
├── data/
│   └── sample_codes.csv                  # Sample data
├── scripts/
│   └── init-ollama.sh                    # Ollama setup script
├── docker-compose.yml                    # Service orchestration
├── pom.xml                               # Maven dependencies
└── README.md
```

---

## Configuration

**`src/main/resources/application.properties`:**
```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/semantic_search
spring.datasource.username=postgres
spring.datasource.password=postgres

# JPA/Hibernate
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

# Batch Processing
spring.jpa.properties.hibernate.jdbc.batch_size=50

# File Upload
spring.servlet.multipart.max-file-size=10MB
```

**Change these if needed:**
- Database credentials
- Batch size (for CSV uploads)
- SQL logging
- File upload limits

---

## Troubleshooting

### Check Service Status

**With Docker Compose:**
```bash
docker-compose ps
```

**View logs:**
```bash
docker-compose logs postgres
docker-compose logs ollama
```

### Common Issues

**1. "Connection refused" to PostgreSQL**
- Ensure PostgreSQL is running: `docker-compose ps` or `pg_isready`
- Check port 5432 is available: `lsof -i :5432`

**2. "Connection refused" to Ollama**
- Ensure Ollama is running: `curl http://localhost:11434/api/tags`
- Check port 11434 is available: `lsof -i :11434`

**3. "Model not found" error**
- Pull the model: `docker exec semantic-search-ollama ollama pull nomic-embed-text`

**4. Slow CSV upload**
- Normal! Generating 100 embeddings takes ~2-3 minutes
- Each embedding requires an Ollama API call
- Watch progress in application logs

**5. Vector dimension mismatch**
- Ensure you're using `nomic-embed-text` (768 dimensions)
- If table exists with wrong dimension, drop and recreate:
```bash
  docker exec -it semantic-search-postgres psql -U postgres -d semantic_search
  DROP TABLE industry_codes;
  \q
  # Restart app to recreate table
```

### Reset Everything

**With Docker Compose:**
```bash
# Stop and remove containers + volumes (deletes all data)
docker-compose down -v

# Start fresh
docker-compose up -d
./scripts/init-ollama.sh
./mvnw spring-boot:run
```

**Without Docker Compose:**
```bash
# Drop and recreate database
psql semantic_search -c "DROP TABLE industry_codes;"
./mvnw spring-boot:run
```

---

## Development

**Run tests:**
```bash
./mvnw test
```

**Build JAR:**
```bash
./mvnw clean package
java -jar target/semantic-search-0.0.1-SNAPSHOT.jar
```

**Hot reload (with Spring Boot DevTools):**
- Already included in dependencies
- Code changes auto-reload while running

---

## How It Works

1. **Upload:** Code descriptions are sent to Ollama's `nomic-embed-text` model
2. **Embedding:** Ollama generates a 768-dimensional vector representing semantic meaning
3. **Storage:** Vector is stored in PostgreSQL using the pgvector extension
4. **Search:** Query text is converted to a vector, then pgvector's cosine similarity finds the most similar codes
5. **Ranking:** Results are ranked by similarity score (1.0 = identical, 0.0 = unrelated)

**Why it works:**  
Semantically similar descriptions have similar embeddings in vector space. "High blood sugar" and "Type 2 diabetes" are close together in 768-dimensional space, even though they share no words.

---

## License

This is a learning project. Feel free to use and modify as needed.

---

## Contributing

This is a personal learning project, but suggestions are welcome! Open an issue or submit a pull request.

---

## Contact

Questions? Issues? Reach out to the team or check the troubleshooting section above.