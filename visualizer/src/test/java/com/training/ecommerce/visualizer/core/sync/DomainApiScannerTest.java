package com.training.ecommerce.visualizer.core.sync;

import com.training.ecommerce.visualizer.core.sync.fixture.Account;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Scanner chạy trên domain giả ({@code core.sync.fixture}) để chứng minh nó không gắn với ecommerce. */
class DomainApiScannerTest {

    private static final DomainApiSpec SPEC = new DomainApiSpec(
            "com.training.ecommerce.visualizer.core.sync.fixture", Account.class, "/khong-can.txt");

    private final List<String> lines = DomainApiScanner.scan(SPEC);

    @Test
    void timThayChuKyCuaDomainGia() {
        assertTrue(lines.contains("class Account"));
        assertTrue(lines.contains("ctor Account(String)"));
        assertTrue(lines.contains("method Account#balance(): int"));
        assertTrue(lines.contains("method Account#open(String): Account static"));
        assertTrue(lines.contains("field Account.LIMIT: int static"));
        assertTrue(lines.contains("enum Currency"));
    }

    @Test
    void boQuaLopKhongPublicVaThanhVienKhongPublic() {
        assertFalse(lines.stream().anyMatch(l -> l.contains("PackagePrivateThing")));
        assertFalse(lines.stream().anyMatch(l -> l.contains("hidden") || l.contains("anonymous")));
    }

    @Test
    void daSapXepVaKhongTrung() {
        List<String> sorted = new ArrayList<>(lines);
        sorted.sort(null);
        assertEquals(sorted, lines);
        assertEquals(lines.size(), new HashSet<>(lines).size());
    }

    @Test
    void khongCoLopAnDanhVaKhongCoThanhVienRiengTu() {
        assertTrue(lines.stream().noneMatch(l -> l.matches(".*\\$\\d.*") || l.contains("$")));
        assertFalse(lines.stream().anyMatch(l -> l.contains("$VALUES")));
        assertFalse(lines.stream().anyMatch(l -> l.contains("java.util.")));
    }

    @Test
    void specVaScanValidate() {
        assertThrows(IllegalArgumentException.class, () -> DomainApiScanner.scan(null));
        assertThrows(IllegalArgumentException.class, () -> new DomainApiSpec(" ", Account.class, "/x.txt"));
        assertThrows(IllegalArgumentException.class, () -> new DomainApiSpec("a.b", null, "/x.txt"));
        assertThrows(IllegalArgumentException.class, () -> new DomainApiSpec("a.b", Account.class, null));
    }
}
