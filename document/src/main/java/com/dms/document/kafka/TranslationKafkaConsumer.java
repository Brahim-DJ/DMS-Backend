package com.dms.document.kafka;

import com.dms.document.model.Document;
import com.dms.document.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TranslationKafkaConsumer {

    private final DocumentRepository documentRepository;

    @KafkaListener(
        topics = "${spring.kafka.topics.translation-completed}",
        containerFactory = "translationKafkaListenerContainerFactory"
    )
    @Transactional
    public void handleTranslationCompleted(TranslationCompletedMessage message) {
        log.info("Received translation for document ID: {}", message.getId());
        
        documentRepository.findById(message.getId())
                .ifPresentOrElse(
                    document -> {
                        document.setTranslatedTitle(message.getTranslatedTitle());
                        documentRepository.save(document);
                        log.info("Updated document with ID: {} with translated title: {}", 
                                message.getId(), message.getTranslatedTitle());
                    },
                    () -> log.warn("Document with ID: {} not found", message.getId())
                );
    }
}