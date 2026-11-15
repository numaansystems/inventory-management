package com.numaansystems.inventory.product;

/** Catalogue category of a product, mirroring the sections of the Bumi Spices storefront. */
public enum ProductCategory {
    SPICES_SEASONINGS("Spices & Seasonings"),
    HERBS_AROMATICS("Herbs & Aromatics"),
    WHOLE_SPICES("Whole Spices"),
    SPECIALTY_SPICE_MIXES("Specialty Spice Mixes"),
    BLENDED_SPICES("Blended Spices"),
    FLOURS_GRAINS("Flours & Grains"),
    LENTILS_PULSES("Lentils & Pulses"),
    RICE("Basmati Rice");

    private final String label;

    ProductCategory(String label) {
        this.label = label;
    }

    /** Human-readable name shown in the catalogue. */
    public String label() {
        return label;
    }
}
