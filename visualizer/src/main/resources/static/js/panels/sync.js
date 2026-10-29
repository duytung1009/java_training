// Banner cảnh báo khi API domain lệch so với snapshot lần sync cuối (dựng bằng textContent, không innerHTML)
import { h } from '../dom.js';

const MAX_LINES = 50;

function addLines(box, items, sign, cls) {
  items.slice(0, MAX_LINES).forEach(s => box.append(h('div', cls, `${sign} ${s}`)));
  if (items.length > MAX_LINES) box.append(h('div', 'more', `… và ${items.length - MAX_LINES} dòng nữa`));
}

export function renderSync(el, sync) {
  el.replaceChildren();
  if (!sync || sync.inSync) { el.hidden = true; return; }
  const head = sync.message
    ?? `Visualizer chưa đồng bộ với domain: +${sync.added.length} / −${sync.removed.length} chữ ký API thay đổi so với lần sync cuối.`;
  const details = h('details');
  details.append(h('summary', null, 'Chi tiết'));
  addLines(details, sync.added, '+', 'add');
  addLines(details, sync.removed, '−', 'del');
  el.append(h('strong', null, `⚠ ${head}`), ' Chạy skill visualizer-sync để cập nhật.', details);
  el.hidden = false;
}
