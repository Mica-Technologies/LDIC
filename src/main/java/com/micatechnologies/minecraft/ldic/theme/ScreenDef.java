package com.micatechnologies.minecraft.ldic.theme;

import java.util.ArrayList;
import java.util.List;

/** One screen of a theme: its element tree plus optional open and escape actions. */
public final class ScreenDef {

    public final String id;
    public final List<ElementDef> elements = new ArrayList<>();
    public ActionDef onOpen;
    /** Run on Escape; null means "go back" (or nothing on the main screen). */
    public ActionDef onEscape;

    ScreenDef(String id) {
        this.id = id;
    }
}
