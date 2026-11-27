package dev.cheat.client;

import dev.cheat.client.command.CommandManager;
import dev.cheat.client.config.ConfigManager;
import dev.cheat.client.event.EventBus;
import dev.cheat.client.module.ModuleManager;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Client entry point. Created once from {@link dev.cheat.client.hook.Hooks#onStartup} at the end of the
 * {@link Minecraft} constructor and shut down from {@link Minecraft#close()}.
 */
public final class Client {
    public static final String NAME;
    public static final String VERSION;
    public static final String MINECRAFT_VERSION;
    public static final Logger LOGGER;

    static {
        Properties props = new Properties();
        try (InputStream in = Client.class.getResourceAsStream("/client.properties")) {
            if (in != null) props.load(in);
        } catch (IOException ignored) {
        }
        NAME = props.getProperty("name", "Client");
        VERSION = props.getProperty("version", "dev");
        MINECRAFT_VERSION = props.getProperty("minecraft", "?");
        LOGGER = LoggerFactory.getLogger(NAME);
    }

    private static Client instance;
    private static volatile boolean ready;

    public final EventBus eventBus = new EventBus();
    public final ModuleManager moduleManager;
    public final CommandManager commandManager;
    public final ConfigManager configManager;

    private Client(Minecraft mc) {
        moduleManager = new ModuleManager(eventBus);
        commandManager = new CommandManager();
        configManager = new ConfigManager(mc.gameDirectory.toPath().resolve(NAME.toLowerCase()), moduleManager);
    }

    public static void start(Minecraft mc) {
        if (instance != null) return;
        long started = System.nanoTime();
        Client client = new Client(mc);
        instance = client;
        client.moduleManager.registerDefaults();
        client.commandManager.registerDefaults();
        client.configManager.load();
        ready = true;
        LOGGER.info("{} {} started for Minecraft {} in {} ms ({} modules, {} commands)", NAME, VERSION,
                MINECRAFT_VERSION, (System.nanoTime() - started) / 1_000_000,
                client.moduleManager.getModules().size(), client.commandManager.getCommands().size());
    }

    public void shutdown() {
        configManager.save();
        LOGGER.info("{} shut down", NAME);
    }

    /** The running client, or {@code null} before startup (hooks must check {@link #isReady()}). */
    public static Client get() {
        return instance;
    }

    /** Whether startup completed. Hooks do nothing until then. */
    public static boolean isReady() {
        return ready;
    }

    public static EventBus events() {
        return instance.eventBus;
    }

    public static ModuleManager modules() {
        return instance.moduleManager;
    }

    public static CommandManager commands() {
        return instance.commandManager;
    }

    public static ConfigManager config() {
        return instance.configManager;
    }
}
