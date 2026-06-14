package com.muasamcong.config;

import com.muasamcong.enums.RecordStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class RecordStatusConverter implements AttributeConverter<RecordStatus, Integer> {
    @Override
    public Integer convertToDatabaseColumn(RecordStatus attribute) {
        return attribute == null ? RecordStatus.ACTIVE.getCode() : attribute.getCode();
    }

    @Override
    public RecordStatus convertToEntityAttribute(Integer dbData) {
        return RecordStatus.fromCode(dbData);
    }
}
