// Call log: một dòng mỗi event đã chạy
import { h } from '../dom.js';

// pool-1-thread-2 → p1-t2, http-nio-8081-exec-3 → http-3; tên lạ thì cắt, tên đầy đủ nằm ở title
export function shortThread(name) {
  const m = /^pool-(\d+)-thread-(\d+)$/.exec(name);
  if (m) return `p${m[1]}-t${m[2]}`;
  const http = /^http-nio-\d+-exec-(\d+)$/.exec(name);
  if (http) return `http-${http[1]}`;
  return name.length > 7 ? name.slice(0, 6) + '…' : name;
}

// Màu thread theo thứ tự xuất hiện đầu tiên (không dùng đỏ: dành cho exception)
const THREAD_COLORS = ['#6f9cf0', '#e0894f', '#74c48a', '#a78bfa', '#4fb8b0', '#e8c46a', '#e88fb4', '#9ab4ff'];

export function renderLog(ol, events, selected, onSelect) {
  ol.textContent = '';
  // Chỉ tô màu khi có từ 2 thread trở lên; một thread thì giữ màu xám như cũ
  const threads = [...new Set(events.map(e => e.thread))];
  const colorOf = t => threads.length > 1 ? THREAD_COLORS[threads.indexOf(t) % THREAD_COLORS.length] : null;
  events.forEach((e, i) => {
    const thrown = e.outcome === 'THROWN';
    const li = h('li', (thrown ? 'thrown ' : '') + (i === selected ? 'sel' : ''));
    const m = h('span', 'm');
    m.append(`${e.className}.`, h('b', '', e.method), `(${(e.args ?? []).join(', ')})`);
    if (thrown) {
      m.append(document.createElement('br'), `✗ ${e.exception?.type}: ${e.exception?.message}`);
    } else if (e.result) m.append(` → ${e.result}`);
    const th = h('span', 'th', shortThread(e.thread));
    th.title = e.thread;
    const c = colorOf(e.thread);
    if (c) { li.style.setProperty('--tc', c); li.classList.add('mt'); }
    li.append(h('span', 'ts', (e.at ?? '').slice(11, 23)), th, m, h('span', thrown ? 'err' : 'ok', thrown ? '✗' : '✓'));
    li.onclick = () => onSelect(i);
    ol.appendChild(li);
  });
  ol.scrollTop = ol.scrollHeight;
}
