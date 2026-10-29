package com.training.ecommerce.infrastructure.persistence.jdbc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Nền chung cho test JDBC trên PostgreSQL thật (Testcontainers, container singleton). Cần Docker; loại trừ bằng {@code -DexcludedGroups=docker}.
 *
 * <p>Slice {@code @JdbcTest} chạy schema.sql + data.sql (profile dev đã đặt {@code spring.sql.init.mode=always}).
 * Test không tự rollback ({@code NOT_SUPPORTED}) để test được transaction và đa luồng thật; mỗi test dọn bảng ở đầu.
 */
@Tag("docker")
@ActiveProfiles("dev")      // thay profile mặc định local (đang loại trừ DataSource / JDBC autoconfig)
@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
abstract class PostgresJdbcTestSupport {

    /**
     * Một container dùng chung cho mọi class test (singleton, Ryuk dọn khi JVM thoát). Không dùng
     * {@code @Container}: Spring cache context giữa các class, mà container theo class sẽ bị tắt trong khi
     * context cache vẫn trỏ tới nó.
     */
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17");

    static {
        POSTGRES.start();
    }

    @Autowired
    protected JdbcClient jdbc;

    @BeforeEach
    void cleanTables() {
        jdbc.sql("TRUNCATE shipment_events, shipments, order_lines, orders, bundle_items, products").update();
    }
}
