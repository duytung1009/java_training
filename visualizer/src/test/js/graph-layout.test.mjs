import test from 'node:test';
import assert from 'node:assert/strict';
import { layeredLayout, placeLabels } from '../../main/resources/static/js/view/graph-layout.js';

const N = (...ids) => ids.map(id => ({ id }));
const E = (...pairs) => pairs.map(([from, to], i) => ({ id: `e${i}`, from, to }));

// Đoạn thẳng (ngang/dọc) cắt phần trong của chữ nhật (co 0.5px để chạm viền không tính)
function segHitsRect([x1, y1], [x2, y2], r) {
  const lx = Math.min(x1, x2), hx = Math.max(x1, x2), ly = Math.min(y1, y2), hy = Math.max(y1, y2);
  return hx > r.x + 0.5 && lx < r.x + r.w - 0.5 && hy > r.y + 0.5 && ly < r.y + r.h - 0.5;
}
// Trả về danh sách vi phạm: đoạn của cạnh cắt khối không phải đầu mút; đoạn chéo cũng bị báo
function violations(layout, edges) {
  const bad = [];
  for (const e of edges) {
    const pts = layout.edges[e.id].points;
    for (let i = 0; i + 1 < pts.length; i++) {
      const [a, b] = [pts[i], pts[i + 1]];
      if (a[0] !== b[0] && a[1] !== b[1]) bad.push(`${e.id}: đoạn chéo`);
      for (const [id, r] of Object.entries(layout.nodes)) {
        if (id === e.from || id === e.to) continue;
        if (segHitsRect(a, b, r)) bad.push(`${e.id} cắt khối ${id}`);
      }
    }
  }
  return bad;
}
// Khối không chồng nhau
function overlaps(layout) {
  const rs = Object.entries(layout.nodes);
  const bad = [];
  for (let i = 0; i < rs.length; i++) for (let j = i + 1; j < rs.length; j++) {
    if (segHitsRect([rs[i][1].x, rs[i][1].y], [rs[i][1].x + rs[i][1].w, rs[i][1].y + rs[i][1].h], rs[j][1])) bad.push(`${rs[i][0]}~${rs[j][0]}`);
  }
  return bad;
}
// Bộ sinh số ngẫu nhiên có hạt giống
const rng = seed => () => (seed = (seed * 1664525 + 1013904223) % 4294967296) / 4294967296;

test('chuỗi a→b→c: ba tầng theo thứ tự', () => {
  const l = layeredLayout(N('a', 'b', 'c'), E(['a', 'b'], ['b', 'c']));
  assert.deepEqual(['a', 'b', 'c'].map(id => l.nodes[id].layer), [0, 1, 2]);
  assert.ok(l.nodes.a.y < l.nodes.b.y && l.nodes.b.y < l.nodes.c.y);
  assert.deepEqual(violations(l, E(['a', 'b'], ['b', 'c'])), []);
});

test('nguồn ở trên các nút phụ thuộc; điểm đầu/cuối nằm trên viền khối', () => {
  const es = E(['up', 'x'], ['up', 'y'], ['x', 'z']);
  const l = layeredLayout(N('x', 'y', 'z', 'up'), es);
  assert.equal(l.nodes.up.layer, 0);
  assert.ok(l.nodes.up.y < l.nodes.x.y && l.nodes.up.y < l.nodes.y.y && l.nodes.x.y < l.nodes.z.y);
  const pts = l.edges.e0.points, up = l.nodes.up, x = l.nodes.x;
  assert.equal(pts[0][1], up.y + up.h); // rời đáy khối trên
  assert.equal(pts.at(-1)[1], x.y); // vào đỉnh khối dưới
  assert.ok(pts[0][0] > up.x && pts[0][0] < up.x + up.w);
});

test('chu trình: không lặp vô hạn, có cạnh bị đảo (reversed)', () => {
  const es = E(['a', 'b'], ['b', 'c'], ['c', 'a']);
  const l = layeredLayout(N('a', 'b', 'c'), es);
  assert.equal(Object.values(l.edges).filter(e => e.reversed).length, 1);
  assert.equal(l.edges.e2.reversed, true);
  assert.deepEqual(violations(l, es), []);
  // points đi từ `from` tới `to` dù đã đảo: cạnh c→a bắt đầu ở c
  const c = l.nodes.c, p = l.edges.e2.points;
  assert.ok(p[0][1] >= c.y && p[0][1] <= c.y + c.h);
  assert.equal(layeredLayout(N('a'), E(['a', 'a'])).edges.e0.points.length, 0); // tự trỏ
});

test('cạnh dài có nút giả và không cắt khối nào', () => {
  const es = E(['a', 'b'], ['b', 'c'], ['c', 'd'], ['a', 'd'], ['a', 'c']);
  const l = layeredLayout(N('a', 'b', 'c', 'd'), es);
  assert.equal(l.edges.e3.dummies, 2);
  assert.equal(l.edges.e4.dummies, 1);
  assert.deepEqual(violations(l, es), []);
  assert.deepEqual(overlaps(l), []);
});

test('đồ thị ngẫu nhiên (có hạt giống): không đoạn nào cắt khối, khối không chồng, toàn đoạn ngang/dọc', () => {
  for (let seed = 1; seed <= 40; seed++) {
    const r = rng(seed), n = 3 + Math.floor(r() * 14), ids = Array.from({ length: n }, (_, i) => `n${i}`);
    const nodes = ids.map(id => ({ id, w: 100 + Math.floor(r() * 120), h: 40 + Math.floor(r() * 80) }));
    const es = [];
    for (let i = 0, m = Math.floor(r() * n * 1.8); i < m; i++) es.push({ id: `e${i}`, from: ids[Math.floor(r() * n)], to: ids[Math.floor(r() * n)] });
    if (seed % 5 === 0) nodes[0].rank = 2; // thêm nút ghim, sinh cạnh cùng tầng / cạnh lên
    const l = layeredLayout(nodes, es);
    assert.deepEqual(violations(l, es), [], `seed ${seed}`);
    assert.deepEqual(overlaps(l), [], `seed ${seed}`);
    assert.ok(l.crossings <= l.crossingsBefore);
    for (const p of Object.values(l.nodes)) assert.ok(p.x >= 0 && p.y >= 0 && p.x + p.w <= l.width && p.y + p.h <= l.height);
    for (const e of Object.values(l.edges)) for (const [px, py] of e.points) assert.ok(px >= 0 && px <= l.width && py >= 0 && py <= l.height, `seed ${seed}`);
  }
});

test('giảm giao cắt: đồ thị biết trước sau quét không tệ hơn trước, và hết giao cắt', () => {
  // a1→b2, a2→b1 ở thứ tự đầu vào gây giao cắt; quét barycenter gỡ được
  const es = E(['a1', 'b2'], ['a2', 'b1']);
  const l = layeredLayout(N('a1', 'a2', 'b1', 'b2'), es);
  assert.equal(l.crossingsBefore, 1);
  assert.equal(l.crossings, 0);
  assert.ok(l.nodes.b2.x < l.nodes.b1.x);
});

test('xác định: cùng đầu vào cho cùng kết quả', () => {
  const es = E(['a', 'b'], ['a', 'c'], ['b', 'd'], ['c', 'd'], ['a', 'd'], ['d', 'a']);
  const run = () => layeredLayout(N('a', 'b', 'c', 'd'), es);
  assert.deepEqual(run(), run());
});

test('nút cô lập ở tầng 0; đồ thị rỗng; cạnh tới nút lạ bị bỏ', () => {
  const l = layeredLayout(N('a', 'b', 'iso'), E(['a', 'b'], ['a', 'ghost']));
  assert.equal(l.nodes.iso.layer, 0);
  assert.equal(l.edges.e1, undefined);
  const empty = layeredLayout([], []);
  assert.deepEqual([empty.nodes, empty.edges], [{}, {}]);
  assert.ok(empty.width > 0 && empty.height > 0);
});

test('rank ghim nút vào tầng; cạnh cùng tầng đi vòng dưới và không cắt khối', () => {
  const nodes = [{ id: 'a' }, { id: 'b', rank: 0 }, { id: 'c' }];
  const es = E(['a', 'b'], ['a', 'c']);
  const l = layeredLayout(nodes, es);
  assert.equal(l.nodes.b.layer, 0);
  assert.equal(l.nodes.a.layer, 0);
  assert.equal(l.nodes.c.layer, 1);
  const flat = l.edges.e0.points; // a→b cùng tầng: rời đáy a, quay lên đáy b
  assert.ok(flat.length >= 4);
  assert.deepEqual(violations(l, es), []);
  assert.equal(layeredLayout([{ id: 'p', rank: 3 }], []).nodes.p.layer, 3);
});

test('kích thước theo nút (w/h, sizeOf) được tôn trọng', () => {
  const l = layeredLayout([{ id: 'a', w: 300, h: 90 }, { id: 'b' }, { id: 'c' }], E(['a', 'b'], ['a', 'c']), { sizeOf: n => (n.id === 'c' ? { w: 50, h: 20 } : null) });
  assert.deepEqual([l.nodes.a.w, l.nodes.a.h, l.nodes.c.w, l.nodes.c.h, l.nodes.b.w], [300, 90, 50, 20, 160]);
});

const perLayer = l => {
  const c = {};
  for (const n of Object.values(l.nodes)) c[n.layer] = (c[n.layer] ?? 0) + 1;
  return c;
};

test('maxPerLayer: sao 8 nút phụ thuộc không có tầng nào rộng hơn giới hạn, cạnh vẫn hướng xuống', () => {
  const ids = Array.from({ length: 8 }, (_, i) => `d${i}`);
  const es = ids.map((d, i) => ({ id: `e${i}`, from: 'root', to: d }));
  const l = layeredLayout(N('root', ...ids), es);
  const bound = 4; // n = 9 → ceil(√9) + 1
  for (const c of Object.values(perLayer(l))) assert.ok(c <= bound);
  for (const e of es) assert.ok(l.nodes[e.from].layer < l.nodes[e.to].layer);
  assert.deepEqual(violations(l, es), []);
  assert.deepEqual(overlaps(l), []);
  assert.ok(l.width < 8 * 160); // hẹp hơn một hàng 8 khối
  const wide = layeredLayout(N('root', ...ids), es, { maxPerLayer: 8 });
  assert.equal(perLayer(wide)[1], 8); // tuỳ chọn ghi đè được
});

test('maxPerLayer: nút ghim giữ nguyên tầng, nút kéo theo vẫn nằm dưới nguồn', () => {
  const nodes = [{ id: 'a' }, { id: 'b', rank: 1 }, { id: 'c' }, { id: 'd' }, { id: 'e' }, { id: 'f' }];
  const es = E(['a', 'b'], ['a', 'c'], ['a', 'd'], ['a', 'e'], ['c', 'f']);
  const l = layeredLayout(nodes, es, { maxPerLayer: 2 });
  assert.equal(l.nodes.b.layer, 1);
  for (const e of es) if (e.to !== 'b') assert.ok(l.nodes[e.from].layer < l.nodes[e.to].layer);
  assert.deepEqual(violations(l, es), []);
});

test('placeLabels: nhãn không đè nhau và không đè khối; không chỗ thì dịch vuông góc', () => {
  const pts = [[0, 0], [200, 0]];
  const items = [{ id: 'a', points: pts, text: '1 lời gọi' }, { id: 'b', points: pts, text: '2 lời gọi' }];
  const out = placeLabels(items, []);
  assert.equal(out.a.x, 100); // giữa đoạn
  const box = o => ({ x: o.x - o.w / 2, y: o.y - o.h / 2, w: o.w, h: o.h });
  const [ra, rb] = [box(out.a), box(out.b)];
  assert.ok(ra.x + ra.w <= rb.x || rb.x + rb.w <= ra.x || ra.y + ra.h <= rb.y || rb.y + rb.h <= ra.y);
  assert.equal(out.b.y, 15); // 50/30/70% đều đè nhãn a (đoạn 200px, nhãn ~60px) → dịch xuống một chiều cao nhãn
  const long = placeLabels(items.map(i => ({ ...i, points: [[0, 0], [400, 0]] })), []);
  assert.deepEqual([long.a.x, long.b.x], [200, 120]); // đủ dài: nhãn b lùi sang 30%
  // khối chặn giữa đoạn → chọn vị trí khác
  const blocked = placeLabels([{ ...items[0], points: [[0, 0], [400, 0]] }], [{ x: 180, y: -10, w: 40, h: 20 }]);
  assert.equal(blocked.a.x, 120); // giữa bị chặn → thử 30%
  // đoạn quá ngắn không chỗ nào vừa → dịch xuống một chiều cao nhãn
  const tight = placeLabels([{ id: 'x', points: [[0, 0], [10, 0]], text: 'rất dài rất dài' }], [{ x: -100, y: -8, w: 300, h: 15 }]);
  assert.equal(tight.x.y, 0 + 15);
  assert.deepEqual(placeLabels(items, []), out); // xác định
});
