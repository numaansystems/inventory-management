package com.numaansystems.inventory.product;

import com.numaansystems.inventory.common.BusinessRuleException;
import com.numaansystems.inventory.common.NotFoundException;
import com.numaansystems.inventory.stock.StockLevel;
import com.numaansystems.inventory.stock.StockLevelRepository;
import com.numaansystems.inventory.stock.StockMovementRepository;
import java.time.Clock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProductService {

    private final ProductRepository products;
    private final StockLevelRepository stockLevels;
    private final StockMovementRepository stockMovements;
    private final Clock clock;

    ProductService(
            ProductRepository products,
            StockLevelRepository stockLevels,
            StockMovementRepository stockMovements,
            Clock clock) {
        this.products = products;
        this.stockLevels = stockLevels;
        this.stockMovements = stockMovements;
        this.clock = clock;
    }

    /** Creates the product together with an empty stock level. */
    public Product create(ProductRequest request) {
        requireUniqueSku(request.sku());
        Product product = new Product(request.sku(), request.name(), request.description(), request.unit(),
                request.packSize(), request.activeOrDefault(), clock.instant());
        products.save(product);
        stockLevels.save(new StockLevel(product, clock.instant()));
        return product;
    }

    @Transactional(readOnly = true)
    public Product get(long id) {
        return products.findById(id).orElseThrow(() -> new NotFoundException("Product " + id + " not found"));
    }

    @Transactional(readOnly = true)
    public Page<Product> list(Boolean active, Pageable pageable) {
        return active == null ? products.findAll(pageable) : products.findByActive(active, pageable);
    }

    public Product update(long id, ProductRequest request) {
        Product product = get(id);
        if (!product.getSku().equals(request.sku())) {
            requireUniqueSku(request.sku());
        }
        product.update(request.sku(), request.name(), request.description(), request.unit(), request.packSize(),
                request.activeOrDefault(), clock.instant());
        return product;
    }

    /**
     * Deletes a product that has never had stock recorded against it. Products with history must be
     * deactivated instead so the movement ledger stays intact.
     */
    public void delete(long id) {
        Product product = get(id);
        if (stockMovements.existsByProductId(id)) {
            throw new BusinessRuleException("PRODUCT_HAS_STOCK_HISTORY",
                    "Product " + product.getSku() + " has stock movements; deactivate it instead of deleting");
        }
        stockLevels.deleteById(id);
        products.delete(product);
    }

    private void requireUniqueSku(String sku) {
        if (products.existsBySku(sku)) {
            throw new BusinessRuleException("DUPLICATE_SKU", "A product with SKU " + sku + " already exists");
        }
    }
}
