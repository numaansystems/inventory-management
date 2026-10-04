package com.numaansystems.inventory.stock;

import com.numaansystems.inventory.product.Product;
import com.numaansystems.inventory.product.Unit;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Stock on hand for one product. {@code quantity} is in packs; {@code totalAmount} is the same stock
 * expressed in {@code unit} (quantity x packSize).
 */
public record StockLevelResponse(
        Long productId,
        String sku,
        String name,
        long quantity,
        Unit unit,
        BigDecimal packSize,
        BigDecimal totalAmount,
        Instant updatedAt) {

    static StockLevelResponse from(StockLevel level) {
        Product product = level.getProduct();
        return new StockLevelResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                level.getQuantity(),
                product.getUnit(),
                product.getPackSize(),
                product.getPackSize().multiply(BigDecimal.valueOf(level.getQuantity())),
                level.getUpdatedAt());
    }
}
