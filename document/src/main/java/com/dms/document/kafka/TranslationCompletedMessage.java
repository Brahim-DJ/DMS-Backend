package com.dms.document.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranslationCompletedMessage {
    private Long id;
    private String originalTitle;
    private String translatedTitle;
    private String language;
}