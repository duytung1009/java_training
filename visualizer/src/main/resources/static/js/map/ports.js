// Hình học "cổng" ở tầng package (hàm thuần, không DOM): cắt đường nối tại chỗ rời area và xếp khối cổng ngoài mép
const EPS = 1e-6;

const within = ([x, y], r) => x >= r.x - EPS && x <= r.x + r.w + EPS && y >= r.y - EPS && y <= r.y + r.d + EPS;
const near = (a, b) => Math.abs(a - b) < EPS;
const same = (p, q) => Math.abs(p[0] - q[0]) < EPS && Math.abs(p[1] - q[1]) < EPS;

// Mép area mà đoạn p0 → p1 (ngang/dọc) chạm khi đi theo chiều đó, hoặc null. Điểm nằm đúng mép vẫn tính là "trong"
function edgeHit([x0, y0], [x1, y1], r) {
  if (near(y0, y1)) return x1 > x0 ? (x1 >= r.x + r.w - EPS ? 'right' : null) : (x1 <= r.x + EPS ? 'left' : null);
  return y1 > y0 ? (y1 >= r.y + r.d - EPS ? 'bottom' : null) : (y1 <= r.y + EPS ? 'top' : null);
}

// Đi dọc đường (đoạn ngang/dọc) từ đầu trong của area `rect` ({x, y, w, d}); fromStart=false: đầu trong là điểm cuối.
// Trả { inner: điểm từ đầu trong tới chỗ cắt, cross: [x, y], side } ở lần rời area đầu tiên, hoặc null nếu đường không rời area.
// Đầu mút nằm đúng mép: điểm trên mép vẫn là trong; đường kết thúc đúng trên mép (theo chiều đi) cũng tính là cắt tại đó
export function clipWire(pts, rect, fromStart = true) {
  const p = fromStart ? pts : pts.slice().reverse();
  if (p.length < 2 || !within(p[0], rect)) return null;
  const inner = [p[0]];
  for (let i = 1; i < p.length; i++) {
    const [a, b] = [p[i - 1], p[i]], out = !within(b, rect);
    if (out || i === p.length - 1) {
      const side = edgeHit(a, b, rect);
      if (side) {
        const cross = side === 'left' || side === 'right' ? [side === 'left' ? rect.x : rect.x + rect.w, a[1]] : [a[0], side === 'top' ? rect.y : rect.y + rect.d];
        if (!same(inner.at(-1), cross)) inner.push(cross);
        return { inner, cross, side };
      }
      if (out) return null; // lệch trục (không phải đường ngang/dọc): bỏ
    }
    inner.push(b);
  }
  return null;
}

// Xếp khối cổng {w × d} ngoài mép rect cho từng điểm cắt [{ cross, side }] (kết quả cùng thứ tự đầu vào).
// Top/bottom: tâm theo x, cách mép `gap`; left/right: cạnh gần cách mép `gap`, tâm theo y. Cùng mép thì sắp dọc mép và đẩy khối sau ra (cách nhau `space`).
// Mỗi kết quả: { x, y, w, d, side, stub } — stub: đường ngắn trực giao từ chỗ cắt tới cạnh gần của khối
export function placePorts(crossings, rect, { w = 2.6, d = 0.8, gap = 0.15, space = 0.1 } = {}) {
  const out = new Array(crossings.length);
  for (const side of ['top', 'bottom', 'left', 'right']) {
    const horiz = side === 'top' || side === 'bottom', size = horiz ? w : d, k = horiz ? 0 : 1;
    let end = -Infinity;
    crossings.map((c, i) => [c, i]).filter(([c]) => c.side === side)
      .sort((p, q) => p[0].cross[k] - q[0].cross[k] || p[1] - q[1])
      .forEach(([{ cross }, i]) => {
        const s = Math.max(cross[k] - size / 2, end + space);
        end = s + size;
        const clamp = v => Math.min(Math.max(v, s), s + size);
        let r, stub;
        if (horiz) {
          const y = side === 'top' ? rect.y - gap - d : rect.y + rect.d + gap, ny = side === 'top' ? rect.y - gap : rect.y + rect.d + gap;
          r = { x: s, y, w, d }; stub = [cross, [cross[0], ny], [clamp(cross[0]), ny]];
        } else {
          const x = side === 'left' ? rect.x - gap - w : rect.x + rect.w + gap, nx = side === 'left' ? rect.x - gap : rect.x + rect.w + gap;
          r = { x, y: s, w, d }; stub = [cross, [nx, cross[1]], [nx, clamp(cross[1])]];
        }
        out[i] = { ...r, side, stub: stub.filter((q, j) => !j || !same(q, stub[j - 1])) };
      });
  }
  return out;
}
