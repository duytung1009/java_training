// Màn "?": tóm tắt hành trình phát triển visualizer (ý tưởng → các lần sửa / refactor → sản phẩm hiện tại)
import { h } from '../dom.js';
import { adapter } from '../../adapter/index.js';

const { INTRO, STAGES, PRODUCT, LATER, LESSONS } = adapter.about;

// STAGES: mỗi chặng gồm tiêu đề, commit (nếu có), vấn đề / ý tưởng dẫn tới chặng đó, và thay đổi chính

function list(items) {
  const ul = h('ul');
  for (const t of items) ul.appendChild(h('li', '', t));
  return ul;
}

function section(title, body) {
  const s = h('section', 'about-sec');
  s.append(h('h3', '', title), body);
  return s;
}

// Gắn nút "?" mở dialog; nội dung dựng một lần
export function initAbout(button, dialog) {
  const body = dialog.querySelector('.about-body');
  body.append(h('p', 'about-intro', INTRO));

  const tl = h('ol', 'timeline');
  STAGES.forEach((st, i) => {
    const li = h('li');
    const head = h('div', 'st-head');
    head.append(h('span', 'st-no', String(i + 1).padStart(2, '0')), h('span', 'st-title', st.title));
    if (st.commit) head.append(h('code', 'st-commit', st.commit));
    li.append(head, h('p', 'st-why', st.why), list(st.what));
    tl.appendChild(li);
  });
  body.append(section('Hành trình', tl), section('Sản phẩm hiện tại', list(PRODUCT)),
    section('Để sau', list(LATER)), section('Bài học', list(LESSONS)));

  button.addEventListener('click', () => dialog.showModal());
  dialog.querySelector('.about-close').addEventListener('click', () => dialog.close());
  // Click nền ngoài khung cũng đóng
  dialog.addEventListener('click', e => { if (e.target === dialog) dialog.close(); });
}
