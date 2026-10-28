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
