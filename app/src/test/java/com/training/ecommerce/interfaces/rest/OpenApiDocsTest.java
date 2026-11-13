package com.training.ecommerce.interfaces.rest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Khởi động cả context ở profile local (in-memory, không DB) và kiểm tra tài liệu OpenAPI + luồng đặt đơn. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class OpenApiDocsTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void apiDocsListProductAndOrderPaths() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Java Training Ecommerce API"))
                .andExpect(jsonPath("$.paths['/api/products']").exists())
                .andExpect(jsonPath("$.paths['/api/products/{id}']").exists())
                .andExpect(jsonPath("$.paths['/api/orders']").exists())
                .andExpect(jsonPath("$.paths['/api/orders/{id}']").exists());
    }

    @Test
    void schemasExposeBeanValidationConstraints() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.schemas.PlaceOrderRequest.properties.discountRate.minimum").value(0))
                .andExpect(jsonPath("$.components.schemas.PlaceOrderRequest.properties.discountRate.maximum").value(1))
                .andExpect(jsonPath("$.components.schemas.PlaceOrderRequest.properties.taxRate.minimum").value(0))
                .andExpect(jsonPath("$.components.schemas.PlaceOrderRequest.required").value(hasItem("lines")))
                .andExpect(jsonPath("$.components.schemas.ProductRequest.required").value(hasItems("name", "type")))
                .andExpect(jsonPath("$.components.schemas.ProductRequest.properties.name.maxLength").value(255));
    }

    @Test
    void localProfileServesSeedData() throws Exception {
        mvc.perform(get("/api/products/P-COMBO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("BUNDLE"))
                .andExpect(jsonPath("$.itemIds.length()").value(2));
    }
}
