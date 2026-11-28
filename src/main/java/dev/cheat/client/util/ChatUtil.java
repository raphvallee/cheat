package dev.cheat.client.util;

import dev.cheat.client.Client;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Client-side chat output. Messages are only shown locally, never sent to the server.
 */
public final class ChatUtil {
    private ChatUtil() {
    }

    public static void info(String message) {
        print(Component.literal(message).withStyle(ChatFormatting.GRAY));
    }

    public static void success(String message) {
        print(Component.literal(message).withStyle(ChatFormatting.GREEN));
    }

    public static void error(String message) {
        print(Component.literal(message).withStyle(ChatFormatting.RED));
    }

    /**
     * Prints {@code component} prefixed with the client name. Safe to call from any thread.
     */
    public static void print(Component component) {
        Minecraft mc = Minecraft.getInstance();
        MutableComponent line = Component.literal("[").withStyle(ChatFormatting.DARK_GRAY)
                                         .append(Component.literal(Client.NAME).withStyle(ChatFormatting.AQUA))
                                         .append(Component.literal("] ").withStyle(ChatFormatting.DARK_GRAY))
                                         .append(component);
        mc.execute(() -> mc.gui.hud.getChat().addClientSystemMessage(line));
    }
}
