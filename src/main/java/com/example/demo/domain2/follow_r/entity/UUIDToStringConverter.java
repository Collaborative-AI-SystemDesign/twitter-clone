package com.example.demo.domain2.follow_r.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.UUID;

@Converter(autoApply = true)
public class UUIDToStringConverter implements AttributeConverter<UUID, String> {
  @Override
  public String convertToDatabaseColumn(UUID attribute) {
    return (attribute == null) ? null : attribute.toString();
  }

  @Override
  public UUID convertToEntityAttribute(String dbData) {
    return (dbData == null) ? null : UUID.fromString(dbData);
  }
}
