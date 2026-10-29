package com.training.ecommerce.visualizer.trace;

import com.training.ecommerce.visualizer.world.WorldView;

import java.util.List;

/**
 * Một lần gọi method domain đã ghi lại. Tên field JSON khớp {@code fixtures/sample-run.json}.
 *
 * @param seq        số thứ tự (tăng dần theo thứ tự hoàn tất)
 * @param at         thời điểm ISO-8601
 * @param thread     tên thread gọi
 * @param className  class domain được gọi
 * @param method     method được gọi
 * @param args       tham số dạng chuỗi
 * @param outcome    OK hoặc THROWN
 * @param result     kết quả dạng chuỗi (null nếu void / lỗi)
 * @param exception  thông tin lỗi khi THROWN, ngược lại null
 * @param flows      hướng chảy dữ liệu theo thứ tự phát, rỗng nếu không có
 * @param panels     panel id cần nháy viền
 * @param changes    các object có field đổi (rỗng nếu không đổi gì)
 * @param worldAfter trạng thái thế giới sau lần gọi
 */
public record TraceEvent(
        long seq,
        String at,
        String thread,
        String className,
        String method,
        List<String> args,
        Outcome outcome,
        String result,
        ExceptionInfo exception,
        List<Flow> flows,
        List<String> panels,
        List<ObjectChange> changes,
        WorldView worldAfter) {
}
