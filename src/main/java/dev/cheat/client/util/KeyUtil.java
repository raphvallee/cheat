package dev.cheat.client.util;

import com.mojang.blaze3d.platform.InputConstants;

import java.util.Locale;
import java.util.Map;

/** Converts between key codes ({@code InputConstants.KEY_*}) and readable names. */
public final class KeyUtil {
    /** Key code meaning "not bound". */
    public static final int NONE = InputConstants.UNKNOWN.getValue();

    private static final Map<String, String> ALIASES = Map.ofEntries(
            Map.entry("rshift", "right.shift"), Map.entry("lshift", "left.shift"),
            Map.entry("rctrl", "right.control"), Map.entry("lctrl", "left.control"),
            Map.entry("rcontrol", "right.control"), Map.entry("lcontrol", "left.control"),
            Map.entry("ralt", "right.alt"), Map.entry("lalt", "left.alt"),
            Map.entry("esc", "escape"), Map.entry("return", "enter"),
            Map.entry("del", "delete"), Map.entry("ins", "insert"),
            Map.entry("pgup", "page.up"), Map.entry("pgdown", "page.down"),
            Map.entry("up", "up"), Map.entry("down", "down"), Map.entry("left", "left"), Map.entry("right", "right"));

    private KeyUtil() {}

    public static String name(int key) {
        if (key == NONE) return "None";
        return InputConstants.Type.KEYBOARD.getOrCreate(key).getDisplayName().getString();
    }

    /** Parses names such as {@code r}, {@code f6}, {@code rshift}, {@code right.shift} or {@code key.keyboard.r}. */
    public static int parse(String name) {
        String n = name.toLowerCase(Locale.ROOT).trim();
        if (n.isEmpty() || n.equals("none") || n.equals("null")) return NONE;
        n = ALIASES.getOrDefault(n, n).replace('_', '.');
        if (!n.startsWith("key.")) n = "key.keyboard." + n;
        try {
            return InputConstants.getKey(n).getValue();
        } catch (RuntimeException e) {
            return NONE;
        }
    }
}
