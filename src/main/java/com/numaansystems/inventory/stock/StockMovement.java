package com.numaansystems.inventory.stock;

import com.numaansystems.inventory.product.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/** Immutable ledger entry recording one change to a product's stock level. */
@Entity
@Table(name = "stock_movements")
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StockMovementType type;

    /** Signed change in packs: positive adds stock, negative removes it. */
    @Column(name = "quantity_change", nullable = false)
    private long quantityChange;

    @Column(name = "balance_after", nullable = false)
    private long balanceAfter;

    @Column(length = 100)
    private String reference;

    @Column(length = 500)
    private String note;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected StockMovement() {}

    public StockMovement(
            Product product,
            StockMovementType type,
            long quantityChange,
            long balanceAfter,
            String reference,
            String note,
            Instant occurredAt) {
        this.product = product;
        this.type = type;
        this.quantityChange = quantityChange;
        this.balanceAfter = balanceAfter;
        this.reference = reference;
        this.note = note;
        this.occurredAt = occurredAt;
    }

    public Long getId() {
        return id;
    }

    public Product getProduct() {
        return product;
    }

    public StockMovementType getType() {
        return type;
    }

    public long getQuantityChange() {
        return quantityChange;
    }

    public long getBalanceAfter() {
        return balanceAfter;
    }

    public String getReference() {
        return reference;
    }

    public String getNote() {
        return note;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
