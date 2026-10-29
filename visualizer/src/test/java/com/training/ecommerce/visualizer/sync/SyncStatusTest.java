package com.training.ecommerce.visualizer.sync;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SyncStatusTest {

    @Test
    void bangNhauThiInSync() {
        SyncStatus s = SyncStatus.compare(List.of("a", "b"), List.of("b", "a"));
        assertTrue(s.inSync());
        assertTrue(s.added().isEmpty() && s.removed().isEmpty());
        assertEquals(2, s.snapshotSize());
        assertEquals(2, s.currentSize());
    }

    @Test
    void baoAddedVaRemoved() {
        SyncStatus s = SyncStatus.compare(List.of("a", "old"), List.of("a", "new"));
        assertFalse(s.inSync());
        assertEquals(List.of("new"), s.added());
        assertEquals(List.of("old"), s.removed());
    }

    @Test
    void parseBoQuaChuThichVaDongTrong() {
        assertEquals(List.of("a", "b"), SyncStatus.parseSnapshot(List.of("# c", "", "  ", "a", " b ")));
    }

    @Test
    void thieuSnapshotKhongCrash() {
        SyncStatus s = new DomainSyncService("/khong-ton-tai.txt").status();
        assertFalse(s.inSync());
        assertTrue(s.message().contains("snapshot"));
    }

    @Test
    void thamSoNullNemIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> SyncStatus.compare(null, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new DomainSyncService(" "));
    }
}
