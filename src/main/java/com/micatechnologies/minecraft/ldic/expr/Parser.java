package com.micatechnologies.minecraft.ldic.expr;

import com.micatechnologies.minecraft.ldic.expr.Lexer.Kind;
import com.micatechnologies.minecraft.ldic.expr.Lexer.Token;
import java.util.ArrayList;
import java.util.List;

/**
 * Recursive-descent parser that type-checks while it parses and emits {@link Expr} nodes
 * directly. Names resolve against a {@link Scope} at compile time; any subtree whose inputs are
 * all constant is folded.
 *
 * <p>Precedence, loosest first: {@code ?:}, {@code ||}, {@code &&}, {@code |}, {@code ^},
 * {@code &}, {@code == !=}, {@code < > <= >=}, {@code << >> >>>}, {@code + -},
 * {@code * / %}, unary {@code - + ! ~}, {@code **} (right-associative), then calls,
 * {@code a.f(b)} method sugar for {@code f(a, b)}, and dotted names such as {@code mem.used}.</p>
 */
public final class Parser {

    private final String src;
    private final List<Token> tokens;
    private final Scope scope;
    private int index;

    private Parser(String src, Scope scope) {
        this.src = src;
        this.scope = scope;
        this.tokens = Lexer.tokenize(src);
    }

    /** Parses {@code src} in {@code scope}; the result has whatever type the expression has. */
    public static Expr parse(String src, Scope scope) {
        if (src == null || src.trim().isEmpty()) {
            throw new ExprException("empty expression", src, 0);
        }
        try {
            Parser p = new Parser(src, scope);
            Expr e = p.ternary();
            if (p.peek().kind != Kind.EOF) {
                throw p.error("unexpected " + p.peek());
            }
            return e;
        } catch (NumberFormatException e) {
            throw new ExprException("bad number: " + e.getMessage(), src, 0);
        }
    }

    /**
     * Parses and checks the result type. A string is accepted anywhere: any value converts to
     * one. A number or bool must match exactly.
     */
    public static Expr parse(String src, Scope scope, Type expected) {
        Expr e = parse(src, scope);
        return coerce(e, expected, src);
    }

    static Expr coerce(Expr e, Type expected, String src) {
        if (e.type() == expected) {
            return e;
        }
        if (expected == Type.STRING) {
            return foldIfConstant(new Ops.ToStr(e), e);
        }
        throw new ExprException("expected a " + expected.display() + " but the expression is a " + e.type().display(), src, 0);
    }

    // -------------------------------------------------------------------------------------

    private Token peek() {
        return tokens.get(index);
    }

    private Token next() {
        return tokens.get(index++);
    }

    private boolean acceptOp(String op) {
        if (peek().is(Kind.OP, op)) {
            index++;
            return true;
        }
        return false;
    }

    private Token expect(Kind kind, String what) {
        Token t = peek();
        if (t.kind != kind) {
            throw error("expected " + what + " but found " + t);
        }
        return next();
    }

    private ExprException error(String message) {
        return new ExprException(message, src, peek().pos);
    }

    private ExprException error(String message, int pos) {
        return new ExprException(message, src, pos);
    }

    private static Expr foldIfConstant(Expr node, Expr... operands) {
        for (Expr o : operands) {
            if (!o.isConstant()) {
                return node;
            }
        }
        return Expr.fold(node);
    }

    private Expr ternary() {
        Expr cond = or();
        if (peek().kind != Kind.QUESTION) {
            return cond;
        }
        int pos = next().pos;
        requireType(cond, Type.BOOL, "condition of ?:", pos);
        Expr a = ternary();
        expect(Kind.COLON, "':'");
        Expr b = ternary();
        Type type;
        if (a.type() == b.type()) {
            type = a.type();
        } else if (a.type() == Type.STRING || b.type() == Type.STRING) {
            type = Type.STRING;
        } else {
            throw error("both branches of ?: must have the same type (" + a.type().display() + " vs " + b.type().display() + ")", pos);
        }
        if (cond.isConstant()) {
            Expr chosen = cond.bool() ? a : b;
            return coerce(chosen, type, src);
        }
        return new Ops.Ternary(cond, a, b, type);
    }

    private Expr or() {
        Expr a = and();
        while (peek().is(Kind.OP, "||")) {
            int pos = next().pos;
            Expr b = and();
            requireType(a, Type.BOOL, "'||'", pos);
            requireType(b, Type.BOOL, "'||'", pos);
            a = foldIfConstant(new Ops.Logic(a, b, false), a, b);
        }
        return a;
    }

    private Expr and() {
        Expr a = bitOr();
        while (peek().is(Kind.OP, "&&")) {
            int pos = next().pos;
            Expr b = bitOr();
            requireType(a, Type.BOOL, "'&&'", pos);
            requireType(b, Type.BOOL, "'&&'", pos);
            a = foldIfConstant(new Ops.Logic(a, b, true), a, b);
        }
        return a;
    }

    private Expr bitOr() {
        Expr a = bitXor();
        while (peek().is(Kind.OP, "|")) {
            int pos = next().pos;
            a = bits(Ops.NumOp.OR, a, bitXor(), pos);
        }
        return a;
    }

    private Expr bitXor() {
        Expr a = bitAnd();
        while (peek().is(Kind.OP, "^")) {
            int pos = next().pos;
            a = bits(Ops.NumOp.XOR, a, bitAnd(), pos);
        }
        return a;
    }

    private Expr bitAnd() {
        Expr a = equality();
        while (peek().is(Kind.OP, "&")) {
            int pos = next().pos;
            a = bits(Ops.NumOp.AND, a, equality(), pos);
        }
        return a;
    }

    private Expr bits(Ops.NumOp op, Expr a, Expr b, int pos) {
        if (a.type() == Type.BOOL && b.type() == Type.BOOL) {
            return foldIfConstant(new Ops.BoolBits(op, a, b), a, b);
        }
        requireType(a, Type.NUMBER, "a bitwise operator", pos);
        requireType(b, Type.NUMBER, "a bitwise operator", pos);
        return foldIfConstant(new Ops.Arith(op, a, b), a, b);
    }

    private Expr equality() {
        Expr a = relational();
        while (true) {
            Token t = peek();
            boolean eq = t.is(Kind.OP, "==");
            if (!eq && !t.is(Kind.OP, "!=")) {
                return a;
            }
            next();
            Expr b = relational();
            if (a.type() != b.type()) {
                throw error("cannot compare a " + a.type().display() + " with a " + b.type().display(), t.pos);
            }
            a = foldIfConstant(new Ops.Equals(a, b, !eq), a, b);
        }
    }

    private Expr relational() {
        Expr a = shift();
        while (true) {
            Token t = peek();
            Ops.CmpOp op;
            if (t.is(Kind.OP, "<")) {
                op = Ops.CmpOp.LT;
            } else if (t.is(Kind.OP, ">")) {
                op = Ops.CmpOp.GT;
            } else if (t.is(Kind.OP, "<=")) {
                op = Ops.CmpOp.LE;
            } else if (t.is(Kind.OP, ">=")) {
                op = Ops.CmpOp.GE;
            } else {
                return a;
            }
            next();
            Expr b = shift();
            requireType(a, Type.NUMBER, t.toString(), t.pos);
            requireType(b, Type.NUMBER, t.toString(), t.pos);
            a = foldIfConstant(new Ops.Compare(op, a, b), a, b);
        }
    }

    private Expr shift() {
        Expr a = additive();
        while (true) {
            Token t = peek();
            Ops.NumOp op;
            if (t.is(Kind.OP, "<<")) {
                op = Ops.NumOp.SHL;
            } else if (t.is(Kind.OP, ">>")) {
                op = Ops.NumOp.SHR;
            } else if (t.is(Kind.OP, ">>>")) {
                op = Ops.NumOp.USHR;
            } else {
                return a;
            }
            next();
            a = arith(op, a, additive(), t);
        }
    }

    private Expr additive() {
        Expr a = multiplicative();
        while (true) {
            Token t = peek();
            if (t.is(Kind.OP, "+")) {
                next();
                Expr b = multiplicative();
                if (a.type() == Type.STRING || b.type() == Type.STRING) {
                    a = foldIfConstant(new Ops.Concat(a, b), a, b);
                } else {
                    a = arith(Ops.NumOp.ADD, a, b, t);
                }
            } else if (t.is(Kind.OP, "-")) {
                next();
                a = arith(Ops.NumOp.SUB, a, multiplicative(), t);
            } else {
                return a;
            }
        }
    }

    private Expr multiplicative() {
        Expr a = unary();
        while (true) {
            Token t = peek();
            Ops.NumOp op;
            if (t.is(Kind.OP, "*")) {
                op = Ops.NumOp.MUL;
            } else if (t.is(Kind.OP, "/")) {
                op = Ops.NumOp.DIV;
            } else if (t.is(Kind.OP, "%")) {
                op = Ops.NumOp.MOD;
            } else {
                return a;
            }
            next();
            a = arith(op, a, unary(), t);
        }
    }

    private Expr arith(Ops.NumOp op, Expr a, Expr b, Token t) {
        requireType(a, Type.NUMBER, t.toString(), t.pos);
        requireType(b, Type.NUMBER, t.toString(), t.pos);
        return foldIfConstant(new Ops.Arith(op, a, b), a, b);
    }

    private Expr unary() {
        Token t = peek();
        if (t.is(Kind.OP, "-")) {
            next();
            Expr a = unary();
            requireType(a, Type.NUMBER, "unary '-'", t.pos);
            return foldIfConstant(new Ops.Neg(a), a);
        }
        if (t.is(Kind.OP, "+")) {
            next();
            Expr a = unary();
            requireType(a, Type.NUMBER, "unary '+'", t.pos);
            return a;
        }
        if (t.is(Kind.OP, "!")) {
            next();
            Expr a = unary();
            requireType(a, Type.BOOL, "'!'", t.pos);
            return foldIfConstant(new Ops.Not(a), a);
        }
        if (t.is(Kind.OP, "~")) {
            next();
            Expr a = unary();
            requireType(a, Type.NUMBER, "'~'", t.pos);
            return foldIfConstant(new Ops.BitNot(a), a);
        }
        return power();
    }

    private Expr power() {
        Expr a = postfix();
        Token t = peek();
        if (t.is(Kind.OP, "**")) {
            next();
            Expr b = unary();
            return arith(Ops.NumOp.POW, a, b, t);
        }
        return a;
    }

    private Expr postfix() {
        Expr e = primary();
        while (peek().kind == Kind.DOT) {
            Token dot = next();
            Token name = expect(Kind.IDENT, "a function name after '.'");
            if (peek().kind != Kind.LPAREN) {
                throw error("'.' must be followed by a method call such as .upper()", dot.pos);
            }
            List<Expr> args = new ArrayList<>();
            args.add(e);
            args.addAll(arguments());
            e = call(name.text, args, name.pos);
        }
        return e;
    }

    private Expr primary() {
        Token t = next();
        switch (t.kind) {
            case NUMBER:
                return Expr.constant(t.number);
            case STRING:
                return Expr.constant(t.text);
            case LPAREN: {
                Expr e = ternary();
                expect(Kind.RPAREN, "')'");
                return e;
            }
            case IDENT:
                return identifier(t);
            default:
                index--;
                throw error("expected a value but found " + t);
        }
    }

    private Expr identifier(Token first) {
        if (first.text.equals("true")) {
            return Expr.constant(true);
        }
        if (first.text.equals("false")) {
            return Expr.constant(false);
        }
        if (peek().kind == Kind.LPAREN) {
            return call(first.text, arguments(), first.pos);
        }
        // Dotted names (mem.used) bind as one name when the scope has it; otherwise the dot is
        // method sugar, handled by postfix().
        String name = first.text;
        while (peek().kind == Kind.DOT && tokens.get(index + 1).kind == Kind.IDENT
                && tokens.get(index + 2).kind != Kind.LPAREN) {
            String dotted = name + "." + tokens.get(index + 1).text;
            if (scope.lookup(dotted) == null && !hasDottedPrefix(dotted)) {
                break;
            }
            name = dotted;
            index += 2;
        }
        Slot slot = scope.lookup(name);
        if (slot == null) {
            if (scope.hasFunction(name)) {
                throw error("'" + name + "' is a function; call it as " + name + "(...)", first.pos);
            }
            throw error("unknown name '" + name + "'", first.pos);
        }
        return Expr.read(slot);
    }

    /** True when some visible slot name starts with {@code prefix + "."}, e.g. {@code mem} for {@code mem.used}. */
    private boolean hasDottedPrefix(String prefix) {
        String p = prefix + ".";
        for (Scope s = scope; s != null; s = s.parent()) {
            for (String n : s.localSlots().keySet()) {
                if (n.startsWith(p)) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<Expr> arguments() {
        expect(Kind.LPAREN, "'('");
        List<Expr> args = new ArrayList<>();
        if (peek().kind == Kind.RPAREN) {
            next();
            return args;
        }
        while (true) {
            args.add(ternary());
            if (peek().kind == Kind.COMMA) {
                next();
                continue;
            }
            expect(Kind.RPAREN, "',' or ')'");
            return args;
        }
    }

    private Expr call(String name, List<Expr> args, int pos) {
        Type[] types = new Type[args.size()];
        for (int i = 0; i < types.length; i++) {
            types[i] = args.get(i).type();
        }
        Function fn = scope.resolve(name, types);
        if (fn == null) {
            List<Function> overloads = scope.overloads(name);
            if (overloads.isEmpty()) {
                throw error("unknown function '" + name + "'", pos);
            }
            StringBuilder sb = new StringBuilder("no overload of '").append(name).append("' takes (");
            for (int i = 0; i < types.length; i++) {
                sb.append(i > 0 ? ", " : "").append(types[i].display());
            }
            sb.append("); expected ");
            for (int i = 0; i < overloads.size(); i++) {
                sb.append(i > 0 ? " or " : "").append(overloads.get(i).signature());
            }
            throw error(sb.toString(), pos);
        }
        Expr[] argArray = args.toArray(new Expr[0]);
        Expr node = fn.call(argArray);
        return node.isConstant() ? Expr.fold(node) : node;
    }

    private void requireType(Expr e, Type type, String context, int pos) {
        if (e.type() != type) {
            throw error(context + " needs a " + type.display() + " but got a " + e.type().display(), pos);
        }
    }
}
