package com.training.ecommerce.visualizer.core.sync;

import com.training.ecommerce.visualizer.core.spi.VisualizerAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** So sánh API domain hiện tại với file snapshot của adapter (vd. {@code domain-api.txt}); tính một lần rồi cache. */
@Service
public class DomainSyncService {

    private final DomainApiSpec spec;
    private volatile SyncStatus cached;

    /**
     * @throws IllegalArgumentException nếu adapter null
     */
    @Autowired
    public DomainSyncService(VisualizerAdapter<?> adapter) {
        this(adapter == null ? null : adapter.domainApi());
    }

    /**
     * @throws IllegalArgumentException nếu spec null
     */
    DomainSyncService(DomainApiSpec spec) {
        if (spec == null) {
            throw new IllegalArgumentException("spec không được null");
        }
        this.spec = spec;
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
        List<String> current = DomainApiScanner.scan(spec);
        try (InputStream in = DomainSyncService.class.getResourceAsStream(spec.snapshotResource())) {
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
