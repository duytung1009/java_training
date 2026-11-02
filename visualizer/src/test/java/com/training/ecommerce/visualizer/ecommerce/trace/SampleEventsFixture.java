package com.training.ecommerce.visualizer.ecommerce.trace;

import com.training.ecommerce.domain.catalog.Product;
import com.training.ecommerce.visualizer.core.scenario.RunResult;
import com.training.ecommerce.visualizer.core.spi.DomainEventMapper;
import com.training.ecommerce.visualizer.core.spi.EventFlow;
import com.training.ecommerce.visualizer.core.trace.Flow;
import com.training.ecommerce.visualizer.core.trace.TraceRecorder;
import com.training.ecommerce.visualizer.ecommerce.world.World;
import com.training.ecommerce.visualizer.ecommerce.world.WorldFactory;
import com.training.ecommerce.visualizer.ecommerce.world.WorldSnapshotter;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Sinh {@code static/fixtures/sample-events.json}: một {@link RunResult} y như
 * {@code POST /api/viz/scenarios/{id}/run} để xem domain event trên trình duyệt qua {@code ?fixture=events}.
 *
 * <p>Nằm ở test của <b>ecommerce</b> (không phải test lõi) vì bản đồ vẽ theo bố cục của adapter ecommerce: fixture
 * từ domain giả thuần sẽ không có chỗ để vẽ. Dùng {@code WorldFactory}/{@code WorldSnapshotter} thật cho phần
 * view, còn event ({@code StockReserved}, {@code OrderPlaced}, {@code CartAudited}) là record chỉ có trong test này.
 * Ba tình huống: một lời gọi có 2 event (một có flow, một không), một phản ứng có {@code causedBy}, và một lời
 * gọi THROWN vẫn mang event ghi trước lúc ném. Test {@code SampleEventsFixtureTest} đảm bảo file commit khớp
 * kết quả sinh lại (bỏ qua {@code at}, {@code thread}).
 *
 * <p>Sinh lại file (từ thư mục gốc repo):
 * <pre>
 * ./mvnw -q -pl visualizer -am test-compile dependency:build-classpath -pl visualizer \
 *     -Dmdep.outputFile=/tmp/viz-cp.txt -Dmdep.includeScope=test
 * java -cp "visualizer/target/test-classes:visualizer/target/classes:domain/target/classes:$(cat /tmp/viz-cp.txt)" \
 *     com.training.ecommerce.visualizer.ecommerce.trace.SampleEventsFixture \
 *     visualizer/src/main/resources/static/fixtures/sample-events.json
 * </pre>
 */
public final class SampleEventsFixture {

    /** Domain event giả (chỉ dùng cho fixture): đã giữ chỗ tồn kho. */
    record StockReserved(String productId, int quantity) {
    }

    /** Domain event giả: đơn được tạo từ giỏ. */
    record OrderPlaced(String productId, int quantity) {
    }

    /** Domain event giả không có flow: chỉ hiện trong log. */
    record CartAudited(String note) {
    }

    private SampleEventsFixture() {
    }

    /** Mapper của fixture; các cặp node đều có đường nối trong {@code adapter/layout.js}. */
    static DomainEventMapper mapper() {
        return event -> switch (event) {
            case StockReserved s -> Optional.of(new EventFlow(WorldSnapshotter.HUB, WorldSnapshotter.CART, null,
                    s.productId() + " ×" + s.quantity()));
            case OrderPlaced o -> Optional.of(new EventFlow(WorldSnapshotter.CART, "orders", null,
                    o.productId() + " ×" + o.quantity()));
            default -> Optional.empty();
        };
    }

    /** Dựng và chạy kịch bản mẫu, trả kết quả như API. */
    public static RunResult generate() {
        World world = new WorldFactory().create();
        WorldSnapshotter snapshotter = new WorldSnapshotter();
        List<Object> pending = new ArrayList<>(); // nguồn event kiểu pullDomainEvents
        TraceRecorder recorder = new TraceRecorder(() -> snapshotter.snapshot(world), () -> {
            List<Object> out = List.copyOf(pending);
            pending.clear();
            return out;
        }, mapper());
        var before = snapshotter.snapshot(world).view();
        Product laptop = world.product("LAPTOP");
        Flow hubToCart = new Flow(WorldSnapshotter.HUB, WorldSnapshotter.CART);
        List<String> cartPanel = List.of(WorldSnapshotter.CART);

        // 1. Một lời gọi, 2 event: StockReserved (emit, có flow) rồi CartAudited (rút từ nguồn, không flow)
        recorder.run("Cart", "add", List.of("LAPTOP", "1"), hubToCart, cartPanel, () -> {
            world.cart().add(laptop, 1);
            recorder.emit(new StockReserved("LAPTOP", 1));
            pending.add(new CartAudited("giỏ có 1 dòng"));
        });
        // 2. Phản ứng của listener trước StockReserved: có causedBy
        for (Object event : recorder.lastEvents()) {
            if (event instanceof StockReserved) {
                recorder.react(event, "StockAuditor", "onStockReserved", List.of("LAPTOP", "1"),
                        List.of(new Flow(WorldSnapshotter.CART, "orders")), List.of("orders"),
                        () -> world.cart().changeQuantity(laptop, 2));
            }
        }
        // 3. Lời gọi THROWN: OrderPlaced đã ghi trước khi Cart.add ném (quá tồn kho)
        try {
            recorder.run("Cart", "add", List.of("LAPTOP", "999"), hubToCart, cartPanel, () -> {
                recorder.emit(new OrderPlaced("LAPTOP", 999));
                world.cart().add(laptop, 999);
            });
        } catch (RuntimeException expected) {
            // đã được recorder ghi thành THROWN; fixture muốn cho thấy đúng điều đó
        }
        return new RunResult("sample-events", "Domain event → flow (mẫu)", false, before, recorder.events());
    }

    /** JSON pretty-print của {@link #generate()}. */
    public static String toJson() {
        return JsonMapper.builder().enable(SerializationFeature.INDENT_OUTPUT).build().writeValueAsString(generate());
    }

    /**
     * @param args {@code args[0]} là đường dẫn file JSON cần ghi
     */
    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            throw new IllegalArgumentException("Cần đúng một tham số: đường dẫn file JSON đầu ra");
        }
        Files.writeString(Path.of(args[0]), toJson() + "\n", StandardCharsets.UTF_8);
    }
}
