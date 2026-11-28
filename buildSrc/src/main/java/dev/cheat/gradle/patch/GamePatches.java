package dev.cheat.gradle.patch;

import dev.cheat.gradle.patch.Injection.At;

import java.util.List;

/**
 * Every modification made to vanilla Minecraft. Each entry inserts one call into
 * {@code dev.cheat.client.hook.Hooks}. The build fails with a precise message when a target or a hook
 * signature no longer matches (for example after a Minecraft update).
 *
 * <p>Read vanilla code with {@code ./gradlew genSources}, then find descriptors with
 * {@code javap -p -s -cp <jar> <class>}.</p>
 */
public final class GamePatches {
    public static final String HOOKS = "dev/cheat/client/hook/Hooks";

    private static final String MINECRAFT = "net/minecraft/client/Minecraft";
    private static final String LOCAL_PLAYER = "net/minecraft/client/player/LocalPlayer";
    private static final String CONNECTION = "net/minecraft/network/Connection";

    private GamePatches() {}

    public static List<Injection> all() {
        return List.of(
                // Lifecycle
                Injection.into(MINECRAFT).method("<init>", "(Lnet/minecraft/client/main/GameConfig;)V")
                        .at(At.RETURN).call("onStartup"),
                Injection.into(MINECRAFT).method("close", "()V")
                        .at(At.HEAD).call("onShutdown"),
                Injection.into(MINECRAFT).method("emergencySaveAndCrash", "(Lnet/minecraft/CrashReport;)V")
                        .at(At.HEAD).call("onCrash"),

                // Game loop
                Injection.into(MINECRAFT).method("tick", "()V")
                        .at(At.HEAD).call("onTickPre"),
                Injection.into(MINECRAFT).method("tick", "()V")
                        .at(At.RETURN).call("onTickPost"),

                // World rendering: runs while the per-frame gizmo collector is open, so Gizmos.* draws in-world
                Injection.into(MINECRAFT).method("renderFrame", "(Z)V")
                        .beforeInvoke("net/minecraft/client/renderer/GameRenderer", "extract",
                                "(Lnet/minecraft/client/DeltaTracker;Z)V")
                        .call("onRender3D"),

                // World changes
                Injection.into(MINECRAFT).method("setLevel", "(Lnet/minecraft/client/multiplayer/ClientLevel;)V")
                        .at(At.RETURN).call("onSetLevel"),
                Injection.into(MINECRAFT).method("disconnect", "(Lnet/minecraft/client/gui/screens/Screen;ZZ)V")
                        .at(At.HEAD).call("onDisconnect"),
                Injection.into(MINECRAFT).method("clearClientLevel", "(Lnet/minecraft/client/gui/screens/Screen;)V")
                        .at(At.HEAD).call("onClearLevel"),

                // Input
                Injection.into("net/minecraft/client/KeyboardHandler")
                        .method("keyPress", "(JILnet/minecraft/client/input/KeyEvent;)V")
                        .at(At.HEAD).cancellable().call("onKeyPress"),
                Injection.into("net/minecraft/client/MouseHandler")
                        .method("onButton", "(JLnet/minecraft/client/input/MouseButtonInfo;I)V")
                        .at(At.HEAD).cancellable().call("onMouseButton"),

                // HUD rendering
                Injection.into("net/minecraft/client/gui/Hud")
                        .method("extractRenderState", "(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V")
                        .at(At.RETURN).call("onRender2D"),

                // Player
                Injection.into(LOCAL_PLAYER).method("tick", "()V")
                        .at(At.HEAD).call("onPlayerTick"),
                Injection.into(LOCAL_PLAYER).method("sendChanges", "()V")
                        .beforeInvoke(LOCAL_PLAYER, "isPassenger", "()Z").call("onMotionPre"),
                Injection.into(LOCAL_PLAYER).method("sendChanges", "()V")
                        .at(At.RETURN).call("onMotionPost"),

                // Network
                Injection.into(CONNECTION)
                        .method("send", "(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;Z)V")
                        .at(At.HEAD).cancellable().call("onPacketSend"),
                Injection.into(CONNECTION)
                        .method("channelRead0", "(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;)V")
                        .at(At.HEAD).cancellable().call("onPacketReceive"),
                Injection.into("net/minecraft/client/multiplayer/ClientPacketListener")
                        .method("handleBundlePacket", "(Lnet/minecraft/network/protocol/game/ClientboundBundlePacket;)V")
                        .at(At.HEAD).cancellable().call("onBundlePacket"),

                // Chat (client commands)
                Injection.into("net/minecraft/client/gui/screens/ChatScreen")
                        .method("handleChatInput", "(Ljava/lang/String;Z)V")
                        .at(At.HEAD).cancellable().call("onChatInput")
        );
    }
}
