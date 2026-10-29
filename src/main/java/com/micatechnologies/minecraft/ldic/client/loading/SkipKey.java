package com.micatechnologies.minecraft.ldic.client.loading;

import com.micatechnologies.minecraft.ldic.Ldic;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import java.util.Locale;
import org.lwjgl.input.Keyboard;

/**
 * Whether the player is holding Space, read from the loading-screen thread.
 *
 * <p>On Windows, LWJGL cannot tell: the game window belongs to the main thread, and Windows
 * delivers its key messages to that thread's queue, which nobody pumps while the game loads.
 * So there LDIC asks Windows directly ({@code GetAsyncKeyState}, through the JNA that Minecraft
 * ships), and only while the game window is the one in front, so typing elsewhere never skips
 * anything. Elsewhere LWJGL's keyboard state works from this thread.</p>
 */
final class SkipKey {

    private interface User32 extends Library {
        short GetAsyncKeyState(int virtualKey);

        Pointer GetForegroundWindow();

        int GetWindowThreadProcessId(Pointer window, IntByReference processId);
    }

    private interface Kernel32 extends Library {
        int GetCurrentProcessId();
    }

    private static final int VK_SPACE = 0x20;

    private static final User32 USER32;
    private static final int PROCESS_ID;

    static {
        User32 user32 = null;
        int pid = 0;
        if (System.getProperty("os.name", "").toLowerCase(Locale.ROOT).startsWith("windows")) {
            try {
                user32 = (User32) Native.loadLibrary("user32", User32.class);
                pid = ((Kernel32) Native.loadLibrary("kernel32", Kernel32.class)).GetCurrentProcessId();
            } catch (Throwable t) {
                Ldic.LOGGER.warn("LDIC: cannot read the keyboard while loading; intro screens cannot be skipped", t);
                user32 = null;
            }
        }
        USER32 = user32;
        PROCESS_ID = pid;
    }

    private SkipKey() {
    }

    static boolean spaceDown() {
        if (USER32 != null) {
            Pointer front = USER32.GetForegroundWindow();
            if (front == null) {
                return false;
            }
            IntByReference owner = new IntByReference();
            USER32.GetWindowThreadProcessId(front, owner);
            return owner.getValue() == PROCESS_ID && (USER32.GetAsyncKeyState(VK_SPACE) & 0x8000) != 0;
        }
        return Keyboard.isCreated() && Keyboard.isKeyDown(Keyboard.KEY_SPACE);
    }
}
