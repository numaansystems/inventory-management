package com.numaansystems.inventory.stock;

import com.numaansystems.inventory.common.BusinessRuleException;
import com.numaansystems.inventory.common.NotFoundException;
import com.numaansystems.inventory.product.Product;
import java.time.Clock;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The only component allowed to change stock levels. Every change is written to the movement ledger in
 * the same transaction, so the ledger always reconciles with the current level.
 */
@Service
@Transactional
public class StockService {

    private final StockLevelRepository stockLevels;
    private final StockMovementRepository stockMovements;
    private final Clock clock;

    StockService(StockLevelRepository stockLevels, StockMovementRepository stockMovements, Clock clock) {
        this.stockLevels = stockLevels;
        this.stockMovements = stockMovements;
        this.clock = clock;
    }

    public StockMovement record(long productId, StockMovementRequest request) {
        long change = request.type().toSignedChange(request.quantity());
        StockLevel level = stockLevels.findForUpdate(productId).orElseThrow(() -> productNotFound(productId));
        Product product = level.getProduct();
        if (!product.isActive() && request.type() != StockMovementType.ADJUST) {
            throw new BusinessRuleException("PRODUCT_INACTIVE",
                    "Product " + product.getSku() + " is inactive; only ADJUST movements are allowed");
        }
        Instant now = clock.instant();
        long balance = level.apply(change, now);
        return stockMovements.save(new StockMovement(
                product, request.type(), change, balance, request.reference(), request.note(), now));
    }

    @Transactional(readOnly = true)
    public StockLevel getLevel(long productId) {
        StockLevel level = stockLevels.findById(productId).orElseThrow(() -> productNotFound(productId));
        level.getProduct().getSku(); // initialise while the session is open
        return level;
    }

    @Transactional(readOnly = true)
    public Page<StockLevel> listLevels(Pageable pageable) {
        return stockLevels.findAllWithProduct(pageable);
    }

    @Transactional(readOnly = true)
    public Page<StockMovement> listMovements(long productId, Pageable pageable) {
        if (!stockLevels.existsById(productId)) {
            throw productNotFound(productId);
        }
        return stockMovements.findByProductIdOrderByOccurredAtDescIdDesc(productId, pageable);
    }

    private static NotFoundException productNotFound(long productId) {
        return new NotFoundException("Product " + productId + " not found");
    }
}
