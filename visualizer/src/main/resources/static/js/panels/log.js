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

export function renderLog(ol, events, selected, onSelect) {
  ol.textContent = '';
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
    li.append(h('span', 'ts', (e.at ?? '').slice(11, 23)), th, m, h('span', thrown ? 'err' : 'ok', thrown ? '✗' : '✓'));
    li.onclick = () => onSelect(i);
    ol.appendChild(li);
  });
  ol.scrollTop = ol.scrollHeight;
}
