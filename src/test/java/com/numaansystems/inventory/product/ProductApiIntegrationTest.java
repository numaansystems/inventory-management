package com.numaansystems.inventory.product;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.numaansystems.inventory.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class ProductApiIntegrationTest extends IntegrationTest {

    @Test
    void createsAndFetchesProductWithEmptyStock() throws Exception {
        String location = postJson("/api/products", """
                {"sku": "CIN-50G", "name": "Ceylon cinnamon sticks", "description": "Grade C5",
                 "unit": "GRAM", "packSize": 50.5}
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sku").value("CIN-50G"))
                .andExpect(jsonPath("$.packSize").value(50.5))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.createdAt").isString())
                .andReturn().getResponse().getHeader("Location");

        mvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ceylon cinnamon sticks"))
                .andExpect(jsonPath("$.description").value("Grade C5"))
                .andExpect(jsonPath("$.unit").value("GRAM"));

        mvc.perform(get(location + "/stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(0));
    }

    @Test
    void listsProductsSortedBySkuAndFiltersByActive() throws Exception {
        createProduct("PEP-100G");
        long cardamom = createProduct("CAR-100G");
        mvc.perform(put("/api/products/" + cardamom).contentType(MediaType.APPLICATION_JSON).content("""
                {"sku": "CAR-100G", "name": "Green cardamom", "unit": "GRAM", "packSize": 100, "active": false}
                """)).andExpect(status().isOk());

        mvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].sku").value(contains("CAR-100G", "PEP-100G")))
                .andExpect(jsonPath("$.totalElements").value(2));

        mvc.perform(get("/api/products").param("active", "true"))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].sku").value("PEP-100G"));
    }

    @Test
    void updatesProduct() throws Exception {
        long id = createProduct("CLV-100G");

        mvc.perform(put("/api/products/" + id).contentType(MediaType.APPLICATION_JSON).content("""
                {"sku": "CLV-1KG", "name": "Whole cloves", "unit": "KILOGRAM", "packSize": 1}
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("CLV-1KG"))
                .andExpect(jsonPath("$.unit").value("KILOGRAM"));
    }

    @Test
    void rejectsDuplicateSkuOnCreateAndUpdate() throws Exception {
        createProduct("SAF-1G");
        long other = createProduct("SAF-2G");

        postJson("/api/products", """
                {"sku": "SAF-1G", "name": "Saffron", "unit": "GRAM", "packSize": 1}
                """)
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("DUPLICATE_SKU"));

        mvc.perform(put("/api/products/" + other).contentType(MediaType.APPLICATION_JSON).content("""
                {"sku": "SAF-1G", "name": "Saffron", "unit": "GRAM", "packSize": 2}
                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_SKU"));
    }

    @Test
    void rejectsInvalidProductWithFieldErrors() throws Exception {
        postJson("/api/products", """
                {"sku": "bad sku", "name": " ", "packSize": -1}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[*].field")
                        .value(containsInAnyOrder("name", "packSize", "sku", "unit")));
    }

    @Test
    void rejectsMalformedJson() throws Exception {
        postJson("/api/products", "{\"sku\": ")
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void rejectsUnknownEnumValue() throws Exception {
        postJson("/api/products", """
                {"sku": "NUT-100G", "name": "Nutmeg", "unit": "BUSHEL", "packSize": 100}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void rejectsUnknownSortProperty() throws Exception {
        mvc.perform(get("/api/products").param("sort", "nonsense"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("sort"));
    }

    @Test
    void returnsNotFoundProblemForUnknownProduct() throws Exception {
        mvc.perform(get("/api/products/999999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.detail").value("Product 999999 not found"));
    }

    @Test
    void deletesProductWithoutStockHistory() throws Exception {
        long id = createProduct("FEN-100G");

        mvc.perform(delete("/api/products/" + id)).andExpect(status().isNoContent());

        mvc.perform(get("/api/products/" + id)).andExpect(status().isNotFound());
        mvc.perform(get("/api/products/" + id + "/stock")).andExpect(status().isNotFound());
    }

    @Test
    void refusesToDeleteProductWithStockHistory() throws Exception {
        long id = createProduct("STA-50G");
        recordMovement(id, "RECEIVE", 3).andExpect(status().isCreated());

        mvc.perform(delete("/api/products/" + id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PRODUCT_HAS_STOCK_HISTORY"));

        mvc.perform(get("/api/products/" + id)).andExpect(status().isOk());
    }

    @Test
    void exposesHealthEndpoint() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("json")))
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
