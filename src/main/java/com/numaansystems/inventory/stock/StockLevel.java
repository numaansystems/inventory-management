package com.numaansystems.inventory.stock;

import com.numaansystems.inventory.product.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

/** Current on-hand quantity of a product, in packs. Never negative. */
@Entity
@Table(name = "stock_levels")
public class StockLevel {

    @Id
    @Column(name = "product_id")
    private Long productId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(nullable = false)
    private long quantity;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected StockLevel() {}

    public StockLevel(Product product, Instant now) {
        this.product = product;
        this.quantity = 0;
        this.updatedAt = now;
    }

    /**
     * Applies a signed change and returns the new balance.
     *
     * @throws InsufficientStockException if the change would take the balance below zero
     */
    public long apply(long signedChange, Instant now) {
        long newQuantity = Math.addExact(quantity, signedChange);
        if (newQuantity < 0) {
            throw new InsufficientStockException(product.getSku(), quantity, -signedChange);
        }
        quantity = newQuantity;
        updatedAt = now;
        return quantity;
    }

    public Long getProductId() {
        return productId;
    }

    public Product getProduct() {
        return product;
    }

    public long getQuantity() {
        return quantity;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
