package com.training.ecommerce.visualizer.world;

import com.training.ecommerce.domain.catalog.PhysicalProduct;
import com.training.ecommerce.domain.inventory.Warehouse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/** Dựng thế giới mới từ dữ liệu mẫu cố định cho mỗi lượt chạy. */
@Component
public class WorldFactory {

    public static final String WH_HN = "WH-HN";
    public static final String WH_HCM = "WH-HCM";

    /** Thế giới mẫu: 2 kho, hub có LAPTOP / PHONE / BOOK, giỏ rỗng, chưa có đơn. */
    public World create() {
        Warehouse hn = new Warehouse(WH_HN);
        Warehouse hcm = new Warehouse(WH_HCM);
        World world = new World(List.of(hn, hcm));

        // Thứ tự này quyết định thứ tự ô trong panel kho
        for (String id : List.of("LAPTOP", "PHONE", "HEADPHONE", "BOOK")) {
            world.trackProductId(id);
        }
        hn.add("LAPTOP", 10);
        hn.add("PHONE", 6);
        hn.add("HEADPHONE", 4);
        hcm.add("LAPTOP", 2);
        hcm.add("PHONE", 4);

        world.registerProduct(new PhysicalProduct("LAPTOP", "Laptop", new BigDecimal("20000000"), 5,
                new BigDecimal("2.0")));
        world.registerProduct(new PhysicalProduct("PHONE", "Điện thoại", new BigDecimal("12000000"), 8,
                new BigDecimal("0.4")));
        world.registerProduct(new PhysicalProduct("BOOK", "Sách", new BigDecimal("150000"), 12,
                new BigDecimal("0.5")));
        return world;
    }
}
