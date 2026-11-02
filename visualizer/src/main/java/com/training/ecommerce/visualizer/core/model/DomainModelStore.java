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
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Nơi giữ mô hình domain hiện hành. Thứ tự nạp: file ở {@code visualizer.model.path} (origin {@code file}),
 * rồi file trên classpath ở {@code visualizer.model.resource}, mặc định {@code /domain-model.json} ({@code classpath}), rồi gợi ý từ reflection ({@code suggestion}).
 *
 * <p>Đường dẫn mặc định {@code src/main/resources/domain-model.json} tính từ thư mục làm việc của tiến trình;
 * {@code spring-boot:run} chạy trong {@code visualizer/} nên đó là file trong module. Kết quả được cache và
 * {@link #save} làm mới cache, nên mô hình vừa lưu có hiệu lực ngay mà không cần build lại.
 */
@Component
public class DomainModelStore {

    private final DomainApiSpec spec;
    private final Function<Set<String>, List<DomainModel.RelationDef>> observed; // nhận tên các class domain
    private final Path path;
    private final String classpathResource; // null: không đọc classpath
    private final JsonMapper mapper = JsonMapper.builder().enable(SerializationFeature.INDENT_OUTPUT).build();

    private ModelSnapshot current;
    private DomainModel suggestion;
    private Map<String, Class<?>> classes; // quét classpath tốn kém nên tính một lần, lười

    /**
     * Dựng store cho Spring. {@link ScenarioService} chỉ được dùng để quan sát quan hệ.
     *
     * @param modelPath     đường dẫn file mô hình
     * @param modelResource đường dẫn classpath dự phòng khi chưa có file; rỗng thì bỏ qua classpath
     * @throws IllegalArgumentException nếu adapter, scenarios null hoặc modelPath rỗng
     */
    @Autowired
    public DomainModelStore(VisualizerAdapter<?> adapter, ScenarioService scenarios,
                            @Value("${visualizer.model.path:src/main/resources/domain-model.json}")
                            String modelPath,
                            @Value("${visualizer.model.resource:/domain-model.json}") String modelResource) {
        this(requireAdapter(adapter).domainApi(), observer(scenarios),
                Path.of(requireText(modelPath)), modelResource == null || modelResource.isBlank() ? null : modelResource);
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

    private static Function<Set<String>, List<DomainModel.RelationDef>> observer(ScenarioService scenarios) {
        if (scenarios == null) {
            throw new IllegalArgumentException("scenarios không được null");
        }
        return known -> ObservedRelations.observe(scenarios, known);
    }

    /**
     * @param spec     nơi lấy class domain
     * @param observed cung cấp quan hệ quan sát được (chỉ gọi lười, một lần)
     * @param path     đường dẫn file mô hình
     * @throws IllegalArgumentException nếu tham số null
     */
    public DomainModelStore(DomainApiSpec spec, Supplier<List<DomainModel.RelationDef>> observed, Path path) {
        this(spec, observed, path, null);
    }

    /**
     * @param classpathResource đường dẫn classpath dự phòng khi chưa có file, null thì không đọc classpath
     * @throws IllegalArgumentException nếu spec, observed hoặc path null
     */
    public DomainModelStore(DomainApiSpec spec, Supplier<List<DomainModel.RelationDef>> observed, Path path,
                            String classpathResource) {
        this(spec, observed == null ? null : known -> observed.get(), path, classpathResource);
    }

    private DomainModelStore(DomainApiSpec spec, Function<Set<String>, List<DomainModel.RelationDef>> observed,
                             Path path, String classpathResource) {
        if (spec == null || observed == null || path == null) {
            throw new IllegalArgumentException("spec, observed và path không được null");
        }
        this.spec = spec;
        this.observed = observed;
        this.path = path;
        this.classpathResource = classpathResource;
    }

    /** Class domain, quét một lần rồi dùng lại cho quan sát, kiểm tra và gợi ý. */
    private Map<String, Class<?>> classes() {
        if (classes == null) {
            classes = DomainModelSuggester.domainClasses(spec);
        }
        return classes;
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
            suggestion = DomainModelSuggester.suggest(spec, classes(), observed.apply(classes().keySet()));
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
        List<ModelError> errors = new DomainModelValidator(classes().keySet())
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
            try (InputStream in = classpathResource == null ? null
                    : DomainModelStore.class.getResourceAsStream(classpathResource)) {
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
