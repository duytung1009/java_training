package com.training.ecommerce.visualizer.ecommerce.sync;

import com.training.ecommerce.visualizer.core.sync.DomainApiScanner;
import com.training.ecommerce.visualizer.ecommerce.EcommerceVisualizerAdapter;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Tái tạo snapshot API công khai của module domain ecommerce. Java thuần (không dùng Spring) để chạy được
 * bằng {@code java -cp} trần:
 * {@code java -cp visualizer/target/classes:domain/target/classes
 * com.training.ecommerce.visualizer.ecommerce.sync.EcommerceDomainApi visualizer/src/main/resources/domain-api.txt}
 */
public final class EcommerceDomainApi {

    static final String DEFAULT_OUTPUT = "visualizer/src/main/resources/domain-api.txt";
    static final String REGENERATE_COMMAND = "./mvnw -q -pl visualizer -am compile && java -cp "
            + "visualizer/target/classes:domain/target/classes "
            + "com.training.ecommerce.visualizer.ecommerce.sync.EcommerceDomainApi "
            + DEFAULT_OUTPUT;

    private EcommerceDomainApi() {
    }

    /**
     * Ghi snapshot ra file. {@code args[0]} là đường dẫn đích, mặc định {@value #DEFAULT_OUTPUT}.
     */
    public static void main(String[] args) throws IOException {
        Path target = Path.of(args.length > 0 ? args[0] : DEFAULT_OUTPUT);
        DomainApiScanner.write(new EcommerceVisualizerAdapter().domainApi(), target, REGENERATE_COMMAND);
    }
}
