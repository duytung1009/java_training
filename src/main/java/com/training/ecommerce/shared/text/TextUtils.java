package com.training.ecommerce.shared.text;

import java.util.List;

public class TextUtils {

    private TextUtils() {}

    public static String toTitleCase(String input) {
        if (input == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder(input.length());
        boolean nextTitleCase = true;
        for (char c : input.toCharArray()) {
            if (Character.isSpaceChar(c) || '\n' == c || '\t' == c) {
                nextTitleCase = true;
            } else if (nextTitleCase) {
                c = Character.toTitleCase(c);
                nextTitleCase = false;
            } else {
                c = Character.toLowerCase(c);
            }
            sb.append(c);
        }
        return sb.toString();
    }

    public static String slugify(String input) {
        if (input == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder(input.length());
        List<Character> textA = List.of('á', 'à', 'ả', 'ã', 'ạ', 'ă', 'ắ', 'ằ', 'ẳ', 'ẵ', 'ặ', 'â', 'ấ', 'ầ', 'ẩ', 'ẫ', 'ậ');
        List<Character> textE = List.of('é', 'è', 'ẻ', 'ẽ', 'ẹ', 'ê', 'ế', 'ề', 'ể', 'ễ', 'ệ');
        List<Character> textI = List.of('í', 'ì', 'ỉ', 'ĩ', 'ị');
        List<Character> textO = List.of('ó', 'ò', 'ỏ', 'õ', 'ọ', 'ô', 'ố', 'ồ', 'ổ', 'ỗ', 'ộ', 'ơ', 'ớ', 'ờ', 'ở', 'ỡ', 'ợ');
        List<Character> textU = List.of('ú', 'ù', 'ủ', 'ũ', 'ụ', 'ư', 'ứ', 'ừ', 'ử', 'ữ', 'ự');
        List<Character> textY = List.of('ý', 'ỳ', 'ỷ', 'ỹ', 'ỵ');
        List<Character> textD = List.of('đ');
        for (char c : input.toCharArray()) {
            char lower = Character.toLowerCase(c);
            if (textA.contains(lower)) {
                sb.append("a");
            } else if (textE.contains(lower)) {
                sb.append("e");
            } else if (textI.contains(lower)) {
                sb.append("i");
            } else if (textO.contains(lower)) {
                sb.append("o");
            } else if (textU.contains(lower)) {
                sb.append("u");
            } else if (textY.contains(lower)) {
                sb.append("y");
            } else if (textD.contains(lower)) {
                sb.append("d");
            } else if ((lower >= 'a' && lower <= 'z') || (lower >= '0' && lower <= '9')) {
                sb.append(lower);
            } else if (!sb.isEmpty() && sb.charAt(sb.length() - 1) != '-') {
                sb.append("-");
            }
        }
        if (!sb.isEmpty() && sb.charAt(sb.length() - 1) == '-') {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }
}
