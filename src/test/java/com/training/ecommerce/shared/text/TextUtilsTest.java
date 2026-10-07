// SPEC S01 – kiểm định 07/10 bởi java-start-backlog. Không sửa test để cho pass; muốn đổi spec thì sửa bảng Spec trong backlog trước.
package com.training.ecommerce.shared.text;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TextUtilsTest {

    @Nested
    @DisplayName("toTitleCase")
    class ToTitleCase {

        @ParameterizedTest(name = "\"{0}\" -> \"{1}\"")
        @DisplayName("Viết hoa chữ cái đầu của mỗi từ")
        @CsvSource({
                "hello, Hello",
                "hello world, Hello World",
                "java training ecommerce, Java Training Ecommerce",
                "Hello World, Hello World",
                "'', ''",
                "a, A",
                "123 abc, 123 Abc",
                "điện thoại, Điện Thoại",
                "áo sơ mi, Áo Sơ Mi",
        })
        void capitalizesFirstLetterOfEachWord(String input, String expected) {
            assertEquals(expected, TextUtils.toTitleCase(input));
        }

        // SPEC S01 – sửa 07/10 (Q4): các chữ còn lại chuyển về chữ thường (trước đây giữ nguyên)
        @Test
        @DisplayName("Các chữ còn lại của từ chuyển về chữ thường")
        void lowercasesRemainingLetters() {
            assertEquals("Hello World", TextUtils.toTitleCase("hELLO woRLD"));
        }

        // SPEC S01 (bổ sung 07/10)
        @Test
        @DisplayName("Tên sản phẩm viết hoa toàn bộ → Title Case")
        void convertsUppercaseProductName() {
            assertEquals("Áo Thun Nam", TextUtils.toTitleCase("ÁO THUN NAM"));
        }

        @Test
        @DisplayName("Giữ nguyên khoảng trắng ở đầu, cuối và giữa các từ")
        void preservesLeadingTrailingAndMultipleSpaces() {
            assertEquals("  Hello   World  ", TextUtils.toTitleCase("  hello   world  "));
        }

        // SPEC S01 (bổ sung 07/10)
        @Test
        @DisplayName("Chuỗi chỉ có khoảng trắng giữ nguyên")
        void keepsWhitespaceOnlyInput() {
            assertEquals("   ", TextUtils.toTitleCase("   "));
        }

        @Test
        @DisplayName("Non-breaking space tách từ và được giữ nguyên")
        void treatsNonBreakingSpaceAsSeparator() {
            assertEquals("Hello World", TextUtils.toTitleCase("hello world"));
        }

        // SPEC S01 – sửa 07/10 (Q5): tab/newline cũng tách từ (trước đây không)
        @Test
        @DisplayName("Tab và xuống dòng tách từ và được giữ nguyên")
        void treatsTabAndNewlineAsSeparator() {
            assertEquals("Hello\tWorld", TextUtils.toTitleCase("hello\tworld"));
            assertEquals("Hello\nWorld", TextUtils.toTitleCase("hello\nworld"));
        }

        // SPEC S01 (bổ sung 07/10)
        @Test
        @DisplayName("Dấu gạch ngang không tách từ")
        void doesNotTreatHyphenAsSeparator() {
            assertEquals("Áo-thun Nam", TextUtils.toTitleCase("áo-thun nam"));
        }

        // SPEC S01 – sửa 07/10 (Q2): null → null (trước đây NullPointerException)
        @Test
        @DisplayName("null trả về null")
        void returnsNullForNull() {
            assertNull(TextUtils.toTitleCase(null));
        }
    }

    @Nested
    @DisplayName("slugify")
    class Slugify {

        @ParameterizedTest(name = "\"{0}\" -> \"{1}\"")
        @DisplayName("Chữ thường, nối các từ bằng dấu gạch ngang")
        @CsvSource({
                "hello, hello",
                "Hello World, hello-world",
                "'', ''",
                "iPhone 15 Pro, iphone-15-pro",
                "Quan jean 2026, quan-jean-2026",
        })
        void convertsToLowercaseHyphenated(String input, String expected) {
            assertEquals(expected, TextUtils.slugify(input));
        }

        @Tag("bonus")
        @ParameterizedTest(name = "\"{0}\" -> \"{1}\"")
        @DisplayName("Bỏ dấu tiếng Việt trong tên sản phẩm")
        @CsvSource({
                "Áo Thun Nam, ao-thun-nam",
                "Áo sơ mi nam, ao-so-mi-nam",
                "Điện thoại di động, dien-thoai-di-dong",
                "Bàn phím cơ, ban-phim-co",
                "Tiếng Việt, tieng-viet",
                "Quần jean xanh, quan-jean-xanh",
                "Sữa tươi, sua-tuoi",
                "Kỹ thuật, ky-thuat",
                "Mỹ phẩm, my-pham",
        })
        void removesVietnameseDiacriticsInProductNames(String input, String expected) {
            assertEquals(expected, TextUtils.slugify(input));
        }

        // SPEC S01 – gộp 07/10 (Q6): 6 test nguyên âm cũ gộp thành 1, thêm đ
        @Tag("bonus")
        @ParameterizedTest(name = "\"{0}\" -> \"{1}\"")
        @DisplayName("Bỏ dấu mọi biến thể nguyên âm và chữ đ")
        @CsvSource({
                "áàảãạăắằẳẵặâấầẩẫậ, aaaaaaaaaaaaaaaaa",
                "éèẻẽẹêếềểễệ, eeeeeeeeeee",
                "íìỉĩị, iiiii",
                "óòỏõọôốồổỗộơớờởỡợ, ooooooooooooooooo",
                "úùủũụưứừửữự, uuuuuuuuuuu",
                "ýỳỷỹỵ, yyyyy",
                "đĐ, dd",
        })
        void removesAllVietnameseDiacritics(String input, String expected) {
            assertEquals(expected, TextUtils.slugify(input));
        }

        @Tag("bonus")
        @Test
        @DisplayName("Bỏ dấu cả chữ hoa tiếng Việt")
        void handlesUppercaseVietnameseCharacters() {
            assertEquals("dao-uoc", TextUtils.slugify("ĐẠO ƯỚC"));
        }

        @Test
        @DisplayName("Nhiều khoảng trắng liên tiếp gộp thành một dấu gạch")
        void collapsesConsecutiveSpacesIntoSingleHyphen() {
            assertEquals("hello-world", TextUtils.slugify("hello   world"));
        }

        @Test
        @DisplayName("Không có dấu gạch ở đầu và cuối")
        void trimsLeadingAndTrailingHyphens() {
            assertEquals("hello-world", TextUtils.slugify("  hello world  "));
        }

        @Test
        @DisplayName("Dấu câu bị loại, không để lại dấu gạch thừa")
        void collapsesHyphensLeftByRemovedPunctuation() {
            assertEquals("ao-quan", TextUtils.slugify("Áo & quần"));
            assertEquals("sale", TextUtils.slugify("!!! Sale !!!"));
        }

        @Test
        @DisplayName("Loại bỏ dấu câu")
        void removesPunctuation() {
            assertEquals("hello-world", TextUtils.slugify("Hello, World!"));
            assertEquals("ao-thun-100-cotton", TextUtils.slugify("Áo thun (100% cotton)"));
        }

        // SPEC S01 (bổ sung 07/10, Q3): mọi ký tự không phải chữ/số là ký tự tách
        @ParameterizedTest(name = "\"{0}\" -> \"{1}\"")
        @DisplayName("Gạch ngang, gạch dưới, dấu gạch dài, dấu / đều là ký tự tách")
        @CsvSource({
                "ao-thun-nam, ao-thun-nam",
                "Ao_thun nam, ao-thun-nam",
                "Iphone—15, iphone-15",
                "Ao thun / nam, ao-thun-nam",
                "a - b, a-b",
                "--- Sale ---, sale",
        })
        void treatsNonAlphanumericAsSeparator(String input, String expected) {
            assertEquals(expected, TextUtils.slugify(input));
        }

        // SPEC S01 (bổ sung 07/10, Q3)
        @Test
        @DisplayName("Tab và xuống dòng là ký tự tách")
        void treatsTabAndNewlineAsSeparator() {
            assertEquals("ao-thun-nam", TextUtils.slugify("ao\tthun\nnam"));
        }

        @Test
        @DisplayName("Chỉ có khoảng trắng, dấu câu hoặc ký tự tách → chuỗi rỗng")
        void returnsEmptyWhenOnlySpacesOrPunctuation() {
            assertEquals("", TextUtils.slugify("   "));
            assertEquals("", TextUtils.slugify(" ?! "));
            // SPEC S01 (bổ sung 07/10)
            assertEquals("", TextUtils.slugify("---"));
            assertEquals("", TextUtils.slugify("\t\n"));
        }

        // SPEC S01 – sửa 07/10 (Q2): null → null (trước đây NullPointerException)
        @Test
        @DisplayName("null trả về null")
        void returnsNullForNull() {
            assertNull(TextUtils.slugify(null));
        }
    }
}
