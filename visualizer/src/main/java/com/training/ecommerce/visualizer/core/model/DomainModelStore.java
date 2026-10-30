package com.training.ecommerce.visualizer.core.model;

import com.training.ecommerce.visualizer.core.scenario.ScenarioService;
import com.training.ecommerce.visualizer.core.spi.VisualizerAdapter;
import com.training.ecommerce.visualizer.core.sync.DomainApiSpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.function.Supplier;

/**
 * Nơi giữ mô hình domain hiện hành. Thứ tự nạp: file ở {@code visualizer.model.path} (origin {@code file}),
 * rồi {@code /domain-model.json} trên classpath ({@code classpath}), rồi gợi ý từ reflection ({@code suggestion}).
 *
 * <p>Đường dẫn mặc định {@code src/main/resources/domain-model.json} tính từ thư mục làm việc của tiến trình;
 * {@code spring-boot:run} chạy trong {@code visualizer/} nên đó là file trong module. Kết quả được cache và
 * {@link #save} làm mới cache, nên mô hình vừa lưu có hiệu lực ngay mà không cần build lại.
 */
@Component
public class DomainModelStore {

    private static final String CLASSPATH_RESOURCE = "/domain-model.json";

    private final DomainApiSpec spec;
    private final Supplier<List<DomainModel.RelationDef>> observed;
    private final Path path;
    private final JsonMapper mapper = JsonMapper.builder().enable(SerializationFeature.INDENT_OUTPUT).build();

    private ModelSnapshot current;
    private DomainModel suggestion;

    /**
     * Dựng store cho Spring. {@link ScenarioService} chỉ được dùng để quan sát quan hệ; việc điền
     * context/aggregate vào node làm ở controller nên không có vòng phụ thuộc.
     *
     * @param modelPath đường dẫn file mô hình
     * @throws IllegalArgumentException nếu tham số null/rỗng
     */
    @Autowired
    public DomainModelStore(VisualizerAdapter<?> adapter, ScenarioService scenarios,
                            @Value("${visualizer.model.path:src/main/resources/domain-model.json}")
                            String modelPath) {
        this(requireAdapter(adapter).domainApi(), observer(scenarios, adapter.domainApi()),
                Path.of(requireText(modelPath)));
    }

    private static VisualizerAdapter<?> requireAdapter(VisualizerAdapter<?> adapter) {
        if (adapter == null) {
            throw new IllegalArgumentException("adapter không được null");
        }
        return adapter;
    }

    private static String requireText(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("visualizer.model.path không được rỗng");
        }
        return text;
    }

    private static Supplier<List<DomainModel.RelationDef>> observer(ScenarioService scenarios, DomainApiSpec spec) {
        if (scenarios == null) {
            throw new IllegalArgumentException("scenarios không được null");
        }
        return () -> ObservedRelations.observe(scenarios, DomainModelSuggester.domainClasses(spec).keySet());
    }

    /**
     * @param spec     nơi lấy class domain
     * @param observed cung cấp quan hệ quan sát được (chỉ gọi lười, một lần)
     * @param path     đường dẫn file mô hình
     * @throws IllegalArgumentException nếu tham số null
     */
    public DomainModelStore(DomainApiSpec spec, Supplier<List<DomainModel.RelationDef>> observed, Path path) {
        if (spec == null || observed == null || path == null) {
            throw new IllegalArgumentException("spec, observed và path không được null");
        }
        this.spec = spec;
        this.observed = observed;
        this.path = path;
    }

    /** Mô hình hiện hành kèm nguồn gốc; được cache cho tới khi {@link #save}. */
    public synchronized ModelSnapshot current() {
        if (current == null) {
            current = load();
        }
        return current;
    }

    /** Gợi ý từ reflection cộng quan hệ quan sát; tính một lần (chạy mọi kịch bản) rồi cache. */
    public synchronized DomainModel suggestion() {
        if (suggestion == null) {
            suggestion = DomainModelSuggester.suggest(spec, observed.get());
        }
        return suggestion;
    }

    /**
     * Kiểm tra rồi ghi nguyên tử (file tạm cùng thư mục, rồi move) và làm mới cache.
     *
     * @throws IllegalArgumentException   nếu model null
     * @throws InvalidModelException      nếu mô hình không hợp lệ (file không bị đụng tới)
     * @throws ModelNotWritableException  nếu không ghi được file
     */
    public synchronized ModelSnapshot save(DomainModel model) {
        if (model == null) {
            throw new IllegalArgumentException("model không được null");
        }
        List<ModelError> errors = new DomainModelValidator(DomainModelSuggester.domainClasses(spec).keySet())
                .validate(model);
        if (!errors.isEmpty()) {
            throw new InvalidModelException(errors);
        }
        Path target = path.toAbsolutePath();
        Path temp = null;
        try {
            Path dir = target.getParent();
            Files.createDirectories(dir);
            temp = Files.createTempFile(dir, "domain-model", ".tmp");
            Files.writeString(temp, mapper.writeValueAsString(model) + "\n", StandardCharsets.UTF_8);
            try {
                Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            if (temp != null) {
                try {
                    Files.deleteIfExists(temp);
                } catch (IOException ignored) {
                    // file tạm mồ côi không ảnh hưởng mô hình
                }
            }
            throw new ModelNotWritableException("Không ghi được file mô hình " + target + ": " + e.getMessage(), e);
        }
        current = new ModelSnapshot("file", model);
        return current;
    }

    private ModelSnapshot load() {
        try {
            if (Files.isRegularFile(path)) {
                return new ModelSnapshot("file", mapper.readValue(Files.readString(path, StandardCharsets.UTF_8),
                        DomainModel.class));
            }
            try (InputStream in = DomainModelStore.class.getResourceAsStream(CLASSPATH_RESOURCE)) {
                if (in != null) {
                    return new ModelSnapshot("classpath", mapper.readValue(
                            new String(in.readAllBytes(), StandardCharsets.UTF_8), DomainModel.class));
                }
            }
        } catch (IOException | RuntimeException e) {
            throw new IllegalStateException("Không đọc được file mô hình domain-model.json: " + e.getMessage(), e);
        }
        return new ModelSnapshot("suggestion", suggestion());
    }
}
