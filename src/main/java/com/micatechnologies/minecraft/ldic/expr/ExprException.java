package com.micatechnologies.minecraft.ldic.expr;

/**
 * A parse or type error in an expression, carrying the character offset it was found at so the
 * message can point into the source.
 */
public class ExprException extends RuntimeException {

    private final String source;
    private final int position;

    public ExprException(String message, String source, int position) {
        super(message);
        this.source = source;
        this.position = position;
    }

    public String getSource() {
        return source;
    }

    public int getPosition() {
        return position;
    }

    /** The message followed by the source with a caret under the offending character. */
    public String describe() {
        if (source == null || position < 0) {
            return getMessage();
        }
        int caret = Math.min(position, source.length());
        StringBuilder sb = new StringBuilder(getMessage()).append(" at ").append(caret).append(": ");
        sb.append(source).append('\n');
        sb.append(repeat(' ', getMessage().length() + 6 + String.valueOf(caret).length())).append(repeat(' ', caret)).append('^');
        return sb.toString();
    }

    private static String repeat(char c, int n) {
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) {
            sb.append(c);
        }
        return sb.toString();
    }
}
