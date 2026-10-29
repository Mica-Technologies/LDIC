package com.micatechnologies.minecraft.ldic.expr;

import java.util.ArrayList;
import java.util.List;

/**
 * Text with embedded expressions, such as <code>"Memory: {mem.used} MiB"</code>. A literal brace
 * is written doubled. A template with no expressions compiles to a string constant.
 */
public final class Template {

    private Template() {
    }

    public static Expr compile(String text, Scope scope) {
        if (text == null) {
            return Expr.constant("");
        }
        List<Expr> parts = new ArrayList<>();
        StringBuilder literal = new StringBuilder();
        int i = 0;
        int n = text.length();
        while (i < n) {
            char c = text.charAt(i);
            if (c == '{') {
                if (i + 1 < n && text.charAt(i + 1) == '{') {
                    literal.append('{');
                    i += 2;
                    continue;
                }
                int end = findClose(text, i + 1);
                if (end < 0) {
                    throw new ExprException("unclosed '{' in text", text, i);
                }
                if (literal.length() > 0) {
                    parts.add(Expr.constant(literal.toString()));
                    literal.setLength(0);
                }
                String inner = text.substring(i + 1, end);
                try {
                    parts.add(Parser.parse(inner, scope));
                } catch (ExprException e) {
                    throw new ExprException(e.getMessage(), text, i + 1 + Math.max(0, e.getPosition()));
                }
                i = end + 1;
                continue;
            }
            if (c == '}' && i + 1 < n && text.charAt(i + 1) == '}') {
                literal.append('}');
                i += 2;
                continue;
            }
            literal.append(c);
            i++;
        }
        if (literal.length() > 0) {
            parts.add(Expr.constant(literal.toString()));
        }
        if (parts.isEmpty()) {
            return Expr.constant("");
        }
        if (parts.size() == 1) {
            return Parser.coerce(parts.get(0), Type.STRING, text);
        }
        boolean constant = true;
        for (Expr p : parts) {
            constant &= p.isConstant();
        }
        Expr join = new Ops.Join(parts.toArray(new Expr[0]));
        return constant ? Expr.fold(join) : join;
    }

    /** Finds the '}' closing an expression, skipping quoted strings and nested parentheses. */
    private static int findClose(String text, int from) {
        char quote = 0;
        for (int i = from; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quote != 0) {
                if (c == '\\') {
                    i++;
                } else if (c == quote) {
                    quote = 0;
                }
            } else if (c == '\'' || c == '"') {
                quote = c;
            } else if (c == '}') {
                return i;
            }
        }
        return -1;
    }
}
