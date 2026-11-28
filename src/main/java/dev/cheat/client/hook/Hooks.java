package dev.cheat.client.hook;

import dev.cheat.client.Client;
import dev.cheat.client.event.events.*;
import dev.cheat.client.util.PacketUtil;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.CrashReport;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.main.GameConfig;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;

/**
 * Static entry points called from patched vanilla code (see {@code buildSrc/.../GamePatches.java}).
 *
 * <p>Each hook receives the patched object followed by the original method's arguments. Hooks returning
 * {@code boolean} are cancellable: {@code true} makes the vanilla method return immediately.
 * Keep this class thin: translate into events and let modules do the work.</p>
 *
 * <p>Adding a hook: add an {@code Injection} to {@code GamePatches}, build, and the build error prints the
 * exact signature to add here.</p>
 */
public final class Hooks {
    private static Minecraft mc;

    // Values replaced during MotionEvent PRE, restored after the movement packet was sent.
    private static boolean motionActive;
    private static boolean motionSpoofed;
    private static float realYaw;
    private static float realPitch;
    private static boolean realOnGround;

    private Hooks() {}

    // ---------------------------------------------------------------- lifecycle

    public static void onStartup(Minecraft minecraft, GameConfig config) {
        mc = minecraft;
        try {
            Client.start(minecraft);
        } catch (Throwable t) {
            Client.LOGGER.error("{} failed to start, continuing as vanilla", Client.NAME, t);
        }
    }

    public static void onShutdown(Minecraft minecraft) {
        if (Client.isReady()) Client.get().shutdown();
    }

    public static void onCrash(Minecraft minecraft, CrashReport report) {
        if (!Client.isReady()) return;
        try {
            Client.get().shutdown();
        } catch (Throwable t) {
            Client.LOGGER.error("Could not save the config while crashing", t);
        }
    }

    // ---------------------------------------------------------------- game loop

    public static void onTickPre(Minecraft minecraft) {
        if (Client.isReady()) Client.events().post(new TickEvent(TickEvent.State.PRE));
    }

    public static void onTickPost(Minecraft minecraft) {
        if (Client.isReady()) Client.events().post(new TickEvent(TickEvent.State.POST));
    }

    public static void onRender3D(Minecraft minecraft, boolean advanceGameTime) {
        if (!Client.isReady() || minecraft.level == null || minecraft.player == null) return;
        Client.events().post(new Render3DEvent(minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(true)));
    }

    public static void onSetLevel(Minecraft minecraft, ClientLevel level) {
        if (Client.isReady()) Client.events().post(new WorldChangeEvent(level));
    }

    public static void onDisconnect(Minecraft minecraft, Screen nextScreen, boolean keepResourcePacks, boolean stopSound) {
        if (Client.isReady() && minecraft.level != null) Client.events().post(new WorldChangeEvent(null));
    }

    public static void onClearLevel(Minecraft minecraft, Screen nextScreen) {
        if (Client.isReady() && minecraft.level != null) Client.events().post(new WorldChangeEvent(null));
    }

    // ---------------------------------------------------------------- input

    public static boolean onKeyPress(KeyboardHandler handler, long window, int action, KeyEvent event) {
        if (!Client.isReady() || window != mc.getWindow().handle()) return false;
        return Client.events().post(new KeyInputEvent(event.key(), event.keycode(), action, event.modifiers(),
                mc.gui.screen() != null)).isCancelled();
    }

    public static boolean onMouseButton(MouseHandler handler, long window, MouseButtonInfo info, int action) {
        if (!Client.isReady() || window != mc.getWindow().handle()) return false;
        return Client.events().post(new MouseInputEvent(info.button(), action, info.modifiers(),
                mc.gui.screen() != null)).isCancelled();
    }

    // ---------------------------------------------------------------- rendering

    public static void onRender2D(Hud hud, GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        // Vanilla also runs this with the HUD hidden (F1) and returns early on the loading screen.
        if (!Client.isReady() || hud.isHidden() || mc.gui.screen() instanceof LevelLoadingScreen) return;
        Client.events().post(new Render2DEvent(graphics, deltaTracker));
    }

    // ---------------------------------------------------------------- player

    public static void onPlayerTick(LocalPlayer player) {
        if (Client.isReady()) Client.events().post(new PlayerTickEvent(player));
    }

    public static void onMotionPre(LocalPlayer player) {
        if (!Client.isReady()) return;
        motionActive = true;
        realYaw = player.getYRot();
        realPitch = player.getXRot();
        realOnGround = player.onGround();
        MotionEvent event = Client.events().post(new MotionEvent(MotionEvent.State.PRE,
                player.getX(), player.getY(), player.getZ(), realYaw, realPitch, realOnGround));
        motionSpoofed = event.getYaw() != realYaw || event.getPitch() != realPitch || event.isOnGround() != realOnGround;
        if (motionSpoofed) {
            player.setYRot(event.getYaw());
            player.setXRot(event.getPitch());
            player.setOnGround(event.isOnGround());
        }
    }

    public static void onMotionPost(LocalPlayer player) {
        if (!Client.isReady() || !motionActive) return;
        motionActive = false;
        if (motionSpoofed) {
            player.setYRot(realYaw);
            player.setXRot(realPitch);
            player.setOnGround(realOnGround);
            motionSpoofed = false;
        }
        Client.events().post(new MotionEvent(MotionEvent.State.POST,
                player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot(), player.onGround()));
    }

    public static void onPreTravel(LocalPlayer player) {
        if (Client.isReady()) Client.events().post(new PreTravelEvent(player));
    }

    // ---------------------------------------------------------------- network

    public static boolean onPacketSend(Connection connection, Packet<?> packet, ChannelFutureListener listener, boolean flush) {
        // Singleplayer also runs server-side connections in this JVM; only look at the client's own connection.
        if (!Client.isReady() || connection.getSending() != PacketFlow.SERVERBOUND || PacketUtil.isSilent()) return false;
        return Client.events().post(new PacketSendEvent(packet)).isCancelled();
    }

    public static boolean onPacketReceive(Connection connection, ChannelHandlerContext context, Packet<?> packet) {
        if (!Client.isReady() || connection.getReceiving() != PacketFlow.CLIENTBOUND) return false;
        return Client.events().post(new PacketReceiveEvent(packet)).isCancelled();
    }

    public static boolean onBundlePacket(ClientPacketListener listener, ClientboundBundlePacket bundle) {
        if (!Client.isReady() || !mc.isSameThread()) return false;
        for (Packet<? super ClientGamePacketListener> packet : bundle.subPackets()) {
            if (!Client.events().post(new PacketReceiveEvent(packet)).isCancelled()) packet.handle(listener);
        }
        return true;
    }

    // ---------------------------------------------------------------- chat

    public static boolean onChatInput(ChatScreen screen, String message, boolean addToHistory) {
        if (!Client.isReady()) return false;
        String trimmed = message.trim();
        if (Client.commands().isCommand(trimmed)) {
            if (addToHistory) mc.gui.hud.getChat().addRecentChat(trimmed);
            Client.commands().execute(trimmed);
            return true;
        }
        return Client.events().post(new ChatSendEvent(trimmed)).isCancelled();
    }
}
