import test from 'node:test';
import assert from 'node:assert/strict';
import {
  parseHash, toHash, normalizeState, nodeLookup, blockOf, buildContextBlocks, buildAggregateBlocks,
  resolveFlowAt, countFlows, layoutGraph, edgeWidth, contextColor, buildInstanceBlocks, instanceLayout, itemRows, touchedFirst, hasFocus,
} from '../../main/resources/static/js/view/levels.js';

const model = () => ({
  version: 1,
  contexts: [{ id: 'sales', title: 'Bán hàng', color: null }, { id: 'billing', title: 'Thanh toán', color: '--hub' }, { id: 'empty', title: 'Trống', color: null }],
  classes: [
    { cls: 'Order', context: 'sales', role: 'aggregate', hidden: false },
    { cls: 'Line', context: 'sales', role: 'entity', hidden: false },
    { cls: 'Cart', context: 'sales', role: 'service', hidden: false },
    { cls: 'Oops', context: 'sales', role: 'other', hidden: true },
    { cls: 'Invoice', context: 'billing', role: 'aggregate', hidden: false },
  ],
  relations: [
    { from: 'Order', to: 'Line', source: 'field' },
    { from: 'Order', to: 'Invoice', source: 'observed:s1' },
    { from: 'Cart', to: 'Invoice', source: 'param' },
    { from: 'Invoice', to: 'Order', source: 'param' },
  ],
});
const node = (id, kind, cls, extra = {}) => ({ id, kind, cls, parent: null, items: [], ...extra });
const world = () => ({
  nodes: [
    node('sales', 'area', 'domain.sales'),
    node('orders', 'collection', 'List<Order>', { parent: 'sales' }),
    node('order:1', 'object', 'Order', { parent: 'orders' }),
    node('order:2', 'object', 'Order', { parent: 'orders' }),
    node('line:1', 'object', 'Line'),
    node('cart', 'object', 'Cart'),
    node('billing', 'area', 'domain.billing'),
    node('invoice:1', 'object', 'Invoice', { parent: 'billing' }),
    node('free', 'object', 'Mystery'),
  ],
});
const lk = () => nodeLookup(null, world(), null, [['order:', 'orders'], ['inv:', 'billing']]);
const S = { context: { level: 'context', ctx: null, agg: null }, agg: { level: 'aggregate', ctx: 'sales', agg: null }, inst: { level: 'instance', ctx: null, agg: null } };

test('hash: round-trip và hash lạ về mặc định', () => {
  for (const s of [S.context, S.agg, S.inst, { level: 'instance', ctx: 'sales', agg: 'Order' }]) assert.deepEqual(parseHash(toHash(s)), s);
  assert.equal(toHash({ level: 'aggregate', ctx: 'sales', agg: 'Order' }), '#level=aggregate&ctx=sales&agg=Order');
  assert.deepEqual(parseHash('#level=aggregate&ctx=order&agg=Order'), { level: 'aggregate', ctx: 'order', agg: 'Order' });
  for (const bad of ['', '#', '#foo', '#level=galaxy&ctx=x', undefined]) assert.deepEqual(parseHash(bad), S.inst);
});

test('normalizeState: không mô hình → instance; aggregate thiếu ctx lấy context đầu; ctx lạ bị bỏ', () => {
  assert.deepEqual(normalizeState(S.context, null), S.inst);
  assert.deepEqual(normalizeState({ level: 'aggregate', ctx: null, agg: null }, model()), { level: 'aggregate', ctx: 'sales', agg: null });
  assert.deepEqual(normalizeState({ level: 'aggregate', ctx: 'nope', agg: 'X' }, model()), { level: 'aggregate', ctx: 'sales', agg: null });
  assert.deepEqual(normalizeState({ level: 'instance', ctx: 'nope', agg: 'X' }, model()), S.inst);
  assert.deepEqual(normalizeState({ level: 'instance', ctx: 'sales', agg: 'Order' }, model()), { level: 'instance', ctx: 'sales', agg: 'Order' });
});

test('nodeLookup: world sau thắng world hiện tại thắng world trước; id lạ rơi về đầu mút theo tiền tố', () => {
  const b = { nodes: [node('x', 'object', 'Old'), node('only-before', 'object', 'B')] };
  const n = { nodes: [node('x', 'object', 'Now'), node('only-now', 'object', 'N')] };
  const a = { nodes: [node('x', 'object', 'New')] };
  const l = nodeLookup(b, n, a, [['order:', 'x']]);
  assert.equal(l('x').cls, 'New');
  assert.equal(l('only-now').cls, 'N');
  assert.equal(l('only-before').cls, 'B');
  assert.equal(l('order:99').cls, 'New'); // tiền tố → đầu mút x
  assert.equal(l('zzz'), undefined);
  assert.equal(l(null), undefined);
});

test('blockOf: tầng context, aggregate, instance', () => {
  const m = model(), l = lk();
  assert.equal(blockOf(S.context, m, l('order:1')), 'ctx:sales');
  assert.equal(blockOf(S.context, m, l('sales')), 'ctx:sales'); // area cũng theo context
  assert.equal(blockOf(S.context, m, l('free')), null); // class chưa xếp
  assert.equal(blockOf(S.agg, m, l('order:1')), 'agg:Order');
  assert.equal(blockOf(S.agg, m, l('orders')), 'agg:Order'); // collection List<Order>
  assert.equal(blockOf(S.agg, m, l('cart')), 'agg:Cart'); // service có khối riêng
  assert.equal(blockOf(S.agg, m, l('sales')), null); // area của chính context
  assert.equal(blockOf(S.agg, m, l('invoice:1')), 'port:billing');
  assert.equal(blockOf(S.agg, m, l('free')), null);
  assert.equal(blockOf(S.inst, m, l('cart')), 'cart');
  assert.equal(blockOf(S.inst, null, l('cart')), 'cart');
  assert.equal(blockOf(S.context, null, l('cart')), null);
});

test('buildContextBlocks: thứ tự theo mô hình, bỏ context rỗng, đếm instance, tóm tắt role, một cạnh cho mỗi cặp', () => {
  const { blocks, edges } = buildContextBlocks(model(), world());
  assert.deepEqual(blocks.map(b => b.id), ['ctx:sales', 'ctx:billing']);
  assert.deepEqual(blocks.map(b => b.count), [4, 1]); // order x2, line, cart
  assert.equal(blocks[0].title, 'Bán hàng');
  assert.equal(blocks[0].sub, '1 aggregate · 1 entity · 1 service'); // Oops ẩn nên không tính
  assert.deepEqual(blocks[0].lines, ['Order']);
  assert.equal(blocks[0].right, '4 instance');
  assert.equal(edges.length, 1);
  // Order→Invoice (observed), Cart→Invoice (param), Invoice→Order (param): sales phụ thuộc billing 2 lần, ngược lại 1 lần
  assert.deepEqual({ ...edges[0], tip: undefined }, { id: 'ctx:sales>ctx:billing', a: 'ctx:sales', b: 'ctx:billing', relations: 3, style: 'solid', calls: 0, events: 0, tip: undefined });
  assert.match(edges[0].tip, /Order → Invoice \(observed:s1\)/);
});

test('buildContextBlocks: chỉ có quan hệ observed → nét đứt; tên aggregate cắt còn 4 + "+n"; flow-only tạo cạnh theo đa số flow', () => {
  const m = model();
  m.relations = [{ from: 'Order', to: 'Invoice', source: 'observed:s1' }];
  for (const c of ['A', 'B', 'C', 'D', 'E']) m.classes.push({ cls: c, context: 'billing', role: 'aggregate', hidden: false });
  const r = buildContextBlocks(m, world());
  assert.equal(r.edges[0].style, 'observed');
  assert.deepEqual(r.blocks[1].lines, ['Invoice', 'A', 'B', 'C', '+2']);
  // không có quan hệ: Invoice gọi Order nhiều hơn → billing phụ thuộc sales
  m.relations = [];
  const counts = countFlows(S.context, m, lk(), [{ from: 'invoice:1', to: 'order:1' }, { from: 'invoice:1', to: 'order:2' }, { from: 'order:1', to: 'invoice:1' }]);
  const f = buildContextBlocks(m, world(), counts).edges;
  assert.equal(f.length, 1);
  assert.deepEqual([f[0].a, f[0].b, f[0].style, f[0].relations, f[0].calls], ['ctx:billing', 'ctx:sales', 'observed', 0, 3]);
});

test('edgeWidth: 1–3px theo số quan hệ', () => {
  assert.deepEqual([1, 2, 3, 4, 5, 9].map(edgeWidth), [1, 1, 2, 2, 3, 3]);
});

test('buildAggregateBlocks: đếm instance đúng, entity gộp vào aggregate sở hữu, class ẩn bị bỏ, service có khối riêng, cổng sang context khác', () => {
  const { blocks, ports } = buildAggregateBlocks(model(), world(), 'sales');
  // Order sở hữu Line qua field: order:1, order:2, line:1 đếm vào Order; collection không đếm
  assert.deepEqual(blocks.map(b => [b.id, b.role, b.count]), [['agg:Order', 'aggregate', 3], ['agg:Cart', 'service', 1]]);
  assert.deepEqual(ports.map(p => [p.id, p.port]), [['port:billing', true]]);
  assert.deepEqual(buildAggregateBlocks(model(), world(), 'billing').ports.map(p => p.id), ['port:sales']);
});

test('buildAggregateBlocks: khung aggregate có thành viên, cạnh theo id/param/observed, hướng theo phụ thuộc', () => {
  const { blocks, ports, edges } = buildAggregateBlocks(model(), world(), 'sales');
  const order = blocks.find(b => b.id === 'agg:Order');
  assert.equal(order.box, true);
  assert.deepEqual(order.members, [{ name: 'Line', tag: 'entity' }]);
  assert.equal(blocks.find(b => b.id === 'agg:Cart').box, false);
  assert.equal(ports[0].title, '→ Thanh toán');
  const byId = Object.fromEntries(edges.map(e => [e.id, e]));
  // Order→Line cùng khối (bỏ); Order→Invoice observed; Cart→Invoice param; Invoice→Order param
  assert.deepEqual(Object.keys(byId).sort(), ['agg:Cart>port:billing', 'agg:Order>port:billing']);
  assert.equal(byId['agg:Order>port:billing'].style, 'param'); // param mạnh hơn observed
  assert.equal(byId['agg:Cart>port:billing'].style, 'param');
  // field giữa hai aggregate là tham chiếu theo id
  const m = model();
  m.relations.push({ from: 'Cart', to: 'Order', source: 'field' });
  const e = buildAggregateBlocks(m, world(), 'sales').edges.find(x => x.id === 'agg:Cart>agg:Order');
  assert.equal(e.style, 'id');
});

test('buildAggregateBlocks: thành viên hiện tối đa 6 dòng + "+n"; flow tới context khác thêm cổng', () => {
  const m = model();
  for (let i = 0; i < 8; i++) { m.classes.push({ cls: `L${i}`, context: 'sales', role: 'entity', hidden: false }); m.relations.push({ from: 'Order', to: `L${i}`, source: 'field' }); }
  const o = buildAggregateBlocks(m, world(), 'sales').blocks[0];
  assert.equal(o.memberRows.length, 7);
  assert.deepEqual(o.memberRows.at(-1), { name: '+3', tag: '' }); // 9 thành viên - 6 dòng
  m.relations = [];
  const counts = countFlows(S.agg, m, lk(), [{ from: 'cart', to: 'invoice:1' }]);
  const r = buildAggregateBlocks(m, world(), 'sales', counts);
  assert.deepEqual(r.ports.map(p => p.id), ['port:billing']);
  assert.deepEqual(r.edges.map(e => [e.a, e.b, e.style]), [['agg:Cart', 'port:billing', 'observed']]);
});

test('buildAggregateBlocks: entity không có chủ thì có khối riêng', () => {
  const m = model();
  m.relations = m.relations.filter(r => r.source !== 'field');
  const { blocks } = buildAggregateBlocks(m, world(), 'sales');
  assert.deepEqual(blocks.map(b => [b.id, b.count]), [['agg:Order', 2], ['agg:Line', 1], ['agg:Cart', 1]]);
});

test('resolveFlowAt: khác context → mũi tên ctx:a → ctx:b', () => {
  const r = resolveFlowAt(S.context, model(), lk(), { from: 'order:1', to: 'invoice:1' });
  assert.deepEqual(r, { kind: 'arrow', from: 'ctx:sales', to: 'ctx:billing' });
});

test('resolveFlowAt: cùng context → nháy khối', () => {
  const r = resolveFlowAt(S.context, model(), lk(), { from: 'cart', to: 'order:1' });
  assert.deepEqual(r, { kind: 'flash', block: 'ctx:sales' });
});

test('resolveFlowAt: tầng aggregate, đầu mút ở context khác thành cổng; đầu không có khối → null', () => {
  const l = lk();
  assert.deepEqual(resolveFlowAt(S.agg, model(), l, { from: 'cart', to: 'invoice:1' }), { kind: 'arrow', from: 'agg:Cart', to: 'port:billing' });
  assert.deepEqual(resolveFlowAt(S.agg, model(), l, { from: 'order:1', to: 'order:2' }), { kind: 'flash', block: 'agg:Order' });
  assert.equal(resolveFlowAt(S.agg, model(), l, { from: 'sales', to: 'cart' }), null);
  assert.equal(resolveFlowAt(S.context, model(), l, { from: 'free', to: 'cart' }), null);
  assert.equal(resolveFlowAt(S.context, model(), l, { from: 'nope', to: 'cart' }), null);
});

test('resolveFlowAt: id con lạ đoán qua tiền tố', () => {
  const r = resolveFlowAt(S.context, model(), lk(), { from: 'inv:7', to: 'order:1' });
  assert.deepEqual(r, { kind: 'arrow', from: 'ctx:billing', to: 'ctx:sales' });
});

test('countFlows: gộp hai chiều, tách call và event, bỏ flow nháy', () => {
  const flows = [
    { from: 'order:1', to: 'invoice:1' }, { from: 'invoice:1', to: 'order:1', kind: 'EVENT' },
    { from: 'order:1', to: 'invoice:1', kind: 'CALL' }, { from: 'cart', to: 'order:1' },
  ];
  const c = countFlows(S.context, model(), lk(), flows);
  assert.equal(c.size, 1);
  assert.deepEqual([...c.values()][0], { a: 'ctx:billing', b: 'ctx:sales', calls: 2, events: 1, fwd: 1, back: 2 });
});

test('layoutGraph: upstream nằm trên, đường đi từ khối phụ thuộc tới upstream, có rect cho mọi khối', () => {
  const blocks = [{ id: 'a', w: 100, h: 50 }, { id: 'b', w: 100, h: 50 }];
  const g = layoutGraph(blocks, [{ id: 'a>b', a: 'a', b: 'b' }]); // a phụ thuộc b
  assert.ok(g.rects.b.y < g.rects.a.y);
  const pts = g.routes['a>b'];
  assert.equal(pts[0][1], g.rects.a.y); // bắt đầu ở đỉnh a
  assert.equal(pts.at(-1)[1], g.rects.b.y + g.rects.b.h); // kết thúc ở đáy b (mũi tên)
  assert.deepEqual(layoutGraph(blocks, [{ id: 'a>b', a: 'a', b: 'b' }]), g);
  assert.deepEqual(layoutGraph([], []).rects, {});
});

test('contextColor: dùng color của mô hình, không có thì bảng màu theo thứ tự', () => {
  const m = model();
  assert.equal(contextColor(m, 'billing'), 'var(--hub)');
  assert.equal(contextColor(m, 'sales'), 'var(--lv-1)');
  assert.equal(contextColor(m, 'empty'), 'var(--lv-3)');
});

// ---- Tầng instance có focus ----
const F = { ctx: { level: 'instance', ctx: 'sales', agg: null }, agg: { level: 'instance', ctx: 'sales', agg: 'Order' } };
const bigWorld = n => ({
  nodes: [
    ...world().nodes,
    ...Array.from({ length: n }, (_, i) => node(`o${i}`, 'object', 'Order', { parent: 'orders', title: `O${i}`, sub: 'sub', items: [{ key: 'k', value: i }] })),
  ],
});

test('buildInstanceBlocks: chỉ object trong focus, nhóm theo aggregate theo thứ tự mô hình', () => {
  const r = buildInstanceBlocks(model(), world(), F.ctx);
  // area, collection, context khác và class chưa xếp không có khối; Line gộp vào Order; Cart là nhóm riêng
  assert.deepEqual(r.blocks.map(b => [b.id, b.group]), [['order:1', 'Order'], ['order:2', 'Order'], ['line:1', 'Order'], ['cart', 'Cart']]);
  assert.deepEqual(Object.keys(r.blocks[0]).sort(), ['group', 'id', 'items', 'sub', 'title']);
  assert.deepEqual(r.ports, []);
  const onlyCart = buildInstanceBlocks(model(), world(), { level: 'instance', ctx: 'sales', agg: 'Cart' });
  assert.deepEqual(onlyCart.blocks.map(b => b.id), ['cart']);
  assert.deepEqual(buildInstanceBlocks(model(), world(), F.agg).blocks.map(b => b.group), ['Order', 'Order', 'Order']);
  assert.deepEqual(buildInstanceBlocks(null, world(), F.ctx).blocks, []);
});

test('buildInstanceBlocks: quá limit chỉ giữ node đã chạm kèm khối "+N chưa chạm"; đủ limit thì hiện hết', () => {
  const w = bigWorld(10); // Order: order:1, order:2, line:1 + 10 = 13 instance
  const r = buildInstanceBlocks(model(), w, F.agg, new Set(['o3', 'order:2']), { limit: 8 });
  assert.deepEqual(r.blocks.map(b => b.id), ['order:2', 'o3', 'more:Order']);
  const more = r.blocks.at(-1);
  assert.equal(more.title, '+11 chưa chạm');
  assert.equal(more.summary, true);
  assert.equal(r.hiddenOf.get('o0'), 'more:Order');
  assert.equal(r.hiddenOf.has('o3'), false);
  // tất cả đã chạm → không cần khối +N
  const all = new Set(w.nodes.map(n => n.id));
  assert.equal(buildInstanceBlocks(model(), w, F.agg, all, { limit: 8 }).blocks.some(b => b.summary), false);
  // không quá limit → hiện hết dù chưa chạm
  assert.equal(buildInstanceBlocks(model(), w, F.agg, new Set(), { limit: 13 }).blocks.length, 13);
  assert.equal(buildInstanceBlocks(model(), w, F.agg, new Set()).blocks.length, 1); // mặc định limit 8: chỉ còn khối +N
});

test('buildInstanceBlocks: cổng chỉ cho đầu mút đã chạm nằm ngoài focus', () => {
  const l = lk(), ep = ids => ids.map(id => l(id));
  assert.deepEqual(buildInstanceBlocks(model(), world(), F.ctx, new Set(), { endpoints: ep(['order:1', 'cart']) }).ports, []);
  const ctxPorts = buildInstanceBlocks(model(), world(), F.ctx, new Set(), { endpoints: ep(['invoice:1', 'order:1']) }).ports;
  assert.deepEqual(ctxPorts.map(p => [p.id, p.port, p.ctx]), [['port:billing', true, 'billing']]);
  // focus aggregate: Cart cùng context nhưng khác aggregate → cổng aggregate; Invoice → cổng context
  const aggPorts = buildInstanceBlocks(model(), world(), F.agg, new Set(), { endpoints: ep(['cart', 'invoice:1', 'order:1', 'free', 'sales']) }).ports;
  assert.deepEqual(aggPorts.map(p => p.id), ['port:billing', 'port:agg:Cart']);
  assert.equal(aggPorts[1].agg, 'Cart');
});

test('resolveFlowAt tầng instance có focus: trong→trong, trong→cổng, đầu bị gộp→khối +N', () => {
  const l = lk(), m = model();
  assert.deepEqual(resolveFlowAt(F.ctx, m, l, { from: 'order:1', to: 'cart' }), { kind: 'arrow', from: 'order:1', to: 'cart' });
  assert.deepEqual(resolveFlowAt(F.ctx, m, l, { from: 'order:1', to: 'order:1' }), { kind: 'flash', block: 'order:1' });
  assert.deepEqual(resolveFlowAt(F.ctx, m, l, { from: 'order:1', to: 'invoice:1' }), { kind: 'arrow', from: 'order:1', to: 'port:billing' });
  assert.deepEqual(resolveFlowAt(F.agg, m, l, { from: 'cart', to: 'order:1' }), { kind: 'arrow', from: 'port:agg:Cart', to: 'order:1' });
  assert.equal(resolveFlowAt(F.ctx, m, l, { from: 'orders', to: 'cart' }), null); // collection không có khối
  assert.equal(resolveFlowAt(F.ctx, m, l, { from: 'free', to: 'cart' }), null);
  // đầu bị gộp: o0 và o1 cùng khối +N → nháy; o0 → đầu đã chạm → mũi tên
  const w = bigWorld(10), inst = buildInstanceBlocks(m, w, F.agg, new Set(['o3']), { limit: 8 });
  const lw = nodeLookup(null, w, null);
  assert.deepEqual(resolveFlowAt(F.agg, m, lw, { from: 'o0', to: 'o1' }, inst), { kind: 'flash', block: 'more:Order' });
  assert.deepEqual(resolveFlowAt(F.agg, m, lw, { from: 'o0', to: 'o3' }, inst), { kind: 'arrow', from: 'more:Order', to: 'o3' });
  // không có focus: vẫn là chính node id như trước
  assert.equal(blockOf(S.inst, m, l('cart')), 'cart');
  assert.equal(hasFocus(F.ctx), true);
  assert.equal(hasFocus(S.inst), false);
});

test('itemRows: cắt còn 6 dòng kèm "+n"; instanceLayout xác định, nhóm có nhãn, cổng bên phải', () => {
  assert.deepEqual(itemRows([{ key: 'a', value: 1 }, { key: 'b', label: 'B', value: 'x' }]), [{ key: 'a', value: '1' }, { key: 'B', value: 'x' }]);
  const many = Array.from({ length: 9 }, (_, i) => ({ key: `k${i}`, value: i }));
  const rows = itemRows(many);
  assert.equal(rows.length, 7);
  assert.deepEqual(rows.at(-1), { more: 3 });
  assert.deepEqual(itemRows(null), []);
  const blocks = [
    { id: 'a', group: 'G1', items: [] }, { id: 'b', group: 'G1', items: many }, { id: 'c', group: 'G2', items: [] },
    { id: 'm', group: 'G2', items: null, summary: true }, { id: 'p', port: true },
  ];
  const g = instanceLayout(blocks, { cols: 2, w: 100, gap: 10, margin: 5 });
  assert.deepEqual(g.labels.map(l => l.group), ['G1', 'G2']);
  assert.equal(g.rects.a.h, g.rects.b.h); // cùng hàng → cùng chiều cao (khối cao nhất)
  assert.ok(g.rects.c.y > g.rects.a.y + g.rects.a.h); // nhóm sau nằm dưới nhóm trước
  assert.ok(g.rects.p.x > g.rects.b.x + 100);
  assert.deepEqual(instanceLayout(blocks, { cols: 2, w: 100, gap: 10, margin: 5 }), g);
  assert.deepEqual(instanceLayout([]).rects, {});
});

test('touchedFirst: stable partition, pred null giữ nguyên, không đổi mảng gốc', () => {
  const xs = [1, 2, 3, 4, 5, 6];
  assert.deepEqual(touchedFirst(xs, x => x % 3 === 0), [3, 6, 1, 2, 4, 5]);
  assert.deepEqual(touchedFirst(xs, null), xs);
  assert.deepEqual(touchedFirst(xs, () => false), xs);
  assert.deepEqual(touchedFirst(xs, () => true), xs);
  assert.deepEqual(xs, [1, 2, 3, 4, 5, 6]);
});
