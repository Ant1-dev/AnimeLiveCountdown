package com.Toine.animeCountdownBackend.models.discovery;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.List;

/** Stores a show's streaming links as a JSON array in one TEXT column. */
@Converter
public class StreamingLinksConverter implements AttributeConverter<List<StreamingLink>, String> {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<StreamingLink>> TYPE = new TypeReference<>() {};

    @Override
    public String convertToDatabaseColumn(List<StreamingLink> links) {
        if (links == null) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(links);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Could not serialize streaming links", e);
        }
    }

    @Override
    public List<StreamingLink> convertToEntityAttribute(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readValue(json, TYPE);
        } catch (JsonProcessingException e) {
            // A bad value shouldn't break the whole media_info row; the next sync rewrites it.
            return List.of();
        }
    }
}
