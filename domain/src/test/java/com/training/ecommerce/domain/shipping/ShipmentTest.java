// SPEC S07-extra – sinh 30/10 (shipment-lifecycle). Không sửa test để cho pass; muốn đổi spec thì sửa bảng Spec trong backlog trước.
package com.training.ecommerce.domain.shipping;

import com.training.ecommerce.domain.customer.Address;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Shipment: vòng đời vận đơn (S07-extra)")
class ShipmentTest {

    private static final Address HANOI = new Address("1 Phố Huế", "Hà Nội");

    private Shipment shipment;

    @BeforeEach
    void setUp() {
        shipment = new Shipment("o-1", HANOI, new BigDecimal("30000"));
    }

    private Shipment inTransit() {
        shipment.pickUp();
        return shipment;
    }

    @Nested
    @DisplayName("Tạo vận đơn (G1)")
    class Creation {

        @Test
        @DisplayName("Vận đơn mới: CREATED, attempts 0, trackingCode không rỗng, fee scale 2, history 1 event CREATED")
        void startsCreated() {
            assertEquals(ShipmentStatus.CREATED, shipment.getStatus());
            assertEquals(0, shipment.getFailedAttempts());
            assertEquals("o-1", shipment.getOrderId());
            assertEquals(HANOI, shipment.getAddress());
            assertFalse(shipment.getTrackingCode() == null || shipment.getTrackingCode().isBlank());
            assertFalse(shipment.getId() == null || shipment.getId().isBlank());
            assertEquals(new BigDecimal("30000.00"), shipment.getFee());
            assertEquals(1, shipment.history().size());
            assertEquals(ShipmentStatus.CREATED, shipment.history().get(0).status());
        }

        @Test
        @DisplayName("Hai vận đơn có id khác nhau")
        void generatesDistinctIds() {
            Shipment other = new Shipment("o-1", HANOI, new BigDecimal("30000"));

            assertNotEquals(shipment.getId(), other.getId());
        }

        @Test
        @DisplayName("fee 22500.455 → làm tròn HALF_UP về scale 2")
        void roundsFee() {
            Shipment s = new Shipment("o-1", HANOI, new BigDecimal("22500.455"));

            assertEquals(new BigDecimal("22500.46"), s.getFee());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"  "})
        @DisplayName("orderId null / rỗng / blank → IAE")
        void rejectsBlankOrderId(String orderId) {
            assertThrows(IllegalArgumentException.class, () -> new Shipment(orderId, HANOI, BigDecimal.TEN));
        }

        @Test
        @DisplayName("address null → IAE")
        void rejectsNullAddress() {
            assertThrows(IllegalArgumentException.class, () -> new Shipment("o-1", null, BigDecimal.TEN));
        }

        @Test
        @DisplayName("fee null hoặc âm → IAE; fee 0 hợp lệ")
        void rejectsInvalidFee() {
            assertThrows(IllegalArgumentException.class, () -> new Shipment("o-1", HANOI, null));
            assertThrows(IllegalArgumentException.class, () -> new Shipment("o-1", HANOI, new BigDecimal("-0.01")));
            assertEquals(new BigDecimal("0.00"), new Shipment("o-1", HANOI, BigDecimal.ZERO).getFee());
        }
    }

    @Nested
    @DisplayName("Giao thành công")
    class Delivery {

        @Test
        @DisplayName("pickUp → markDelivered: DELIVERED, history 3 event theo thứ tự")
        void deliversSuccessfully() {
            shipment.pickUp();
            assertEquals(ShipmentStatus.IN_TRANSIT, shipment.getStatus());
            shipment.markDelivered();

            assertEquals(ShipmentStatus.DELIVERED, shipment.getStatus());
            assertEquals(List.of(ShipmentStatus.CREATED, ShipmentStatus.IN_TRANSIT, ShipmentStatus.DELIVERED),
                    shipment.history().stream().map(ShipmentEvent::status).toList());
        }

        @Test
        @DisplayName("markDelivered khi chưa lấy hàng (CREATED) → ISE, message nêu CREATED")
        void cannotDeliverBeforePickUp() {
            IllegalStateException e = assertThrows(IllegalStateException.class, () -> shipment.markDelivered());
            assertTrue(e.getMessage().contains("CREATED"));
        }
    }

    @Nested
    @DisplayName("Giao thất bại (G4)")
    class FailedAttempts {

        @Test
        @DisplayName("Thất bại 2 lần → vẫn IN_TRANSIT, attempts 2, mỗi lần thêm event IN_TRANSIT kèm lý do")
        void staysInTransitBelowMax() {
            inTransit();
            shipment.failAttempt("Không có người nhận");
            shipment.failAttempt("Sai địa chỉ");

            assertEquals(ShipmentStatus.IN_TRANSIT, shipment.getStatus());
            assertEquals(2, shipment.getFailedAttempts());
            assertEquals(4, shipment.history().size());
            assertEquals(new ShipmentEvent(ShipmentStatus.IN_TRANSIT, "Không có người nhận"), shipment.history().get(2));
            assertEquals(new ShipmentEvent(ShipmentStatus.IN_TRANSIT, "Sai địa chỉ"), shipment.history().get(3));
        }

        @Test
        @DisplayName("Thất bại lần thứ 3 (MAX_ATTEMPTS) → tự chuyển RETURNING, event cuối là RETURNING")
        void autoReturnsAfterThirdFailure() {
            assertEquals(3, Shipment.MAX_ATTEMPTS);
            inTransit();
            shipment.failAttempt("lần 1");
            shipment.failAttempt("lần 2");
            shipment.failAttempt("lần 3");

            assertEquals(ShipmentStatus.RETURNING, shipment.getStatus());
            assertEquals(3, shipment.getFailedAttempts());
            assertEquals(new ShipmentEvent(ShipmentStatus.RETURNING, "lần 3"),
                    shipment.history().get(shipment.history().size() - 1));
        }

        @Test
        @DisplayName("Sau khi tự chuyển RETURNING, failAttempt tiếp → ISE, attempts vẫn 3")
        void cannotFailAfterReturning() {
            inTransit();
            shipment.failAttempt("1");
            shipment.failAttempt("2");
            shipment.failAttempt("3");

            IllegalStateException e = assertThrows(IllegalStateException.class, () -> shipment.failAttempt("4"));
            assertTrue(e.getMessage().contains("RETURNING"));
            assertEquals(3, shipment.getFailedAttempts());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"  "})
        @DisplayName("Lý do null / rỗng / blank → IAE, attempts và history không đổi")
        void rejectsBlankReason(String reason) {
            inTransit();
            int historySize = shipment.history().size();

            assertThrows(IllegalArgumentException.class, () -> shipment.failAttempt(reason));
            assertThrows(IllegalArgumentException.class, () -> shipment.refuse(reason));
            assertEquals(0, shipment.getFailedAttempts());
            assertEquals(ShipmentStatus.IN_TRANSIT, shipment.getStatus());
            assertEquals(historySize, shipment.history().size());
        }

        @Test
        @DisplayName("Lý do rỗng ở trạng thái sai (CREATED) → vẫn IAE: kiểm lý do trước trạng thái")
        void reasonCheckedBeforeState() {
            assertThrows(IllegalArgumentException.class, () -> shipment.failAttempt("  "));
            assertThrows(IllegalArgumentException.class, () -> shipment.failAttempt(null));
            assertThrows(IllegalArgumentException.class, () -> shipment.refuse(""));
        }

        @Test
        @DisplayName("failAttempt ở CREATED → ISE, message nêu CREATED, attempts và history không đổi")
        void failAttemptRequiresInTransit() {
            IllegalStateException e = assertThrows(IllegalStateException.class, () -> shipment.failAttempt("lý do"));

            assertTrue(e.getMessage().contains("CREATED"));
            assertEquals(0, shipment.getFailedAttempts());
            assertEquals(1, shipment.history().size());
        }
    }

    @Nested
    @DisplayName("Từ chối và hoàn")
    class Refusal {

        @Test
        @DisplayName("refuse → RETURNING (event kèm lý do), markReturned → RETURNED")
        void returnsAfterRefusal() {
            inTransit();
            shipment.refuse("Khách từ chối nhận");
            assertEquals(ShipmentStatus.RETURNING, shipment.getStatus());
            assertEquals(new ShipmentEvent(ShipmentStatus.RETURNING, "Khách từ chối nhận"),
                    shipment.history().get(shipment.history().size() - 1));

            shipment.markReturned();

            assertEquals(ShipmentStatus.RETURNED, shipment.getStatus());
            assertEquals(4, shipment.history().size());
        }

        @Test
        @DisplayName("markReturned khi chưa RETURNING (IN_TRANSIT) → ISE")
        void markReturnedRequiresReturning() {
            inTransit();

            IllegalStateException e = assertThrows(IllegalStateException.class, () -> shipment.markReturned());
            assertTrue(e.getMessage().contains("IN_TRANSIT"));
        }

        @Test
        @DisplayName("refuse ở CREATED → ISE")
        void refuseRequiresInTransit() {
            assertThrows(IllegalStateException.class, () -> shipment.refuse("lý do"));
        }
    }

    @Nested
    @DisplayName("Huỷ (G5)")
    class Cancellation {

        @Test
        @DisplayName("cancel ở CREATED → CANCELLED, history thêm event CANCELLED")
        void cancelsBeforePickUp() {
            shipment.cancel();

            assertEquals(ShipmentStatus.CANCELLED, shipment.getStatus());
            assertEquals(ShipmentStatus.CANCELLED, shipment.history().get(1).status());
        }

        @Test
        @DisplayName("cancel ở IN_TRANSIT → ISE (phải refuse → markReturned)")
        void cannotCancelAfterPickUp() {
            inTransit();

            IllegalStateException e = assertThrows(IllegalStateException.class, () -> shipment.cancel());
            assertTrue(e.getMessage().contains("IN_TRANSIT"));
            assertEquals(ShipmentStatus.IN_TRANSIT, shipment.getStatus());
        }
    }

    @Nested
    @DisplayName("Trạng thái kết thúc (G6)")
    class TerminalStates {

        private Shipment shipmentIn(ShipmentStatus target) {
            Shipment s = new Shipment("o-1", HANOI, new BigDecimal("30000"));
            switch (target) {
                case DELIVERED -> {
                    s.pickUp();
                    s.markDelivered();
                }
                case RETURNED -> {
                    s.pickUp();
                    s.refuse("từ chối");
                    s.markReturned();
                }
                case CANCELLED -> s.cancel();
                default -> throw new IllegalArgumentException(target.name());
            }
            return s;
        }

        @ParameterizedTest
        @EnumSource(value = ShipmentStatus.class, names = {"DELIVERED", "RETURNED", "CANCELLED"})
        @DisplayName("Không thao tác nào chuyển đi được: ISE, message nêu trạng thái, status + attempts + history không đổi")
        void terminalStatesAreFinal(ShipmentStatus terminal) {
            Shipment s = shipmentIn(terminal);
            List<ShipmentEvent> historyBefore = s.history();
            Map<String, Consumer<Shipment>> operations = new LinkedHashMap<>();
            operations.put("pickUp", Shipment::pickUp);
            operations.put("markDelivered", Shipment::markDelivered);
            operations.put("failAttempt", x -> x.failAttempt("lý do"));
            operations.put("refuse", x -> x.refuse("lý do"));
            operations.put("markReturned", Shipment::markReturned);
            operations.put("cancel", Shipment::cancel);

            operations.forEach((name, operation) -> {
                IllegalStateException e = assertThrows(IllegalStateException.class, () -> operation.accept(s),
                        name + " ở " + terminal + " phải ném ISE");
                assertTrue(e.getMessage().contains(terminal.name()), name + ": message phải nêu " + terminal);
                assertEquals(terminal, s.getStatus());
                assertEquals(0, s.getFailedAttempts());
                assertEquals(historyBefore, s.history());
            });
        }
    }

    @Nested
    @DisplayName("history() và ShipmentStatus")
    class HistoryAndStatus {

        @Test
        @DisplayName("history() là bản copy không sửa được; sửa thử không ảnh hưởng vận đơn")
        void historyIsUnmodifiable() {
            List<ShipmentEvent> history = shipment.history();

            assertThrows(UnsupportedOperationException.class,
                    () -> history.add(new ShipmentEvent(ShipmentStatus.DELIVERED, "giả")));
            assertEquals(1, shipment.history().size());
        }

        @Test
        @DisplayName("history() lấy trước không đổi khi vận đơn chuyển trạng thái tiếp (snapshot)")
        void historyIsSnapshot() {
            List<ShipmentEvent> before = shipment.history();
            shipment.pickUp();

            assertEquals(1, before.size());
            assertEquals(2, shipment.history().size());
        }

        @Test
        @DisplayName("isTerminal: chỉ DELIVERED, RETURNED, CANCELLED")
        void isTerminal() {
            List<ShipmentStatus> terminals = new ArrayList<>();
            for (ShipmentStatus status : ShipmentStatus.values()) {
                if (status.isTerminal()) {
                    terminals.add(status);
                }
            }

            assertEquals(List.of(ShipmentStatus.DELIVERED, ShipmentStatus.RETURNED, ShipmentStatus.CANCELLED), terminals);
        }
    }
}
