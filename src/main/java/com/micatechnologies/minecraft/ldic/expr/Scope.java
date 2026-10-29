package com.micatechnologies.minecraft.ldic.expr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Names visible to an expression: slots and functions, chained to a parent scope.
 *
 * <p>A theme compiles against a tree of scopes: the root holds the built-in library and host
 * bindings, the theme scope holds its constants, variables and functions, and each element gets
 * a child scope for its local values ({@code hover}, {@code elem_width}, ...). Inner names shadow
 * outer ones.</p>
 */
public class Scope {

    private static final Pattern NAME = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)*");
    private static final Pattern SIGNATURE = Pattern.compile("\\s*([A-Za-z_][A-Za-z0-9_]*)\\s*\\((.*)\\)\\s*");

    private final Scope parent;
    private final Map<String, Slot> slots = new LinkedHashMap<>();
    private final Map<String, List<Function>> functions = new LinkedHashMap<>();

    public Scope(Scope parent) {
        this.parent = parent;
    }

    /** A root scope with the built-in function library installed. */
    public static Scope withLibrary() {
        Scope s = new Scope(null);
        Library.install(s);
        return s;
    }

    public Scope child() {
        return new Scope(this);
    }

    public Scope parent() {
        return parent;
    }

    public static boolean isValidName(String name) {
        return name != null && NAME.matcher(name).matches();
    }

    /** Declares a slot in this scope, replacing any slot of the same name declared here. */
    public Slot declare(String name, Type type) {
        if (!isValidName(name)) {
            throw new ExprException("invalid name '" + name + "'", name, 0);
        }
        Slot slot = new Slot(name, type);
        slots.put(name, slot);
        return slot;
    }

    /** The slot declared in this scope (not a parent), or null. */
    public Slot local(String name) {
        return slots.get(name);
    }

    public Slot lookup(String name) {
        for (Scope s = this; s != null; s = s.parent) {
            Slot slot = s.slots.get(name);
            if (slot != null) {
                return slot;
            }
        }
        return null;
    }

    public void define(Function fn) {
        functions.computeIfAbsent(fn.name(), k -> new ArrayList<>()).add(fn);
    }

    /** The innermost overload of {@code name} accepting {@code argTypes}, or null. */
    public Function resolve(String name, Type[] argTypes) {
        for (Scope s = this; s != null; s = s.parent) {
            List<Function> list = s.functions.get(name);
            if (list != null) {
                for (Function fn : list) {
                    if (fn.accepts(argTypes)) {
                        return fn;
                    }
                }
            }
        }
        return null;
    }

    /** Every overload of {@code name} visible from here, innermost first; for error messages. */
    public List<Function> overloads(String name) {
        List<Function> out = new ArrayList<>();
        for (Scope s = this; s != null; s = s.parent) {
            List<Function> list = s.functions.get(name);
            if (list != null) {
                out.addAll(list);
            }
        }
        return out;
    }

    public boolean hasFunction(String name) {
        return !overloads(name).isEmpty();
    }

    public Map<String, Slot> localSlots() {
        return Collections.unmodifiableMap(slots);
    }

    /**
     * Defines a theme function from a signature such as {@code "fade(number t, bool on)"} and a
     * body expression. Untyped parameters are numbers. The body can see this scope, so it can
     * call functions defined before it, but not itself.
     */
    public Function defineUserFunction(String signature, String body) {
        Matcher m = SIGNATURE.matcher(signature);
        if (!m.matches()) {
            throw new ExprException("function signature must look like name(type a, type b)", signature, 0);
        }
        String name = m.group(1);
        String paramList = m.group(2).trim();
        Scope bodyScope = child();
        List<Slot> params = new ArrayList<>();
        if (!paramList.isEmpty()) {
            for (String p : paramList.split(",")) {
                String[] parts = p.trim().split("\\s+");
                Type type;
                String pname;
                if (parts.length == 1) {
                    type = Type.NUMBER;
                    pname = parts[0];
                } else if (parts.length == 2) {
                    type = Type.parse(parts[0]);
                    pname = parts[1];
                    if (type == null) {
                        throw new ExprException("unknown parameter type '" + parts[0] + "'", signature, signature.indexOf(parts[0]));
                    }
                } else {
                    throw new ExprException("bad parameter '" + p.trim() + "'", signature, signature.indexOf(p));
                }
                if (bodyScope.local(pname) != null) {
                    throw new ExprException("duplicate parameter '" + pname + "'", signature, signature.indexOf(pname));
                }
                params.add(bodyScope.declare(pname, type));
            }
        }
        Function.UserFunction uf = new Function.UserFunction(name, params.toArray(new Slot[0]));
        uf.body = Parser.parse(body, bodyScope);
        Function fn = Function.user(uf);
        define(fn);
        return fn;
    }
}
