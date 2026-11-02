// Helper animation dùng chung (camera, phép chiếu, đếm số, chuyển cảnh)
export const reduced = () => matchMedia('(prefers-reduced-motion: reduce)').matches;
export const easeOut = t => 1 - (1 - t) ** 3; // ease-out cubic

// Chạy requestAnimationFrame trong ms: onFrame(t) với t ∈ [0, 1] (khung đầu gọi ngay với t = 0), xong thì onDone().
// Trả cancel(): dừng giữa chừng, không gọi onDone nữa
export function tween(ms, onFrame, onDone) {
  let on = true;
  const start = performance.now();
  (function frame(now) {
    if (!on) return;
    const t = Math.min(1, (now - start) / ms);
    onFrame(t);
    if (!on) return; // onFrame huỷ chính nó
    if (t < 1) requestAnimationFrame(frame); else { on = false; onDone?.(); }
  })(start);
  return () => { on = false; };
}
