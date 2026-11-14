package com.micatechnologies.minecraft.ldic.client.menu;

import com.micatechnologies.minecraft.ldic.Ldic;
import com.micatechnologies.minecraft.ldic.api.MenuPage;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;

/**
 * A mod's {@link MenuPage} as a plain vanilla-style screen: for when the menu theme is turned
 * off or failed to load, so a mod's settings are reachable whatever state LDIC's menus are in.
 * One column of rows, with Previous/Next when they do not all fit.
 */
final class ClassicPageScreen extends GuiScreen {

    private static final int DONE = 0;
    private static final int PREVIOUS = 1;
    private static final int NEXT = 2;
    private static final int FIRST_ROW = 100;
    private static final int ROW_HEIGHT = 24;
    private static final int TOP = 36;
    private static final int COLUMN = 240;

    private final MenuPage page;
    private final GuiScreen parent;
    private final List<MenuPage.Row> rows;
    private int first;
    private int perPage = 1;
    private boolean failureLogged;
    /** Another page was asked for; the buttons are rebuilt at the start of the next frame. */
    private boolean rebuild;

    ClassicPageScreen(MenuPage page, GuiScreen parent) {
        this.page = page;
        this.parent = parent;
        this.rows = page.rows();
    }

    @Override
    public void initGui() {
        buttonList.clear();
        perPage = Math.max(1, (height - TOP - 40) / ROW_HEIGHT);
        first = Math.max(0, Math.min(first, Math.max(0, rows.size() - 1)) / perPage * perPage);
        int x = (width - COLUMN) / 2;
        for (int i = first; i < Math.min(rows.size(), first + perPage); i++) {
            MenuPage.Row row = rows.get(i);
            int y = TOP + (i - first) * ROW_HEIGHT;
            switch (row.kind()) {
                case TOGGLE:
                case CHOICE:
                case BUTTON:
                    buttonList.add(new GuiButton(FIRST_ROW + i, x, y, COLUMN, 20, ""));
                    break;
                case SLIDER:
                    buttonList.add(new Slider(FIRST_ROW + i, x, y, row));
                    break;
                default:
                    break;
            }
        }
        boolean paged = rows.size() > perPage;
        int bottom = height - 28;
        if (paged) {
            GuiButton previous = new GuiButton(PREVIOUS, width / 2 - 154, bottom, 100, 20, "<");
            GuiButton next = new GuiButton(NEXT, width / 2 + 54, bottom, 100, 20, ">");
            previous.enabled = first > 0;
            next.enabled = first + perPage < rows.size();
            buttonList.add(previous);
            buttonList.add(next);
        }
        buttonList.add(new GuiButton(DONE, width / 2 - 50, bottom, 100, 20, I18n.format("gui.done")));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == DONE) {
            close();
        } else if (button.id == PREVIOUS && !rebuild) {
            // The buttons are rebuilt before the next frame, not here: GuiScreen is still walking
            // buttonList for this click, and a rebuilt button under the pointer would fire too.
            first = Math.max(0, first - perPage);
            rebuild = true;
        } else if (button.id == NEXT && !rebuild) {
            if (first + perPage < rows.size()) {
                first += perPage;
            }
            rebuild = true;
        } else if (button.id >= FIRST_ROW && button.id - FIRST_ROW < rows.size() && !(button instanceof Slider)) {
            try {
                rows.get(button.id - FIRST_ROW).activate();
            } catch (RuntimeException e) {
                failed(e);
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        if (keyCode == org.lwjgl.input.Keyboard.KEY_ESCAPE) {
            close();
        }
    }

    private void close() {
        mc.displayGuiScreen(parent);
        if (parent == null && mc.world != null) {
            mc.setIngameFocus();
        }
    }

    @Override
    public void onGuiClosed() {
        try {
            page.closed();
        } catch (RuntimeException e) {
            failed(e);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (rebuild) {
            rebuild = false;
            initGui();
        }
        drawDefaultBackground();
        String tooltip = null;
        try {
            drawCenteredString(fontRenderer, page.title(), width / 2, 15, 0xFFFFFF);
            int x = (width - COLUMN) / 2;
            for (int i = first; i < Math.min(rows.size(), first + perPage); i++) {
                MenuPage.Row row = rows.get(i);
                int y = TOP + (i - first) * ROW_HEIGHT;
                if (row.kind() == MenuPage.Kind.SECTION) {
                    drawString(fontRenderer, row.label(), x, y + 8, 0xFFFFA0);
                } else if (row.kind() == MenuPage.Kind.TEXT) {
                    drawString(fontRenderer, row.label(), x, y + 6, 0xC0C0C0);
                }
                if (mouseX >= x && mouseX < x + COLUMN && mouseY >= y && mouseY < y + 20 && !row.tooltip().isEmpty()) {
                    tooltip = row.tooltip();
                }
            }
            for (GuiButton b : buttonList) {
                int index = b.id - FIRST_ROW;
                if (index >= 0 && index < rows.size()) {
                    MenuPage.Row row = rows.get(index);
                    b.displayString = PageView.text(row);
                    b.enabled = row.enabled();
                }
            }
        } catch (RuntimeException e) {
            failed(e);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
        if (tooltip != null) {
            drawHoveringText(tooltip, mouseX, mouseY);
        }
    }

    private void failed(RuntimeException e) {
        if (!failureLogged) {
            failureLogged = true;
            Ldic.LOGGER.error("LDIC: a mod's menu page failed (logged once per page)", e);
        }
    }

    /** A vanilla-look slider over a page row, dragged like vanilla's option sliders. */
    private static final class Slider extends GuiButton {

        private final MenuPage.Row row;
        private boolean dragging;

        Slider(int id, int x, int y, MenuPage.Row row) {
            super(id, x, y, COLUMN, 20, "");
            this.row = row;
        }

        @Override
        protected int getHoverState(boolean mouseOver) {
            return 0;
        }

        @Override
        protected void mouseDragged(Minecraft mc, int mouseX, int mouseY) {
            if (!visible) {
                return;
            }
            if (dragging) {
                set(mouseX);
            }
            double f;
            try {
                f = row.fraction();
            } catch (RuntimeException e) {
                f = 0;
            }
            mc.getTextureManager().bindTexture(BUTTON_TEXTURES);
            GlStateManager.color(1, 1, 1, 1);
            int knob = x + (int) (f * (width - 8));
            drawTexturedModalRect(knob, y, 0, 66, 4, 20);
            drawTexturedModalRect(knob + 4, y, 196, 66, 4, 20);
        }

        @Override
        public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
            if (super.mousePressed(mc, mouseX, mouseY)) {
                dragging = true;
                set(mouseX);
                return true;
            }
            return false;
        }

        @Override
        public void mouseReleased(int mouseX, int mouseY) {
            dragging = false;
        }

        private void set(int mouseX) {
            try {
                row.setFraction((mouseX - (x + 4)) / (double) (width - 8));
            } catch (RuntimeException e) {
                dragging = false;
            }
        }
    }
}
