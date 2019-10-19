
// public class Add implements Eval {
//     public Integer exec(Integer n1, Integer n2) {
//         return n1 + n2;
//     }
// }

// public class Multiply implements Eval {
//     public Integer exec(Integer n1, Integer n2) {
//         return n1 * n2;
//     }
// }

public class Operate {
    public static class Add implements Eval {
        public Integer exec(Integer n1, Integer n2) {
            return n1 + n2;
        }
    }

    public static class Multiply implements Eval {
        public Integer exec(Integer n1, Integer n2) {
            return n1 * n2;
        }
    }
}