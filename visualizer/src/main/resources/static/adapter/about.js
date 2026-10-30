// Nội dung màn "?" (hành trình phát triển visualizer ecommerce); js/panels/about.js chỉ lo phần dựng DOM
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
  {
    title: 'Kịch bản tổng quan',
    why: 'Mỗi kịch bản chỉ soi một buổi học; thiếu một lượt chạy cho thấy các package nối với nhau thế nào.',
    what: [
      'Kịch bản overview đứng đầu danh sách: gom hàng giữa hai kho, nhập lên hub, tính giá, giỏ hàng, mã giảm giá, chốt đơn, trừ kho, phí ship — flow đi qua cả 5 vùng.',
      'Ghép lại từ bước của các kịch bản theo buổi (OrderSteps.checkout / decreaseStock, transfer, replenish, price, ship), không viết lại logic gọi domain.',
    ],
  },
  {
    title: 'Chốt giỏ bằng mã giảm giá',
    why: 'Cart.toOrder chỉ nhận tỉ lệ giảm giá trần, DiscountCode của domain.pricing không nối được vào đơn.',
    what: [
      'Domain thêm Cart.toOrder(code, taxRate): tương đương toOrder(code.getRate(), taxRate), Order chỉ giữ tỉ lệ, code null → IllegalArgumentException.',
      'Kịch bản giỏ hàng và tổng quan có thêm bước new DiscountCode(SALE10) và đường nối orders ↔ pricing; catalog hiện sẵn mọi Product của hub ngay từ đầu.',
    ],
  },
  {
    title: 'Vận đơn và giao hàng',
    why: 'Domain thêm Shipment (vòng đời CREATED → IN_TRANSIT → DELIVERED / RETURNING → RETURNED / CANCELLED), ShippingFeeCalculator và InventoryManager.restock; bản đồ chưa có chỗ cho object sinh ra lúc chạy trong domain.shipping.',
    what: [
      'Hai kịch bản: giao thành công (kho không hoàn) và giao thất bại 3 lần → trả hàng → restock, tồn kho hub về như trước khi đặt; có một thao tác sai (cancel khi đang giao) để thấy IllegalShipmentTransitionException.',
      'Shipment hiện thành khối con của domain.shipping với ô status / failed / fee / tracking, diff cho thấy trạng thái đổi qua từng bước; đường nối mới orders → shipping, hoàn kho đi lại đường shipping → hub.',
    ],
  },
  {
    title: 'Visualizer tổng quát: tách lõi, event, mô hình, tầng và bộ lọc',
    why: 'Visualizer dính chặt vào ecommerce (id, toạ độ, tên class nằm lẫn trong lõi), chưa thấy domain event, và bản đồ chỉ có một mức chi tiết nên domain lớn thì rối.',
    what: [
      'P1: tách lõi (core) khỏi adapter ecommerce, có test kiến trúc chặn lõi import domain; phía FE, dữ liệu riêng của domain nằm sau adapter/index.js.',
      'P2: domain event hiện thành flow kiểu event (nét chấm) và dòng con trong call log, phát sau lời gọi sinh ra nó.',
      'P3: mô hình domain (context, aggregate, quan hệ) gợi ý từ reflection, sửa trên giao diện bằng nút Mô hình và lưu thành domain-model.json.',
      'Xem theo tầng context map → aggregate → instance (click khối để vào sâu, breadcrumb và Esc để lùi, hash URL giữ tầng), cùng bộ lọc context, aggregate, kết quả, loại flow, thread, chỉ object có thay đổi.',
    ],
  },
];

const PRODUCT = [
  '5 vùng package, khối object / collection / area, ô computed tách khỏi state.',
  '12 kịch bản: tổng quan một lượt mua từ kho tới đơn, giỏ hàng → đơn, giá theo loại sản phẩm, trừ kho thành công / thất bại, chuyển kho, chuyển kho song song không deadlock, tranh nhau món hàng cuối, phí ship, nhập hàng kho → hub, vận đơn giao thành công / bị trả về.',
  'Run / Pause / Step / Back / Reset, tốc độ 0.5–2×, call log có badge thread, diff before → after, bảng chú thích khi click khối.',
  'Mô hình domain sửa được trên giao diện, xem theo ba tầng context → aggregate → instance với bộ lọc (context, aggregate, kết quả, loại flow, thread, chỉ object có thay đổi); domain event hiện như flow nét chấm.',
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

export const about = { INTRO, STAGES, PRODUCT, LATER, LESSONS };
