#!/bin/bash

echo "Checking if Ollama container is running..."
if ! docker ps | grep -q semantic-search-ollama; then
    echo "Error: Ollama container is not running. Start with: docker-compose up -d ollama"
    exit 1
fi

echo "Pulling nomic-embed-text model..."
docker exec semantic-search-ollama ollama pull nomic-embed-text

echo "Model ready!"