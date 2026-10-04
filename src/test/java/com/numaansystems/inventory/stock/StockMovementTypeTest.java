package com.numaansystems.inventory.stock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.numaansystems.inventory.common.InvalidRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

class StockMovementTypeTest {

    @Test
    void receiveAddsStock() {
        assertThat(StockMovementType.RECEIVE.toSignedChange(12)).isEqualTo(12);
    }

    @Test
    void sellRemovesStock() {
        assertThat(StockMovementType.SELL.toSignedChange(5)).isEqualTo(-5);
    }

    @ParameterizedTest
    @ValueSource(longs = {-3, 7})
    void adjustPassesSignedDeltaThrough(long delta) {
        assertThat(StockMovementType.ADJUST.toSignedChange(delta)).isEqualTo(delta);
    }

    @ParameterizedTest
    @EnumSource(value = StockMovementType.class, names = {"RECEIVE", "SELL"})
    void receiveAndSellRequirePositiveQuantity(StockMovementType type) {
        assertThatThrownBy(() -> type.toSignedChange(0))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("greater than 0");
        assertThatThrownBy(() -> type.toSignedChange(-1)).isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void adjustRejectsZero() {
        assertThatThrownBy(() -> StockMovementType.ADJUST.toSignedChange(0))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("must not be 0");
    }
}
