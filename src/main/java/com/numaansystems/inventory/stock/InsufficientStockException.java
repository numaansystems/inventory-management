package com.numaansystems.inventory.stock;

import com.numaansystems.inventory.common.BusinessRuleException;

public class InsufficientStockException extends BusinessRuleException {

    public InsufficientStockException(String sku, long available, long requested) {
        super("INSUFFICIENT_STOCK",
                "Insufficient stock for " + sku + ": " + available + " available, " + requested + " requested");
    }
}
