<img src="assets/icon.svg" width="64" height="64" alt="">

# Cheat

A client template for Minecraft **26.3** in the style of LiquidBounce. It is built by patching the vanilla game jar, so it needs no Fabric or Forge loader.

## How it works

1. Gradle downloads the vanilla 26.3 client jar and its libraries from Mojang and caches them in `~/.gradle/caches/client-template`. Since 26.1 the jar ships unobfuscated, so no mappings are needed.
2. `patchJar` uses ASM to insert calls to `dev.cheat.client.hook.Hooks` into vanilla methods. The list of patches is in [`GamePatches.java`](buildSrc/src/main/java/dev/cheat/gradle/patch/GamePatches.java). It also removes Mojang's signature and merges the client classes into the jar.
3. `createVersion` writes a standalone launcher version, `build/version/Cheat-26.3/` (a `.json` and a `.jar`).

If a patch target or a hook signature stops matching, for example after a Minecraft update, the build fails. The error names the missing method and gives the exact `Hooks` signature it expects.

## Requirements

- JDK 25 to run Gradle. The Minecraft Launcher ships one at `C:\Program Files (x86)\Minecraft Launcher\runtime\java-runtime-epsilon\windows-x64\java-runtime-epsilon`.
- If no JDK 25 is installed, the foojay plugin provisions one for compilation.

```sh
export JAVA_HOME="/c/Program Files (x86)/Minecraft Launcher/runtime/java-runtime-epsilon/windows-x64/java-runtime-epsilon"
```

## Tasks

| Command | What it does |
| --- | --- |
| `./gradlew build` | Builds the patched jar and the launcher version folder |
| `./gradlew runClient` | Starts an offline dev session with `run/` as the game directory |
| `./gradlew runClient -PquickWorld="New World"` | Same, but joins a world from `run/saves` directly |
| `./gradlew installClient` | Copies the version into `.minecraft/versions` and adds a launcher installation. Launcher profile files are backed up to `*.bak` first |
| `./gradlew genSources` | Decompiles Minecraft into `build/minecraft-sources` so you can read it |

The client name, version and Minecraft version are set in `gradle.properties`.

## In game

- **Right Shift** opens the ClickGUI.
  - Left-click a module to toggle it. Right-click it to show its settings and key bind.
  - Drag a panel by its header. Right-click a header to collapse the panel.
  - Scroll to move through long panels. Right-click a slider to reset it.
  - Set the GUI size with ClickGUI > Scale: click the value, type a number (0.5-2) and press Enter. The scale is capped so the GUI always fits in the window.
- Chat commands use the `.` prefix:
  - `.help`
  - `.t <module>` toggles a module.
  - `.bind <module> <key|none>` sets or clears a key bind.
  - `.set <module> [setting] [value]` lists or changes settings.
  - `.hide <module>` hides a module from the HUD list.
  - `.config <save|load|list> [name]` manages named configs.
- The config is stored in `<gameDir>/cheat/config.json`. It is saved when the GUI closes and when the game exits.

## Layout

```
buildSrc/                     Gradle side: Mojang downloads, ASM patcher, version/install tasks
  patch/GamePatches.java      every vanilla modification (add hooks here)
src/main/java/dev/cheat/client/
  Client.java                 startup / shutdown, managers
  hook/Hooks.java             entry points called by patched vanilla code -> events
  event/                      EventBus, @Subscribe, events/
  module/                     Module, Category, ModuleManager, setting/, impl/
  command/                    Command, CommandManager, impl/
  config/ConfigManager.java   JSON save/load
  gui/clickgui/               ClickGuiScreen, Panel, Theme, component/
  util/                       ChatUtil, PacketUtil, RenderUtil, KeyUtil, ColorUtil, TimerUtil
```

## Events

| Event | Fired from | Notes |
| --- | --- | --- |
| `TickEvent` (PRE/POST) | `Minecraft.tick` | every client tick |
| `PlayerTickEvent` | `LocalPlayer.tick` | in world only, main module logic |
| `MotionEvent` (PRE/POST) | `LocalPlayer.sendPosition` | yaw, pitch and onGround can be spoofed in PRE |
| `PacketSendEvent` | `Connection.send` | cancellable, client connection only |
| `PacketReceiveEvent` | `Connection.channelRead0` | cancellable, **Netty thread** |
| `KeyInputEvent` / `MouseInputEvent` | `KeyboardHandler.keyPress` / `MouseHandler.onButton` | cancellable |
| `Render2DEvent` | `Hud.extractRenderState` | draw with `GuiGraphicsExtractor`, ARGB colors |
| `Render3DEvent` | `Minecraft.renderFrame` | draw with `Gizmos` / `RenderUtil.box` in world coordinates |
| `ChatSendEvent` | `ChatScreen.handleChatInput` | cancellable |
| `WorldChangeEvent` | `Minecraft.setLevel` / `clearClientLevel` | level is `null` when leaving |

## Adding a module

```java
public final class Fly extends Module {
    private final NumberSetting speed = number("Speed", 1, 0.1, 5, 0.1);
    private final ModeSetting mode = mode("Mode", "Vanilla", "Vanilla", "Glide");
    private final BooleanSetting glideFast = bool("Fast glide", false).visibleIf(() -> mode.is("Glide"));

    public Fly() {
        super("Fly", "Lets you fly.", Category.MOVEMENT);
    }

    @Subscribe
    private void onTick(PlayerTickEvent event) {
        // ...
    }

    @Override
    protected void onDisable() {
        // called outside of a world too: null-check mc.player
    }
}
```

Then register it in `ModuleManager.registerDefaults()`. It appears in the GUI, the config and the commands automatically. The bundled modules double as examples:

- `Sprint` uses `PlayerTickEvent`.
- `NoFall` uses `MotionEvent`.
- `Velocity` uses packets, modes, `visibleIf` and threading.
- `ESP` uses `Render3DEvent`.
- `HUD` uses `Render2DEvent`.

## Adding a hook

1. Add an `Injection` to `GamePatches.all()`, with HEAD, RETURN or before an invoke, and optionally `cancellable()`.
2. Run `./gradlew build`. The error prints the exact `public static ... onX(...)` signature that `Hooks` needs.
3. Add that method to `Hooks`, then post your event from it.

Use `genSources` and `javap -p -s` to find method names and descriptors.

## Notes

- The title screen shows "Minecraft 26.3 (Modded)" because the jar is no longer Mojang-signed.
- `runClient` uses an offline account, so it works in singleplayer and on offline-mode servers. For online servers, use `installClient` and start the game from the official launcher.
- Using a client like this on servers that forbid it can get your account banned.
