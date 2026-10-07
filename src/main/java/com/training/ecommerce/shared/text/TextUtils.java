package com.training.ecommerce.shared.text;

import java.util.*;

public class TextUtils {

    public String toTitleCase(String input) {
        StringBuilder sb = new StringBuilder(input.length());
        boolean nextTitleCase = true;
        for (char c : input.toCharArray()) {
            if (Character.isSpaceChar(c)) {
                nextTitleCase = true;
            } else if (nextTitleCase) {
                c = Character.toTitleCase(c);
                nextTitleCase = false;
            }
            sb.append(c);
        }
        return sb.toString();
    }

    public String slugify(String input) {
        StringBuilder sb = new StringBuilder(input.length());
        List<Character> text_a = List.of('á', 'à', 'ả', 'ã', 'ạ', 'ă', 'ắ', 'ằ', 'ẳ', 'ẵ', 'ặ', 'â', 'ấ', 'ầ', 'ẩ', 'ẫ', 'ậ');
        List<Character> text_e = List.of('é', 'è', 'ẻ', 'ẽ', 'ẹ', 'ê', 'ế', 'ề', 'ể', 'ễ', 'ệ');
        List<Character> text_i = List.of('í', 'ì', 'ỉ', 'ĩ', 'ị');
        List<Character> text_o = List.of('ó', 'ò', 'ỏ', 'õ', 'ọ', 'ô', 'ố', 'ồ', 'ổ', 'ỗ', 'ộ', 'ơ', 'ớ', 'ờ', 'ở', 'ỡ', 'ợ');
        List<Character> text_u = List.of('ú', 'ù', 'ủ', 'ũ', 'ụ', 'ư', 'ứ', 'ừ', 'ử', 'ữ', 'ự');
        List<Character> text_y = List.of('ý', 'ỳ', 'ỷ', 'ỹ', 'ỵ');
        List<Character> text_d = List.of('d', 'đ');
        for (char c : input.toCharArray()) {
            if (Character.isSpaceChar(c)) {
                if (!sb.isEmpty() && sb.charAt(sb.length() - 1) != '-') {
                    sb.append("-");
                }
            } else {
                char lower = Character.toLowerCase(c);
                if (text_a.contains(lower)) {
                    sb.append("a");
                } else if (text_e.contains(lower)) {
                    sb.append("e");
                } else if (text_i.contains(lower)) {
                    sb.append("i");
                } else if (text_o.contains(lower)) {
                    sb.append("o");
                } else if (text_u.contains(lower)) {
                    sb.append("u");
                } else if (text_y.contains(lower)) {
                    sb.append("y");
                } else if (text_d.contains(lower)) {
                    sb.append("d");
                } else if ((lower >= 'a' && lower <= 'z') || (lower >= '0' && lower <= '9')) {
                    sb.append(lower);
                }
            }
        }
        if (!sb.isEmpty() && sb.charAt(sb.length() - 1) == '-') {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }
}
