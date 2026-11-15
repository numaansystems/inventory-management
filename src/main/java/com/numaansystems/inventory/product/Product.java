package com.numaansystems.inventory.product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;

/** A sellable spice product, stocked and sold in packs of {@link #getPackSize()} {@link #getUnit()}. */
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String sku;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Unit unit;

    @Column(name = "pack_size", nullable = false, precision = 12, scale = 3)
    private BigDecimal packSize;

    @Enumerated(EnumType.STRING)
    @Column(length = 40)
    private ProductCategory category;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected Product() {}

    /** Creates an uncategorised product. */
    public Product(
            String sku, String name, String description, Unit unit, BigDecimal packSize, boolean active, Instant now) {
        this(sku, name, description, null, unit, packSize, active, now);
    }

    public Product(
            String sku, String name, String description, ProductCategory category, Unit unit, BigDecimal packSize,
            boolean active, Instant now) {
        this.sku = sku;
        this.name = name;
        this.description = description;
        this.category = category;
        this.unit = unit;
        this.packSize = packSize;
        this.active = active;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** Replaces the product's details, keeping its current category. */
    public void update(
            String sku, String name, String description, Unit unit, BigDecimal packSize, boolean active, Instant now) {
        update(sku, name, description, this.category, unit, packSize, active, now);
    }

    public void update(
            String sku, String name, String description, ProductCategory category, Unit unit, BigDecimal packSize,
            boolean active, Instant now) {
        this.sku = sku;
        this.name = name;
        this.description = description;
        this.category = category;
        this.unit = unit;
        this.packSize = packSize;
        this.active = active;
        this.updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    /** May be {@code null} for products that have not been categorised. */
    public ProductCategory getCategory() {
        return category;
    }

    public Unit getUnit() {
        return unit;
    }

    public BigDecimal getPackSize() {
        return packSize;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public long getVersion() {
        return version;
    }
}
