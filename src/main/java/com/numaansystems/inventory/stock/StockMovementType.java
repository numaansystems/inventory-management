package com.numaansystems.inventory.stock;

import com.numaansystems.inventory.common.InvalidRequestException;

/** The kinds of change that can be made to a stock level. Quantities are always counted in packs. */
public enum StockMovementType {

    /** Goods received from a supplier. Quantity must be positive and is added to stock. */
    RECEIVE,

    /** Goods sold to a customer. Quantity must be positive and is removed from stock. */
    SELL,

    /** Manual correction (stock count, damage, spoilage). Quantity is a signed, non-zero delta. */
    ADJUST;

    /** Converts the quantity supplied by the caller into the signed change applied to the stock level. */
    public long toSignedChange(long quantity) {
        return switch (this) {
            case RECEIVE, SELL -> {
                if (quantity <= 0) {
                    throw new InvalidRequestException("quantity", "must be greater than 0 for " + this);
                }
                yield this == RECEIVE ? quantity : -quantity;
            }
            case ADJUST -> {
                if (quantity == 0) {
                    throw new InvalidRequestException("quantity", "must not be 0 for " + this);
                }
                yield quantity;
            }
        };
    }
}
