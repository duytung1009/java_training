// SPEC S02 – sinh bởi java-start-backlog 20/10. Không sửa test để cho pass; muốn đổi spec thì sửa bảng Spec trong backlog trước.
package com.training.ecommerce.interfaces.cli;

import com.training.ecommerce.domain.catalog.Product;
import com.training.ecommerce.domain.order.Order;
import com.training.ecommerce.domain.order.OrderLine;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class ReceiptPrinterTest {

    private Locale originalLocale;
    private ReceiptPrinter printer;
    private Order order;

    @BeforeEach
    void setUp() {
        originalLocale = Locale.getDefault();
        Locale.setDefault(Locale.US);

        printer = new ReceiptPrinter();
        Product tshirt = new Product("P001", "Áo thun nam", new BigDecimal("150000"), 50);
        Product jeans = new Product("P002", "Quần jean", new BigDecimal("450000"), 20);
        order = new Order(new BigDecimal("0.10"), new BigDecimal("0.08"));
        order.addLine(new OrderLine(tshirt, 2));
        order.addLine(new OrderLine(jeans, 1));
    }

    @AfterEach
    void restoreLocale() {
        Locale.setDefault(originalLocale);
    }

    private static String expectedRow(String name, int quantity, String lineTotal) {
        return String.format(Locale.ROOT, "%-20s%5d%12.2f", name, quantity, new BigDecimal(lineTotal));
    }

    private static int indexOfLineStartingWith(String receipt, String prefix) {
        String[] lines = receipt.split("\\R");
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].startsWith(prefix)) {
                return i;
            }
        }
        return -1;
    }

    private static String lineStartingWith(String receipt, String prefix) {
        int index = indexOfLineStartingWith(receipt, prefix);
        if (index < 0) {
            fail("Receipt không có dòng nào bắt đầu bằng \"" + prefix + "\":\n" + receipt);
        }
        return receipt.split("\\R")[index];
    }

    private static void assertContainsAmount(String line, String amount) {
        Pattern exactAmount = Pattern.compile("(?<![\\d.,])" + Pattern.quote(amount) + "(?![\\d])");
        assertTrue(exactAmount.matcher(line).find(),
                () -> "Dòng \"" + line + "\" phải chứa số tiền " + amount);
    }

    @Test
    @DisplayName("Mỗi line: tên căn trái 20, quantity căn phải 5, lineTotal căn phải 12 với 2 chữ số thập phân")
    void formatsEachItemRow() {
        String receipt = printer.print(order);

        assertEquals(expectedRow("Áo thun nam", 2, "300000.00"), lineStartingWith(receipt, "Áo thun nam"));
        assertEquals(expectedRow("Quần jean", 1, "450000.00"), lineStartingWith(receipt, "Quần jean"));
    }

    @Test
    @DisplayName("Các dòng hàng in theo thứ tự add")
    void printsItemRowsInOrder() {
        String receipt = printer.print(order);

        assertTrue(indexOfLineStartingWith(receipt, "Áo thun nam") < indexOfLineStartingWith(receipt, "Quần jean"),
                "Áo thun nam phải in trước Quần jean");
    }

    @Test
    @DisplayName("Tên dài hơn 20 ký tự được giữ nguyên, không cắt")
    void keepsLongNameInFull() {
        Product longName = new Product("P004", "Áo thun nam cổ tròn tay ngắn", new BigDecimal("199000"), 10);
        Order longOrder = new Order(BigDecimal.ZERO, BigDecimal.ZERO);
        longOrder.addLine(new OrderLine(longName, 1));

        String receipt = printer.print(longOrder);

        assertEquals(expectedRow("Áo thun nam cổ tròn tay ngắn", 1, "199000.00"),
                lineStartingWith(receipt, "Áo thun nam cổ tròn tay ngắn"));
    }

    @ParameterizedTest(name = "{0} → {1}")
    @DisplayName("Có các dòng tổng Subtotal, Discount, Tax, Total với số tiền 2 chữ số thập phân")
    @CsvSource({
            "Subtotal, 750000.00",
            "Discount, 75000.00",
            "Tax, 54000.00",
            "Total, 729000.00",
    })
    void printsSummaryLines(String label, String amount) {
        String receipt = printer.print(order);

        assertContainsAmount(lineStartingWith(receipt, label), amount);
    }

    @Test
    @DisplayName("Đơn rỗng vẫn in được, Total = 0.00")
    void printsEmptyOrder() {
        String receipt = printer.print(new Order(BigDecimal.ZERO, new BigDecimal("0.08")));

        assertContainsAmount(lineStartingWith(receipt, "Total"), "0.00");
    }

    @Test
    @DisplayName("order null → IllegalArgumentException")
    void rejectsNullOrder() {
        assertThrows(IllegalArgumentException.class, () -> printer.print(null));
    }

    @Test
    @Tag("bonus")
    @DisplayName("Máy đặt locale vi-VN vẫn dùng dấu chấm thập phân (không ra 300000,00)")
    void usesDotDecimalSeparatorRegardlessOfDefaultLocale() {
        Locale.setDefault(Locale.forLanguageTag("vi-VN"));

        String receipt = printer.print(order);

        assertEquals(expectedRow("Áo thun nam", 2, "300000.00"), lineStartingWith(receipt, "Áo thun nam"));
        assertContainsAmount(lineStartingWith(receipt, "Total"), "729000.00");
    }
}
