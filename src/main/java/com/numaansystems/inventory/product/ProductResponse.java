package com.numaansystems.inventory.product;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductResponse(
        Long id,
        String sku,
        String name,
        String description,
        Unit unit,
        BigDecimal packSize,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {

    static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getDescription(),
                product.getUnit(),
                product.getPackSize(),
                product.isActive(),
                product.getCreatedAt(),
                product.getUpdatedAt());
    }
}
