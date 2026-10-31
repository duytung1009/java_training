// Hiển thị item { key, label?, value, computed? } của node; dùng chung cho bản đồ, bảng chú thích và thẻ instance
// Giá trị tính ra có tiền tố "= " trừ khi nhãn đã tự bắt đầu bằng "="
export const valPrefix = it => (it.computed && !String(it.label ?? '').startsWith('=') ? '= ' : '');
export const dispVal = it => valPrefix(it) + String(it.value);
// Nhãn item: label nếu có, không thì key (mỗi nơi gọi tự quyết khi nào dùng nhãn: router chỉ dùng cho item tính ra, info thêm "(tính ra)")
export const itemLabel = it => it.label ?? it.key;
