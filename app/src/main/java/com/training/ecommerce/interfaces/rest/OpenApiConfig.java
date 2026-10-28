package com.training.ecommerce.interfaces.rest;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Thông tin hiển thị trên Swagger UI và {@code /v3/api-docs}.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI ecommerceOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Java Training Ecommerce API")
                .version("0.1.0")
                .description("API quản lý sản phẩm và đặt đơn hàng của khóa Java training. "
                        + "Profile local lưu in-memory, profile dev lưu PostgreSQL."));
    }
}
