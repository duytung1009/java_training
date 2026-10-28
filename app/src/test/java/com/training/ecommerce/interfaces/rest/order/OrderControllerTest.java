package com.training.ecommerce.interfaces.rest.order;

import com.training.ecommerce.application.order.OrderNotFoundException;
import com.training.ecommerce.application.order.OrderService;
import com.training.ecommerce.application.order.PlaceOrderCommand;
import com.training.ecommerce.domain.catalog.OutOfStockException;
import com.training.ecommerce.domain.catalog.PhysicalProduct;
import com.training.ecommerce.domain.catalog.ProductNotFoundException;
import com.training.ecommerce.domain.order.Order;
import com.training.ecommerce.domain.order.OrderLine;
import com.training.ecommerce.interfaces.rest.RestExceptionHandler;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@Import(RestExceptionHandler.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private OrderService service;

    private static Order order() {
        Order order = new Order("O1", new BigDecimal("0.1"), new BigDecimal("0.08"));
        order.addLine(new OrderLine(new PhysicalProduct("P1", "Áo", new BigDecimal("100000"), 10, BigDecimal.ONE), 2));
        return order;
    }

    private static final String BODY = """
            {"discountRate":0.1,"taxRate":0.08,"lines":[{"productId":"P1","quantity":2}]}""";

    @Test
    void postCreates201WithLocationAndTotals() throws Exception {
        when(service.placeOrder(any())).thenReturn(order());

        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/orders/O1"))
                .andExpect(jsonPath("$.id").value("O1"))
                .andExpect(jsonPath("$.lines[0].productName").value("Áo"))
                .andExpect(jsonPath("$.lines[0].unitPrice").value(100000.0))
                .andExpect(jsonPath("$.lines[0].lineTotal").value(200000.0))
                .andExpect(jsonPath("$.subtotal").value(200000.0))
                .andExpect(jsonPath("$.discountAmount").value(20000.0))
                .andExpect(jsonPath("$.taxAmount").value(14400.0))
                .andExpect(jsonPath("$.finalAmount").value(194400.0));
        ArgumentCaptor<PlaceOrderCommand> command = ArgumentCaptor.forClass(PlaceOrderCommand.class);
        verify(service).placeOrder(command.capture());
        assertThat(command.getValue().lines()).containsExactly(new PlaceOrderCommand.Line("P1", 2));
    }

    @Test
    void missingRatesDefaultToZero() throws Exception {
        when(service.placeOrder(any())).thenReturn(order());

        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lines\":[{\"productId\":\"P1\",\"quantity\":1}]}"))
                .andExpect(status().isCreated());
        ArgumentCaptor<PlaceOrderCommand> command = ArgumentCaptor.forClass(PlaceOrderCommand.class);
        verify(service).placeOrder(command.capture());
        assertThat(command.getValue().discountRate()).isEqualByComparingTo("0");
        assertThat(command.getValue().taxRate()).isEqualByComparingTo("0");
    }

    @Test
    void invalidBodyReturns400() throws Exception {
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content("{\"lines\":[]}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lines\":[{\"productId\":\"P1\",\"quantity\":0}]}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content("not json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownProductReturns404() throws Exception {
        when(service.placeOrder(any())).thenThrow(new ProductNotFoundException("P1"));

        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isNotFound());
    }

    @Test
    void outOfStockReturns409() throws Exception {
        when(service.placeOrder(any())).thenThrow(new OutOfStockException("P1", 2, 1));

        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void getReturns200() throws Exception {
        when(service.findById("O1")).thenReturn(order());

        mvc.perform(get("/api/orders/O1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("O1"));
    }

    @Test
    void getMissingReturns404() throws Exception {
        when(service.findById("nope")).thenThrow(new OrderNotFoundException("nope"));

        mvc.perform(get("/api/orders/nope")).andExpect(status().isNotFound());
    }

    @Test
    void listReturns200() throws Exception {
        when(service.findAll()).thenReturn(List.of(order()));

        mvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("O1"));
    }
}
