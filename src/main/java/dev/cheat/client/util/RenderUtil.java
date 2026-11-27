package dev.cheat.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.gizmos.GizmoProperties;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Drawing helpers for 2D (GUI coordinates, ARGB colors) and 3D (world coordinates, via Gizmos). */
public final class RenderUtil {
    private RenderUtil() {}

    // ---------------------------------------------------------------- 2D

    public static Font font() {
        return Minecraft.getInstance().font;
    }

    public static void rect(GuiGraphicsExtractor g, int x, int y, int width, int height, int color) {
        g.fill(x, y, x + width, y + height, color);
    }

    public static void outline(GuiGraphicsExtractor g, int x, int y, int width, int height, int color) {
        g.outline(x, y, width, height, color);
    }

    public static void text(GuiGraphicsExtractor g, String text, int x, int y, int color, boolean shadow) {
        g.text(font(), text, x, y, color, shadow);
    }

    public static void centeredText(GuiGraphicsExtractor g, String text, int centerX, int y, int color) {
        g.text(font(), text, centerX - font().width(text) / 2, y, color, true);
    }

    public static int width(String text) {
        return font().width(text);
    }

    public static int fontHeight() {
        return font().lineHeight;
    }

    public static boolean hovered(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    // ---------------------------------------------------------------- 3D (call from Render3DEvent)

    /** Bounding box of {@code entity} interpolated to the current frame. */
    public static AABB interpolatedBox(Entity entity, float partialTicks) {
        double dx = Mth.lerp(partialTicks, entity.xOld, entity.getX()) - entity.getX();
        double dy = Mth.lerp(partialTicks, entity.yOld, entity.getY()) - entity.getY();
        double dz = Mth.lerp(partialTicks, entity.zOld, entity.getZ()) - entity.getZ();
        return entity.getBoundingBox().move(dx, dy, dz);
    }

    public static void box(AABB box, int color, float lineWidth) {
        box(box, color, lineWidth, false);
    }

    /** @param throughWalls draw on top of the world instead of being hidden by blocks */
    public static void box(AABB box, int color, float lineWidth, boolean throughWalls) {
        onTop(Gizmos.cuboid(box, GizmoStyle.stroke(color, lineWidth)), throughWalls);
    }

    public static void filledBox(AABB box, int lineColor, float lineWidth, int fillColor) {
        filledBox(box, lineColor, lineWidth, fillColor, false);
    }

    /** @param throughWalls draw on top of the world instead of being hidden by blocks */
    public static void filledBox(AABB box, int lineColor, float lineWidth, int fillColor, boolean throughWalls) {
        onTop(Gizmos.cuboid(box, GizmoStyle.strokeAndFill(lineColor, lineWidth, fillColor)), throughWalls);
    }

    private static void onTop(GizmoProperties properties, boolean throughWalls) {
        if (throughWalls) properties.setAlwaysOnTop();
    }

    public static void line(Vec3 from, Vec3 to, int color, float lineWidth) {
        Gizmos.line(from, to, color, lineWidth);
    }
}
