package com.training.ecommerce.visualizer.core.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DomainModelControllerTest {

    private static final Path DIR = tempDir();

    private static Path tempDir() {
        try {
            return Files.createTempDirectory("viz-model-test");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("visualizer.model.path", () -> DIR.resolve("sub").resolve("domain-model.json").toString());
        // repo đã có domain-model.json trên classpath; bỏ classpath để "chưa lưu" thật sự trả gợi ý
        registry.add("visualizer.model.resource", () -> "");
    }

    @Autowired
    private MockMvc mockMvc;

    private static final String VALID = """
            {"version":1,
             "contexts":[{"id":"inventory","title":"Kho","color":"--hub"}],
             "classes":[{"cls":"Warehouse","context":"inventory","role":"aggregate","hidden":false}],
             "relations":[]}""";

    @Test
    void goiYTraVeMoHinhVoiVersion1() throws Exception {
        mockMvc.perform(get("/api/viz/model/suggestion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.contexts[*].id", hasItem("inventory")))
                .andExpect(jsonPath("$.classes[?(@.cls=='Order')].role").value("aggregate"));
    }

    // Một test tuần tự vì store giữ cache: GET trước khi lưu → PUT → GET lại
    @Test
    void vongDoiGetPutGet() throws Exception {
        mockMvc.perform(get("/api/viz/model"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.origin").value("suggestion"))
                .andExpect(jsonPath("$.model.version").value(1));

        mockMvc.perform(put("/api/viz/model").contentType(MediaType.APPLICATION_JSON).content(VALID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.origin").value("file"))
                .andExpect(jsonPath("$.model.contexts[0].title").value("Kho"));

        mockMvc.perform(get("/api/viz/model"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.origin").value("file"))
                .andExpect(jsonPath("$.model.contexts", hasSize(1)))
                .andExpect(jsonPath("$.model.contexts[0].color").value("--hub"))
                .andExpect(jsonPath("$.model.classes", hasSize(1)))
                .andExpect(jsonPath("$.model.classes[0].cls").value("Warehouse"));

        // Mô hình mới có hiệu lực ngay trên world: Warehouse thuộc inventory, các class khác chưa xếp
        mockMvc.perform(get("/api/viz/world"))
                .andExpect(jsonPath("$.nodes[1].context").value("inventory"))
                .andExpect(jsonPath("$.nodes[1].aggregate").value("Warehouse"))
                .andExpect(jsonPath("$.nodes[3].context").value((Object) null));
    }

    @Test
    void putKhongHopLeTra400KemDanhSachLoi() throws Exception {
        String bad = """
                {"contexts":[],"classes":[{"cls":"KhongCo","context":null,"role":"value","hidden":false}],
                 "relations":[]}""";
        mockMvc.perform(put("/api/viz/model").contentType(MediaType.APPLICATION_JSON).content(bad))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Mô hình không hợp lệ"))
                .andExpect(jsonPath("$.errors", hasSize(2)))
                .andExpect(jsonPath("$.errors[*].cls", hasItem("KhongCo")))
                .andExpect(jsonPath("$.errors[0].message").isString());
    }

    @Test
    void runTraVeNodeCoContext() throws Exception {
        mockMvc.perform(post("/api/viz/scenarios/decrease-stock-ok/run"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.worldBefore.nodes[1].id").value("warehouse:WH-HN"))
                .andExpect(jsonPath("$.worldBefore.nodes[1].context").value("inventory"))
                .andExpect(jsonPath("$.events[0].worldAfter.nodes[1].context").value("inventory"));
    }
}
