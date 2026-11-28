package dev.cheat.client.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as an event handler. The method takes exactly one parameter, the event type:
 *
 * <pre>{@code
 * @Subscribe
 * private void onTick(TickEvent event) { ... }
 * }</pre>
 * <p>
 * Handlers of a {@link dev.cheat.client.module.Module} only receive events while the module is enabled.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Subscribe {
    /**
     * Higher runs first. Use the constants in {@link Priority}.
     */
    int priority() default Priority.NORMAL;

    /**
     * Whether this handler still runs after an earlier handler cancelled the event.
     */
    boolean receiveCancelled() default false;
}
