import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/** S03 – chạy: java playground/session03/OopPitfalls.java — ĐOÁN output trước! */
public class OopPitfalls {
    static class Animal {
        String name = "animal";
        static String kind() { return "Animal.kind"; }
        String sound() { return "..."; }
        String describe() { return name + " says " + sound(); }
    }

    static class Dog extends Animal {
        String name = "dog";                           // field HIDING, không phải override
        static String kind() { return "Dog.kind"; }    // static HIDING
        @Override String sound() { return "woof"; }
    }

    static class Printer {
        void print(Object o) { System.out.println("   print(Object)"); }
        void print(String s) { System.out.println("   print(String)"); }
        void print(long l)   { System.out.println("   print(long)"); }
        void print(Integer i){ System.out.println("   print(Integer)"); }
        void print(int... xs){ System.out.println("   print(int...)"); }
    }

    static class NullPrinter {
        void print(Object o) { System.out.println("   null -> print(Object)"); }
        void print(String s) { System.out.println("   null -> print(String)"); }
    }

    static class NoHash {
        final String id;
        NoHash(String id) { this.id = id; }
        @Override public boolean equals(Object o) {
            return o instanceof NoHash && ((NoHash) o).id.equals(id);
        }
        // QUÊN override hashCode()
    }

    static class GoodProduct {
        final String id;
        GoodProduct(String id) { this.id = id; }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof GoodProduct)) return false;
            return id.equals(((GoodProduct) o).id);
        }
        @Override public int hashCode() { return Objects.hash(id); }
    }

    public static void main(String[] args) {
        Animal a = new Dog();
        System.out.println("1: " + a.sound());      // runtime dispatch
        System.out.println("2: " + a.name);         // field theo kiểu reference
        System.out.println("3: " + ((Dog) a).name);
        System.out.println("4: " + a.describe());   // name trong Animal + sound() của Dog
        System.out.println("5: " + Animal.kind() + " / " + Dog.kind());

        System.out.println("6: overload resolution");
        Printer p = new Printer();
        p.print(5);        // int: không có print(int) -> widening (long) thắng boxing & varargs
        p.print("s");
        // p.print(null); // COMPILE ERROR: ambiguous – String, Integer, int[] đều nhận null, không cái nào cụ thể hơn
        new NullPrinter().print(null);   // chỉ có Object & String -> chọn String (cụ thể hơn)
        Object o = "s";
        p.print(o);        // overload chọn theo kiểu COMPILE-TIME

        try {
            Animal cat = new Animal();
            Dog dog = (Dog) cat;
        } catch (ClassCastException e) {
            System.out.println("7: ClassCastException");
        }

        Set<NoHash> bad = new HashSet<>();
        bad.add(new NoHash("P1"));
        bad.add(new NoHash("P1"));
        System.out.println("8: size=" + bad.size() + " (thường là 2 vì không override hashCode)");

        Set<GoodProduct> good = new HashSet<>();
        good.add(new GoodProduct("P1"));
        good.add(new GoodProduct("P1"));
        System.out.println("9: size=" + good.size());
    }
}
