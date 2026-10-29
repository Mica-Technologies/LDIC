package com.micatechnologies.minecraft.ldic.expr;

import java.util.ArrayList;
import java.util.List;

/** Splits expression source into tokens. */
final class Lexer {

    enum Kind { NUMBER, STRING, IDENT, OP, LPAREN, RPAREN, COMMA, QUESTION, COLON, DOT, EOF }

    static final class Token {
        final Kind kind;
        final String text;
        final double number;
        final int pos;

        Token(Kind kind, String text, double number, int pos) {
            this.kind = kind;
            this.text = text;
            this.number = number;
            this.pos = pos;
        }

        boolean is(Kind k, String t) {
            return kind == k && text.equals(t);
        }

        @Override
        public String toString() {
            return kind == Kind.EOF ? "end of expression" : "'" + text + "'";
        }
    }

    /** Longest first, so {@code >>>} wins over {@code >>} and {@code >}. */
    private static final String[] OPERATORS = {
        ">>>", "**", "==", "!=", "<=", ">=", "&&", "||", "<<", ">>",
        "+", "-", "*", "/", "%", "<", ">", "!", "~", "&", "|", "^"
    };

    private Lexer() {
    }

    static List<Token> tokenize(String src) {
        List<Token> out = new ArrayList<>();
        int i = 0;
        int n = src.length();
        while (i < n) {
            char c = src.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }
            int start = i;
            if (Character.isDigit(c) || (c == '.' && i + 1 < n && Character.isDigit(src.charAt(i + 1)))) {
                i = number(src, i, out);
                continue;
            }
            if (Character.isLetter(c) || c == '_') {
                while (i < n && (Character.isLetterOrDigit(src.charAt(i)) || src.charAt(i) == '_')) {
                    i++;
                }
                out.add(new Token(Kind.IDENT, src.substring(start, i), 0, start));
                continue;
            }
            if (c == '\'' || c == '"') {
                i = string(src, i, out);
                continue;
            }
            switch (c) {
                case '(':
                    out.add(new Token(Kind.LPAREN, "(", 0, i++));
                    continue;
                case ')':
                    out.add(new Token(Kind.RPAREN, ")", 0, i++));
                    continue;
                case ',':
                    out.add(new Token(Kind.COMMA, ",", 0, i++));
                    continue;
                case '?':
                    out.add(new Token(Kind.QUESTION, "?", 0, i++));
                    continue;
                case ':':
                    out.add(new Token(Kind.COLON, ":", 0, i++));
                    continue;
                case '.':
                    out.add(new Token(Kind.DOT, ".", 0, i++));
                    continue;
                default:
                    break;
            }
            String op = null;
            for (String candidate : OPERATORS) {
                if (src.startsWith(candidate, i)) {
                    op = candidate;
                    break;
                }
            }
            if (op == null) {
                throw new ExprException("unexpected character '" + c + "'", src, i);
            }
            out.add(new Token(Kind.OP, op, 0, i));
            i += op.length();
        }
        out.add(new Token(Kind.EOF, "", 0, n));
        return out;
    }

    private static int number(String src, int i, List<Token> out) {
        int start = i;
        int n = src.length();
        if (src.startsWith("0x", i) || src.startsWith("0X", i)) {
            i += 2;
            StringBuilder digits = new StringBuilder();
            while (i < n && (isHex(src.charAt(i)) || src.charAt(i) == '_')) {
                if (src.charAt(i) != '_') {
                    digits.append(src.charAt(i));
                }
                i++;
            }
            if (digits.length() == 0 || digits.length() > 15) {
                throw new ExprException("bad hex literal", src, start);
            }
            out.add(new Token(Kind.NUMBER, src.substring(start, i), Long.parseLong(digits.toString(), 16), start));
            return i;
        }
        StringBuilder digits = new StringBuilder();
        boolean seenDot = false;
        boolean seenExp = false;
        while (i < n) {
            char c = src.charAt(i);
            if (Character.isDigit(c)) {
                digits.append(c);
            } else if (c == '_') {
                // digit separator
            } else if (c == '.' && !seenDot && !seenExp && i + 1 < n && Character.isDigit(src.charAt(i + 1))) {
                seenDot = true;
                digits.append(c);
            } else if ((c == 'e' || c == 'E') && !seenExp && i + 1 < n
                    && (Character.isDigit(src.charAt(i + 1))
                        || ((src.charAt(i + 1) == '-' || src.charAt(i + 1) == '+') && i + 2 < n && Character.isDigit(src.charAt(i + 2))))) {
                seenExp = true;
                digits.append(c).append(src.charAt(i + 1));
                i++;
            } else {
                break;
            }
            i++;
        }
        out.add(new Token(Kind.NUMBER, src.substring(start, i), Double.parseDouble(digits.toString()), start));
        return i;
    }

    private static int string(String src, int i, List<Token> out) {
        char quote = src.charAt(i);
        int start = i++;
        StringBuilder sb = new StringBuilder();
        int n = src.length();
        while (true) {
            if (i >= n) {
                throw new ExprException("unterminated string", src, start);
            }
            char c = src.charAt(i++);
            if (c == quote) {
                break;
            }
            if (c == '\\') {
                if (i >= n) {
                    throw new ExprException("unterminated string", src, start);
                }
                char e = src.charAt(i++);
                switch (e) {
                    case 'n':
                        sb.append('\n');
                        break;
                    case 't':
                        sb.append('\t');
                        break;
                    case '\\':
                    case '\'':
                    case '"':
                        sb.append(e);
                        break;
                    case 'u':
                        if (i + 4 > n) {
                            throw new ExprException("bad \\u escape", src, i - 2);
                        }
                        sb.append((char) Integer.parseInt(src.substring(i, i + 4), 16));
                        i += 4;
                        break;
                    default:
                        throw new ExprException("unknown escape '\\" + e + "'", src, i - 2);
                }
            } else {
                sb.append(c);
            }
        }
        out.add(new Token(Kind.STRING, sb.toString(), 0, start));
        return i;
    }

    private static boolean isHex(char c) {
        return Character.isDigit(c) || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }
}
