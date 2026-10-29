package com.training.ecommerce.visualizer.sync;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** So sánh API domain hiện tại với snapshot {@code domain-api.txt}; tính một lần rồi cache. */
@Service
public class DomainSyncService {

    static final String SNAPSHOT_RESOURCE = "/domain-api.txt";

    private final String resource;
    private volatile SyncStatus cached;

    public DomainSyncService() {
        this(SNAPSHOT_RESOURCE);
    }

    /**
     * @throws IllegalArgumentException nếu resource null hoặc rỗng
     */
    DomainSyncService(String resource) {
        if (resource == null || resource.isBlank()) {
            throw new IllegalArgumentException("resource không được null hoặc rỗng");
        }
        this.resource = resource;
    }

    public SyncStatus status() {
        SyncStatus status = cached;
        if (status == null) {
            status = compute();
            cached = status;
        }
        return status;
    }

    private SyncStatus compute() {
        List<String> current = DomainApiScanner.scan();
        try (InputStream in = DomainSyncService.class.getResourceAsStream(resource)) {
            if (in == null) {
                return SyncStatus.missingSnapshot(current.size());
            }
            List<String> raw = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)).lines().toList();
            return SyncStatus.compare(SyncStatus.parseSnapshot(raw), current);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
