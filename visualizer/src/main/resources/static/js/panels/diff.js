// Panel state changes: before → after cho event được chọn
import { h } from '../dom.js';

const NOTE_CONCURRENT = 'Kịch bản đa luồng: before/after có thể gồm thay đổi của thread khác.';

function fieldRow(k, before, after) {
  const row = h('div', 'f');
  row.append(h('span', '', k));
  let cls, txt;
  if (before === null || !(k in before)) { cls = 'new'; txt = `+ ${after[k]}`; }
  else if (after === null || !(k in after)) { cls = 'gone'; txt = `${before[k]} → (xoá)`; }
  else {
    const b = before[k], a = after[k];
    cls = b === a ? 'same' : typeof a === 'number' && typeof b === 'number' ? (a > b ? 'up' : 'down') : 'new';
    txt = `${b} → ${a}`;
  }
  row.append(h('span', cls, txt));
  return row;
}

function changeBlock(c) {
  const box = h('div', 'obj');
  const head = h('div', '', c.ref);
  if (c.before === null) head.append(' ', h('span', 'new', '(mới tạo)'));
  if (c.after === null) head.append(' ', h('span', 'gone', '(đã xoá)'));
  box.append(head);
  const keys = [...new Set([...Object.keys(c.before ?? {}), ...Object.keys(c.after ?? {})])];
  for (const k of keys) box.append(fieldRow(k, c.before, c.after));
  return box;
}

export function renderDiff(box, event, run) {
  box.textContent = '';
  if (run?.concurrent) box.append(h('div', 'note', NOTE_CONCURRENT));
  if (!event) { box.append(h('span', 'note', 'Chọn một dòng log hoặc bấm Run.')); return; }
  if (event.outcome === 'THROWN') {
    box.append(h('div', 'note err', `✗ ${event.exception?.type}: ${event.exception?.message}`));
    if (!event.changes?.length) box.append(h('div', 'note', 'Không object nào đổi — all-or-nothing.'));
  }
  if (!event.changes?.length) {
    if (event.outcome !== 'THROWN') {
      box.append(h('div', 'note', 'Không đổi state (hàm thuần)'));
      if (event.result) box.append(h('div', 'note', `Kết quả: ${event.result}`));
    }
    return;
  }
  for (const c of event.changes) box.append(changeBlock(c));
}
