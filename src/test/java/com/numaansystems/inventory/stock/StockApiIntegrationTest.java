package com.numaansystems.inventory.stock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.numaansystems.inventory.support.IntegrationTest;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class StockApiIntegrationTest extends IntegrationTest {

    @Test
    void receiveSellAndAdjustUpdateStockAndLedger() throws Exception {
        long id = createProduct("TUR-100G");

        postJson("/api/products/" + id + "/stock/movements", """
                {"type": "RECEIVE", "quantity": 20, "reference": "PO-1001", "note": "Supplier delivery"}
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productId").value(id))
                .andExpect(jsonPath("$.type").value("RECEIVE"))
                .andExpect(jsonPath("$.quantityChange").value(20))
                .andExpect(jsonPath("$.balanceAfter").value(20))
                .andExpect(jsonPath("$.reference").value("PO-1001"));
        recordMovement(id, "SELL", 7)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quantityChange").value(-7))
                .andExpect(jsonPath("$.balanceAfter").value(13));
        recordMovement(id, "ADJUST", -1)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.balanceAfter").value(12));

        mvc.perform(get("/api/products/" + id + "/stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("TUR-100G"))
                .andExpect(jsonPath("$.quantity").value(12))
                .andExpect(jsonPath("$.unit").value("GRAM"))
                .andExpect(jsonPath("$.totalAmount").value(1200));

        mvc.perform(get("/api/products/" + id + "/stock/movements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[*].type").value(contains("ADJUST", "SELL", "RECEIVE")))
                .andExpect(jsonPath("$.content[*].balanceAfter").value(contains(12, 13, 20)));

        assertLedgerReconciles(id);
    }

    @Test
    void listsStockForAllProducts() throws Exception {
        long pepper = createProduct("PEP-100G");
        createProduct("ANI-100G");
        recordMovement(pepper, "RECEIVE", 4).andExpect(status().isCreated());

        mvc.perform(get("/api/stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].sku").value(contains("ANI-100G", "PEP-100G")))
                .andExpect(jsonPath("$.content[*].quantity").value(contains(0, 4)));
    }

    @Test
    void rejectsSellingMoreThanOnHand() throws Exception {
        long id = createProduct("CHI-100G");
        recordMovement(id, "RECEIVE", 5).andExpect(status().isCreated());

        recordMovement(id, "SELL", 6)
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"))
                .andExpect(jsonPath("$.detail").value("Insufficient stock for CHI-100G: 5 available, 6 requested"));

        mvc.perform(get("/api/products/" + id + "/stock")).andExpect(jsonPath("$.quantity").value(5));
        mvc.perform(get("/api/products/" + id + "/stock/movements")).andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void rejectsNegativeAdjustmentBelowZero() throws Exception {
        long id = createProduct("MUS-100G");

        recordMovement(id, "ADJUST", -1)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"));
    }

    @Test
    void validatesMovementRequests() throws Exception {
        long id = createProduct("COR-100G");

        postJson("/api/products/" + id + "/stock/movements", "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[*].field").value(contains("quantity", "type")));

        recordMovement(id, "RECEIVE", 0)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("quantity"))
                .andExpect(jsonPath("$.errors[0].message").value("must be greater than 0 for RECEIVE"));

        recordMovement(id, "SELL", -2).andExpect(status().isBadRequest());
        recordMovement(id, "ADJUST", 0).andExpect(status().isBadRequest());
    }

    @Test
    void returnsNotFoundForUnknownProduct() throws Exception {
        recordMovement(999_999, "RECEIVE", 1)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        mvc.perform(get("/api/products/999999/stock")).andExpect(status().isNotFound());
        mvc.perform(get("/api/products/999999/stock/movements")).andExpect(status().isNotFound());
    }

    @Test
    void inactiveProductsAcceptOnlyAdjustments() throws Exception {
        long id = createProduct("SUM-100G");
        recordMovement(id, "RECEIVE", 3).andExpect(status().isCreated());
        mvc.perform(put("/api/products/" + id).contentType(MediaType.APPLICATION_JSON).content("""
                {"sku": "SUM-100G", "name": "Sumac", "unit": "GRAM", "packSize": 100, "active": false}
                """)).andExpect(status().isOk());

        recordMovement(id, "SELL", 1)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PRODUCT_INACTIVE"));
        recordMovement(id, "RECEIVE", 1).andExpect(status().isConflict());
        recordMovement(id, "ADJUST", -3)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.balanceAfter").value(0));
    }

    @Test
    void concurrentSalesNeverOversell() throws Exception {
        long id = createProduct("PAP-100G");
        recordMovement(id, "RECEIVE", 10).andExpect(status().isCreated());

        int attempts = 25;
        CountDownLatch start = new CountDownLatch(1);
        List<Callable<Integer>> sales = new ArrayList<>();
        for (int i = 0; i < attempts; i++) {
            sales.add(() -> {
                start.await();
                return recordMovement(id, "SELL", 1).andReturn().getResponse().getStatus();
            });
        }

        List<Integer> statuses = new ArrayList<>();
        try (ExecutorService pool = Executors.newFixedThreadPool(8)) {
            List<Future<Integer>> futures = sales.stream().map(pool::submit).toList();
            start.countDown();
            for (Future<Integer> future : futures) {
                statuses.add(future.get());
            }
        }

        assertThat(statuses).filteredOn(s -> s == 201).hasSize(10);
        assertThat(statuses).filteredOn(s -> s == 409).hasSize(attempts - 10);
        mvc.perform(get("/api/products/" + id + "/stock")).andExpect(jsonPath("$.quantity").value(0));
        assertLedgerReconciles(id);
    }

    /** The sum of all movements must always equal the stored level. */
    private void assertLedgerReconciles(long productId) {
        Long ledgerTotal = jdbc.queryForObject(
                "select coalesce(sum(quantity_change), 0) from stock_movements where product_id = ?",
                Long.class, productId);
        Long level = jdbc.queryForObject(
                "select quantity from stock_levels where product_id = ?", Long.class, productId);
        assertThat(ledgerTotal).isEqualTo(level);
    }
}
