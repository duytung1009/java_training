package com.training.ecommerce.interfaces.rest.catalog;

import com.training.ecommerce.application.catalog.ProductInUseException;
import com.training.ecommerce.application.catalog.ProductService;
import com.training.ecommerce.domain.catalog.BundleProduct;
import com.training.ecommerce.domain.catalog.Category;
import com.training.ecommerce.domain.catalog.DigitalProduct;
import com.training.ecommerce.domain.catalog.PhysicalProduct;
import com.training.ecommerce.domain.catalog.Product;
import com.training.ecommerce.domain.catalog.ProductNotFoundException;
import com.training.ecommerce.interfaces.rest.RestExceptionHandler;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@Import(RestExceptionHandler.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ProductService service;

    private static Product tshirt() {
        return new PhysicalProduct("P1", "Áo", new BigDecimal("150000"), 5, new BigDecimal("0.5"), Category.FASHION);
    }

    private static final String PHYSICAL_JSON = """
            {"id":"P1","type":"PHYSICAL","name":"Áo","basePrice":150000,"stock":5,"category":"FASHION","weightKg":0.5}""";

    @Test
    void listReturns200() throws Exception {
        when(service.findAll()).thenReturn(List.of(tshirt()));

        mvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("P1"))
                .andExpect(jsonPath("$[0].type").value("PHYSICAL"))
                .andExpect(jsonPath("$[0].finalPrice").value(150000.0))
                .andExpect(jsonPath("$[0].weightKg").value(0.5));
    }

    @Test
    void getReturnsBundleWithItemIds() throws Exception {
        Product ebook = new DigitalProduct("P2", "Ebook", new BigDecimal("100"), 1);
        when(service.findById("B1")).thenReturn(new BundleProduct("B1", "Combo", 2, new BigDecimal("0.1"), tshirt(), ebook));

        mvc.perform(get("/api/products/B1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("BUNDLE"))
                .andExpect(jsonPath("$.itemIds[0]").value("P1"))
                .andExpect(jsonPath("$.itemIds[1]").value("P2"))
                .andExpect(jsonPath("$.discountRate").value(0.1));
    }

    @Test
    void getMissingReturns404ProblemDetail() throws Exception {
        when(service.findById("nope")).thenThrow(new ProductNotFoundException("nope"));

        mvc.perform(get("/api/products/nope"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    void postCreates201WithLocation() throws Exception {
        when(service.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(PHYSICAL_JSON))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/products/P1"))
                .andExpect(jsonPath("$.id").value("P1"));
    }

    @Test
    void postBundleLooksUpItems() throws Exception {
        when(service.findById("P1")).thenReturn(tshirt());
        when(service.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content("""
                        {"id":"B1","type":"BUNDLE","name":"Combo","stock":1,"discountRate":0.2,"itemIds":["P1"]}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.basePrice").value(150000.0))
                .andExpect(jsonPath("$.finalPrice").value(120000.0));
    }

    @Test
    void postBundleWithMissingItemReturns404() throws Exception {
        when(service.findById("X")).thenThrow(new ProductNotFoundException("X"));

        mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content("""
                        {"id":"B1","type":"BUNDLE","name":"Combo","stock":1,"discountRate":0.2,"itemIds":["X"]}"""))
                .andExpect(status().isNotFound());
        verify(service, never()).save(any());
    }

    @Test
    void postInvalidFieldsReturn400() throws Exception {
        mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"P1\",\"type\":\"PHYSICAL\",\"name\":\"Áo\",\"basePrice\":10,\"stock\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("weightKg"));
        mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"P1\",\"type\":\"UNKNOWN\",\"name\":\"Áo\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Body JSON không hợp lệ"));
        mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"P1\",\"type\":\"SUBSCRIPTION\",\"name\":\"Tin\",\"basePrice\":10,\"stock\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("months"));
    }

    @Test
    void reportsAllFieldErrors() throws Exception {
        mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content("""
                        {"id":"P1","type":"PHYSICAL","name":"","basePrice":10,"stock":-1}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Request không hợp lệ"))
                .andExpect(jsonPath("$.errors.length()").value(3))
                .andExpect(jsonPath("$.errors[*].field").value(containsInAnyOrder("name", "stock", "weightKg")))
                .andExpect(jsonPath("$.errors[*].message").value(everyItem(not(emptyOrNullString()))));
        verifyNoInteractions(service);
    }

    @Test
    void invalidCategoryIsReportedByField() throws Exception {
        mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content("""
                        {"id":"P1","type":"DIGITAL","name":"Ebook","basePrice":10,"stock":1,"category":"food"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].field").value("category"));
        verifyNoInteractions(service);
    }

    @Test
    void putAlsoValidatesBody() throws Exception {
        mvc.perform(put("/api/products/P1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"SUBSCRIPTION\",\"name\":\"Tin\",\"basePrice\":10,\"stock\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("months"));
        verifyNoInteractions(service);
    }

    @Test
    void unknownTypeIsUnreadableBody() throws Exception {
        mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"P1\",\"type\":\"FOOD\",\"name\":\"Áo\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Body JSON không hợp lệ"))
                .andExpect(jsonPath("$.errors").doesNotExist());
        verifyNoInteractions(service);
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void putUsesPathIdAndReturns200() throws Exception {
        when(service.update(eq("P1"), any(Product.class))).thenAnswer(inv -> inv.getArgument(1));

        mvc.perform(put("/api/products/P1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"DIGITAL\",\"name\":\"Mới\",\"basePrice\":5,\"stock\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("P1"))
                .andExpect(jsonPath("$.type").value("DIGITAL"));
        ArgumentCaptor<Product> saved = ArgumentCaptor.forClass(Product.class);
        verify(service).update(eq("P1"), saved.capture());
        assertThat(saved.getValue().getId()).isEqualTo("P1");
    }

    @Test
    void putIdMismatchReturns400() throws Exception {
        mvc.perform(put("/api/products/P9").contentType(MediaType.APPLICATION_JSON).content(PHYSICAL_JSON))
                .andExpect(status().isBadRequest());
        verify(service, never()).update(any(), any());
    }

    @Test
    void putMissingReturns404() throws Exception {
        when(service.update(eq("P1"), any(Product.class))).thenThrow(new ProductNotFoundException("P1"));

        mvc.perform(put("/api/products/P1").contentType(MediaType.APPLICATION_JSON).content(PHYSICAL_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteReturns204() throws Exception {
        mvc.perform(delete("/api/products/P1")).andExpect(status().isNoContent());
        verify(service).delete("P1");
    }

    @Test
    void deleteMissingReturns404() throws Exception {
        doThrow(new ProductNotFoundException("P1")).when(service).delete(anyString());

        mvc.perform(delete("/api/products/P1")).andExpect(status().isNotFound());
    }

    @Test
    void deleteReferencedProductReturns409() throws Exception {
        doThrow(new ProductInUseException("P1", new DataIntegrityViolationException("fk"))).when(service).delete("P1");

        mvc.perform(delete("/api/products/P1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("P1")));
    }

    @Test
    void genericDataIntegrityViolationReturns409WithGenericMessage() throws Exception {
        when(service.save(any(Product.class))).thenThrow(new DataIntegrityViolationException("fk"));

        mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(PHYSICAL_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Dữ liệu vi phạm ràng buộc"));
    }
}
