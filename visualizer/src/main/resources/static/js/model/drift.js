// So mô hình đang sửa với gợi ý hiện tại (gợi ý = danh sách class của domain bây giờ). Hàm thuần.

// added: ClassDef của gợi ý mà mô hình chưa có; removed: tên class mô hình có mà gợi ý không còn
export function diffModel(model, suggestion) {
  const have = new Set(model.classes.map(c => c.cls));
  const now = new Set(suggestion.classes.map(c => c.cls));
  return {
    added: suggestion.classes.filter(c => !have.has(c.cls)),
    removed: model.classes.filter(c => !now.has(c.cls)).map(c => c.cls),
  };
}

// Đưa các class trong clsList vào mô hình theo định nghĩa gợi ý; thêm context còn thiếu và
// quan hệ của gợi ý nối class vừa thêm với class đã có trong mô hình
export function adoptAdded(model, suggestion, clsList) {
  const want = new Set(clsList);
  const defs = suggestion.classes.filter(c => want.has(c.cls) && !model.classes.some(x => x.cls === c.cls));
  if (!defs.length) return model;
  const adopted = new Set(defs.map(d => d.cls));
  const known = new Set([...model.classes.map(c => c.cls), ...adopted]);
  const contexts = [...model.contexts];
  for (const d of defs) {
    if (d.context && !contexts.some(c => c.id === d.context)) {
      const ctx = suggestion.contexts.find(c => c.id === d.context);
      if (ctx) contexts.push({ ...ctx });
    }
  }
  const relations = [...model.relations];
  for (const r of suggestion.relations ?? []) {
    if ((adopted.has(r.from) || adopted.has(r.to)) && known.has(r.from) && known.has(r.to)
        && !relations.some(x => x.from === r.from && x.to === r.to)) relations.push({ ...r });
  }
  return { ...model, contexts, classes: [...model.classes, ...defs.map(d => ({ ...d }))], relations };
}
