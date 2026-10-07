package com.training.ecommerce.shared.text;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TextUtilsTest {

    private final TextUtils textUtils = new TextUtils();

    @Nested
    @DisplayName("toTitleCase")
    class ToTitleCase {

        @ParameterizedTest(name = "\"{0}\" -> \"{1}\"")
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
            assertEquals(expected, textUtils.toTitleCase(input));
        }

        @Test
        void keepsRemainingLettersUnchanged() {
            assertEquals("HELLO WoRLD", textUtils.toTitleCase("hELLO woRLD"));
        }

        @Test
        void preservesLeadingTrailingAndMultipleSpaces() {
            assertEquals("  Hello   World  ", textUtils.toTitleCase("  hello   world  "));
        }

        @Test
        void treatsNonBreakingSpaceAsSeparator() {
            assertEquals("Hello World", textUtils.toTitleCase("hello world"));
        }

        @Test
        void doesNotTreatTabOrNewlineAsSeparator() {
            // Character.isSpaceChar() không coi '\t', '\n' là khoảng trắng
            assertEquals("Hello\tworld", textUtils.toTitleCase("hello\tworld"));
            assertEquals("Hello\nworld", textUtils.toTitleCase("hello\nworld"));
        }

        @Test
        void throwsOnNull() {
            assertThrows(NullPointerException.class, () -> textUtils.toTitleCase(null));
        }
    }

    @Nested
    @DisplayName("slugify")
    class Slugify {

        @ParameterizedTest(name = "\"{0}\" -> \"{1}\"")
        @CsvSource({
                "hello, hello",
                "Hello World, hello-world",
                "'', ''",
                "iPhone 15 Pro, iphone-15-pro",
                "Áo sơ mi nam, ao-so-mi-nam",
                "Điện thoại di động, dien-thoai-di-dong",
                "Bàn phím cơ, ban-phim-co",
                "Tiếng Việt, tieng-viet",
                "Quần jean xanh, quan-jean-xanh",
                "Sữa tươi, sua-tuoi",
                "Kỹ thuật, ky-thuat",
                "Mỹ phẩm, my-pham",
        })
        void convertsToLowercaseHyphenatedAscii(String input, String expected) {
            assertEquals(expected, textUtils.slugify(input));
        }

        @Test
        void removesAllVietnameseDiacriticsForA() {
            assertEquals("aaaaaaaaaaaaaaaaa", textUtils.slugify("áàảãạăắằẳẵặâấầẩẫậ"));
        }

        @Test
        void removesAllVietnameseDiacriticsForE() {
            assertEquals("eeeeeeeeeee", textUtils.slugify("éèẻẽẹêếềểễệ"));
        }

        @Test
        void removesAllVietnameseDiacriticsForI() {
            assertEquals("iiiii", textUtils.slugify("íìỉĩị"));
        }

        @Test
        void removesAllVietnameseDiacriticsForO() {
            assertEquals("ooooooooooooooooo", textUtils.slugify("óòỏõọôốồổỗộơớờởỡợ"));
        }

        @Test
        void removesAllVietnameseDiacriticsForU() {
            assertEquals("uuuuuuuuuuu", textUtils.slugify("úùủũụưứừửữự"));
        }

        @Test
        void removesAllVietnameseDiacriticsForY() {
            assertEquals("yyyyy", textUtils.slugify("ýỳỷỹỵ"));
        }

        @Test
        void handlesUppercaseVietnameseCharacters() {
            assertEquals("dao-uoc", textUtils.slugify("ĐẠO ƯỚC"));
        }

        @Test
        void collapsesConsecutiveSpacesIntoSingleHyphen() {
            assertEquals("hello-world", textUtils.slugify("hello   world"));
        }

        @Test
        void trimsLeadingAndTrailingHyphens() {
            assertEquals("hello-world", textUtils.slugify("  hello world  "));
        }

        @Test
        void collapsesHyphensLeftByRemovedPunctuation() {
            assertEquals("ao-quan", textUtils.slugify("Áo & quần"));
            assertEquals("sale", textUtils.slugify("!!! Sale !!!"));
        }

        @Test
        void returnsEmptyWhenOnlySpacesOrPunctuation() {
            assertEquals("", textUtils.slugify("   "));
            assertEquals("", textUtils.slugify(" ?! "));
        }

        @Test
        void removesPunctuation() {
            assertEquals("hello-world", textUtils.slugify("Hello, World!"));
            assertEquals("ao-thun-100-cotton", textUtils.slugify("Áo thun (100% cotton)"));
        }

        @Test
        void throwsOnNull() {
            assertThrows(NullPointerException.class, () -> textUtils.slugify(null));
        }
    }
}
