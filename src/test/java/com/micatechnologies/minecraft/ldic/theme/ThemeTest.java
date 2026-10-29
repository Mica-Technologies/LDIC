package com.micatechnologies.minecraft.ldic.theme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.micatechnologies.minecraft.ldic.expr.Scope;
import com.micatechnologies.minecraft.ldic.expr.Type;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ThemeTest {

    private static final class MapSource implements ThemeSource {
        final Map<String, String> files = new HashMap<>();

        @Override
        public Document open(String reference) throws IOException {
            String text = files.get(reference);
            if (text == null) {
                throw new FileNotFoundException(reference);
            }
            String location = reference.startsWith("builtin:") ? "builtin/" + reference.substring(8) : "config/themes/" + reference;
            return new Document(location, reference, text);
        }
    }

    private static Scope host() {
        Scope s = Scope.withLibrary().child();
        s.declare("time", Type.NUMBER);
        s.declare("screen_width", Type.NUMBER).set(854);
        return s;
    }

    private static ThemeDef compile(String json, Problems problems) {
        return ThemeCompiler.compile(Json.parse(json).getAsJsonObject(), host(), problems);
    }

    @Test
    void mergeById() {
        JsonObject parent = Json.parse("{a:1, list:[{id:'x', v:1, keep:true}, {id:'y', v:2}], plain:[1,2]}").getAsJsonObject();
        JsonObject child = Json.parse("{a:2, list:[{id:'x', v:9}, {id:'z', v:3}], plain:[3], remove:['y']}").getAsJsonObject();
        JsonObject m = JsonMerge.merge(parent, child);
        assertEquals(2, m.get("a").getAsInt());
        assertEquals(2, m.getAsJsonArray("list").size());
        JsonObject x = m.getAsJsonArray("list").get(0).getAsJsonObject();
        assertEquals(9, x.get("v").getAsInt());
        assertTrue(x.get("keep").getAsBoolean());
        assertEquals("z", m.getAsJsonArray("list").get(1).getAsJsonObject().get("id").getAsString());
        assertEquals(1, m.getAsJsonArray("plain").size());
        // The parent is not modified.
        assertEquals(2, parent.getAsJsonArray("list").size());
    }

    @Test
    void extendsChainRewritesAssetsPerFile() {
        MapSource src = new MapSource();
        src.files.put("builtin:base", "{ldic:1, elements:[{id:'logo', type:'image', image:'logo.png'}]}");
        src.files.put("mine", "{ldic:1, extends:'builtin:base', elements:[{id:'bg', type:'image', image:'bg.png'}, {id:'web', type:'image', image:'https://x/y.png'}]}");
        Problems p = new Problems();
        ThemeLoader.Result r = ThemeLoader.load(src, uncheckedOpen(src, "mine"), p);
        assertNotNull(r);
        assertTrue(p.isEmpty(), p.all().toString());
        assertEquals("theme://builtin/base/logo.png", image(r.json, 0));
        assertEquals("theme://config/themes/mine/bg.png", image(r.json, 1));
        assertEquals("https://x/y.png", image(r.json, 2));
    }

    @Test
    void extendsCycleIsReported() {
        MapSource src = new MapSource();
        src.files.put("a", "{extends:'b'}");
        src.files.put("b", "{extends:'a'}");
        Problems p = new Problems();
        assertNotNull(ThemeLoader.load(src, uncheckedOpen(src, "a"), p));
        assertTrue(p.hasErrors());
    }

    @Test
    void brokenJsonFailsTheLoad() {
        MapSource src = new MapSource();
        src.files.put("bad", "{elements: [");
        Problems p = new Problems();
        assertNull(ThemeLoader.load(src, uncheckedOpen(src, "bad"), p));
        assertTrue(p.hasErrors());
    }

    @Test
    void compilesElementsWithDefaults() {
        Problems p = new Problems();
        ThemeDef t = compile("{ldic:1, name:'T', constants:{accent:'#1144CC'}, variables:{pulse:'wave(time, 2)'},"
                + "elements:[{type:'rect', id:'bar', anchor:'bottom', width:'100%', height:5, colour:'accent'},"
                + "{type:'text', text:'{fixed(pulse, 1)}', align:'center'}, {type:'button', text:'Play', action:'quit'}]}", p);
        assertTrue(p.isEmpty(), p.all().toString());
        assertEquals("T", t.name);
        ScreenDef main = t.screen("main");
        assertEquals(3, main.elements.size());
        ElementDef bar = main.elements.get(0);
        assertEquals(0xFF1144CCL, (long) bar.num("colour", 0));
        assertEquals(1, bar.pivot.fy);
        ElementDef text = main.elements.get(1);
        assertNull(text.width);
        assertEquals("center", text.statik("align", null));
        assertEquals(9, text.num("size", 0));
        ElementDef button = main.elements.get(2);
        assertEquals(200, button.width.resolve(0));
        assertEquals("quit", button.action.type);
    }

    @Test
    void badValuesAreReportedNotFatal() {
        Problems p = new Problems();
        ThemeDef t = compile("{ldic:1, elements:[{type:'nope'}, {type:'rect', colour:'undefined_name', bogus:1},"
                + "{type:'button', action:{type:'open_link'}}, {type:'image', fit:'sideways'}]}", p);
        assertEquals(3, t.screen("main").elements.size());
        assertTrue(p.size() >= 4, p.all().toString());
        // The bad colour fell back to the default.
        assertEquals(0, t.screen("main").elements.get(0).num("colour", -1));
    }

    @Test
    void stateAndSetActions() {
        Problems p = new Problems();
        ThemeDef t = compile("{ldic:1, state:{tab:0, open:false}, elements:[{type:'button', action:[{type:'set', name:'tab', value:'tab + 1'}, {type:'toggle', name:'open'}]}]}", p);
        assertTrue(p.isEmpty(), p.all().toString());
        ActionDef chain = t.screen("main").elements.get(0).action;
        assertEquals("chain", chain.type);
        assertEquals(2, chain.chain.size());
        assertEquals(t.state.get("tab"), chain.chain.get(0).target);
    }

    @Test
    void designScaling() {
        Problems p = new Problems();
        ThemeDef t = compile("{ldic:1, design:{mode:'fit', width:400, height:200}, elements:[]}", p);
        assertEquals(2, t.uiScale(800, 1000));
        assertEquals(1, t.uiScale(400, 200));
    }

    private static String image(JsonObject json, int i) {
        return json.getAsJsonArray("elements").get(i).getAsJsonObject().get("image").getAsString();
    }

    private static ThemeSource.Document uncheckedOpen(ThemeSource src, String ref) {
        try {
            return src.open(ref);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }
}
