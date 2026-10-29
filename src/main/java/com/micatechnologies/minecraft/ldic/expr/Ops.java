package com.micatechnologies.minecraft.ldic.expr;

/**
 * Operator nodes. Each class handles one operand-type combination; the parser picks the class
 * once at compile time, so evaluation never inspects types.
 */
final class Ops {

    private Ops() {
    }

    enum NumOp { ADD, SUB, MUL, DIV, MOD, POW, AND, OR, XOR, SHL, SHR, USHR }

    enum CmpOp { LT, GT, LE, GE }

    static final class Arith extends Expr {
        private final NumOp op;
        private final Expr a;
        private final Expr b;

        Arith(NumOp op, Expr a, Expr b) {
            this.op = op;
            this.a = a;
            this.b = b;
        }

        @Override
        public Type type() {
            return Type.NUMBER;
        }

        @Override
        public double num() {
            double x = a.num();
            double y = b.num();
            switch (op) {
                case ADD:
                    return x + y;
                case SUB:
                    return x - y;
                case MUL:
                    return x * y;
                case DIV:
                    return x / y;
                case MOD:
                    return x % y;
                case POW:
                    return Math.pow(x, y);
                case AND:
                    return (double) ((long) x & (long) y);
                case OR:
                    return (double) ((long) x | (long) y);
                case XOR:
                    return (double) ((long) x ^ (long) y);
                case SHL:
                    return (double) ((long) x << (long) y);
                case SHR:
                    return (double) ((long) x >> (long) y);
                default:
                    // Unsigned shift on the 32-bit pattern, which is what colour maths wants.
                    return (double) (((long) x & 0xFFFFFFFFL) >>> (long) y);
            }
        }
    }

    static final class Neg extends Expr {
        private final Expr a;

        Neg(Expr a) {
            this.a = a;
        }

        @Override
        public Type type() {
            return Type.NUMBER;
        }

        @Override
        public double num() {
            return -a.num();
        }
    }

    static final class BitNot extends Expr {
        private final Expr a;

        BitNot(Expr a) {
            this.a = a;
        }

        @Override
        public Type type() {
            return Type.NUMBER;
        }

        @Override
        public double num() {
            return (double) (~(long) a.num());
        }
    }

    static final class Not extends Expr {
        private final Expr a;

        Not(Expr a) {
            this.a = a;
        }

        @Override
        public Type type() {
            return Type.BOOL;
        }

        @Override
        public boolean bool() {
            return !a.bool();
        }
    }

    static final class Compare extends Expr {
        private final CmpOp op;
        private final Expr a;
        private final Expr b;

        Compare(CmpOp op, Expr a, Expr b) {
            this.op = op;
            this.a = a;
            this.b = b;
        }

        @Override
        public Type type() {
            return Type.BOOL;
        }

        @Override
        public boolean bool() {
            double x = a.num();
            double y = b.num();
            switch (op) {
                case LT:
                    return x < y;
                case GT:
                    return x > y;
                case LE:
                    return x <= y;
                default:
                    return x >= y;
            }
        }
    }

    /** {@code ==} / {@code !=} over two operands of the same type. */
    static final class Equals extends Expr {
        private final Expr a;
        private final Expr b;
        private final boolean negate;

        Equals(Expr a, Expr b, boolean negate) {
            this.a = a;
            this.b = b;
            this.negate = negate;
        }

        @Override
        public Type type() {
            return Type.BOOL;
        }

        @Override
        public boolean bool() {
            boolean eq;
            switch (a.type()) {
                case NUMBER:
                    eq = a.num() == b.num();
                    break;
                case BOOL:
                    eq = a.bool() == b.bool();
                    break;
                default:
                    eq = a.str().equals(b.str());
                    break;
            }
            return eq != negate;
        }
    }

    /** Short-circuit {@code &&} / {@code ||}. */
    static final class Logic extends Expr {
        private final Expr a;
        private final Expr b;
        private final boolean isAnd;

        Logic(Expr a, Expr b, boolean isAnd) {
            this.a = a;
            this.b = b;
            this.isAnd = isAnd;
        }

        @Override
        public Type type() {
            return Type.BOOL;
        }

        @Override
        public boolean bool() {
            return isAnd ? a.bool() && b.bool() : a.bool() || b.bool();
        }
    }

    /** Non-short-circuit {@code &}, {@code |}, {@code ^} on booleans. */
    static final class BoolBits extends Expr {
        private final Expr a;
        private final Expr b;
        private final NumOp op;

        BoolBits(NumOp op, Expr a, Expr b) {
            this.op = op;
            this.a = a;
            this.b = b;
        }

        @Override
        public Type type() {
            return Type.BOOL;
        }

        @Override
        public boolean bool() {
            boolean x = a.bool();
            boolean y = b.bool();
            switch (op) {
                case AND:
                    return x & y;
                case OR:
                    return x | y;
                default:
                    return x ^ y;
            }
        }
    }

    /** String {@code +}: either side may be any type, formatted with {@link Expr#asString()}. */
    static final class Concat extends Expr {
        private final Expr a;
        private final Expr b;

        Concat(Expr a, Expr b) {
            this.a = a;
            this.b = b;
        }

        @Override
        public Type type() {
            return Type.STRING;
        }

        @Override
        public String str() {
            return a.asString().concat(b.asString());
        }
    }

    /** Wraps any expression as a string, for string contexts such as text templates. */
    static final class ToStr extends Expr {
        private final Expr a;

        ToStr(Expr a) {
            this.a = a;
        }

        @Override
        public Type type() {
            return Type.STRING;
        }

        @Override
        public String str() {
            return a.asString();
        }
    }

    /** Concatenation of many parts, used by text templates to avoid a chain of pairs. */
    static final class Join extends Expr {
        private final Expr[] parts;

        Join(Expr[] parts) {
            this.parts = parts;
        }

        @Override
        public Type type() {
            return Type.STRING;
        }

        @Override
        public String str() {
            StringBuilder sb = new StringBuilder();
            for (Expr p : parts) {
                sb.append(p.asString());
            }
            return sb.toString();
        }
    }

    static final class Ternary extends Expr {
        private final Expr cond;
        private final Expr a;
        private final Expr b;
        private final Type type;

        Ternary(Expr cond, Expr a, Expr b, Type type) {
            this.cond = cond;
            this.a = a;
            this.b = b;
            this.type = type;
        }

        @Override
        public Type type() {
            return type;
        }

        @Override
        public double num() {
            return cond.bool() ? a.num() : b.num();
        }

        @Override
        public boolean bool() {
            return cond.bool() ? a.bool() : b.bool();
        }

        @Override
        public String str() {
            return cond.bool() ? a.asString() : b.asString();
        }
    }
}
