import test from 'node:test';
import assert from 'node:assert/strict';
import { iso, flat, CELL } from '../../main/resources/static/js/map/iso.js';
import { IDENTITY, multiply, invert, apply, bounds, lerp, toAttr, GRID, planeMatrix } from '../../main/resources/static/js/map/projection.js';

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
