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
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tier 2 flows: transfers, purchase orders, dashboard.
 * Seed data: product 1 = brake pads (Chennai 6, Pune 70, Delhi 12);
 * warehouses 1 Chennai, 2 Pune, 3 Delhi; supplier 1 = Bosch India.
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = "app.seed.enabled=true")
class WorkflowIntegrationTest {

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
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(body, "$.token");
    }

    private int stockIn(String auth, int productId, int warehouseId) throws Exception {
        String body = mvc.perform(get("/api/products/" + productId).header("Authorization", auth))
                .andReturn().getResponse().getContentAsString();
        java.util.List<Integer> q = JsonPath.read(body, "$.stock[?(@.warehouseId == " + warehouseId + ")].quantity");
        return q.isEmpty() ? 0 : q.get(0);
    }

    // ---------- transfers ----------

    @Test
    void managerTransfersStockBetweenWarehouses() throws Exception {
        String manager = login("manager@stockpilot.dev");

        mvc.perform(post("/api/stock/transfer").header("Authorization", manager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId":1,"fromWarehouseId":2,"toWarehouseId":1,"quantity":20,"reason":"Chennai running low"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from.quantity").value(50))  // Pune 70 - 20
                .andExpect(jsonPath("$.to.quantity").value(26));   // Chennai 6 + 20

        mvc.perform(get("/api/products/1/movements").header("Authorization", manager))
                .andExpect(jsonPath("$.content[*].type", hasItem("TRANSFER_OUT")))
                .andExpect(jsonPath("$.content[*].type", hasItem("TRANSFER_IN")));
    }

    @Test
    void failedTransferChangesNothing() throws Exception {
        String manager = login("manager@stockpilot.dev");

        mvc.perform(post("/api/stock/transfer").header("Authorization", manager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId":1,"fromWarehouseId":1,"toWarehouseId":2,"quantity":500}"""))
                .andExpect(status().is(422));

        org.assertj.core.api.Assertions.assertThat(stockIn(manager, 1, 1)).isEqualTo(6);
        org.assertj.core.api.Assertions.assertThat(stockIn(manager, 1, 2)).isEqualTo(70);
    }

    @Test
    void staffCannotTransfer() throws Exception {
        mvc.perform(post("/api/stock/transfer").header("Authorization", login("staff@stockpilot.dev"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId":1,"fromWarehouseId":2,"toWarehouseId":1,"quantity":1}"""))
                .andExpect(status().isForbidden());
    }

    @Test
    void transferToSameWarehouseIsRejected() throws Exception {
        mvc.perform(post("/api/stock/transfer").header("Authorization", login("manager@stockpilot.dev"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId":1,"fromWarehouseId":1,"toWarehouseId":1,"quantity":1}"""))
                .andExpect(status().isBadRequest());
    }

    // ---------- purchase orders ----------

    @Test
    void purchaseOrderLifecycleAddsStockOnReceipt() throws Exception {
        String manager = login("manager@stockpilot.dev");

        String created = mvc.perform(post("/api/purchase-orders").header("Authorization", manager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"supplierId":1,"warehouseId":1,
                                 "items":[{"productId":1,"quantity":50,"unitCost":1500.00}]}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.totalCost").value(75000.0))
                .andReturn().getResponse().getContentAsString();
        Integer id = JsonPath.read(created, "$.id");

        // Can't receive a DRAFT: it must be ORDERED first.
        mvc.perform(patch("/api/purchase-orders/" + id + "/status").header("Authorization", manager)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"RECEIVED\"}"))
                .andExpect(status().is(422));

        mvc.perform(patch("/api/purchase-orders/" + id + "/status").header("Authorization", manager)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ORDERED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ORDERED"));

        mvc.perform(patch("/api/purchase-orders/" + id + "/status").header("Authorization", manager)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"RECEIVED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECEIVED"));

        org.assertj.core.api.Assertions.assertThat(stockIn(manager, 1, 1)).isEqualTo(56); // 6 + 50

        // Final state: can't cancel a received order.
        mvc.perform(patch("/api/purchase-orders/" + id + "/status").header("Authorization", manager)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.message", containsString("RECEIVED")));
    }

    @Test
    void purchaseOrderNeedsAtLeastOneItem() throws Exception {
        mvc.perform(post("/api/purchase-orders").header("Authorization", login("manager@stockpilot.dev"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"supplierId":1,"warehouseId":1,"items":[]}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.items").exists());
    }

    @Test
    void staffCanViewButNotCreatePurchaseOrders() throws Exception {
        String staff = login("staff@stockpilot.dev");
        mvc.perform(get("/api/purchase-orders").header("Authorization", staff))
                .andExpect(status().isOk());
        mvc.perform(post("/api/purchase-orders").header("Authorization", staff)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"supplierId":1,"warehouseId":1,"items":[{"productId":1,"quantity":1,"unitCost":1}]}"""))
                .andExpect(status().isForbidden());
    }

    // ---------- dashboard ----------

    @Test
    void dashboardShowsLowStockAndTotals() throws Exception {
        String staff = login("staff@stockpilot.dev");

        // Brake disc: 10 + 3 = 13 units, reorder level 15 -> low, short by 2.
        mvc.perform(get("/api/dashboard/low-stock").header("Authorization", staff))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.sku == 'BRK-DSC-02')].shortfall").value(hasItem(2)));

        mvc.perform(get("/api/dashboard/summary").header("Authorization", staff))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProducts").value(6))
                .andExpect(jsonPath("$.stockByWarehouse.length()").value(3));
    }
}
