package com.training.ecommerce.application;

/**
 * S06: thông tin vận chuyển.
 *
 * @throws IllegalArgumentException khi carrier hoặc status null
 */
public record ShipmentInfo(String carrier, String status) {
    public ShipmentInfo {
        if (carrier == null || status == null) {
            throw new IllegalArgumentException("carrier, status không được null");
        }
    }
}
