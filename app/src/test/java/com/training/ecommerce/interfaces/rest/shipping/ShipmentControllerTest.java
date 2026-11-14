package com.training.ecommerce.interfaces.rest.shipping;

import com.training.ecommerce.application.order.OrderNotFoundException;
import com.training.ecommerce.application.shipping.ActiveShipmentExistsException;
import com.training.ecommerce.application.shipping.ShipmentNotFoundException;
import com.training.ecommerce.application.shipping.ShipmentService;
import com.training.ecommerce.domain.customer.Address;
import com.training.ecommerce.domain.shipping.IllegalShipmentTransitionException;
import com.training.ecommerce.domain.shipping.Shipment;
import com.training.ecommerce.domain.shipping.ShipmentEvent;
import com.training.ecommerce.domain.shipping.ShipmentStatus;
import com.training.ecommerce.interfaces.rest.RestExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ShipmentController.class)
@Import(RestExceptionHandler.class)
class ShipmentControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ShipmentService service;

    private static Shipment shipment() {
        return new Shipment("S1", "O1", new Address("1 Phố Huế", "Hà Nội"), new BigDecimal("30000"), "SHP-ABCDEF12",
                ShipmentStatus.CREATED, 0, List.of(new ShipmentEvent(ShipmentStatus.CREATED, null)));
    }

    private static final String BODY = """
            {"orderId":"O1","street":"1 Phố Huế","city":"Hà Nội"}""";

    @Test
    void postCreates201WithLocationAndBody() throws Exception {
        when(service.create(eq("O1"), any())).thenReturn(shipment());

        mvc.perform(post("/api/shipments").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/shipments/S1"))
                .andExpect(jsonPath("$.id").value("S1"))
                .andExpect(jsonPath("$.orderId").value("O1"))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.fee").value(30000.0))
                .andExpect(jsonPath("$.city").value("Hà Nội"))
                .andExpect(jsonPath("$.history[0].status").value("CREATED"));
    }

    @Test
    void blankAddressReturns400() throws Exception {
        mvc.perform(post("/api/shipments").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":\"O1\",\"street\":\"\",\"city\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void blankCityAndStreetAreReportedByField() throws Exception {
        mvc.perform(post("/api/shipments").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":\"O1\",\"street\":\"1 Phố Huế\",\"city\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].field").value("city"));
        verifyNoInteractions(service);
    }

    @Test
    void unknownOrderReturns404() throws Exception {
        when(service.create(eq("O1"), any())).thenThrow(new OrderNotFoundException("O1"));

        mvc.perform(post("/api/shipments").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));
    }

    @Test
    void secondActiveShipmentReturns409() throws Exception {
        when(service.create(eq("O1"), any())).thenThrow(new ActiveShipmentExistsException("O1"));

        mvc.perform(post("/api/shipments").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ACTIVE_SHIPMENT_EXISTS"));
    }

    @Test
    void getReturns200AndMissingReturns404() throws Exception {
        when(service.findById("S1")).thenReturn(shipment());
        when(service.findById("nope")).thenThrow(new ShipmentNotFoundException("nope"));

        mvc.perform(get("/api/shipments/S1")).andExpect(status().isOk()).andExpect(jsonPath("$.id").value("S1"));
        mvc.perform(get("/api/shipments/nope")).andExpect(status().isNotFound());
    }

    @Test
    void listByOrderReturns200() throws Exception {
        when(service.findByOrderId("O1")).thenReturn(List.of(shipment()));

        mvc.perform(get("/api/shipments").param("orderId", "O1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("S1"));
    }

    @Test
    void transitionEndpointsReturn200() throws Exception {
        when(service.pickUp("S1")).thenReturn(shipment());
        when(service.deliver("S1")).thenReturn(shipment());
        when(service.markReturned("S1")).thenReturn(shipment());
        when(service.cancel("S1")).thenReturn(shipment());
        when(service.failAttempt("S1", "Không có người nhận")).thenReturn(shipment());
        when(service.refuse("S1", "Từ chối")).thenReturn(shipment());

        mvc.perform(post("/api/shipments/S1/pick-up")).andExpect(status().isOk());
        mvc.perform(post("/api/shipments/S1/deliver")).andExpect(status().isOk());
        mvc.perform(post("/api/shipments/S1/return")).andExpect(status().isOk());
        mvc.perform(post("/api/shipments/S1/cancel")).andExpect(status().isOk());
        mvc.perform(post("/api/shipments/S1/fail").contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"Không có người nhận\"}")).andExpect(status().isOk());
        mvc.perform(post("/api/shipments/S1/refuse").contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"Từ chối\"}")).andExpect(status().isOk());
        verify(service).failAttempt("S1", "Không có người nhận");
        verify(service).refuse("S1", "Từ chối");
    }

    @Test
    void invalidTransitionReturns409() throws Exception {
        when(service.cancel("S1")).thenThrow(new IllegalShipmentTransitionException("cancel", ShipmentStatus.IN_TRANSIT, ShipmentStatus.CREATED));

        mvc.perform(post("/api/shipments/S1/cancel"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("ILLEGAL_SHIPMENT_TRANSITION"));
    }

    @Test
    void blankReasonReturns400() throws Exception {
        mvc.perform(post("/api/shipments/S1/fail").contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("reason"));
        mvc.perform(post("/api/shipments/S1/refuse").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("reason"));
        verifyNoInteractions(service);
    }

    @Test
    void transitionOnMissingShipmentReturns404() throws Exception {
        when(service.pickUp("nope")).thenThrow(new ShipmentNotFoundException("nope"));

        mvc.perform(post("/api/shipments/nope/pick-up")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SHIPMENT_NOT_FOUND"));
    }
}
