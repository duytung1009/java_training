package com.training.ecommerce.visualizer.sync;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Kết quả so sánh API domain hiện tại với snapshot lần sync cuối.
 *
 * @param added        có trong domain hiện tại nhưng không có trong snapshot
 * @param removed      có trong snapshot nhưng domain không còn
 * @param snapshotSize số chữ ký trong snapshot
 * @param currentSize  số chữ ký trong domain hiện tại
 * @param message      ghi chú thêm (vd. thiếu snapshot); null nếu không có
 */
public record SyncStatus(boolean inSync, List<String> added, List<String> removed,
                         int snapshotSize, int currentSize, String message) {

    /**
     * @throws IllegalArgumentException nếu snapshot hoặc current null
     */
    public static SyncStatus compare(Collection<String> snapshot, Collection<String> current) {
        if (snapshot == null || current == null) {
            throw new IllegalArgumentException("snapshot và current không được null");
        }
        Set<String> snap = new TreeSet<>(snapshot);
        Set<String> cur = new TreeSet<>(current);
        List<String> added = cur.stream().filter(s -> !snap.contains(s)).toList();
        List<String> removed = snap.stream().filter(s -> !cur.contains(s)).toList();
        return new SyncStatus(added.isEmpty() && removed.isEmpty(), added, removed, snap.size(), cur.size(), null);
    }

    /** Trạng thái khi không đọc được snapshot. */
    public static SyncStatus missingSnapshot(int currentSize) {
        return new SyncStatus(false, List.of(), List.of(), 0, currentSize,
                "Không tìm thấy snapshot domain-api.txt");
    }

    /** Bỏ dòng trống và dòng chú thích bắt đầu bằng {@code #}. */
    public static List<String> parseSnapshot(Collection<String> rawLines) {
        if (rawLines == null) {
            throw new IllegalArgumentException("rawLines không được null");
        }
        return rawLines.stream().map(String::strip).filter(l -> !l.isEmpty() && !l.startsWith("#")).toList();
    }
}
