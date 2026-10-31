// Chuyển cảnh giữa hai <svg> của panel bản đồ (scene ↔ node-map): cross-fade + một khối "ma" (div.ghost) bay từ khung nguồn tới khung đích.
// Không biết domain: nhận hai phần tử svg và hai hình chữ nhật màn hình.
import { reduced } from '../anim.js';
let cleanup = null; // kết thúc ngay lượt đang chạy (lượt mới / điều hướng dồn dập)

// out: svg đang hiện; inn: svg sắp hiện (đang lv-off).
// from: khung nguồn (px viewport) hoặc null; to(): khung đích, gọi sau khi inn đã hiện (đo được); thiếu một trong hai thì chỉ fade.
export function swapViews({ out, inn, from = null, to = () => null, color = 'var(--c-map)', ms = 350 }) {
  cleanup?.();
  const box = inn.parentElement;
  inn.classList.remove('lv-off');
  if (reduced()) { out.classList.add('lv-off'); return; }
  out.classList.add('lv-under');
  const fade = (node, keys) => node.animate(keys, { duration: ms, easing: 'ease-out', fill: 'forwards' });
  const anims = [fade(out, [{ opacity: 1 }, { opacity: 0 }]), fade(inn, [{ opacity: 0 }, { opacity: 1 }])];
  const dest = from && to();
  let ghost = null;
  if (dest) {
    ghost = document.createElement('div');
    ghost.className = 'ghost';
    ghost.style.borderColor = color;
    box.append(ghost);
    const br = box.getBoundingClientRect();
    const at = r => ({ left: `${r.left - br.left + box.scrollLeft}px`, top: `${r.top - br.top + box.scrollTop}px`, width: `${r.width}px`, height: `${r.height}px` });
    anims.push(ghost.animate([{ ...at(from), opacity: 1 }, { ...at(dest), opacity: 1, offset: 0.85 }, { ...at(dest), opacity: 0 }], { duration: ms, easing: 'cubic-bezier(.3, .7, .3, 1)', fill: 'forwards' }));
  }
  let done = false;
  const finish = () => {
    if (done) return;
    done = true; cleanup = null;
    out.classList.add('lv-off'); out.classList.remove('lv-under');
    for (const a of anims) a.cancel();
    ghost?.remove();
  };
  cleanup = finish;
  Promise.all(anims.map(a => a.finished.catch(() => {}))).then(finish);
}
