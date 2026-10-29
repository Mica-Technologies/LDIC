package com.micatechnologies.minecraft.ldic.theme;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.micatechnologies.minecraft.ldic.expr.Expr;
import com.micatechnologies.minecraft.ldic.expr.ExprException;
import com.micatechnologies.minecraft.ldic.expr.Library;
import com.micatechnologies.minecraft.ldic.expr.Parser;
import com.micatechnologies.minecraft.ldic.expr.Scope;
import com.micatechnologies.minecraft.ldic.expr.Slot;
import com.micatechnologies.minecraft.ldic.expr.Template;
import com.micatechnologies.minecraft.ldic.expr.Type;
import com.micatechnologies.minecraft.ldic.theme.ElementSchema.Kind;
import com.micatechnologies.minecraft.ldic.theme.ElementSchema.Prop;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Compiles a theme's (already merged) JSON into a {@link ThemeDef}.
 *
 * <p>The compiler never throws for bad theme content. A bad value is reported to
 * {@link Problems} and replaced by its default; a bad element is reported and skipped with its
 * children. That way one typo costs one element, not the whole menu.</p>
 */
public final class ThemeCompiler {

    /** The newest theme format this build understands. */
    public static final int FORMAT = 1;

    private static final Pattern PERCENT = Pattern.compile("\\s*-?\\d+(\\.\\d+)?\\s*%\\s*");

    private static final Set<String> TOP_LEVEL = new HashSet<>(Arrays.asList(
            "ldic", "name", "author", "description", "flashing", "extends", "design", "anchors",
            "fonts", "constants", "state", "functions", "variables", "screens", "elements",
            "foreign_buttons", "remove"));

    private static final Map<String, Map<String, Kind>> ACTION_ARGS = new HashMap<>();

    static {
        action("open_link", "url", Kind.TEXT, "confirm", Kind.FLAG);
        action("connect", "address", Kind.TEXT, "name", Kind.TEXT);
        action("load_world", "folder", Kind.TEXT);
        action("open", "screen", Kind.TEXT);
        action("back");
        action("quit");
        action("reload");
        action("open_folder", "path", Kind.TEXT);
        action("mod_config", "modid", Kind.TEXT);
        action("sound", "sound", Kind.TEXT, "volume", Kind.NUMBER, "pitch", Kind.NUMBER);
        action("set", "name", Kind.STATIC, "value", Kind.STATIC);
        action("toggle", "name", Kind.STATIC);
        action("chain", "actions", Kind.STATIC);
    }

    private static void action(String type, Object... argKinds) {
        Map<String, Kind> m = new HashMap<>();
        for (int i = 0; i < argKinds.length; i += 2) {
            m.put((String) argKinds[i], (Kind) argKinds[i + 1]);
        }
        ACTION_ARGS.put(type, m);
    }

    private final Problems problems;
    private final Map<String, Anchor> customAnchors = new HashMap<>();
    private ThemeDef def;

    private ThemeCompiler(Problems problems) {
        this.problems = problems;
    }

    /**
     * Compiles {@code root} against {@code hostScope}, which must already hold the built-in
     * library and the host's bindings ({@code time}, {@code screen_width}, ...).
     */
    public static ThemeDef compile(JsonObject root, Scope hostScope, Problems problems) {
        return new ThemeCompiler(problems).run(root, hostScope);
    }

    /** Names of the built-in action types. */
    public static Set<String> actionTypes() {
        return ACTION_ARGS.keySet();
    }

    private ThemeDef run(JsonObject root, Scope hostScope) {
        def = new ThemeDef(problems);
        Scope scope = hostScope.child();
        def.scope = scope;

        JsonElement format = root.get("ldic");
        if (format == null) {
            problems.warn("ldic", "missing format version; assuming \"ldic\": " + FORMAT);
        } else if (!format.isJsonPrimitive() || !format.getAsJsonPrimitive().isNumber()) {
            problems.warn("ldic", "format version must be a number");
        } else if (format.getAsInt() > FORMAT) {
            problems.warn("ldic", "theme format " + format.getAsInt() + " is newer than this LDIC (" + FORMAT + "); some features may be ignored");
        }
        for (Map.Entry<String, JsonElement> e : root.entrySet()) {
            if (!TOP_LEVEL.contains(e.getKey()) && !isComment(e.getKey())) {
                problems.warn(e.getKey(), "unknown top-level key");
            }
        }

        def.name = string(root, "name", def.name);
        def.author = string(root, "author", def.author);
        def.description = string(root, "description", def.description);
        def.flashing = bool(root, "flashing", false);
        String foreign = string(root, "foreign_buttons", "keep");
        if (!foreign.equals("keep") && !foreign.equals("hide")) {
            problems.warn("foreign_buttons", "must be \"keep\" or \"hide\"");
            foreign = "keep";
        }
        def.foreignButtons = foreign;

        design(root);
        anchors(root);
        fonts(root);
        constants(root, scope);
        state(root, scope);
        functions(root, scope);
        variables(root, scope);
        screens(root, scope);
        return def;
    }

    private static boolean isComment(String key) {
        return key.startsWith("_") || key.startsWith("$");
    }

    // -------------------------------------------------------------------------------------
    // Top-level sections

    private void design(JsonObject root) {
        JsonObject d = object(root, "design");
        if (d == null) {
            return;
        }
        String mode = string(d, "mode", "fit").toUpperCase(Locale.ROOT);
        try {
            def.designMode = ThemeDef.DesignMode.valueOf(mode);
        } catch (IllegalArgumentException e) {
            problems.warn("design.mode", "must be one of none, fit, fill, width, height");
        }
        def.designWidth = positive(d, "width", 854, "design.width");
        def.designHeight = positive(d, "height", 480, "design.height");
        def.minScale = number(d, "min", 0);
        def.maxScale = number(d, "max", Double.MAX_VALUE);
    }

    private void anchors(JsonObject root) {
        JsonObject a = object(root, "anchors");
        if (a == null) {
            return;
        }
        for (Map.Entry<String, JsonElement> e : a.entrySet()) {
            Anchor anchor = anchorFromArray(e.getValue(), "anchors." + e.getKey());
            if (anchor != null) {
                customAnchors.put(e.getKey(), anchor);
            }
        }
    }

    private void fonts(JsonObject root) {
        JsonObject f = object(root, "fonts");
        if (f == null) {
            return;
        }
        for (Map.Entry<String, JsonElement> e : f.entrySet()) {
            if (e.getValue().isJsonPrimitive()) {
                def.fonts.put(e.getKey(), e.getValue().getAsString());
            } else {
                problems.warn("fonts." + e.getKey(), "must be a font name or file");
            }
        }
    }

    private void constants(JsonObject root, Scope scope) {
        JsonObject c = object(root, "constants");
        if (c == null) {
            return;
        }
        for (Map.Entry<String, JsonElement> e : c.entrySet()) {
            String path = "constants." + e.getKey();
            if (!checkName(e.getKey(), path)) {
                continue;
            }
            Expr value = anyValue(e.getValue(), scope, path);
            if (value == null) {
                continue;
            }
            // Evaluated once, now: constants may read host bindings that are fixed at load time.
            Slot slot = scope.declare(e.getKey(), value.type());
            slot.assign(value);
        }
    }

    private void state(JsonObject root, Scope scope) {
        JsonObject s = object(root, "state");
        if (s == null) {
            return;
        }
        for (Map.Entry<String, JsonElement> e : s.entrySet()) {
            String path = "state." + e.getKey();
            if (!checkName(e.getKey(), path)) {
                continue;
            }
            JsonElement v = e.getValue();
            if (!v.isJsonPrimitive()) {
                problems.warn(path, "initial state must be a number, bool or string");
                continue;
            }
            JsonPrimitive p = v.getAsJsonPrimitive();
            Slot slot;
            if (p.isBoolean()) {
                slot = scope.declare(e.getKey(), Type.BOOL);
                slot.set(p.getAsBoolean());
            } else if (p.isNumber()) {
                slot = scope.declare(e.getKey(), Type.NUMBER);
                slot.set(p.getAsDouble());
            } else {
                slot = scope.declare(e.getKey(), Type.STRING);
                slot.set(p.getAsString());
            }
            def.state.put(e.getKey(), slot);
        }
    }

    private void functions(JsonObject root, Scope scope) {
        JsonObject f = object(root, "functions");
        if (f == null) {
            return;
        }
        for (Map.Entry<String, JsonElement> e : f.entrySet()) {
            String path = "functions." + e.getKey();
            if (!e.getValue().isJsonPrimitive()) {
                problems.error(path, "function body must be an expression string");
                continue;
            }
            try {
                scope.defineUserFunction(e.getKey(), e.getValue().getAsString());
            } catch (ExprException ex) {
                problems.error(path, ex.describe());
            }
        }
    }

    private void variables(JsonObject root, Scope scope) {
        JsonObject v = object(root, "variables");
        if (v == null) {
            return;
        }
        for (Map.Entry<String, JsonElement> e : v.entrySet()) {
            String path = "variables." + e.getKey();
            if (!checkName(e.getKey(), path)) {
                continue;
            }
            Expr value = anyValue(e.getValue(), scope, path);
            if (value == null) {
                continue;
            }
            // Declared after compiling, so a variable can only read the ones above it.
            Slot slot = scope.declare(e.getKey(), value.type());
            slot.assign(value);
            def.variableSlots.add(slot);
            def.variableExprs.add(value);
        }
    }

    private void screens(JsonObject root, Scope scope) {
        JsonObject screens = object(root, "screens");
        if (screens != null) {
            for (Map.Entry<String, JsonElement> e : screens.entrySet()) {
                String path = "screens." + e.getKey();
                if (!e.getValue().isJsonObject()) {
                    problems.error(path, "a screen must be an object");
                    continue;
                }
                def.screens.put(e.getKey(), screen(e.getKey(), e.getValue().getAsJsonObject(), scope, path));
            }
        }
        JsonElement elements = root.get("elements");
        if (elements != null) {
            if (def.screens.containsKey("main")) {
                problems.warn("elements", "ignored: the theme also defines screens.main");
            } else {
                JsonObject main = new JsonObject();
                main.add("elements", elements);
                def.screens.put("main", screen("main", main, scope, ""));
            }
        }
    }

    private ScreenDef screen(String id, JsonObject o, Scope scope, String path) {
        ScreenDef screen = new ScreenDef(id);
        String prefix = path.isEmpty() ? "" : path + ".";
        for (Map.Entry<String, JsonElement> e : o.entrySet()) {
            String k = e.getKey();
            if (!k.equals("elements") && !k.equals("on_open") && !k.equals("on_escape") && !k.equals("remove") && !isComment(k)) {
                problems.warn(prefix + k, "unknown screen key");
            }
        }
        JsonArray elements = array(o, "elements", prefix + "elements");
        if (elements != null) {
            compileElements(elements, scope, prefix + "elements", screen.elements);
        }
        if (o.has("on_open")) {
            screen.onOpen = action(o.get("on_open"), scope, prefix + "on_open");
        }
        if (o.has("on_escape")) {
            screen.onEscape = action(o.get("on_escape"), scope, prefix + "on_escape");
        }
        return screen;
    }

    private void compileElements(JsonArray array, Scope scope, String path, List<ElementDef> out) {
        for (int i = 0; i < array.size(); i++) {
            String p = path + "[" + i + "]";
            JsonElement el = array.get(i);
            if (!el.isJsonObject()) {
                problems.error(p, "an element must be an object");
                continue;
            }
            ElementDef d = element(el.getAsJsonObject(), scope, p);
            if (d != null) {
                out.add(d);
            }
        }
        // Stable sort: z orders, declaration order breaks ties.
        out.sort((a, b) -> Double.compare(a.z, b.z));
    }

    // -------------------------------------------------------------------------------------
    // Elements

    private ElementDef element(JsonObject o, Scope parentScope, String path) {
        String rawType = string(o, "type", null);
        if (rawType == null) {
            problems.error(path, "element has no \"type\" (one of " + new java.util.TreeSet<>(ElementSchema.typeNames()) + ")");
            return null;
        }
        String type = ElementSchema.canonical(rawType);
        if (type == null) {
            problems.error(path + ".type", "unknown element type '" + rawType + "' (one of " + new java.util.TreeSet<>(ElementSchema.typeNames()) + ")");
            return null;
        }
        String id = string(o, "id", null);
        if (id != null) {
            path = path + "(" + id + ")";
        }
        ElementDef d = new ElementDef(type, id, path);
        Scope scope = parentScope.child();
        d.locals.hover = scope.declare("hover", Type.BOOL);
        d.locals.hoverT = scope.declare("hover_t", Type.NUMBER);
        d.locals.pressed = scope.declare("pressed", Type.BOOL);
        d.locals.focused = scope.declare("focused", Type.BOOL);
        d.locals.parentWidth = scope.declare("pw", Type.NUMBER);
        d.locals.parentHeight = scope.declare("ph", Type.NUMBER);
        d.locals.width = scope.declare("elem_width", Type.NUMBER);
        d.locals.height = scope.declare("elem_height", Type.NUMBER);

        Anchor anchor = anchor(o.get("anchor"), path + ".anchor");
        d.anchor = anchor == null ? Anchor.TOP_LEFT : anchor;
        Anchor pivot = anchor(o.get("pivot"), path + ".pivot");
        d.pivot = pivot == null ? d.anchor : pivot;

        d.x = length(o.get("x"), scope, path + ".x", Length.units(Expr.constant(0)));
        d.y = length(o.get("y"), scope, path + ".y", Length.units(Expr.constant(0)));
        d.width = length(o.get("width"), scope, path + ".width", null);
        d.height = length(o.get("height"), scope, path + ".height", null);
        if (type.equals("button")) {
            if (d.width == null) {
                d.width = Length.units(Expr.constant(200));
            }
            if (d.height == null) {
                d.height = Length.units(Expr.constant(20));
            }
        } else if (!type.equals("text")) {
            if (d.width == null) {
                d.width = Length.fill();
            }
            if (d.height == null) {
                d.height = Length.fill();
            }
        }

        d.visible = typed(o.get("visible"), Type.BOOL, scope, path + ".visible", Expr.constant(true));
        d.enabled = typed(o.get("enabled"), Type.BOOL, scope, path + ".enabled", Expr.constant(true));
        d.opacity = typed(o.get("opacity"), Type.NUMBER, scope, path + ".opacity", Expr.constant(1));
        d.rotation = typed(o.get("rotation"), Type.NUMBER, scope, path + ".rotation", Expr.constant(0));
        d.scale = typed(o.get("scale"), Type.NUMBER, scope, path + ".scale", Expr.constant(1));
        if (o.has("tooltip")) {
            d.tooltip = text(o.get("tooltip"), scope, path + ".tooltip");
        }
        if (o.has("sound_hover")) {
            d.soundHover = text(o.get("sound_hover"), scope, path + ".sound_hover");
        }
        if (o.has("sound_press")) {
            d.soundPress = text(o.get("sound_press"), scope, path + ".sound_press");
        }
        d.z = number(o, "z", 0);
        d.hoverTime = Math.max(0, number(o, "hover_time", 0.15));

        if (type.equals("factory")) {
            if (!factory(d, o, scope, path)) {
                return null;
            }
        } else {
            props(d, o, scope, path);
        }

        if (o.has("action")) {
            d.action = action(o.get("action"), scope, path + ".action");
        }
        JsonArray children = array(o, "children", path + ".children");
        if (children != null) {
            compileElements(children, scope, path + ".children", d.children);
        }
        return d;
    }

    private void props(ElementDef d, JsonObject o, Scope scope, String path) {
        Map<String, Prop> schema = ElementSchema.props(d.type);
        for (Map.Entry<String, JsonElement> e : o.entrySet()) {
            String key = e.getKey();
            if (ElementSchema.COMMON.contains(key) || isComment(key)) {
                continue;
            }
            Prop prop = schema.get(key);
            if (prop == null) {
                problems.warn(path + "." + key, "unknown property for " + d.type + " (known: " + schema.keySet() + ")");
                continue;
            }
            prop(d, prop, e.getValue(), scope, path + "." + key);
        }
        for (Prop prop : schema.values()) {
            if (prop.def == null || d.props.containsKey(prop.name) || d.statics.containsKey(prop.name) || d.lists.containsKey(prop.name)) {
                continue;
            }
            switch (prop.kind) {
                case ENUM:
                case STATIC:
                    d.statics.put(prop.name, String.valueOf(prop.def));
                    break;
                case FLAG:
                    d.statics.put(prop.name, String.valueOf(prop.def));
                    break;
                case TEXT:
                    d.props.put(prop.name, Template.compile(String.valueOf(prop.def), scope));
                    break;
                case BOOL:
                    d.props.put(prop.name, Expr.constant((Boolean) prop.def));
                    break;
                default:
                    d.props.put(prop.name, Expr.constant(((Number) prop.def).doubleValue()));
                    break;
            }
        }
    }

    private void prop(ElementDef d, Prop prop, JsonElement v, Scope scope, String path) {
        switch (prop.kind) {
            case NUMBER:
            case BOOL:
            case COLOUR: {
                Expr e = typed(v, prop.kind == Kind.BOOL ? Type.BOOL : Type.NUMBER, scope, path, null);
                if (e != null) {
                    d.props.put(prop.name, e);
                }
                break;
            }
            case TEXT: {
                Expr e = text(v, scope, path);
                if (e != null) {
                    d.props.put(prop.name, e);
                }
                break;
            }
            case TEXT_LIST: {
                if (!v.isJsonArray()) {
                    problems.warn(path, "must be an array of strings");
                    break;
                }
                List<Expr> list = new ArrayList<>();
                JsonArray a = v.getAsJsonArray();
                for (int i = 0; i < a.size(); i++) {
                    Expr e = text(a.get(i), scope, path + "[" + i + "]");
                    if (e != null) {
                        list.add(e);
                    }
                }
                d.lists.put(prop.name, list);
                break;
            }
            case ENUM: {
                String s = v.isJsonPrimitive() ? v.getAsString().toLowerCase(Locale.ROOT) : null;
                if (s == null || !prop.values.contains(s)) {
                    problems.warn(path, "must be one of " + prop.values);
                    break;
                }
                d.statics.put(prop.name, s);
                break;
            }
            case STATIC:
                if (!v.isJsonPrimitive()) {
                    problems.warn(path, "must be a string");
                    break;
                }
                d.statics.put(prop.name, v.getAsString());
                break;
            case FLAG:
                if (!v.isJsonPrimitive() || !v.getAsJsonPrimitive().isBoolean()) {
                    problems.warn(path, "must be true or false");
                    break;
                }
                d.statics.put(prop.name, String.valueOf(v.getAsBoolean()));
                break;
            case UV: {
                if (!v.isJsonArray() || v.getAsJsonArray().size() != 4) {
                    problems.warn(path, "must be [u0, v0, u1, v1]");
                    break;
                }
                JsonArray a = v.getAsJsonArray();
                Expr[] uv = new Expr[4];
                for (int i = 0; i < 4; i++) {
                    uv[i] = typed(a.get(i), Type.NUMBER, scope, path + "[" + i + "]", null);
                    if (uv[i] == null) {
                        return;
                    }
                }
                for (int i = 0; i < 4; i++) {
                    d.props.put(prop.name + i, uv[i]);
                }
                break;
            }
            default:
                break;
        }
    }

    private boolean factory(ElementDef d, JsonObject o, Scope scope, String path) {
        for (Map.Entry<String, JsonElement> e : o.entrySet()) {
            String key = e.getKey();
            if (!ElementSchema.COMMON.contains(key) && !ElementSchema.props("factory").containsKey(key) && !isComment(key)) {
                problems.warn(path + "." + key, "unknown property for factory");
            }
        }
        FactoryDef f = new FactoryDef();
        if (o.has("trigger") == o.has("count")) {
            problems.error(path, "a factory needs exactly one of \"trigger\" or \"count\"");
            return false;
        }
        if (o.has("trigger")) {
            f.trigger = anyValue(o.get("trigger"), scope, path + ".trigger");
            if (f.trigger == null) {
                return false;
            }
        } else {
            f.count = typed(o.get("count"), Type.NUMBER, scope, path + ".count", null);
            if (f.count == null) {
                return false;
            }
        }
        f.max = (int) Math.max(1, Math.min(1024, number(o, "max", 32)));

        Scope templateScope = scope.child();
        f.index = templateScope.declare("index", Type.NUMBER);
        f.countSlot = templateScope.declare("count", Type.NUMBER);
        f.age = templateScope.declare("age", Type.NUMBER);
        JsonObject keep = object(o, "keep");
        if (keep != null) {
            for (Map.Entry<String, JsonElement> e : keep.entrySet()) {
                String kp = path + ".keep." + e.getKey();
                if (!checkName(e.getKey(), kp)) {
                    continue;
                }
                // Kept values are computed outside the template, when the instance is created.
                Expr value = anyValue(e.getValue(), scope, kp);
                if (value != null) {
                    f.keepSlots.add(templateScope.declare(e.getKey(), value.type()));
                    f.keepExprs.add(value);
                }
            }
        }
        if (o.has("destroy_when")) {
            f.destroyWhen = typed(o.get("destroy_when"), Type.BOOL, templateScope, path + ".destroy_when", null);
        }
        JsonObject template = object(o, "template");
        if (template == null) {
            problems.error(path, "a factory needs a \"template\" element");
            return false;
        }
        f.template = element(template, templateScope, path + ".template");
        if (f.template == null) {
            return false;
        }
        d.factory = f;
        return true;
    }

    // -------------------------------------------------------------------------------------
    // Actions

    private ActionDef action(JsonElement v, Scope scope, String path) {
        if (v.isJsonArray()) {
            ActionDef chain = new ActionDef("chain", path);
            JsonArray a = v.getAsJsonArray();
            for (int i = 0; i < a.size(); i++) {
                ActionDef sub = action(a.get(i), scope, path + "[" + i + "]");
                if (sub != null) {
                    chain.chain.add(sub);
                }
            }
            return chain;
        }
        if (v.isJsonPrimitive()) {
            String type = v.getAsString();
            if (!ACTION_ARGS.containsKey(type)) {
                problems.error(path, "unknown action '" + type + "' (one of " + new java.util.TreeSet<>(ACTION_ARGS.keySet()) + ")");
                return null;
            }
            return new ActionDef(type, path);
        }
        if (!v.isJsonObject()) {
            problems.error(path, "an action must be a name, an object or an array");
            return null;
        }
        JsonObject o = v.getAsJsonObject();
        String type = string(o, "type", null);
        Map<String, Kind> args = type == null ? null : ACTION_ARGS.get(type);
        if (args == null) {
            problems.error(path, "unknown or missing action type '" + type + "' (one of " + new java.util.TreeSet<>(ACTION_ARGS.keySet()) + ")");
            return null;
        }
        ActionDef a = new ActionDef(type, path);
        for (Map.Entry<String, JsonElement> e : o.entrySet()) {
            String key = e.getKey();
            if (key.equals("type") || isComment(key)) {
                continue;
            }
            Kind kind = args.get(key);
            String p = path + "." + key;
            if (kind == null) {
                problems.warn(p, "unknown argument for " + type + " (known: " + args.keySet() + ")");
                continue;
            }
            switch (kind) {
                case TEXT: {
                    Expr t = text(e.getValue(), scope, p);
                    if (t != null) {
                        a.args.put(key, t);
                    }
                    break;
                }
                case NUMBER: {
                    Expr n = typed(e.getValue(), Type.NUMBER, scope, p, null);
                    if (n != null) {
                        a.args.put(key, n);
                    }
                    break;
                }
                case FLAG:
                    if (e.getValue().isJsonPrimitive() && e.getValue().getAsJsonPrimitive().isBoolean()) {
                        a.statics.put(key, String.valueOf(e.getValue().getAsBoolean()));
                    } else {
                        problems.warn(p, "must be true or false");
                    }
                    break;
                default:
                    break;
            }
        }
        switch (type) {
            case "set":
            case "toggle": {
                String name = string(o, "name", null);
                Slot slot = name == null ? null : def.state.get(name);
                if (slot == null) {
                    problems.error(path + ".name", "'" + name + "' is not declared in \"state\"");
                    return null;
                }
                a.target = slot;
                if (type.equals("toggle")) {
                    if (slot.type() != Type.BOOL) {
                        problems.error(path + ".name", "toggle needs a bool state; '" + name + "' is a " + slot.type().display());
                        return null;
                    }
                } else {
                    if (!o.has("value")) {
                        problems.error(path, "set needs a \"value\"");
                        return null;
                    }
                    a.value = typed(o.get("value"), slot.type(), scope, path + ".value", null);
                    if (a.value == null) {
                        return null;
                    }
                }
                break;
            }
            case "chain": {
                JsonArray list = array(o, "actions", path + ".actions");
                if (list != null) {
                    for (int i = 0; i < list.size(); i++) {
                        ActionDef sub = action(list.get(i), scope, path + ".actions[" + i + "]");
                        if (sub != null) {
                            a.chain.add(sub);
                        }
                    }
                }
                break;
            }
            case "open_link":
                require(a, "url");
                break;
            case "connect":
                require(a, "address");
                break;
            case "load_world":
                require(a, "folder");
                break;
            case "open":
                require(a, "screen");
                break;
            case "open_folder":
                require(a, "path");
                break;
            case "mod_config":
                require(a, "modid");
                break;
            case "sound":
                require(a, "sound");
                break;
            default:
                break;
        }
        return a;
    }

    private void require(ActionDef a, String arg) {
        if (!a.args.containsKey(arg)) {
            problems.error(a.path, a.type + " needs \"" + arg + "\"");
        }
    }

    // -------------------------------------------------------------------------------------
    // Values

    /** A value of any type: JSON literal, colour, or expression string. */
    private Expr anyValue(JsonElement v, Scope scope, String path) {
        if (v == null || !v.isJsonPrimitive()) {
            problems.error(path, "must be a number, bool or expression string");
            return null;
        }
        JsonPrimitive p = v.getAsJsonPrimitive();
        if (p.isBoolean()) {
            return Expr.constant(p.getAsBoolean());
        }
        if (p.isNumber()) {
            return Expr.constant(p.getAsDouble());
        }
        try {
            return Parser.parse(p.getAsString(), scope);
        } catch (ExprException e) {
            problems.error(path, e.describe());
            return null;
        }
    }

    /**
     * A value that must have {@code type}: a matching JSON literal, a colour string for numbers,
     * or an expression string. Returns {@code fallback} (after reporting) when it is bad.
     */
    private Expr typed(JsonElement v, Type type, Scope scope, String path, Expr fallback) {
        if (v == null) {
            return fallback;
        }
        if (!v.isJsonPrimitive()) {
            problems.error(path, "must be a " + type.display() + " or an expression string");
            return fallback;
        }
        JsonPrimitive p = v.getAsJsonPrimitive();
        if (p.isBoolean()) {
            if (type == Type.BOOL) {
                return Expr.constant(p.getAsBoolean());
            }
            problems.error(path, "must be a " + type.display() + ", not true/false");
            return fallback;
        }
        if (p.isNumber()) {
            if (type == Type.NUMBER) {
                return Expr.constant(p.getAsDouble());
            }
            if (type == Type.STRING) {
                return Expr.constant(Expr.formatNumber(p.getAsDouble()));
            }
            problems.error(path, "must be a " + type.display() + ", not a number");
            return fallback;
        }
        String s = p.getAsString();
        if (type == Type.NUMBER) {
            Long colour = Library.parseHexColour(s);
            if (colour != null) {
                return Expr.constant(colour);
            }
        }
        try {
            return Parser.parse(s, scope, type);
        } catch (ExprException e) {
            problems.error(path, e.describe());
            return fallback;
        }
    }

    private Expr text(JsonElement v, Scope scope, String path) {
        if (v == null || !v.isJsonPrimitive()) {
            problems.error(path, "must be text");
            return null;
        }
        JsonPrimitive p = v.getAsJsonPrimitive();
        if (!p.isString()) {
            return Expr.constant(p.isNumber() ? Expr.formatNumber(p.getAsDouble()) : p.getAsString());
        }
        try {
            return Template.compile(p.getAsString(), scope);
        } catch (ExprException e) {
            problems.error(path, e.describe());
            return null;
        }
    }

    private Length length(JsonElement v, Scope scope, String path, Length fallback) {
        if (v == null) {
            return fallback;
        }
        if (v.isJsonPrimitive() && v.getAsJsonPrimitive().isString() && PERCENT.matcher(v.getAsString()).matches()) {
            String s = v.getAsString().trim();
            return Length.percent(Double.parseDouble(s.substring(0, s.length() - 1).trim()));
        }
        Expr e = typed(v, Type.NUMBER, scope, path, null);
        return e == null ? fallback : Length.units(e);
    }

    private Anchor anchor(JsonElement v, String path) {
        if (v == null) {
            return null;
        }
        if (v.isJsonArray()) {
            return anchorFromArray(v, path);
        }
        if (v.isJsonPrimitive()) {
            String name = v.getAsString();
            Anchor custom = customAnchors.get(name);
            if (custom != null) {
                return custom;
            }
            Anchor named = Anchor.named(name);
            if (named != null) {
                return named;
            }
        }
        problems.warn(path, "unknown anchor " + v + "; use a name such as \"center\" or \"bottom_right\", a custom anchor, or [fx, fy]");
        return null;
    }

    private Anchor anchorFromArray(JsonElement v, String path) {
        if (v.isJsonArray() && v.getAsJsonArray().size() == 2) {
            JsonArray a = v.getAsJsonArray();
            if (a.get(0).isJsonPrimitive() && a.get(0).getAsJsonPrimitive().isNumber()
                    && a.get(1).isJsonPrimitive() && a.get(1).getAsJsonPrimitive().isNumber()) {
                return new Anchor(a.get(0).getAsDouble(), a.get(1).getAsDouble());
            }
        }
        problems.warn(path, "an anchor array must be [fx, fy] numbers");
        return null;
    }

    private boolean checkName(String name, String path) {
        if (!Scope.isValidName(name) || name.contains(".")) {
            problems.error(path, "'" + name + "' is not a valid name (letters, digits and _ only, not starting with a digit)");
            return false;
        }
        return true;
    }

    private JsonObject object(JsonObject o, String key) {
        JsonElement e = o.get(key);
        if (e == null || e.isJsonNull()) {
            return null;
        }
        if (!e.isJsonObject()) {
            problems.warn(key, "must be an object");
            return null;
        }
        return e.getAsJsonObject();
    }

    private JsonArray array(JsonObject o, String key, String path) {
        JsonElement e = o.get(key);
        if (e == null || e.isJsonNull()) {
            return null;
        }
        if (!e.isJsonArray()) {
            problems.error(path, "must be an array");
            return null;
        }
        return e.getAsJsonArray();
    }

    private String string(JsonObject o, String key, String fallback) {
        JsonElement e = o.get(key);
        return e != null && e.isJsonPrimitive() ? e.getAsString() : fallback;
    }

    private boolean bool(JsonObject o, String key, boolean fallback) {
        JsonElement e = o.get(key);
        return e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isBoolean() ? e.getAsBoolean() : fallback;
    }

    private double number(JsonObject o, String key, double fallback) {
        JsonElement e = o.get(key);
        return e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber() ? e.getAsDouble() : fallback;
    }

    private double positive(JsonObject o, String key, double fallback, String path) {
        double v = number(o, key, fallback);
        if (v <= 0) {
            problems.warn(path, "must be positive");
            return fallback;
        }
        return v;
    }
}
