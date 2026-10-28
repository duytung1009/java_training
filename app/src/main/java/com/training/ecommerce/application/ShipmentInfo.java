package com.training.ecommerce.application;

/**
 * S06: thông tin vận chuyển.
 *
 * @throws IllegalArgumentException khi carrier hoặc status null
 */
public record ShipmentInfo(String carrier, String status) {
    public ShipmentInfo {
        throw new UnsupportedOperationException("TODO S06");
    }
}
