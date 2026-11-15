package com.numaansystems.inventory.product;

/** A selectable product category: {@code code} is the value used in requests, {@code label} is for display. */
public record ProductCategoryResponse(ProductCategory code, String label) {

    static ProductCategoryResponse from(ProductCategory category) {
        return new ProductCategoryResponse(category, category.label());
    }
}
