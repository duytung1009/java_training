# Curriculum

> Cập nhật sau mỗi buổi T7. Buổi tiếp theo chỉ ghi những gì đã được thông báo, không đoán trước.

| # | Module | Topic | Bài tập / Case study | Áp dụng ecommerce | Slide |
|---|---|---|---|---|---|
| S01 | M1 – Java Fundamentals | JVM/JRE/JDK, ClassLoader, Stack/Heap/Metaspace, GC, primitive & autoboxing, casting, String/StringBuilder | Practice: WordCounter, TextTransformer | `ReceiptPrinter` dùng StringBuilder, `TextUtils` | session-1-v2.pptx |
| S02 | M1 – Java Fundamentals | if/switch (switch expression), loops, arrays, class/constructor, access modifiers, static, constructor overloading/chaining | **Case Study 1**: Inventory & Order Line Calculator | `Product`, `OrderLine`, `Order`, `InventoryManager` (array), `ReceiptPrinter` | session-2.pptx |
| S03 | M2 – OOP | 4 OOP pillars, abstract class vs interface, overriding/overloading, equals/hashCode/toString, SOLID | Practice: Shape hierarchy · **Case Study 2**: Product Pricing | `Product` abstract + Physical/Digital/Subscription, `Discountable`, `Shippable`, `PricingEngine` | session-3-v3.pptx |
| S04 | Tự nghiên cứu (03/10, không có slide) | Generics (bounded, wildcard/PECS, erasure), Collections (List/Set/Map, Queue/Deque, Iterator, Comparable/Comparator), Exception (checked/unchecked, finally, try-with-resources, custom) | – | `Repository<T, ID>` generic, `InventoryManager` dùng `Map`, `Cart`, domain exception | – |
| – | – | _10/10: lớp nghỉ_ | – | – | – |
| S05 | Tự nghiên cứu, tự trình bày (17/10) | Functional Programming (pure function, immutability, higher-order function), functional interface, lambda, method reference, Stream API, Collectors, Optional | Slide tự trình bày + quiz cả lớp | Refactor sang Stream, `PricingRule`, `Category`, `ReportService` | [slide](https://claude.ai/artifact/7P9Vr9WS3AvWsCdYVFUjFS) |
