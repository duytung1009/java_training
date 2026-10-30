package com.training.ecommerce.visualizer.core.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class VisualizerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void worldTraVeCayNode() throws Exception {
        mockMvc.perform(get("/api/viz/world"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nodes", hasSize(16)))
                .andExpect(jsonPath("$.nodes[1].id").value("warehouse:WH-HN"))
                .andExpect(jsonPath("$.nodes[0].kind").value("area"))
                .andExpect(jsonPath("$.nodes[1].items[0].value").value(10))
                .andExpect(jsonPath("$.nodes[1].items[0].computed").value(false))
                .andExpect(jsonPath("$.nodes[6].kind").value("collection"))
                .andExpect(jsonPath("$.nodes[8].parent").value("catalog"))
                .andExpect(jsonPath("$.nodes[13].parent").value("shipping"));
    }

    @Test
    void scenariosTraVeMuoiHaiKichBan() throws Exception {
        mockMvc.perform(get("/api/viz/scenarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(12)))
                .andExpect(jsonPath("$[0].id").value("overview"))
                .andExpect(jsonPath("$[0].session").value("S02–S07"));
    }

    @Test
    void runTraVeRunResultVoiTruongNullVanCoMat() throws Exception {
        mockMvc.perform(post("/api/viz/scenarios/decrease-stock-fail/run"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scenarioId").value("decrease-stock-fail"))
                .andExpect(jsonPath("$.concurrent").value(false))
                .andExpect(jsonPath("$.worldBefore.nodes", hasSize(16)))
                .andExpect(jsonPath("$.events[0].result").hasJsonPath())
                .andExpect(jsonPath("$.events[0].exception").hasJsonPath())
                .andExpect(jsonPath("$.events[0].flows").isArray())
                .andExpect(jsonPath("$.events[0].outcome").value("OK"))
                .andExpect(jsonPath("$.events[0].changes[0].before").hasJsonPath())
                .andExpect(jsonPath("$.events[0].changes[0].before").value((Object) null))
                .andExpect(jsonPath("$.events[3].outcome").value("THROWN"))
                .andExpect(jsonPath("$.events[3].exception.type").value("OutOfStockException"))
                .andExpect(jsonPath("$.events[3].changes", hasSize(0)));
    }

    @Test
    void idSaiTra404ProblemDetail() throws Exception {
        mockMvc.perform(post("/api/viz/scenarios/khong-co/run"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Không tìm thấy kịch bản: khong-co"));
    }

    // Chỉ kiểm hình dạng response: snapshot lệch domain là cảnh báo trên UI, không làm đỏ build
    @Test
    void syncTraVeTrangThaiDongBo() throws Exception {
        mockMvc.perform(get("/api/viz/sync"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inSync").isBoolean())
                .andExpect(jsonPath("$.added").isArray())
                .andExpect(jsonPath("$.removed").isArray())
                .andExpect(jsonPath("$.snapshotSize").isNumber())
                .andExpect(jsonPath("$.currentSize").isNumber());
    }
}
