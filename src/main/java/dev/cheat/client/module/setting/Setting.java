package dev.cheat.client.module.setting;

import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * A configurable module value, shown in the ClickGUI, editable with {@code .set} and saved in the config.
 *
 * @param <T> value type
 */
public abstract class Setting<T> {
    private final String name;
    private final T defaultValue;
    private T value;
    private String description = "";
    private BooleanSupplier visibility = () -> true;
    private final List<Consumer<T>> listeners = new ArrayList<>();

    protected Setting(String name, T defaultValue) {
        this.name = name;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
    }

    public String getName() {
        return name;
    }

    public T get() {
        return value;
    }

    /** Sets the value (validated by {@link #validate}) and notifies listeners when it changed. */
    public void set(T newValue) {
        T validated = validate(newValue);
        if (validated.equals(value)) return;
        value = validated;
        for (Consumer<T> listener : listeners) listener.accept(validated);
    }

    public T getDefault() {
        return defaultValue;
    }

    public void reset() {
        set(defaultValue);
    }

    public String getDescription() {
        return description;
    }

    public boolean isVisible() {
        return visibility.getAsBoolean();
    }

    /** Only show this setting when {@code condition} is true, e.g. {@code visibleIf(() -> mode.is("Packet"))}. */
    @SuppressWarnings("unchecked")
    public <S extends Setting<T>> S visibleIf(BooleanSupplier condition) {
        this.visibility = condition;
        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public <S extends Setting<T>> S describedAs(String description) {
        this.description = description;
        return (S) this;
    }

    /** Runs {@code listener} with the new value whenever it changes. */
    @SuppressWarnings("unchecked")
    public <S extends Setting<T>> S onChange(Consumer<T> listener) {
        listeners.add(listener);
        return (S) this;
    }

    protected T validate(T value) {
        return value;
    }

    /** Parses user input from the {@code .set} command. Throws {@link IllegalArgumentException} when invalid. */
    public abstract void parse(String input);

    public abstract JsonElement toJson();

    /** Loads a saved value; invalid data must be ignored, not thrown. */
    public abstract void fromJson(JsonElement json);

    /** Human readable value for chat and GUI. */
    public String display() {
        return String.valueOf(value);
    }
}
