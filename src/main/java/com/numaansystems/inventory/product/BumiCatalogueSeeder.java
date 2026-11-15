package com.numaansystems.inventory.product;

import java.math.BigDecimal;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Seeds a starter catalogue when the app runs with the {@code local} profile, so there is something to
 * look at on first start. Product names come from the Bumi Spices storefront; SKUs and pack sizes are
 * placeholders to be replaced with the real ones. Products whose SKU already exists are left untouched,
 * so the seeder is safe to run on every start.
 */
@Component
@Profile("local")
class BumiCatalogueSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BumiCatalogueSeeder.class);

    private static final BigDecimal PACK_SIZE_GRAMS = new BigDecimal("100");

    private record Entry(String sku, String name, ProductCategory category) {}

    private static final List<Entry> CATALOGUE = List.of(
            new Entry("BUMI-RED-CHILI-POWDER-100G", "Red Chili Powder", ProductCategory.SPICES_SEASONINGS),
            new Entry("BUMI-TURMERIC-100G", "Turmeric", ProductCategory.SPICES_SEASONINGS),
            new Entry("BUMI-KASURI-METHI-100G", "Kasuri Methi", ProductCategory.HERBS_AROMATICS),
            new Entry("BUMI-BAY-LEAVES-100G", "Cyclone Patta (Bay Leaves)", ProductCategory.HERBS_AROMATICS),
            new Entry("BUMI-SHAHI-JEERA-100G", "Shahi Jeera (Caraway Seeds)", ProductCategory.WHOLE_SPICES),
            new Entry("BUMI-PATHAR-PHOOL-100G", "Pathar Phool", ProductCategory.WHOLE_SPICES),
            new Entry("BUMI-CLOVES-100G", "Cloves", ProductCategory.WHOLE_SPICES),
            new Entry("BUMI-CINNAMON-ROLL-100G", "Cinnamon Roll", ProductCategory.WHOLE_SPICES),
            new Entry("BUMI-RED-CHILI-WHOLE-100G", "Red Chili", ProductCategory.WHOLE_SPICES),
            new Entry("BUMI-ELAICHI-100G", "Elaichi (Cardamom)", ProductCategory.WHOLE_SPICES),
            new Entry("BUMI-GARAM-MASALA-100G", "Garam Masala", ProductCategory.BLENDED_SPICES),
            new Entry("BUMI-RED-CHILI-MASALA-100G", "Red Chili Masala Powder", ProductCategory.BLENDED_SPICES));

    private final ProductService service;
    private final ProductRepository products;

    BumiCatalogueSeeder(ProductService service, ProductRepository products) {
        this.service = service;
        this.products = products;
    }

    @Override
    public void run(ApplicationArguments args) {
        int created = seed();
        if (created > 0) {
            log.info("Seeded {} starter catalogue products", created);
        }
    }

    /** Creates the catalogue products that do not exist yet and returns how many were created. */
    int seed() {
        int created = 0;
        for (Entry entry : CATALOGUE) {
            if (!products.existsBySku(entry.sku())) {
                service.create(new ProductRequest(
                        entry.sku(), entry.name(), null, entry.category(), Unit.GRAM, PACK_SIZE_GRAMS, true));
                created++;
            }
        }
        return created;
    }
}
