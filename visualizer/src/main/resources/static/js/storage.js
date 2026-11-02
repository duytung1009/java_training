// localStorage bọc try/catch: không có storage (bị chặn, chế độ riêng tư) thì app vẫn chạy
export function readJson(key) {
  try { return JSON.parse(localStorage.getItem(key)); } catch { return null; }
}
export function writeJson(key, v) {
  try { localStorage.setItem(key, JSON.stringify(v)); } catch { /* không lưu được: bỏ qua */ }
}
export function removeKey(key) {
  try { localStorage.removeItem(key); } catch { /* bỏ qua */ }
}
