package com.numaansystems.inventory.stock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.numaansystems.inventory.common.BusinessRuleException;
import com.numaansystems.inventory.common.InvalidRequestException;
import com.numaansystems.inventory.common.NotFoundException;
import com.numaansystems.inventory.product.Product;
import com.numaansystems.inventory.product.Unit;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StockServiceTest {

    private static final Instant NOW = Instant.parse("2026-03-15T10:00:00Z");
    private static final long PRODUCT_ID = 42L;

    private final StockLevelRepository stockLevels = mock();
    private final StockMovementRepository stockMovements = mock();
    private final StockService service =
            new StockService(stockLevels, stockMovements, Clock.fixed(NOW, ZoneOffset.UTC));

    private Product cumin;
    private StockLevel level;

    @BeforeEach
    void setUp() {
        cumin = new Product("CUM-250G", "Cumin seeds", null, Unit.GRAM, new BigDecimal("250"), true, NOW);
        level = new StockLevel(cumin, NOW);
        level.apply(10, NOW);
        when(stockLevels.findForUpdate(PRODUCT_ID)).thenReturn(Optional.of(level));
        when(stockMovements.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void receiveIncreasesStockAndRecordsMovement() {
        StockMovement movement = service.record(PRODUCT_ID, request(StockMovementType.RECEIVE, 5));

        assertThat(level.getQuantity()).isEqualTo(15);
        assertThat(movement.getType()).isEqualTo(StockMovementType.RECEIVE);
        assertThat(movement.getQuantityChange()).isEqualTo(5);
        assertThat(movement.getBalanceAfter()).isEqualTo(15);
        assertThat(movement.getReference()).isEqualTo("REF-1");
        assertThat(movement.getOccurredAt()).isEqualTo(NOW);
        assertThat(movement.getProduct()).isSameAs(cumin);
    }

    @Test
    void sellDecreasesStock() {
        StockMovement movement = service.record(PRODUCT_ID, request(StockMovementType.SELL, 4));

        assertThat(level.getQuantity()).isEqualTo(6);
        assertThat(movement.getQuantityChange()).isEqualTo(-4);
        assertThat(movement.getBalanceAfter()).isEqualTo(6);
    }

    @Test
    void negativeAdjustmentDecreasesStock() {
        StockMovement movement = service.record(PRODUCT_ID, request(StockMovementType.ADJUST, -2));

        assertThat(movement.getQuantityChange()).isEqualTo(-2);
        assertThat(level.getQuantity()).isEqualTo(8);
    }

    @Test
    void overSellingIsRejectedWithoutWritingAMovement() {
        assertThatThrownBy(() -> service.record(PRODUCT_ID, request(StockMovementType.SELL, 11)))
                .isInstanceOf(InsufficientStockException.class);

        assertThat(level.getQuantity()).isEqualTo(10);
        verify(stockMovements, never()).save(any());
    }

    @Test
    void invalidQuantityIsRejectedBeforeTouchingTheDatabase() {
        assertThatThrownBy(() -> service.record(PRODUCT_ID, request(StockMovementType.RECEIVE, 0)))
                .isInstanceOf(InvalidRequestException.class);

        verifyNoInteractions(stockLevels, stockMovements);
    }

    @Test
    void unknownProductIsNotFound() {
        when(stockLevels.findForUpdate(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.record(7L, request(StockMovementType.RECEIVE, 1)))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Product 7 not found");
    }

    @Test
    void inactiveProductsOnlyAcceptAdjustments() {
        cumin.update(cumin.getSku(), cumin.getName(), null, cumin.getUnit(), cumin.getPackSize(), false, NOW);

        assertThatThrownBy(() -> service.record(PRODUCT_ID, request(StockMovementType.RECEIVE, 1)))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo("PRODUCT_INACTIVE");
        assertThatThrownBy(() -> service.record(PRODUCT_ID, request(StockMovementType.SELL, 1)))
                .isInstanceOf(BusinessRuleException.class);

        StockMovement writeOff = service.record(PRODUCT_ID, request(StockMovementType.ADJUST, -10));
        assertThat(writeOff.getBalanceAfter()).isZero();
    }

    private static StockMovementRequest request(StockMovementType type, long quantity) {
        return new StockMovementRequest(type, quantity, "REF-1", null);
    }
}
