package com.micatechnologies.minecraft.ldic.theme;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Problems found while loading a theme. Nothing in a theme is allowed to fail silently: every
 * skipped element, unknown key or bad expression lands here with the file and JSON path it came
 * from, and the client logs them and can show them on screen.
 */
public final class Problems {

    public enum Severity { WARNING, ERROR }

    public static final class Problem {
        public final Severity severity;
        public final String source;
        public final String path;
        public final String message;

        Problem(Severity severity, String source, String path, String message) {
            this.severity = severity;
            this.source = source;
            this.path = path;
            this.message = message;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(severity == Severity.ERROR ? "error" : "warning");
            if (source != null) {
                sb.append(" in ").append(source);
            }
            if (path != null && !path.isEmpty()) {
                sb.append(" at ").append(path);
            }
            return sb.append(": ").append(message).toString();
        }
    }

    private final List<Problem> list = new ArrayList<>();
    private String source;

    /** Sets the file name attached to problems reported from now on. */
    public void setSource(String source) {
        this.source = source;
    }

    public void warn(String path, String message) {
        list.add(new Problem(Severity.WARNING, source, path, message));
    }

    public void error(String path, String message) {
        list.add(new Problem(Severity.ERROR, source, path, message));
    }

    public void addAll(Problems other) {
        list.addAll(other.list);
    }

    public List<Problem> all() {
        return Collections.unmodifiableList(list);
    }

    public boolean isEmpty() {
        return list.isEmpty();
    }

    public int size() {
        return list.size();
    }

    public boolean hasErrors() {
        for (Problem p : list) {
            if (p.severity == Severity.ERROR) {
                return true;
            }
        }
        return false;
    }
}
