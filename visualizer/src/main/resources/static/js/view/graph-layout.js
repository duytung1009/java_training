// Bố cục phân tầng kiểu Sugiyama (phiên bản gọn): chọn cạnh ngược để phá chu trình → chia tầng theo đường dài nhất →
// nút giả (dummy) cho cạnh dài → giảm giao cắt bằng barycenter → toạ độ x (hồi quy đơn điệu, không chồng) → cạnh đi vuông góc
// trong "kênh" giữa hai tầng. Hàm thuần (không đụng DOM), xác định: cùng đầu vào luôn ra cùng kết quả.
//
// Ngữ nghĩa: cạnh { from, to } đặt `from` ở tầng trên `to` (từ nguồn xuống). Người gọi muốn "A phụ thuộc B, B nằm trên" thì
// truyền cạnh { from: B, to: A }. `points` của cạnh luôn đi từ `from` tới `to`, dù tầng có bị đảo (cạnh ngược/cạnh lên).
//
// layeredLayout(nodes, edges, { direction: 'TB', nodeW, nodeH, layerGap, nodeGap, margin, sizeOf?, sweeps?, dummyW?, track? })
//   maxPerLayer: số nút thật tối đa mỗi tầng (mặc định min(5, max(3, ceil(√n)+1))); tầng dư thì đẩy nút xuống tầng sau
//   nodes: [{ id, w?, h?, rank? }] (rank = ghim vào tầng đó) · edges: [{ id, from, to }]
//   → { nodes: { id → { x, y, w, h, layer, order } }, edges: { id → { points, reversed, dummies } }, width, height,
//       crossingsBefore, crossings }
// Chỉ hỗ trợ direction 'TB' (tầng từ trên xuống).

const TRACK_CLEAR = 4; // khoảng hở tối thiểu giữa hai đoạn ngang cùng một rãnh

export function layeredLayout(nodes, edges, opts = {}) {
  const { nodeW = 160, nodeH = 60, layerGap = 56, nodeGap = 32, margin = 24, sweeps = 6, dummyW = 16, track = 9, sizeOf } = opts;
  const maxPerLayer = opts.maxPerLayer ?? Math.min(5, Math.max(3, Math.ceil(Math.sqrt(nodes.length)) + 1));

  // ---- Đỉnh thật + cạnh hợp lệ ----
  const verts = nodes.map(n => {
    const s = sizeOf?.(n);
    return { id: n.id, real: true, w: n.w ?? s?.w ?? nodeW, h: n.h ?? s?.h ?? nodeH, rank: n.rank, layer: 0 };
  });
  const index = new Map(verts.map((v, i) => [v.id, i]));
  const N = verts.length;
  const es = edges.map(e => ({ id: e.id, from: index.get(e.from), to: index.get(e.to), reversed: false }))
    .filter(e => e.from !== undefined && e.to !== undefined);
  const real = es.filter(e => e.from !== e.to); // cạnh tự trỏ không có đường

  // ---- 1. Phá chu trình: DFS (bắt đầu từ nút không có cạnh vào, rồi tới các nút còn lại, theo thứ tự đầu vào) ----
  const outs = Array.from({ length: N }, () => []), indeg = new Array(N).fill(0);
  for (const e of real) { outs[e.from].push(e); indeg[e.to]++; }
  const state = new Array(N).fill(0);
  const visit = u => {
    state[u] = 1;
    for (const e of outs[u]) { if (state[e.to] === 1) e.reversed = true; else if (state[e.to] === 0) visit(e.to); }
    state[u] = 2;
  };
  for (let u = 0; u < N; u++) if (!indeg[u] && !state[u]) visit(u);
  for (let u = 0; u < N; u++) if (!state[u]) visit(u);

  // ---- 2. Chia tầng: đường dài nhất từ nguồn (tầng 0 ở trên); nút có rank thì ghim ----
  const pinned = verts.map(v => Number.isFinite(v.rank));
  verts.forEach((v, i) => { v.layer = pinned[i] ? Math.max(0, Math.trunc(v.rank)) : 0; });
  const eff = Array.from({ length: N }, () => []), din = new Array(N).fill(0);
  for (const e of real) {
    const [u, v] = e.reversed ? [e.to, e.from] : [e.from, e.to];
    eff[u].push(v); din[v]++;
  }
  const queue = [];
  for (let u = 0; u < N; u++) if (!din[u]) queue.push(u);
  for (let qi = 0; qi < queue.length; qi++) {
    const u = queue[qi];
    for (const v of eff[u]) {
      if (!pinned[v]) verts[v].layer = Math.max(verts[v].layer, verts[u].layer + 1);
      if (--din[v] === 0) queue.push(v);
    }
  }

  // ---- 2b. Giới hạn bề ngang tầng (kiểu Coffman–Graham): duyệt từ trên xuống, tầng quá `maxPerLayer` thì đẩy nút dư xuống một tầng
  // (ưu tiên nút ít phụ thuộc phía dưới nhất, hoà thì nút đứng sau), rồi kéo các nút phía dưới xuống cho cạnh vẫn hướng xuống. Nút ghim giữ nguyên.
  const effEdges = real.map(e => (e.reversed ? [e.to, e.from] : [e.from, e.to]));
  const dependents = new Array(N).fill(0);
  for (const [u] of effEdges) dependents[u]++;
  const settle = () => {
    for (let changed = true; changed;) {
      changed = false;
      for (const [u, v] of effEdges) if (!pinned[v] && verts[v].layer <= verts[u].layer) { verts[v].layer = verts[u].layer + 1; changed = true; }
    }
  };
  for (let l = 0; l <= verts.reduce((m, v) => Math.max(m, v.layer), 0); l++) {
    for (;;) {
      const movable = verts.map((v, i) => i).filter(i => verts[i].layer === l && !pinned[i]);
      if (verts.filter(v => v.layer === l).length <= maxPerLayer || !movable.length) break;
      const pick = movable.reduce((b, i) => (dependents[i] <= dependents[b] ? i : b));
      verts[pick].layer = l + 1;
      settle();
    }
  }

  // ---- 3. Nút giả cho cạnh vượt hơn một tầng ----
  for (const e of real) {
    const la = verts[e.from].layer, lb = verts[e.to].layer;
    e.flat = la === lb;
    e.top = la <= lb ? e.from : e.to;
    e.bot = e.top === e.from ? e.to : e.from;
    e.flip = e.top !== e.from; // chuỗi dựng từ trên xuống; flip → đảo lại để points đi từ `from`
    e.chain = [e.top];
    if (!e.flat) {
      for (let l = verts[e.top].layer + 1; l < verts[e.bot].layer; l++) {
        verts.push({ id: null, real: false, w: dummyW, h: 0, layer: l });
        e.chain.push(verts.length - 1);
      }
      e.chain.push(e.bot);
    }
  }
  const layerCount = verts.reduce((m, v) => Math.max(m, v.layer + 1), 0);
  const layers = Array.from({ length: layerCount }, () => []);
  verts.forEach((v, i) => layers[v.layer].push(i));
  const up = verts.map(() => []), down = verts.map(() => []);
  for (const e of real) for (let i = 0; i + 1 < e.chain.length; i++) { down[e.chain[i]].push(e.chain[i + 1]); up[e.chain[i + 1]].push(e.chain[i]); }

  // ---- 4. Giảm giao cắt: quét barycenter xuống rồi lên, giữ thứ tự tốt nhất (hoà thì theo thứ tự đầu vào) ----
  const pos = new Array(verts.length).fill(0);
  const setPos = () => layers.forEach(l => l.forEach((v, i) => { pos[v] = i; }));
  const countCrossings = () => {
    let total = 0;
    for (let l = 0; l + 1 < layers.length; l++) {
      const links = [];
      for (const a of layers[l]) for (const b of down[a]) links.push([pos[a], pos[b]]);
      for (let i = 0; i < links.length; i++) for (let j = i + 1; j < links.length; j++) {
        if ((links[i][0] - links[j][0]) * (links[i][1] - links[j][1]) < 0) total++;
      }
    }
    return total;
  };
  const reorder = (l, nbrs) => {
    const key = v => (nbrs[v].length ? nbrs[v].reduce((s, n) => s + pos[n], 0) / nbrs[v].length : pos[v]);
    const keyed = layers[l].map(v => [key(v), pos[v], v]).sort((p, q) => p[0] - q[0] || p[1] - q[1]);
    layers[l] = keyed.map(k => k[2]);
    layers[l].forEach((v, i) => { pos[v] = i; });
  };
  setPos();
  const crossingsBefore = countCrossings();
  let best = layers.map(l => [...l]), bestCross = crossingsBefore;
  for (let s = 0; s < sweeps && bestCross > 0; s++) {
    for (let l = 1; l < layers.length; l++) reorder(l, up);
    for (let l = layers.length - 2; l >= 0; l--) reorder(l, down);
    const c = countCrossings();
    if (c < bestCross) { bestCross = c; best = layers.map(l => [...l]); }
  }
  for (let l = 0; l < layers.length; l++) layers[l] = best[l];
  setPos();

  // ---- 5a. Toạ độ x: xếp sát nhau từ trái sang phải, căn giữa từng tầng, rồi kéo về trung bình hàng xóm (hồi quy đơn điệu) ----
  const x = new Array(verts.length).fill(0); // tâm
  const sep = (a, b) => (verts[a].real && verts[b].real ? nodeGap : 8) + (verts[a].w + verts[b].w) / 2;
  for (const l of layers) {
    for (let i = 1; i < l.length; i++) x[l[i]] = x[l[i - 1]] + sep(l[i - 1], l[i]);
    const mid = l.length ? (x[l[0]] + x[l[l.length - 1]]) / 2 : 0;
    for (const v of l) x[v] -= mid;
  }
  const place = l => {
    const n = l.length;
    if (!n) return;
    const off = [0];
    for (let i = 1; i < n; i++) off.push(off[i - 1] + sep(l[i - 1], l[i]));
    const target = l.map((v, i) => {
      const ns = [...up[v], ...down[v]];
      return (ns.length ? ns.reduce((s, k) => s + x[k], 0) / ns.length : x[v]) - off[i];
    });
    const blocks = []; // PAV: khối { sum, n }; gộp khi trung bình khối trước lớn hơn khối sau
    for (const t of target) {
      blocks.push({ sum: t, n: 1 });
      while (blocks.length > 1) {
        const b = blocks[blocks.length - 1], a = blocks[blocks.length - 2];
        if (a.sum / a.n <= b.sum / b.n) break;
        blocks.splice(blocks.length - 2, 2, { sum: a.sum + b.sum, n: a.n + b.n });
      }
    }
    let i = 0;
    for (const b of blocks) for (let k = 0; k < b.n; k++, i++) x[l[i]] = b.sum / b.n + off[i];
  };
  for (let it = 0; it < 6; it++) {
    if (it % 2 === 0) layers.forEach(place); else for (let l = layers.length - 1; l >= 0; l--) place(layers[l]);
  }
  const minLeft = verts.reduce((m, v, i) => Math.min(m, x[i] - v.w / 2), Infinity);
  const dx = margin - (Number.isFinite(minLeft) ? minLeft : 0);
  for (let i = 0; i < x.length; i++) x[i] += dx;

  // ---- 5b. Cổng: các cạnh rời/vào cùng một cạnh của khối được rải đều dọc theo cạnh, sắp theo x của đầu kia ----
  const bottomItems = verts.map(() => []), topItems = verts.map(() => []);
  real.forEach((e, ei) => {
    if (e.flat) {
      bottomItems[e.from].push({ k: `${ei}:f${e.from}`, tx: x[e.to], ei });
      bottomItems[e.to].push({ k: `${ei}:f${e.to}`, tx: x[e.from], ei });
      return;
    }
    for (let i = 0; i + 1 < e.chain.length; i++) {
      bottomItems[e.chain[i]].push({ k: `${ei}:b${i}`, tx: x[e.chain[i + 1]], ei });
      topItems[e.chain[i + 1]].push({ k: `${ei}:t${i}`, tx: x[e.chain[i]], ei });
    }
  });
  const portX = new Map();
  const spread = (items, v) => {
    const sorted = [...items].sort((p, q) => p.tx - q.tx || p.ei - q.ei);
    const left = x[v] - verts[v].w / 2;
    sorted.forEach((it, i) => portX.set(it.k, left + (verts[v].w * (i + 1)) / (sorted.length + 1)));
  };
  verts.forEach((_, v) => { spread(bottomItems[v], v); spread(topItems[v], v); });

  // ---- 5c. Rãnh ngang trong kênh giữa các tầng (kênh l nằm dưới tầng l); đoạn chồng khoảng x thì khác rãnh ----
  const gapLinks = Array.from({ length: layerCount }, () => []);
  real.forEach((e, ei) => {
    if (e.flat) { gapLinks[verts[e.from].layer].push({ ei, i: 'f', xa: portX.get(`${ei}:f${e.from}`), xb: portX.get(`${ei}:f${e.to}`) }); return; }
    for (let i = 0; i + 1 < e.chain.length; i++) {
      gapLinks[verts[e.chain[i]].layer].push({ ei, i, xa: portX.get(`${ei}:b${i}`), xb: portX.get(`${ei}:t${i}`) });
    }
  });
  const gapH = [], trackOf = new Map();
  gapLinks.forEach((links, l) => {
    const horiz = links.filter(k => k.xa !== k.xb).map(k => ({ ...k, lo: Math.min(k.xa, k.xb), hi: Math.max(k.xa, k.xb) }))
      .sort((p, q) => p.lo - q.lo || p.hi - q.hi || p.ei - q.ei);
    const ends = [];
    for (const k of horiz) {
      let t = ends.findIndex(end => end + TRACK_CLEAR < k.lo);
      if (t < 0) { t = ends.length; ends.push(-Infinity); }
      ends[t] = k.hi;
      trackOf.set(`${k.ei}:${k.i}`, t);
    }
    const last = l === layerCount - 1;
    gapH[l] = last && !links.length ? 0 : Math.max(last ? 0 : layerGap, (ends.length + 1) * track);
    gapLinks[l] = { count: ends.length };
  });

  // ---- 5d. Toạ độ y: tầng căn mép trên, cao bằng khối cao nhất ----
  const layerTop = [], layerBottom = [];
  let y = margin;
  for (let l = 0; l < layerCount; l++) {
    layerTop[l] = y;
    layerBottom[l] = y + layers[l].reduce((m, v) => Math.max(m, verts[v].h), 0);
    y = layerBottom[l] + gapH[l];
  }
  const height = y + margin;
  const nodeBottom = v => layerTop[verts[v].layer] + verts[v].h;

  // ---- 6. Đường đi vuông góc ----
  const out = { nodes: {}, edges: {} };
  verts.forEach((v, i) => {
    if (!v.real) return;
    const order = layers[v.layer].filter(k => verts[k].real).indexOf(i);
    out.nodes[v.id] = { x: x[i] - v.w / 2, y: layerTop[v.layer], w: v.w, h: v.h, layer: v.layer, order };
  });
  const tidy = pts => {
    const dedup = pts.filter((p, i) => !i || p[0] !== pts[i - 1][0] || p[1] !== pts[i - 1][1]);
    return dedup.filter((p, i) => {
      if (!i || i === dedup.length - 1) return true;
      const a = dedup[i - 1], b = dedup[i + 1];
      return !((a[0] === p[0] && p[0] === b[0]) || (a[1] === p[1] && p[1] === b[1]));
    });
  };
  const yAt = (ei, i, l) => {
    const t = trackOf.get(`${ei}:${i}`), count = gapLinks[l].count;
    return t === undefined ? layerBottom[l] + gapH[l] / 2 : layerBottom[l] + (gapH[l] * (t + 1)) / (count + 1);
  };
  real.forEach((e, ei) => {
    let pts;
    if (e.flat) {
      const l = verts[e.from].layer, yc = yAt(ei, 'f', l);
      pts = [[portX.get(`${ei}:f${e.from}`), nodeBottom(e.from)], [portX.get(`${ei}:f${e.from}`), yc],
        [portX.get(`${ei}:f${e.to}`), yc], [portX.get(`${ei}:f${e.to}`), nodeBottom(e.to)]];
    } else {
      pts = [[portX.get(`${ei}:b0`), nodeBottom(e.top)]];
      for (let i = 0; i + 1 < e.chain.length; i++) {
        const yc = yAt(ei, i, verts[e.chain[i]].layer);
        pts.push([portX.get(`${ei}:b${i}`), yc], [portX.get(`${ei}:t${i}`), yc]);
      }
      pts.push([portX.get(`${ei}:t${e.chain.length - 2}`), layerTop[verts[e.bot].layer]]);
    }
    pts = tidy(pts);
    out.edges[e.id] = { points: e.flip ? pts.reverse() : pts, reversed: e.reversed, dummies: Math.max(0, e.chain.length - 2) };
  });
  for (const e of es) if (e.from === e.to) out.edges[e.id] = { points: [], reversed: false, dummies: 0 };

  const width = verts.reduce((m, v, i) => Math.max(m, x[i] + v.w / 2), 0) + margin;
  return { ...out, width: Math.max(width, margin * 2), height: Math.max(height, margin * 2), crossingsBefore, crossings: bestCross };
}

// Đặt nhãn (badge) lên đường đi, tránh đè nhau và đè khối. items: [{ id, points, text }]; obstacles: [{ x, y, w, h }].
// Với mỗi nhãn thử lần lượt từng đoạn (dài trước) ở 50%, 30%, 70%, lấy chỗ đầu tiên không chạm nhãn đã đặt / khối nào;
// không có chỗ thì dịch vuông góc khỏi đoạn dài nhất (đường ngang → xuống một chiều cao nhãn, đường dọc → sang bên cạnh).
// Trả { id → { x, y, w, h } } với (x, y) là tâm nhãn. Hàm thuần, xác định.
export function placeLabels(items, obstacles = [], { h = 15, charW = 5.6, pad = 10 } = {}) {
  const placed = [], out = {};
  const hit = (a, b) => a.x < b.x + b.w && a.x + a.w > b.x && a.y < b.y + b.h && a.y + a.h > b.y;
  const rectAt = (cx, cy, w) => ({ x: cx - w / 2, y: cy - h / 2, w, h });
  const free = r => !placed.some(p => hit(r, p)) && !obstacles.some(o => hit(r, o));
  for (const it of items) {
    const pts = it.points ?? [];
    if (pts.length < 1) continue;
    const w = it.text.length * charW + pad;
    const segs = [];
    for (let i = 0; i + 1 < pts.length; i++) segs.push({ a: pts[i], b: pts[i + 1], len: Math.abs(pts[i + 1][0] - pts[i][0]) + Math.abs(pts[i + 1][1] - pts[i][1]) });
    segs.sort((p, q) => q.len - p.len); // sort ổn định: hoà giữ thứ tự dọc theo đường
    let found = null;
    for (const sg of segs) {
      for (const t of [0.5, 0.3, 0.7]) {
        const r = rectAt(sg.a[0] + (sg.b[0] - sg.a[0]) * t, sg.a[1] + (sg.b[1] - sg.a[1]) * t, w);
        if (free(r)) { found = r; break; }
      }
      if (found) break;
    }
    if (!found) {
      const sg = segs[0] ?? { a: pts[0], b: pts[0] };
      const mx = (sg.a[0] + sg.b[0]) / 2, my = (sg.a[1] + sg.b[1]) / 2;
      found = sg.a[1] === sg.b[1] ? rectAt(mx, my + h, w) : rectAt(mx + w / 2 + 4, my, w);
    }
    placed.push(found);
    out[it.id] = { x: found.x + found.w / 2, y: found.y + found.h / 2, w: found.w, h };
  }
  return out;
}
