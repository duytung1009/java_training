-- Schema PostgreSQL cho profile dev. Idempotent: chạy lại nhiều lần không lỗi.

-- Một bảng cho mọi loại product (single-table); cột riêng của từng loại để NULL với loại khác.
CREATE TABLE IF NOT EXISTS products (
    id            VARCHAR(64)   PRIMARY KEY,
    seq           BIGINT GENERATED ALWAYS AS IDENTITY,          -- thứ tự thêm; upsert không đổi seq
    type          VARCHAR(16)   NOT NULL CHECK (type IN ('PHYSICAL', 'DIGITAL', 'SUBSCRIPTION', 'BUNDLE')),
    name          VARCHAR(255)  NOT NULL,
    base_price    NUMERIC(19,2) NOT NULL CHECK (base_price >= 0), -- SUBSCRIPTION: giá/tháng; BUNDLE: tổng giá item (suy ra)
    stock         INT           NOT NULL CHECK (stock >= 0),      -- chặn âm ở DB, lớp phòng thủ cuối cùng chống oversell
    category      VARCHAR(32)   NOT NULL DEFAULT 'OTHER',
    weight_kg     NUMERIC(10,3) NULL,                              -- PHYSICAL
    months        INT           NULL,                              -- SUBSCRIPTION
    discount_rate NUMERIC(5,4)  NULL                               -- BUNDLE
);

-- Item của bundle theo thứ tự (position). Xóa bundle thì xóa luôn dòng item; không cho xóa product đang là item.
CREATE TABLE IF NOT EXISTS bundle_items (
    bundle_id VARCHAR(64) NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    item_id   VARCHAR(64) NOT NULL REFERENCES products (id),
    position  INT         NOT NULL,
    PRIMARY KEY (bundle_id, position)
);

CREATE TABLE IF NOT EXISTS orders (
    id            VARCHAR(64)   PRIMARY KEY,
    seq           BIGINT GENERATED ALWAYS AS IDENTITY,
    discount_rate NUMERIC(5,4)  NOT NULL CHECK (discount_rate BETWEEN 0 AND 1),
    tax_rate      NUMERIC(7,4)  NOT NULL CHECK (tax_rate >= 0),
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT now()
);

-- unit_price là snapshot giá lúc đặt: đổi giá product sau đó không làm đổi đơn cũ.
CREATE TABLE IF NOT EXISTS order_lines (
    order_id   VARCHAR(64)   NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    line_no    INT           NOT NULL,
    product_id VARCHAR(64)   NOT NULL REFERENCES products (id),
    quantity   INT           NOT NULL CHECK (quantity > 0),
    unit_price NUMERIC(19,2) NOT NULL CHECK (unit_price >= 0),
    PRIMARY KEY (order_id, line_no)
);

-- Vận đơn: aggregate riêng, tham chiếu đơn. Địa chỉ lưu phẳng (street, city).
CREATE TABLE IF NOT EXISTS shipments (
    id              VARCHAR(64)   PRIMARY KEY,
    seq             BIGINT GENERATED ALWAYS AS IDENTITY,
    order_id        VARCHAR(64)   NOT NULL REFERENCES orders (id),
    street          VARCHAR(255)  NOT NULL,
    city            VARCHAR(128)  NOT NULL,
    fee             NUMERIC(19,2) NOT NULL CHECK (fee >= 0),
    tracking_code   VARCHAR(32)   NOT NULL UNIQUE,
    status          VARCHAR(16)   NOT NULL CHECK (status IN ('CREATED', 'IN_TRANSIT', 'DELIVERED', 'RETURNING', 'RETURNED', 'CANCELLED')),
    failed_attempts INT           NOT NULL CHECK (failed_attempts BETWEEN 0 AND 3),
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now()
);

-- Mỗi đơn tối đa một vận đơn còn hiệu lực: lớp chặn cuối, điều kiện phải khớp ShipmentStatus.blocksNewShipment().
CREATE UNIQUE INDEX IF NOT EXISTS ux_shipments_active_order ON shipments (order_id)
    WHERE status NOT IN ('CANCELLED', 'RETURNED');

-- Lịch sử vận đơn theo thứ tự (position).
CREATE TABLE IF NOT EXISTS shipment_events (
    shipment_id VARCHAR(64)  NOT NULL REFERENCES shipments (id) ON DELETE CASCADE,
    position    INT          NOT NULL,
    status      VARCHAR(16)  NOT NULL,
    note        VARCHAR(500) NULL,
    PRIMARY KEY (shipment_id, position)
);
