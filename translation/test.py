import os
import json
import asyncio
import uuid
from aiokafka import AIOKafkaProducer, AIOKafkaConsumer
from dotenv import load_dotenv

# Load environment variables from .env file
load_dotenv()

# Kafka configuration for local testing
# Use localhost since we're running outside of Docker
KAFKA_BOOTSTRAP_SERVERS = 'localhost:9092'
PRODUCER_TOPIC = 'document-created'
CONSUMER_TOPIC = 'translation-completed'


async def produce_sample_documents():
    """
    Produce sample documents to the document-created topic
    """
    producer = AIOKafkaProducer(
        bootstrap_servers=KAFKA_BOOTSTRAP_SERVERS,
        value_serializer=lambda v: json.dumps(v).encode('utf-8')
    )
    
    try:
        await producer.start()
        print(f"Started producer for {PRODUCER_TOPIC}")
        
        # Sample documents to translate
        documents = [
            {"id": str(uuid.uuid4()), "title": "Hello World"},
            {"id": str(uuid.uuid4()), "title": "Machine Learning Introduction"},
            {"id": str(uuid.uuid4()), "title": "Weather Forecast for Tomorrow"},
            {"id": str(uuid.uuid4()), "title": "New Features in Python 3.12"},
            {"id": str(uuid.uuid4()), "title": "How to Make a Perfect Cup of Coffee"}
        ]
        
        # Track the IDs we're sending for verification
        sent_ids = []
        
        for doc in documents:
            await producer.send_and_wait(PRODUCER_TOPIC, doc)
            sent_ids.append(doc["id"])
            print(f"Sent document: {doc}")
            # Wait a bit between messages
            await asyncio.sleep(1)
            
        return sent_ids
    finally:
        await producer.stop()


async def consume_translations(expected_ids=None):
    """
    Consume translation results from the translation-completed topic
    """
    consumer = AIOKafkaConsumer(
        CONSUMER_TOPIC,
        bootstrap_servers=KAFKA_BOOTSTRAP_SERVERS,
        group_id="test-consumer-group",
        auto_offset_reset="earliest",
        value_deserializer=lambda m: json.loads(m.decode('utf-8'))
    )
    
    try:
        await consumer.start()
        print(f"Started consumer for {CONSUMER_TOPIC}")
        
        # Track which IDs we've received translations for
        received_ids = set()
        if expected_ids:
            expected_ids_set = set(expected_ids)
        
        # Listen for translations for 30 seconds or until all expected messages are received
        end_time = asyncio.get_event_loop().time() + 30
        
        while asyncio.get_event_loop().time() < end_time:
            # If we've received all expected translations, break early
            if expected_ids and received_ids == expected_ids_set:
                print("\nAll expected translations received!")
                break
                
            try:
                # Poll for messages with a timeout
                messages = await consumer.getmany(timeout_ms=1000)
                
                for tp, msgs in messages.items():
                    for msg in msgs:
                        translation = msg.value
                        doc_id = translation.get('id')
                        
                        print("\n--- Received Translation ---")
                        print(f"Document ID: {doc_id}")
                        print(f"Original Title: {translation.get('original_title')}")
                        print(f"Translated Title: {translation.get('translated_title')}")
                        print(f"Language: {translation.get('language')}")
                        print("---------------------------\n")
                        
                        if expected_ids and doc_id in expected_ids_set:
                            received_ids.add(doc_id)
                
                # Small delay before polling again
                await asyncio.sleep(0.5)
                    
            except Exception as e:
                print(f"Error consuming message: {e}")
                await asyncio.sleep(1)
                
        # Report on missing translations if any
        if expected_ids and received_ids != expected_ids_set:
            missing = expected_ids_set - received_ids
            print(f"WARNING: Did not receive translations for {len(missing)} document(s):")
            for missing_id in missing:
                print(f"  - {missing_id}")
                
    finally:
        await consumer.stop()


async def run_test():
    """
    Run the complete test: produce documents and consume translations
    """
    print("Starting Kafka translation test...")
    
    # Start by producing sample documents
    sent_ids = await produce_sample_documents()
    
    print("\nWaiting for translations...")
    # Consume translations, checking for the specific IDs we sent
    await consume_translations(expected_ids=sent_ids)
    
    print("Test completed!")


if __name__ == "__main__":
    asyncio.run(run_test())