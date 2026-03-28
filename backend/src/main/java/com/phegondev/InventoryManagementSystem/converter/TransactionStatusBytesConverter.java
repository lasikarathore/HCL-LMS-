package com.phegondev.InventoryManagementSystem.converter;

import com.phegondev.InventoryManagementSystem.enums.TransactionStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.nio.charset.StandardCharsets;

/**
 * Maps {@link TransactionStatus} to PostgreSQL {@code bytea} when the column was created
 * as binary instead of {@code varchar}. Also supports UTF-8 enum names stored in bytea.
 */
@Converter
public class TransactionStatusBytesConverter implements AttributeConverter<TransactionStatus, byte[]> {

    @Override
    public byte[] convertToDatabaseColumn(TransactionStatus attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.name().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public TransactionStatus convertToEntityAttribute(byte[] dbData) {
        if (dbData == null || dbData.length == 0) {
            return null;
        }
        String raw = new String(dbData, StandardCharsets.UTF_8).trim();
        if (!raw.isEmpty()) {
            try {
                return TransactionStatus.valueOf(raw);
            } catch (IllegalArgumentException ignored) {
                // fall through to ordinal
            }
        }
        if (dbData.length == 1) {
            int ord = dbData[0] & 0xFF;
            TransactionStatus[] vals = TransactionStatus.values();
            if (ord < vals.length) {
                return vals[ord];
            }
        }
        return TransactionStatus.COMPLETED;
    }
}
