-- Dữ liệu mẫu (giống LocalDataSeeder của profile local). ON CONFLICT DO NOTHING: chạy lại không trùng.
INSERT INTO products (id, type, name, base_price, stock, category, weight_kg)
VALUES ('P-TSHIRT', 'PHYSICAL', 'Áo thun nam', 150000.00, 50, 'FASHION', 0.500)
ON CONFLICT DO NOTHING;

INSERT INTO products (id, type, name, base_price, stock, category)
VALUES ('P-EBOOK', 'DIGITAL', 'Ebook Java', 99000.00, 100, 'SOFTWARE')
ON CONFLICT DO NOTHING;

INSERT INTO products (id, type, name, base_price, stock, category, months)
VALUES ('P-NEWS', 'SUBSCRIPTION', 'Gói tin tức', 30000.00, 20, 'SUBSCRIPTION', 12)
ON CONFLICT DO NOTHING;

-- Bundle: base_price = tổng giá bán các item (150000 + 99000)
INSERT INTO products (id, type, name, base_price, stock, category, discount_rate)
VALUES ('P-COMBO', 'BUNDLE', 'Combo áo + ebook', 249000.00, 10, 'OTHER', 0.1000)
ON CONFLICT DO NOTHING;

INSERT INTO bundle_items (bundle_id, item_id, position)
VALUES ('P-COMBO', 'P-TSHIRT', 0), ('P-COMBO', 'P-EBOOK', 1)
ON CONFLICT DO NOTHING;
