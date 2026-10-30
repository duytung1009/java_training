// Bảng chú thích khi click một khối trên bản đồ: class domain, vai trò, API chính, trạng thái hiện tại
import { ROOTS, PLACED } from '../map/layout.js';
import { adapter } from '../../adapter/index.js';

const KIND = { object: 'object', collection: 'collection', area: 'area (package)' };

const esc = s => String(s).replace(/[&<>"]/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' })[c]);

// Vẽ bảng cho node (id, kind, cls, title, sub, parent, items); node null thì ẩn bảng
export function renderInfo(el, id, node) {
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
    <p>${esc(info.desc)}</p>
    <div class="h">API chính</div>
    <ul>${info.api.map(a => `<li>${esc(a)}</li>`).join('')}</ul>
    <div class="h">${node.kind === 'object' ? `Trạng thái · ${esc(info.items)}${items.length ? ` · ${items.length}` : ''}` : `Khối con · ${kids.length}`}</div>
    ${node.kind === 'object'
      ? (items.length ? `<ul class="kv">${items.map(row).join('')}</ul>` : '<div class="empty">(trống)</div>')
      : (kids.length ? `<ul>${kids.map(k => `<li>${esc(k.title)}${k.sub ? ` · ${esc(k.sub)}` : ''}</li>`).join('')}</ul>` : '<div class="empty">(chưa có khối con)</div>')}`;
  el.hidden = false;
}
