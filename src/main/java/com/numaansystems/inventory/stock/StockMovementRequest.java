package com.numaansystems.inventory.stock;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Records a stock movement. {@code quantity} is in packs: positive for {@code RECEIVE} and {@code SELL},
 * a signed non-zero delta for {@code ADJUST}.
 */
public record StockMovementRequest(
        @NotNull StockMovementType type,
        @NotNull Long quantity,
        @Size(max = 100) String reference,
        @Size(max = 500) String note) {}
