package com.numaansystems.inventory.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Base class for full-stack tests: real Spring context, Flyway-migrated H2 database (PostgreSQL mode)
 * and MockMvc. Tables are emptied before each test; tests are deliberately not transactional so that
 * locking and commit behaviour are exercised as in production.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class IntegrationTest {

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected JdbcTemplate jdbc;

    @BeforeEach
    void cleanDatabase() {
        jdbc.update("delete from stock_movements");
        jdbc.update("delete from stock_levels");
        jdbc.update("delete from products");
    }

    protected ResultActions postJson(String uri, String json) throws Exception {
        return mvc.perform(post(uri).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    protected long createProduct(String sku) throws Exception {
        String body = postJson("/api/products", """
                {"sku": "%s", "name": "Product %s", "unit": "GRAM", "packSize": 100}
                """.formatted(sku, sku))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    protected ResultActions recordMovement(long productId, String type, long quantity) throws Exception {
        return postJson("/api/products/" + productId + "/stock/movements", """
                {"type": "%s", "quantity": %d}
                """.formatted(type, quantity));
    }
}
