package dev.cheat.client.module.impl.client;

import dev.cheat.client.Client;
import dev.cheat.client.event.Subscribe;
import dev.cheat.client.event.events.Render2DEvent;
import dev.cheat.client.module.Category;
import dev.cheat.client.module.Module;
import dev.cheat.client.module.setting.BooleanSetting;
import dev.cheat.client.module.setting.ModeSetting;
import dev.cheat.client.util.RenderUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Example: 2D overlay with {@link Render2DEvent}: watermark, enabled module list and coordinates. */
public final class HudModule extends Module {
    private final BooleanSetting watermark = bool("Watermark", true);
    private final BooleanSetting arrayList = bool("Module list", true);
    private final ModeSetting sort = mode("Sort", "Length", "Length", "Alphabetical").visibleIf(arrayList::isEnabled);
    private final BooleanSetting background = bool("Background", true).visibleIf(arrayList::isEnabled);
    private final BooleanSetting coords = bool("Coordinates", true);

    public HudModule() {
        super("HUD", "Shows client information on screen.", Category.CLIENT);
        setHidden(true);
        enableByDefault();
    }

    @Subscribe
    private void onRender2D(Render2DEvent event) {
        GuiGraphicsExtractor g = event.getGraphics();
        if (watermark.isEnabled()) {
            RenderUtil.text(g, Client.NAME, 4, 4, ClickGuiModule.accent(0), true);
            RenderUtil.text(g, " " + Client.VERSION, 4 + RenderUtil.width(Client.NAME), 4, 0xFFAAAAAA, true);
        }
        if (arrayList.isEnabled()) drawModuleList(g, event.getWidth());
        if (coords.isEnabled() && mc.player != null) {
            String text = String.format(Locale.ROOT, "XYZ %.1f %.1f %.1f", mc.player.getX(), mc.player.getY(), mc.player.getZ());
            RenderUtil.text(g, text, 4, event.getHeight() - RenderUtil.fontHeight() - 4, 0xFFFFFFFF, true);
        }
    }

    private void drawModuleList(GuiGraphicsExtractor g, int screenWidth) {
        Comparator<Module> order = sort.is("Length")
                ? Comparator.comparingInt((Module m) -> RenderUtil.width(label(m))).reversed()
                : Comparator.comparing(Module::getName, String.CASE_INSENSITIVE_ORDER);
        List<Module> shown = Client.modules().getModules().stream()
                .filter(m -> m.isEnabled() && !m.isHidden())
                .sorted(order)
                .toList();
        int y = 2;
        int lineHeight = RenderUtil.fontHeight() + 2;
        for (int i = 0; i < shown.size(); i++) {
            Module module = shown.get(i);
            int color = ClickGuiModule.accent(i * 150L);
            String tag = module.getTag();
            int width = RenderUtil.width(label(module));
            int x = screenWidth - width - 4;
            if (background.isEnabled()) RenderUtil.rect(g, x - 2, y, width + 4, lineHeight, 0x90000000);
            RenderUtil.rect(g, screenWidth - 2, y, 2, lineHeight, color);
            RenderUtil.text(g, module.getName(), x, y + 1, color, true);
            if (tag != null) {
                RenderUtil.text(g, " " + tag, x + RenderUtil.width(module.getName()), y + 1, 0xFFAAAAAA, true);
            }
            y += lineHeight;
        }
    }

    private static String label(Module module) {
        String tag = module.getTag();
        return tag == null ? module.getName() : module.getName() + " " + tag;
    }
}
