package com.dms.document.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentKafkaProducer {

    @Value("${spring.kafka.topics.document-created}")
    private String documentCreatedTopic;
    
    private final KafkaTemplate<String, DocumentCreatedMessage> kafkaTemplate;
    
    public void sendDocumentCreatedMessage(Long documentId, String title) {
        DocumentCreatedMessage message = DocumentCreatedMessage.builder()
                .id(documentId)
                .title(title)
                .build();
        
        log.info("Sending document created message for document ID: {}", documentId);
        kafkaTemplate.send(documentCreatedTopic, String.valueOf(documentId), message);
    }
}