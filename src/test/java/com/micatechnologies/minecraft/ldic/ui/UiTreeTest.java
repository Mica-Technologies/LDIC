package com.micatechnologies.minecraft.ldic.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.micatechnologies.minecraft.ldic.expr.Scope;
import com.micatechnologies.minecraft.ldic.expr.Slot;
import com.micatechnologies.minecraft.ldic.expr.Type;
import com.micatechnologies.minecraft.ldic.theme.Json;
import com.micatechnologies.minecraft.ldic.theme.Problems;
import com.micatechnologies.minecraft.ldic.theme.ThemeCompiler;
import com.micatechnologies.minecraft.ldic.theme.ThemeDef;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class UiTreeTest {

    /** Records draws; measures text as 6 units per character, 9 high. */
    private static final class Recorder implements NodeRenderer {
        final List<String> drawn = new ArrayList<>();
        final List<UiNode> hovered = new ArrayList<>();

        @Override
        public void measure(UiNode node) {
            String text = node.def.str("text", "");
            node.width = text.length() * 6;
            node.height = 9;
        }

        @Override
        public void begin(UiNode node) {
        }

        @Override
        public void draw(UiNode node) {
            String text = node.def.has("text") ? node.def.str("text", "") : node.def.type;
            drawn.add(text + "@" + (int) node.x + "," + (int) node.y);
        }

        @Override
        public void end(UiNode node) {
        }

        @Override
        public void hoverStarted(UiNode node) {
            hovered.add(node);
        }
    }

    private Slot status;

    private UiTree tree(String json) {
        Scope host = Scope.withLibrary().child();
        status = host.declare("status", Type.STRING);
        Problems p = new Problems();
        ThemeDef t = ThemeCompiler.compile(Json.parse(json).getAsJsonObject(), host, p);
        assertTrue(p.isEmpty(), p.all().toString());
        return new UiTree(t.screen("main"));
    }

    @Test
    void anchorsAndPivots() {
        Recorder r = new Recorder();
        UiTree t = tree("{ldic:1, elements:["
                + "{type:'rect', anchor:'center', width:100, height:50},"
                + "{type:'rect', anchor:'bottom_right', width:10, height:10},"
                + "{type:'rect', anchor:'top_left', pivot:'center', x:20, y:20, width:10, height:10},"
                + "{type:'text', text:'abcd', anchor:'top', y:5}]}");
        t.frame(r, 400, 200, -1, -1, 0, 0);
        assertEquals("rect@150,75", r.drawn.get(0));
        assertEquals("rect@390,190", r.drawn.get(1));
        assertEquals("rect@15,15", r.drawn.get(2));
        assertEquals("abcd@188,5", r.drawn.get(3));
    }

    @Test
    void childrenLayOutInsideTheirParent() {
        Recorder r = new Recorder();
        UiTree t = tree("{ldic:1, elements:[{type:'rect', x:100, y:50, width:200, height:100,"
                + " children:[{type:'rect', anchor:'bottom_right', width:'50%', height:'pw / 4'}]}]}");
        t.frame(r, 800, 600, -1, -1, 0, 0);
        assertEquals("rect@200,100", r.drawn.get(1));
    }

    @Test
    void factorySpawnsOnTriggerChangeAndStaysBounded() {
        Recorder r = new Recorder();
        UiTree t = tree("{ldic:1, elements:[{type:'factory', trigger:'status', max:3, keep:{line:'status'},"
                + " template:{type:'text', text:'{index}:{line}', y:'index * 10'}}]}");
        status.set("a");
        t.frame(r, 100, 100, -1, -1, 0, 0);
        status.set("b");
        t.frame(r, 100, 100, -1, -1, 1, 1);
        r.drawn.clear();
        t.frame(r, 100, 100, -1, -1, 2, 1);
        assertEquals("factory@0,0", r.drawn.get(0));
        assertEquals("0:b@0,0", r.drawn.get(1));
        assertEquals("1:a@0,10", r.drawn.get(2));
        for (String s : new String[] {"c", "d", "e"}) {
            status.set(s);
            t.frame(r, 100, 100, -1, -1, 3, 1);
        }
        r.drawn.clear();
        t.frame(r, 100, 100, -1, -1, 4, 1);
        assertEquals(4, r.drawn.size());
        assertEquals("2:c@0,20", r.drawn.get(3));
    }

    @Test
    void factoryDestroyWhen() {
        Recorder r = new Recorder();
        UiTree t = tree("{ldic:1, elements:[{type:'factory', trigger:'status', destroy_when:'age > 5', template:{type:'text', text:'{index}'}}]}");
        status.set("a");
        t.frame(r, 100, 100, -1, -1, 0, 0);
        status.set("b");
        t.frame(r, 100, 100, -1, -1, 4, 4);
        r.drawn.clear();
        t.frame(r, 100, 100, -1, -1, 7, 3);
        assertEquals(2, r.drawn.size());
    }

    @Test
    void hoverClickAndFocus() {
        Recorder r = new Recorder();
        UiTree t = tree("{ldic:1, elements:[{type:'button', id:'a', x:0, y:0, action:'quit'},"
                + "{type:'button', id:'b', x:0, y:30, action:'reload', scale:2}]}");
        t.frame(r, 400, 400, 10, 10, 0, 0);
        assertEquals("a", t.hot().def.id);
        t.frame(r, 400, 400, 10, 10, 0.1, 0.1);
        assertEquals(1, r.hovered.size());
        assertSame(t.hot(), t.press());
        assertEquals("a", t.release().def.id);
        // Scaled around its top-left pivot: 400 wide on screen.
        t.frame(r, 400, 400, 350, 50, 0.2, 0.1);
        assertEquals("b", t.hot().def.id);
        t.frame(r, 400, 400, 1000, 1000, 0.3, 0.1);
        assertNull(t.hot());
        assertEquals("a", t.moveFocus(true).def.id);
        assertEquals("b", t.moveFocus(true).def.id);
        assertEquals("a", t.moveFocus(true).def.id);
    }
}
