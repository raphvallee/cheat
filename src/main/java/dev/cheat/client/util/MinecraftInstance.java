package dev.cheat.client.util;

import net.minecraft.client.Minecraft;

/** Implement to get a short {@code mc} reference. Only use from classes created after the game started. */
public interface MinecraftInstance {
    Minecraft mc = Minecraft.getInstance();
}
