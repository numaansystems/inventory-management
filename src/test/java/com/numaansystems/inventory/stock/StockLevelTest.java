package com.numaansystems.inventory.stock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.numaansystems.inventory.product.Product;
import com.numaansystems.inventory.product.Unit;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class StockLevelTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant T1 = Instant.parse("2026-01-02T00:00:00Z");

    private final Product turmeric =
            new Product("TUR-100G", "Turmeric powder", null, Unit.GRAM, new BigDecimal("100"), true, T0);

    @Test
    void startsEmpty() {
        StockLevel level = new StockLevel(turmeric, T0);

        assertThat(level.getQuantity()).isZero();
        assertThat(level.getUpdatedAt()).isEqualTo(T0);
    }

    @Test
    void appliesChangesAndReturnsNewBalance() {
        StockLevel level = new StockLevel(turmeric, T0);

        assertThat(level.apply(10, T1)).isEqualTo(10);
        assertThat(level.apply(-4, T1)).isEqualTo(6);
        assertThat(level.getQuantity()).isEqualTo(6);
        assertThat(level.getUpdatedAt()).isEqualTo(T1);
    }

    @Test
    void allowsDrawingDownToExactlyZero() {
        StockLevel level = new StockLevel(turmeric, T0);
        level.apply(3, T0);

        assertThat(level.apply(-3, T1)).isZero();
    }

    @Test
    void rejectsChangesThatWouldGoNegativeAndLeavesBalanceUntouched() {
        StockLevel level = new StockLevel(turmeric, T0);
        level.apply(2, T0);

        assertThatThrownBy(() -> level.apply(-3, T1))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessage("Insufficient stock for TUR-100G: 2 available, 3 requested")
                .extracting("code").isEqualTo("INSUFFICIENT_STOCK");
        assertThat(level.getQuantity()).isEqualTo(2);
        assertThat(level.getUpdatedAt()).isEqualTo(T0);
    }

    @Test
    void rejectsOverflow() {
        StockLevel level = new StockLevel(turmeric, T0);
        level.apply(Long.MAX_VALUE, T0);

        assertThatThrownBy(() -> level.apply(1, T1)).isInstanceOf(ArithmeticException.class);
    }
}
