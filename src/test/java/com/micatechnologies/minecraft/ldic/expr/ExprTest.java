package com.micatechnologies.minecraft.ldic.expr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ExprTest {

    private Scope scope;
    private Slot time;
    private Slot status;

    @BeforeEach
    void setUp() {
        scope = Scope.withLibrary().child();
        time = scope.declare("time", Type.NUMBER);
        status = scope.declare("status", Type.STRING);
        scope.declare("mem.used", Type.NUMBER).set(512);
        scope.declare("ready", Type.BOOL);
    }

    private double num(String src) {
        return Parser.parse(src, scope, Type.NUMBER).num();
    }

    private boolean bool(String src) {
        return Parser.parse(src, scope, Type.BOOL).bool();
    }

    private String str(String src) {
        return Parser.parse(src, scope, Type.STRING).str();
    }

    @Test
    void arithmeticPrecedence() {
        assertEquals(7, num("1 + 2 * 3"));
        assertEquals(9, num("(1 + 2) * 3"));
        assertEquals(-4, num("-2 ** 2"));
        assertEquals(512, num("2 ** 3 ** 2"));
        assertEquals(1, num("7 % 3"));
        assertEquals(2.5, num("5 / 2"));
    }

    @Test
    void numberLiterals() {
        assertEquals(255, num("0xFF"));
        assertEquals(0xFF1144CCL, (long) num("0xFF11_44CC"));
        assertEquals(1000000, num("1_000_000"));
        assertEquals(0.5, num(".5"));
        assertEquals(1500, num("1.5e3"));
    }

    @Test
    void bitwiseOnColours() {
        assertEquals(0xFF, (long) num("0xFF1144CC >>> 24"));
        assertEquals(0x1144CC, (long) num("0xFF1144CC & 0xFFFFFF"));
        assertEquals(0x80FFFFFFL, (long) num("(0x80 << 24) | 0xFFFFFF"));
    }

    @Test
    void comparisonsAndLogic() {
        assertTrue(bool("1 < 2 && 2 <= 2"));
        assertTrue(bool("!(1 > 2) || false"));
        assertTrue(bool("'a' == 'a'"));
        assertTrue(bool("1 != 2"));
        assertFalse(bool("true ^ true"));
    }

    @Test
    void stringConcatenationFormatsNumbers() {
        assertEquals("50%", str("50 + '%'"));
        assertEquals("v1.5", str("'v' + 1.5"));
        assertEquals("0.3333", str("'' + 1/3"));
        assertEquals("ready: false", str("'ready: ' + ready"));
    }

    @Test
    void ternaryTypes() {
        time.set(5);
        assertEquals(1, num("time > 3 ? 1 : 0"));
        assertEquals("x5", str("time > 3 ? 'x' + time : 0"));
        assertThrows(ExprException.class, () -> Parser.parse("ready ? 1 : true", scope));
    }

    @Test
    void slotsAreLive() {
        Expr e = Parser.parse("time * 2", scope, Type.NUMBER);
        time.set(1);
        assertEquals(2, e.num());
        time.set(10);
        assertEquals(20, e.num());
    }

    @Test
    void constantsFold() {
        assertTrue(Parser.parse("1 + 2 * sin(90)", scope).isConstant());
        assertFalse(Parser.parse("time + 1", scope).isConstant());
        assertTrue(Parser.parse("ready && false || true", scope).type() == Type.BOOL);
    }

    @Test
    void dottedNamesAndMethodSugar() {
        assertEquals(512, num("mem.used"));
        status.set("Pre Initialization");
        assertEquals("PRE INITIALIZATION", str("status.upper()"));
        assertEquals("Pre...", str("status.ellipsize(3)"));
        assertEquals(1024, num("mem.used.max(1024)"));
    }

    @Test
    void library() {
        assertEquals(1, num("sin(90)"), 1e-12);
        assertEquals(5, num("clamp(9, 0, 5)"));
        assertEquals(3, num("max(1, 3, 2)"));
        assertEquals(0.5, num("smoothstep(0, 1, 0.5)"));
        assertEquals(2, num("mod(-1, 3)"));
        assertEquals("b", str("pick('a|b|c', 4)"));
        assertEquals("007", str("pad_left('7', 3, '0')"));
        assertEquals("3.14", str("fixed(pi, 2)"));
        assertEquals(0xFF336699L, (long) num("rgb(0x33, 0x66, 0x99)"));
        assertEquals(0x80336699L, (long) num("with_alpha(rgb(0x33, 0x66, 0x99), 128/255)"));
        assertEquals(0xFFFF0000L, (long) num("hsv(0, 1, 1)"));
        assertEquals(0xFF1144CCL, (long) num("hex('#1144CC')"));
        assertEquals(num("random(42)"), num("random(42)"));
    }

    @Test
    void userFunctions() {
        scope.defineUserFunction("double(x)", "x * 2");
        scope.defineUserFunction("label(string s, bool loud)", "loud ? s.upper() : s");
        scope.defineUserFunction("quad(x)", "double(double(x))");
        assertEquals(12, num("double(6)"));
        assertEquals(12, num("quad(3)"));
        assertEquals("HI", str("label('hi', true)"));
        // Arguments are all evaluated before parameters bind.
        scope.defineUserFunction("sub(a, b)", "a - b");
        assertEquals(1, num("sub(2, sub(2, 1))"));
    }

    @Test
    void userFunctionsCannotRecurse() {
        assertThrows(ExprException.class, () -> scope.defineUserFunction("loop(x)", "loop(x)"));
    }

    @Test
    void templates() {
        time.set(3.5);
        assertEquals("Memory: 512 MiB", Template.compile("Memory: {mem.used} MiB", scope).str());
        assertEquals("t=3.5s", Template.compile("t={time}s", scope).str());
        assertEquals("{literal}", Template.compile("{{literal}}", scope).str());
        assertEquals("a}b", Template.compile("{'a}b'}", scope).str());
        assertTrue(Template.compile("plain", scope).isConstant());
    }

    @Test
    void errorsPointAtTheProblem() {
        ExprException e = assertThrows(ExprException.class, () -> Parser.parse("1 + nope", scope));
        assertEquals(4, e.getPosition());
        assertTrue(e.getMessage().contains("unknown name 'nope'"));
        e = assertThrows(ExprException.class, () -> Parser.parse("clamp('a', 1, 2)", scope));
        assertTrue(e.getMessage().contains("clamp(number, number, number)"), e.getMessage());
        assertThrows(ExprException.class, () -> Parser.parse("1 +", scope));
        assertThrows(ExprException.class, () -> Parser.parse("'unterminated", scope));
        assertThrows(ExprException.class, () -> Parser.parse("ready + 1", scope, Type.NUMBER));
        assertThrows(ExprException.class, () -> Parser.parse("1", scope, Type.BOOL));
    }
}
