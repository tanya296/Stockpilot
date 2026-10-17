package com.stockpilot.api;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Functional tests: the whole app starts (controllers, security, services, JPA) against an
 * in-memory H2 database, and MockMvc sends real HTTP requests without opening a port.
 *
 * @Transactional: each test runs in a transaction that is rolled back afterwards, so tests
 * can't affect each other. The demo seeder runs once at startup to give us known data:
 * product 1 = brake pads, warehouse 1 = Chennai with 6 units.
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = "app.seed.enabled=true")
class ApiIntegrationTest {

    @Autowired
    WebApplicationContext context;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private String login(String email) throws Exception {
        String body = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"Password@123\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(body, "$.token");
    }

    // ---------- authentication & authorization ----------

    @Test
    void noTokenGives401() throws Exception {
        mvc.perform(get("/api/products"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void wrongPasswordGives401() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"staff@stockpilot.dev\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password."));
    }

    @Test
    void staffCannotDeleteProducts() throws Exception {
        mvc.perform(delete("/api/products/1").header("Authorization", login("staff@stockpilot.dev")))
                .andExpect(status().isForbidden());
    }

    @Test
    void registeringAlwaysCreatesStaff() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"new@test.dev","password":"Password@123","fullName":"New Person"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("STAFF"));
    }

    // ---------- products ----------

    @Test
    void productsArePaginated() throws Exception {
        mvc.perform(get("/api/products?size=2&page=0").header("Authorization", login("staff@stockpilot.dev")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(6))
                .andExpect(jsonPath("$.totalPages").value(3));
    }

    @Test
    void unknownSortFieldGives400() throws Exception {
        mvc.perform(get("/api/products?sort=passwordHash,asc").header("Authorization", login("staff@stockpilot.dev")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminCanCreateProductButNotADuplicateSku() throws Exception {
        String admin = login("admin@stockpilot.dev");
        String json = """
                {"sku":"TST-001","name":"Test Part","category":"Test","unitPrice":99.50,"reorderLevel":5}""";

        mvc.perform(post("/api/products").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sku").value("TST-001"));

        mvc.perform(post("/api/products").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isConflict());
    }

    @Test
    void unknownProductGives404() throws Exception {
        mvc.perform(get("/api/products/9999").header("Authorization", login("staff@stockpilot.dev")))
                .andExpect(status().isNotFound());
    }

    // ---------- stock ----------

    @Test
    void stockOutMoreThanAvailableGives422() throws Exception {
        mvc.perform(post("/api/stock/adjust").header("Authorization", login("staff@stockpilot.dev"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId":1,"warehouseId":1,"type":"OUT","quantity":100}"""))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.message", containsString("Not enough stock")));
    }

    @Test
    void invalidAdjustmentGives400WithFieldErrors() throws Exception {
        mvc.perform(post("/api/stock/adjust").header("Authorization", login("staff@stockpilot.dev"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId":1,"warehouseId":1,"type":"OUT","quantity":-5}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.quantity").exists());
    }

    @Test
    void stockInUpdatesQuantityAndHistory() throws Exception {
        String staff = login("staff@stockpilot.dev");

        mvc.perform(post("/api/stock/adjust").header("Authorization", staff)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId":1,"warehouseId":1,"type":"IN","quantity":10,"reason":"delivery"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(16)); // 6 seeded + 10

        mvc.perform(get("/api/products/1/movements").header("Authorization", staff))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].type").value("IN"))
                .andExpect(jsonPath("$.content[0].quantity").value(10))
                .andExpect(jsonPath("$.content[0].createdBy").value("staff@stockpilot.dev"));
    }
}
