package org.example;

public class WordCounter {
    static void main() {
        String paragraph = "click the <icon src=\"AllIcons.Actions.Execute\"/> icon in the gutter.";

        int words = paragraph.split(" ").length;
        int sentences = paragraph.split("\\.").length;

        IO.println("Words: " + words + " | Sentences: " + sentences + " | Chars: " + paragraph.length());
    }
}
