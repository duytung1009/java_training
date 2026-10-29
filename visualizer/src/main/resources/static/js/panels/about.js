// Màn "?": tóm tắt hành trình phát triển visualizer (ý tưởng → các lần sửa / refactor → sản phẩm hiện tại)
import { h } from '../dom.js';

const INTRO = 'Visualizer là tài liệu bổ sung cho S07: một trang chạy local để nhìn thấy module domain/ hoạt động. '
  + 'Nó được làm theo vòng lặp ngắn: Tùng xem trên trình duyệt và góp ý, Claude sửa rồi kiểm tra lại bằng test và chạy thử thật.';

// Mỗi chặng: tiêu đề, commit (nếu có), vấn đề / ý tưởng dẫn tới chặng đó, và thay đổi chính
const STAGES = [
  {
    title: 'Ý tưởng và lên kế hoạch',
    why: 'Học viên khó hình dung chuyện gì xảy ra bên trong domain khi gọi một method: object nào đổi, dữ liệu chảy đi đâu, exception làm gì.',
    what: [
      'Plan doc + mockup: bản đồ 2.5D bên trái, call log và diff trạng thái bên phải.',
      'Chốt nguyên tắc: không sửa domain/, ghi log bằng wrapper tường minh TraceRecorder, snapshot viết tay, FE là file tĩnh, bố cục bản đồ thuộc FE.',
      'Gate 1: duyệt design trước khi viết code.',
    ],
  },
  {
    title: 'Bản đầu tiên (P0–P3)', commit: '99e076a',
    why: 'Biến plan thành sản phẩm chạy được, gắn với các buổi S02–S06.',
    what: [
      'Module visualizer/ (Spring Boot, port 8081) chỉ phụ thuộc domain.',
      '8 kịch bản, Run / Pause / Step / Reset, diff before → after, ô có chữ nghiêng theo mặt iso.',
    ],
  },
  {
    title: 'Làm sạch bản đồ', commit: 'b10f463',
    why: 'Một đường nối xám cắt xuyên khối Pricing; chữ chưa nằm phẳng theo phép chiếu.',
    what: [
      'Đường nối chỉ gồm đoạn ngang/dọc trên lưới, checkLayout() cảnh báo khi đường cắt khối hoặc khối chồng nhau.',
      'Chữ ghi chú và tiêu đề vẽ phẳng theo phép chiếu iso; click khối mở bảng chú thích.',
    ],
  },
  {
    title: 'Đồng bộ với domain', commit: '27d8cde',
    why: 'Câu hỏi: sau này đổi domain thì làm sao agent biết phải cập nhật visualizer? Một test chặn build thì quá cứng.',
    what: [
      'Snapshot chữ ký public API (domain-api.txt) + banner cảnh báo khi lệch, không làm đỏ build.',
      'Skill visualizer-sync và một dòng trong CLAUDE.md hướng dẫn agent cập nhật.',
      'Một bước phát được nhiều flow, ô "+N" khi card tràn, bản đồ cuộn/kéo được.',
      'Phát hiện: Cart không trừ kho (đúng domain) → thêm bước decreaseStock để thấy kho giảm lúc chốt đơn.',
    ],
  },
  {
    title: 'Vẽ cho đúng sự thật', commit: '8797a93',
    why: 'Mũi tên Order → ShippingZone không tồn tại trong domain; nhãn class trên ô cho thấy nhiều ô không phải thứ mình tưởng.',
    what: [
      'Phí ship đi qua PhysicalProduct.calculateShipping (hub → zone).',
      'Nhãn class trên từng ô ghi đúng kiểu trong code, vd. Warehouse chỉ giữ Map.Entry productId → qty, không giữ Product.',
    ],
  },
  {
    title: 'Mọi khối là object thật', commit: 'd5107bb',
    why: 'Khối Order và Pricing không phải object: một cái là danh sách, một cái là khái niệm. Bản đồ đang lẫn ba thứ khác nhau.',
    what: [
      'Dữ liệu đổi từ panel phẳng sang cây node với ba dạng khối: object, collection, area.',
      'Ô computed (kết quả hàm thuần) vẽ viền đứt, tách khỏi state.',
      'Bỏ flow Pricing → Cart: toOrder không dùng domain.pricing.',
    ],
  },
  {
    title: 'Mỗi package một vùng', commit: '575c11a',
    why: 'Tùng hỏi: sao order và inventory không có vùng? Dạng area đang gánh hai nghĩa, và các product bị đặt nhầm vào pricing.',
    what: [
      'Area chỉ còn nghĩa package: inventory, order, catalog, pricing, shipping.',
      'Object cố định đặt trong vùng của package chứa class đó; product chuyển sang domain.catalog.',
    ],
  },
  {
    title: 'Trải nghiệm học', commit: '7a6b781 · 22a731f · a5449da · c865b0b',
    why: 'Góp ý từ lúc dùng thật: log hiện trễ, khó lần lại một bước, khó phân biệt các thread.',
    what: [
      'Call log hiện ngay khi bước bắt đầu; nút Back lùi một bước; chữ trong ô căn trái.',
      'Badge màu theo thread; call log cao bằng bản đồ.',
      'Kịch bản tranh nhau món hàng cuối ghi bước tạo 6 đơn để chúng hiện trong List<Order>.',
    ],
  },
  {
    title: 'Container tự giãn',
    why: 'Khay List<Order> chỉ hiện 3 đơn + "+3", không thấy hết 6 đơn đang tranh nhau.',
    what: [
      'Khay và vùng cha giãn theo một chiều từ trên xuống, đẩy các vùng bên dưới xuống; tối đa 4 hàng rồi mới hiện "+N".',
    ],
  },
  {
    title: 'Nối kho với hub',
    why: 'Warehouse và InventoryManager từng là hai model rời nhau, đường kho → hub trên bản đồ chỉ là nét chấm "chưa có".',
    what: [
      'Domain thêm InventoryManager.replenish (kho xuất, Product.stock trên hub cộng đúng số đó, all-or-nothing; khoá hub trước, kho sau) và Product.increaseStock.',
      'Hub = hàng sẵn bán, kho = hàng dự trữ. Kịch bản replenish (thành công, hub không bán sản phẩm, kho không có hàng) và hai đường nối WH → hub nét liền thay cho nét chấm.',
    ],
  },
];

const PRODUCT = [
  '5 vùng package, khối object / collection / area, ô computed tách khỏi state.',
  '9 kịch bản: giỏ hàng → đơn, giá theo loại sản phẩm, trừ kho thành công / thất bại, chuyển kho, chuyển kho song song không deadlock, tranh nhau món hàng cuối, phí ship, nhập hàng kho → hub.',
  'Run / Pause / Step / Back / Reset, tốc độ 0.5–2×, call log có badge thread, diff before → after, bảng chú thích khi click khối.',
  'Banner cảnh báo khi visualizer lệch với domain + skill visualizer-sync; không sửa một dòng nào trong domain/ hay app/.',
];

const LATER = [
  'Ghi đúng từng bước ở kịch bản song song: hiện before/after chụp ngoài lock nên thread khác có thể chen giữa. Hướng đã chọn: chèn code vào method domain lúc chạy (ByteBuddy) và ghi nhật ký thay đổi theo từng object.',
];

const LESSONS = [
  'Đối chiếu với code domain trước khi vẽ: vài chỗ "trông hợp lý" (mũi tên Order → zone, Pricing → Cart) hoá ra không có thật.',
  'Visualizer phải trung thực hơn là đẹp: phân biệt object / danh sách / package, state / kết quả tính ra.',
  'Kiểm tra trên trình duyệt thật sau mỗi thay đổi, không chỉ dựa vào test.',
];

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
