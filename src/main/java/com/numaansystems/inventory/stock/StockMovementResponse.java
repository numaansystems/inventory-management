package com.numaansystems.inventory.stock;

import java.time.Instant;

public record StockMovementResponse(
        Long id,
        Long productId,
        StockMovementType type,
        long quantityChange,
        long balanceAfter,
        String reference,
        String note,
        Instant occurredAt) {

    static StockMovementResponse from(StockMovement movement) {
        return new StockMovementResponse(
                movement.getId(),
                movement.getProduct().getId(),
                movement.getType(),
                movement.getQuantityChange(),
                movement.getBalanceAfter(),
                movement.getReference(),
                movement.getNote(),
                movement.getOccurredAt());
    }
}
