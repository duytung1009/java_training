import test from 'node:test';
import assert from 'node:assert/strict';
import { iso, flat, CELL } from '../../main/resources/static/js/map/iso.js';
import { IDENTITY, multiply, invert, apply, bounds, lerp, toAttr, GRID, planeMatrix, fitRect, visibleRect, lerpRect } from '../../main/resources/static/js/map/projection.js';

const near = (a, b, eps = 1e-6) => assert.ok(Math.abs(a - b) < eps, `${a} ≉ ${b}`);
const nearAll = (a, b, eps) => a.forEach((v, i) => near(v, b[i], eps));
const parse = s => s.match(/-?[\d.]+/g).map(Number);
const det = m => m[0] * m[3] - m[1] * m[2];
const len = ([x, y]) => Math.hypot(x, y);

test('multiply với invert ≈ đơn vị', () => {
  for (const m of [GRID.iso, GRID.flat, [2, 1, -1, 3, 5, 7]]) {
    nearAll(multiply(m, invert(m)), IDENTITY);
    nearAll(multiply(invert(m), m), IDENTITY);
  }
  assert.throws(() => invert([1, 2, 2, 4, 0, 0]));
});

test('multiply áp n trước, m sau', () => {
  const m = [1, 0, 0, 1, 10, 0], n = [2, 0, 0, 2, 0, 0];
  nearAll(apply(multiply(m, n), [1, 1]), apply(m, apply(n, [1, 1])));
  nearAll(apply(multiply(m, n), [1, 1]), [12, 2]);
});

test('planeMatrix: iso là đơn vị, flat đưa điểm iso về lưới × CELL (sai khác một tịnh tiến chung)', () => {
  assert.deepEqual(planeMatrix('iso'), IDENTITY);
  const pm = planeMatrix('flat'), p0 = apply(pm, iso(0, 0));
  for (const [x, y] of [[3, 0], [0, 2], [4.5, 1.5]]) {
    const p = apply(pm, iso(x, y));
    nearAll([p[0] - p0[0], p[1] - p0[1]], [x * CELL, y * CELL]);
  }
  assert.throws(() => planeMatrix('xyz'));
});

test('chữ flat() sau planeMatrix(flat) đứng thẳng, cùng cỡ', () => {
  for (const along of ['x', 'y']) {
    const t = parse(flat(0, 0, along));
    const m = multiply(planeMatrix('flat'), t);
    nearAll(m.slice(0, 4), along === 'x' ? [1, 0, 0, 1] : [0, -1, 1, 0], 0.01); // y: chạy dọc trục y của lưới
  }
  const mx = multiply(planeMatrix('flat'), parse(flat(0, 0, 'x')));
  near(det(mx), 1, 0.01);
});

test('vector đơn vị của lưới dài CELL ở cả hai phép chiếu', () => {
  for (const name of ['iso', 'flat']) {
    const pm = planeMatrix(name), o = apply(pm, iso(0, 0));
    for (const [x, y] of [[1, 0], [0, 1]]) {
      const p = apply(pm, iso(x, y));
      near(len([p[0] - o[0], p[1] - o[1]]), CELL, 1e-6);
    }
  }
});

test('lerp: hai đầu đúng, giữa chừng không suy biến', () => {
  const a = planeMatrix('iso'), b = planeMatrix('flat');
  nearAll(lerp(a, b, 0), a);
  nearAll(lerp(a, b, 1), b);
  assert.ok(Math.abs(det(lerp(a, b, 0.5))) > 0.1);
});

test('bounds và toAttr', () => {
  assert.deepEqual(bounds(IDENTITY, [[1, 2], [4, -1], [3, 5]]), { x: 1, y: -1, w: 3, h: 6 });
  const b = bounds([2, 0, 0, 3, 1, 1], [[0, 0], [1, 1]]);
  assert.deepEqual(b, { x: 1, y: 1, w: 2, h: 3 });
  assert.equal(toAttr([1, 0, 0, 1, 2, 3]), 'matrix(1 0 0 1 2 3)');
});

test('fitRect: vừa nội dung + lề vào hộp, giữ tỉ lệ, căn giữa, svg = hộp', () => {
  const f = fitRect({ x: 100, y: 50, w: 200, h: 100 }, { w: 400, h: 400 }, { margin: 0 });
  assert.equal(f.width, 400); assert.equal(f.height, 400);
  nearAll([f.viewBox.w, f.viewBox.h], [200, 200]); // scale 2: hộp vuông phủ vừa chiều rộng
  nearAll([f.viewBox.x + f.viewBox.w / 2, f.viewBox.y + f.viewBox.h / 2], [200, 100]); // tâm trùng tâm nội dung
  // lề nở nội dung thành 300×200 trước khi vừa hộp 600×300 → scale 1.5
  const m = fitRect({ x: 0, y: 0, w: 200, h: 100 }, { w: 600, h: 300 }, { margin: 50 });
  nearAll([m.viewBox.w, m.viewBox.h], [400, 200]);
  nearAll([m.viewBox.x + m.viewBox.w / 2, m.viewBox.y + m.viewBox.h / 2], [100, 50]);
  near(m.viewBox.w / m.viewBox.h, 600 / 300);
});

test('fitRect: scale bị chặn bởi maxScale (nội dung nhỏ không phóng quá tay)', () => {
  const f = fitRect({ x: 0, y: 0, w: 10, h: 10 }, { w: 500, h: 500 }, { margin: 0, maxScale: 2.5 });
  nearAll([f.viewBox.w, f.viewBox.h], [200, 200]); // 500 / 2.5
  nearAll([f.viewBox.x, f.viewBox.y], [-95, -95]); // vẫn căn giữa nội dung
  assert.equal(f.width, 500);
});

test('visibleRect: vùng viewBox đang thấy theo scroll; svg nhỏ hơn hộp thì kẹp theo svg', () => {
  const vb = { x: 10, y: 20, w: 400, h: 200 };
  // svg 800×400 (scale 2) trong hộp 200×100, scroll (100, 60) → viewBox x = 10 + 50, y = 20 + 30, rộng 100×50
  assert.deepEqual(visibleRect(vb, { w: 800, h: 400 }, { left: 100, top: 60 }, { w: 200, h: 100 }), { x: 60, y: 50, w: 100, h: 50 });
  // scroll vượt biên bị kẹp về svg.w - box.w
  assert.deepEqual(visibleRect(vb, { w: 800, h: 400 }, { left: 9999, top: 9999 }, { w: 200, h: 100 }), { x: 10 + 300, y: 20 + 150, w: 100, h: 50 });
  // svg nhỏ hơn hộp: thấy cả viewBox
  assert.deepEqual(visibleRect(vb, { w: 200, h: 100 }, { left: 0, top: 0 }, { w: 500, h: 500 }), vb);
  assert.deepEqual(visibleRect(vb, { w: 0, h: 0 }, { left: 0, top: 0 }, { w: 500, h: 500 }), vb); // svg chưa có kích thước
});

test('fitRect + visibleRect: svg = hộp, zoom 1, scroll 0 → thấy đúng viewBox của fitRect', () => {
  const box = { w: 640, h: 360 }, f = fitRect({ x: 5, y: 7, w: 90, h: 120 }, box, { margin: 12, maxScale: 2.5 });
  nearAll(Object.values(visibleRect(f.viewBox, { w: f.width, h: f.height }, { left: 0, top: 0 }, box)), Object.values(f.viewBox));
});

test('lerpRect: hai đầu đúng, giữa là trung điểm', () => {
  const a = { x: 0, y: 0, w: 100, h: 50 }, b = { x: 100, y: 20, w: 300, h: 150 };
  assert.deepEqual(lerpRect(a, b, 0), a);
  assert.deepEqual(lerpRect(a, b, 1), b);
  assert.deepEqual(lerpRect(a, b, 0.5), { x: 50, y: 10, w: 200, h: 100 });
});
