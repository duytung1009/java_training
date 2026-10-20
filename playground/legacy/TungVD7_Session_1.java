package org.example;

import java.util.*;

public class TungVD7_Session_1 {
    static void main() {
        IO.println("Hello and welcome!");

        Scanner input = new Scanner(System.in);
        boolean running = true;
        while (running) {
            showMenu();
            switch (input.nextLine().trim()) {
                case "1":
                    int width = 10;
                    int height = 6;
                    drawBox(width, height);
                    break;
                case "2":
                    String paragraph = "click the <icon src=\"AllIcons.Actions.Execute\"/> icon in the gutter.";
                    wordCounter(paragraph);
                    break;
                case "3":
                    String inputSample3 = "tung";
                    toTitleCase(inputSample3);
                    break;
                case "4":
                    String inputSample4 = "tung";
                    reverseWords(inputSample4);
                    break;
                case "5":
                    String inputSample5 = "tung";
                    removeVowels(inputSample5);
                    break;
                default:
                    IO.println("Exited!");
                    running = false;
                    break;
            }
        }

    }

    static void showMenu() {
        IO.println("");
        IO.println("1. Draw box");
        IO.println("2. Word counter");
        IO.println("3. To title case");
        IO.println("4. Reverse words");
        IO.println("5. Remove vowels");
        IO.println("0. Exit");
    }

    static void drawBox(int width, int height) {
        IO.println("[Practice] drawBox:");
        for (int i = 1; i <= height; i++) {
            StringBuilder line = new StringBuilder();
            for (int j = 1; j <= width; j++) {
                if (i == 1 || i == height) {
                    line.append('*');
                } else if (j == 1 || j == width) {
                    line.append('*');
                } else {
                    line.append(' ');
                }
            }
            IO.println(line.toString());
        }
    }

    static void wordCounter(String paragraph) {
        int words = paragraph.split(" ").length;
        int sentences = paragraph.split("\\.").length;
        IO.println("[WordCounter] input: " + paragraph);
        IO.println("[WordCounter] Words: " + words + " | Sentences: " + sentences + " | Chars: " + paragraph.length());
    }

    static void toTitleCase(String input) {
        StringBuilder output = new StringBuilder(input);
        if (!input.isBlank()) {
            output.setCharAt(0, Character.toUpperCase(input.charAt(0)));
        }
        IO.println("[TextTransformer] toTitleCase: " + input + " -> " + output.toString());
    }

    static void reverseWords(String input) {
        StringBuilder output = new StringBuilder(input);
        for (int i = 0; i < input.length(); i++) {
            output.setCharAt(i, input.charAt((input.length() - 1) - i));
        }
        IO.println("[TextTransformer] reverseWords: " + input + " -> " + output.toString());
    }

    static void removeVowels(String input) {
        StringBuilder output = new StringBuilder();
        Set<Character> vowels = Set.of('a', 'e', 'i', 'o', 'u');
        for (int i = 0; i < input.length(); i++) {
            Character normalize = Character.toLowerCase(input.charAt(i));
            if (!vowels.contains(normalize)) {
                output.append(input.charAt(i));
            }
        }
        IO.println("[TextTransformer] removeVowels: " + input + " -> " + output.toString());
    }
}
