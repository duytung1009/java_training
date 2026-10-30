import test from 'node:test';
import assert from 'node:assert/strict';
import {
  parseHash, toHash, normalizeState, nodeLookup, blockOf, buildContextBlocks, buildAggregateBlocks,
  resolveFlowAt, countFlows, gridLayout, contextColor,
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

test('buildContextBlocks: thứ tự theo mô hình, bỏ context rỗng, đếm instance, cạnh gộp theo cặp', () => {
  const { blocks, edges } = buildContextBlocks(model(), world());
  assert.deepEqual(blocks.map(b => b.id), ['ctx:sales', 'ctx:billing']);
  assert.deepEqual(blocks.map(b => b.count), [4, 1]); // order x2, line, cart
  assert.equal(blocks[0].title, 'Bán hàng');
  assert.equal(edges.length, 1);
  // Order→Invoice, Cart→Invoice, Invoice→Order: cùng một cặp không phân biệt hướng
  assert.deepEqual(edges[0], { a: 'ctx:billing', b: 'ctx:sales', relations: 3 });
});

test('buildAggregateBlocks: đếm instance đúng, entity gộp vào aggregate sở hữu, class ẩn bị bỏ, service có khối riêng, cổng sang context khác', () => {
  const { blocks, ports } = buildAggregateBlocks(model(), world(), 'sales');
  // Order sở hữu Line qua field: order:1, order:2, line:1 đếm vào Order; collection không đếm
  assert.deepEqual(blocks.map(b => [b.id, b.role, b.count]), [['agg:Order', 'aggregate', 3], ['agg:Cart', 'service', 1]]);
  assert.deepEqual(ports.map(p => [p.id, p.port]), [['port:billing', true]]);
  assert.deepEqual(buildAggregateBlocks(model(), world(), 'billing').ports.map(p => p.id), ['port:sales']);
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
  assert.deepEqual([...c.values()][0], { a: 'ctx:billing', b: 'ctx:sales', calls: 2, events: 1 });
});

test('gridLayout: xác định, cổng ở cột bên phải', () => {
  const blocks = [{ id: 'a' }, { id: 'b' }, { id: 'c' }, { id: 'p', port: true }];
  const g = gridLayout(blocks, { cols: 2, w: 100, h: 50, gap: 10, margin: 5 });
  assert.deepEqual(g.rects.a, { x: 5, y: 5, w: 100, h: 50 });
  assert.deepEqual(g.rects.b, { x: 115, y: 5, w: 100, h: 50 });
  assert.deepEqual(g.rects.c, { x: 5, y: 65, w: 100, h: 50 });
  assert.ok(g.rects.p.x > g.rects.b.x + 100);
  assert.equal(g.width, g.rects.p.x + 100 + 5);
  assert.equal(g.height, 5 * 2 + 110);
  assert.deepEqual(gridLayout(blocks, { cols: 2, w: 100, h: 50, gap: 10, margin: 5 }), g);
  assert.deepEqual(gridLayout([], {}).rects, {});
});

test('contextColor: dùng color của mô hình, không có thì bảng màu theo thứ tự', () => {
  const m = model();
  assert.equal(contextColor(m, 'billing'), 'var(--hub)');
  assert.equal(contextColor(m, 'sales'), 'var(--lv-1)');
  assert.equal(contextColor(m, 'empty'), 'var(--lv-3)');
});
