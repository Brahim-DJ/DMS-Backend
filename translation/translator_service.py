import os
import json
import asyncio
from typing import Dict, Any, Optional
from contextlib import asynccontextmanager
from fastapi import FastAPI
from aiokafka import AIOKafkaConsumer, AIOKafkaProducer
import google.generativeai as genai
from dotenv import load_dotenv

# Load environment variables from .env file
load_dotenv()

# Configure Gemini API with key from environment variables
GEMINI_API_KEY = os.getenv("GEMINI_API_KEY")
if not GEMINI_API_KEY:
    raise ValueError("GEMINI_API_KEY environment variable not set")

genai.configure(api_key=GEMINI_API_KEY)
model = genai.GenerativeModel('gemini-2.0-flash')

# Kafka configuration for local testing
# Use 'localhost:9092' since we're connecting from outside Docker to the exposed port
KAFKA_BOOTSTRAP_SERVERS = 'localhost:9092'
CONSUMER_TOPIC = 'document-created'
PRODUCER_TOPIC = 'translation-completed'

# Global variables to store Kafka producer and consumer task
consumer_task: Optional[asyncio.Task] = None
producer: Optional[AIOKafkaProducer] = None


async def translate_text(text: str) -> str:
    """
    Translate the given text to Spanish using Gemini API
    """
    prompt = f"Translate the following text to Spanish (answer concisely): '{text}'"
    response = await model.generate_content_async(prompt)
    return response.text.strip()


async def consume_messages(producer: AIOKafkaProducer):
    """
    Continuously consume messages from the document-created topic,
    translate titles, and send the translations to the translation-completed topic
    """
    consumer = AIOKafkaConsumer(
        CONSUMER_TOPIC,
        bootstrap_servers=KAFKA_BOOTSTRAP_SERVERS,
        group_id="translation-service-group",
        auto_offset_reset="earliest",  # To catch messages from the beginning during testing
        value_deserializer=lambda m: json.loads(m.decode('utf-8'))
    )
    
    try:
        await consumer.start()
        print(f"Started consuming from {CONSUMER_TOPIC}")
        
        async for message in consumer:
            try:
                document = message.value
                print(f"Received document: {document}")
                
                # Check if the message has the expected format
                if "title" in document and "id" in document:
                    document_id = document["id"]
                    original_title = document["title"]
                    
                    # Translate the title to Spanish
                    translated_title = await translate_text(original_title)
                    
                    # Prepare response message
                    response = {
                        "id": document_id,
                        "original_title": original_title,
                        "translated_title": translated_title,
                        "language": "es"
                    }
                    
                    # Send response to the translation-completed topic
                    await producer.send_and_wait(
                        PRODUCER_TOPIC,
                        json.dumps(response).encode('utf-8')
                    )
                    print(f"Sent translation: {response}")
                else:
                    print(f"Skipping message with invalid format: {document}")
                    
            except Exception as e:
                print(f"Error processing message: {e}")
                
    finally:
        await consumer.stop()


@asynccontextmanager
async def lifespan(app: FastAPI):
    """
    This replaces the @app.on_event("startup") and @app.on_event("shutdown") pattern
    with the newer lifespan pattern recommended in FastAPI
    """
    global producer, consumer_task
    
    # Startup: Initialize Kafka producer and start consumer task
    try:
        print("Starting Kafka producer and consumer...")
        producer = AIOKafkaProducer(bootstrap_servers=KAFKA_BOOTSTRAP_SERVERS)
        await producer.start()
        
        # Start consumer in background task
        consumer_task = asyncio.create_task(consume_messages(producer))
        print("Kafka producer and consumer started successfully.")
        
        yield  # This is where FastAPI serves requests
    
    # Shutdown: Clean up resources
    finally:
        print("Shutting down Kafka producer and consumer...")
        if consumer_task:
            consumer_task.cancel()
            try:
                await consumer_task
            except asyncio.CancelledError:
                pass
            print("Consumer task cancelled.")
        
        if producer:
            await producer.stop()
            print("Kafka producer stopped.")


# Create FastAPI app with the lifespan context manager
app = FastAPI(
    title="Document Translation Service",
    lifespan=lifespan
)


@app.get("/health")
async def health_check():
    """Health check endpoint"""
    return {"status": "healthy", "service": "document-translation-service"}


@app.get("/")
async def root():
    """Root endpoint"""
    return {
        "service": "Document Translation Service",
        "status": "running",
        "topics": {
            "input": CONSUMER_TOPIC,
            "output": PRODUCER_TOPIC
        }
    }


if __name__ == "__main__":
    import uvicorn
    uvicorn.run("translator_service:app", host="0.0.0.0", port=8000, reload=True)