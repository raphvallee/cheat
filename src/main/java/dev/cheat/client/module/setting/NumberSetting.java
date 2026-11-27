package dev.cheat.client.module.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.math.BigDecimal;

/** A number in {@code [min, max]} snapped to {@code step}. Use step 1 for integers. */
public final class NumberSetting extends Setting<Double> {
    private final double min;
    private final double max;
    private final double step;
    private String suffix = "";

    public NumberSetting(String name, double defaultValue, double min, double max, double step) {
        super(name, defaultValue);
        if (min >= max || step <= 0) throw new IllegalArgumentException("Invalid range for " + name);
        this.min = min;
        this.max = max;
        this.step = step;
        set(defaultValue);
    }

    /** Unit shown after the value, e.g. {@code "ms"} or {@code " blocks"}. */
    public NumberSetting withSuffix(String suffix) {
        this.suffix = suffix;
        return this;
    }

    @Override
    protected Double validate(Double value) {
        double clamped = Math.max(min, Math.min(max, value));
        double snapped = min + Math.round((clamped - min) / step) * step;
        // Strip floating point noise (0.30000000000000004 -> 0.3)
        return BigDecimal.valueOf(Math.min(max, snapped)).setScale(decimals() + 2, java.math.RoundingMode.HALF_UP).doubleValue();
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public double getStep() {
        return step;
    }

    public int getInt() {
        return (int) Math.round(get());
    }

    public float getFloat() {
        return get().floatValue();
    }

    public double getDouble() {
        return get();
    }

    /** Position of the value in the range, 0-1 (used by sliders). */
    public double getPercent() {
        return (get() - min) / (max - min);
    }

    public void setPercent(double percent) {
        set(min + (max - min) * Math.max(0, Math.min(1, percent)));
    }

    private int decimals() {
        BigDecimal s = BigDecimal.valueOf(step).stripTrailingZeros();
        return Math.max(0, s.scale());
    }

    @Override
    public String display() {
        return String.format(java.util.Locale.ROOT, "%." + decimals() + "f", get()) + suffix;
    }

    @Override
    public void parse(String input) {
        try {
            set(Double.parseDouble(input));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Expected a number between " + min + " and " + max);
        }
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(get());
    }

    @Override
    public void fromJson(JsonElement json) {
        if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isNumber()) set(json.getAsDouble());
    }
}
