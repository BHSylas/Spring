package com.example.spring.util;


import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;
import java.util.Collections;

@Converter
public class StringListConverter implements AttributeConverter<List<String>, String> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // List<String> -> String (DB에 저장)
    @Override
    public String convertToDatabaseColumn(List<String> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }
        // Java List를 JSON 문자열로 직렬화
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (Exception e) {
            throw new IllegalArgumentException("List<String> JSON 변환 실패", e);
        }
    }

    // String (DB에서 읽음) -> List<String>
    @Override
    public List<String> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return Collections.emptyList();
        }
        // JSON 문자열을 Java List로 역직렬화
        try {
            return objectMapper.readValue(
                    dbData,
                    new TypeReference<List<String>>() {}
            );
        } catch (Exception e) {
            throw new IllegalArgumentException("JSON → List<String> 변환 실패", e);
        }
    }
}
