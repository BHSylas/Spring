package com.example.spring.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
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
        try {
            // Java List를 JSON 문자열로 직렬화
            return objectMapper.writeValueAsString(attribute);
        } catch (IOException e) {
            // 직렬화 실패 시 예외 처리
            throw new RuntimeException("JSON serialization error", e);
        }
    }

    // String (DB에서 읽음) -> List<String>
    @Override
    public List<String> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            // JSON 문자열을 Java List로 역직렬화
            return objectMapper.readValue(dbData, new TypeReference<List<String>>() {});
        } catch (IOException e) {
            // 역직렬화 실패 시 예외 처리
            throw new RuntimeException("JSON deserialization error", e);
        }
    }
}
