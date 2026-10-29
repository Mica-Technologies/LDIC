package com.micatechnologies.minecraft.ldic.expr;

import java.util.Arrays;

/**
 * A callable function: a built-in, a host binding (such as {@code mod_loaded}), or a function a
 * theme defines.
 *
 * <p>Built-ins receive their argument nodes unevaluated and evaluate what they need, which keeps
 * calls allocation-free and lets functions such as {@code if_else} stay lazy.</p>
 */
public final class Function {

    @FunctionalInterface
    public interface NumImpl {
        double apply(Expr[] args);
    }

    @FunctionalInterface
    public interface BoolImpl {
        boolean apply(Expr[] args);
    }

    @FunctionalInterface
    public interface StrImpl {
        String apply(Expr[] args);
    }

    private final String name;
    private final Type returnType;
    private final Type[] params;
    private final boolean variadic;
    private final boolean pure;
    private final NumImpl numImpl;
    private final BoolImpl boolImpl;
    private final StrImpl strImpl;
    private final UserFunction user;

    private Function(String name, Type returnType, Type[] params, boolean variadic, boolean pure,
                     NumImpl numImpl, BoolImpl boolImpl, StrImpl strImpl, UserFunction user) {
        this.name = name;
        this.returnType = returnType;
        this.params = params;
        this.variadic = variadic;
        this.pure = pure;
        this.numImpl = numImpl;
        this.boolImpl = boolImpl;
        this.strImpl = strImpl;
        this.user = user;
    }

    public static Function num(String name, boolean pure, NumImpl impl, Type... params) {
        return new Function(name, Type.NUMBER, params, false, pure, impl, null, null, null);
    }

    public static Function bool(String name, boolean pure, BoolImpl impl, Type... params) {
        return new Function(name, Type.BOOL, params, false, pure, null, impl, null, null);
    }

    public static Function str(String name, boolean pure, StrImpl impl, Type... params) {
        return new Function(name, Type.STRING, params, false, pure, null, null, impl, null);
    }

    /** A number function taking one or more arguments, all of the type of the last parameter. */
    public static Function numVariadic(String name, NumImpl impl, Type... params) {
        return new Function(name, Type.NUMBER, params, true, true, impl, null, null, null);
    }

    static Function user(UserFunction fn) {
        return new Function(fn.name, fn.body.type(), fn.paramTypes(), false, false, null, null, null, fn);
    }

    public String name() {
        return name;
    }

    public Type returnType() {
        return returnType;
    }

    public boolean isPure() {
        return pure;
    }

    /** True when these argument types are accepted. */
    public boolean accepts(Type[] argTypes) {
        if (variadic) {
            if (argTypes.length < params.length) {
                return false;
            }
            for (int i = 0; i < argTypes.length; i++) {
                Type want = params[Math.min(i, params.length - 1)];
                if (argTypes[i] != want) {
                    return false;
                }
            }
            return true;
        }
        return Arrays.equals(params, argTypes);
    }

    public String signature() {
        StringBuilder sb = new StringBuilder(name).append('(');
        for (int i = 0; i < params.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(params[i].display());
        }
        if (variadic) {
            sb.append("...");
        }
        return sb.append(')').toString();
    }

    Expr call(Expr[] args) {
        if (user != null) {
            return new UserCall(user, args);
        }
        return new Call(this, args);
    }

    static final class Call extends Expr {
        private final Function fn;
        private final Expr[] args;

        Call(Function fn, Expr[] args) {
            this.fn = fn;
            this.args = args;
        }

        @Override
        public Type type() {
            return fn.returnType;
        }

        @Override
        public double num() {
            return fn.numImpl.apply(args);
        }

        @Override
        public boolean bool() {
            return fn.boolImpl.apply(args);
        }

        @Override
        public String str() {
            return fn.strImpl.apply(args);
        }

        @Override
        public boolean isConstant() {
            if (!fn.pure) {
                return false;
            }
            for (Expr a : args) {
                if (!a.isConstant()) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * A call to a theme-defined function. Arguments are evaluated into this call site's own
     * scratch space before any parameter slot is written, so {@code f(2, f(1))} sees the right
     * values. Recursion is impossible by construction: a function body can only call functions
     * declared before it.
     */
    static final class UserCall extends Expr {
        private final UserFunction fn;
        private final Expr[] args;
        private final Object[] scratch;

        UserCall(UserFunction fn, Expr[] args) {
            this.fn = fn;
            this.args = args;
            this.scratch = new Object[args.length];
        }

        private void bind() {
            for (int i = 0; i < args.length; i++) {
                scratch[i] = args[i].value();
            }
            for (int i = 0; i < args.length; i++) {
                fn.params[i].restore(scratch[i]);
            }
        }

        @Override
        public Type type() {
            return fn.body.type();
        }

        @Override
        public double num() {
            bind();
            return fn.body.num();
        }

        @Override
        public boolean bool() {
            bind();
            return fn.body.bool();
        }

        @Override
        public String str() {
            bind();
            return fn.body.str();
        }
    }

    /** A theme-defined function: parameter slots plus a body compiled against them. */
    static final class UserFunction {
        final String name;
        final Slot[] params;
        Expr body;

        UserFunction(String name, Slot[] params) {
            this.name = name;
            this.params = params;
        }

        Type[] paramTypes() {
            Type[] t = new Type[params.length];
            for (int i = 0; i < params.length; i++) {
                t[i] = params[i].type();
            }
            return t;
        }
    }
}
