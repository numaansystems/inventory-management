package com.numaansystems.inventory.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** Payload for creating or replacing a product. {@code active} defaults to {@code true} when omitted. */
public record ProductRequest(
        @NotBlank
        @Pattern(regexp = "^[A-Z0-9][A-Z0-9-]{1,39}$",
                message = "must be 2-40 characters of upper-case letters, digits and hyphens")
        String sku,
        @NotBlank @Size(max = 200) String name,
        @Size(max = 2000) String description,
        @NotNull Unit unit,
        @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 9, fraction = 3) BigDecimal packSize,
        Boolean active) {

    boolean activeOrDefault() {
        return active == null || active;
    }
}
