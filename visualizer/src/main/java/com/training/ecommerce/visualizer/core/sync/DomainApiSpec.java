package com.training.ecommerce.visualizer.core.sync;

/**
 * Mô tả phần domain cần quét API công khai.
 *
 * @param basePackage      package gốc của domain, vd. {@code com.training.ecommerce.domain}
 * @param anchor           một class thuộc domain, dùng để tìm nơi chứa class (thư mục hoặc jar)
 * @param snapshotResource đường dẫn classpath của file snapshot, vd. {@code /domain-api.txt}
 */
public record DomainApiSpec(String basePackage, Class<?> anchor, String snapshotResource) {

    /**
     * @throws IllegalArgumentException nếu basePackage hoặc snapshotResource null/rỗng, hoặc anchor null
     */
    public DomainApiSpec {
        if (basePackage == null || basePackage.isBlank()) {
            throw new IllegalArgumentException("basePackage không được null hoặc rỗng");
        }
        if (anchor == null) {
            throw new IllegalArgumentException("anchor không được null");
        }
        if (snapshotResource == null || snapshotResource.isBlank()) {
            throw new IllegalArgumentException("snapshotResource không được null hoặc rỗng");
        }
    }
}
