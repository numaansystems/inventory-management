package com.numaansystems.inventory.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.numaansystems.inventory.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class BumiCatalogueSeederTest extends IntegrationTest {

    @Autowired
    ProductService service;

    @Autowired
    ProductRepository products;

    @Test
    void seedsCatalogueOnceWithCategoriesAndEmptyStock() throws Exception {
        BumiCatalogueSeeder seeder = new BumiCatalogueSeeder(service, products);

        int firstRun = seeder.seed();
        int secondRun = seeder.seed();

        assertThat(firstRun).isEqualTo(12);
        assertThat(secondRun).isZero();
        assertThat(products.count()).isEqualTo(12);

        mvc.perform(get("/api/products").param("category", "WHOLE_SPICES"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(6));

        mvc.perform(get("/api/stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(12))
                .andExpect(jsonPath("$.content[0].quantity").value(0));
    }

    @Test
    void keepsExistingProductsUntouched() throws Exception {
        postJson("/api/products", """
                {"sku": "BUMI-TURMERIC-100G", "name": "Our own turmeric", "unit": "GRAM", "packSize": 250}
                """).andExpect(status().isCreated());

        int created = new BumiCatalogueSeeder(service, products).seed();

        assertThat(created).isEqualTo(11);
        mvc.perform(get("/api/products").param("size", "100"))
                .andExpect(jsonPath("$.content[?(@.sku == 'BUMI-TURMERIC-100G')].name").value("Our own turmeric"));
    }
}
