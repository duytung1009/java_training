// Bảng chú thích khi click một khối trên bản đồ: class domain, vai trò, API chính, trạng thái hiện tại
import { ROOTS, PLACED } from '../map/layout.js';
import { adapter } from '../../adapter/index.js';
import { classOfNode, contextOfNode, lookup, aggregateOf } from '../model/draft.js';

const KIND = { object: 'object', collection: 'collection', area: 'area (package)' };

const esc = s => String(s).replace(/[&<>"]/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' })[c]);

// Dòng "context · role" theo mô hình hiện hành (nháp hoặc đã lưu); không có mô hình thì không hiện
function modelLine(model, node) {
  if (!model) return '';
  const ctxId = contextOfNode(model, node);
  const def = lookup(model, classOfNode(node));
  const ctx = model.contexts.find(c => c.id === ctxId);
  const parts = [ctx ? ctx.title : 'chưa xếp'];
  if (def) {
    parts.push(def.role + (def.hidden ? ' (ẩn)' : ''));
    const agg = aggregateOf(model, def.cls);
    if (agg && agg !== def.cls) parts.push(`aggregate ${agg}`);
  }
  return `<div class="mdl">${parts.map(esc).join(' · ')}</div>`;
}

// Vẽ bảng cho node (id, kind, cls, title, sub, parent, items); node null thì ẩn bảng.
// model (tuỳ chọn): mô hình domain hiện hành để thêm dòng context · role
export function renderInfo(el, id, node, model = null) {
  const info = node && (adapter.infoOf(node));
  if (!id || !node || !info) { el.hidden = true; return; }
  const top = (PLACED[id] ?? ROOTS[id]) ? id : node.parent; // con: lấy màu container
  const root = ROOTS[top] ?? PLACED[top];
  const items = node.items ?? [];
  const kids = node.children ?? [];
  el.style.setProperty('--c', `var(${root?.color ?? '--c-log'})`);
  const row = it => `<li><span>${esc(it.computed ? '(tính ra) ' : '')}${esc(it.label ?? it.key)}</span><b>${esc(it.computed && !String(it.label ?? '').startsWith('=') ? '= ' : '')}${esc(it.value)}</b></li>`;
  el.innerHTML = `
    <button class="close" aria-label="Đóng">×</button>
    <div class="t">${esc(node.title)} <span class="kind">${esc(KIND[node.kind] ?? node.kind)}</span></div>
    <div class="cls">${esc(info.cls)} <span>· ${esc(info.pkg)} · ${esc(info.session)}</span></div>
    ${modelLine(model, node)}
    <p>${esc(info.desc)}</p>
    <div class="h">API chính</div>
    <ul>${info.api.map(a => `<li>${esc(a)}</li>`).join('')}</ul>
    <div class="h">${node.kind === 'object' ? `Trạng thái · ${esc(info.items)}${items.length ? ` · ${items.length}` : ''}` : `Khối con · ${kids.length}`}</div>
    ${node.kind === 'object'
      ? (items.length ? `<ul class="kv">${items.map(row).join('')}</ul>` : '<div class="empty">(trống)</div>')
      : (kids.length ? `<ul>${kids.map(k => `<li>${esc(k.title)}${k.sub ? ` · ${esc(k.sub)}` : ''}</li>`).join('')}</ul>` : '<div class="empty">(chưa có khối con)</div>')}`;
  el.hidden = false;
}
