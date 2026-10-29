package com.training.ecommerce.visualizer.sync;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DomainApiScannerTest {

    private final List<String> lines = DomainApiScanner.scan();

    @Test
    void timThayChuKyDaBiet() {
        assertTrue(lines.contains("class inventory.Warehouse"));
        assertTrue(lines.contains("class catalog.Product abstract"));
        assertTrue(lines.stream().anyMatch(l -> l.startsWith("method inventory.InventoryManager#decreaseStock(")));
        assertTrue(lines.contains("field shipping.ShippingZone.INNER_CITY: ShippingZone static"));
        assertTrue(lines.contains("enum shipping.ShippingZone"));
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
}
