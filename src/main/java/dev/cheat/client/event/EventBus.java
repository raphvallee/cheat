package dev.cheat.client.event;

import dev.cheat.client.Client;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Annotation based event bus. Thread safe: packet events are posted from the Netty thread while
 * everything else is posted from the render/client thread.
 *
 * <p>A handler that throws is logged (a few times, then silenced) and never crashes the game.</p>
 */
public final class EventBus {
    private static final int MAX_LOGGED_ERRORS = 5;
    private static final MethodType HANDLER_TYPE = MethodType.methodType(void.class, Event.class);

    private final Map<Class<?>, CopyOnWriteArrayList<Handler>> handlers = new ConcurrentHashMap<>();
    private final Map<Object, List<Handler>> byOwner = Collections.synchronizedMap(new IdentityHashMap<>());
    private final Map<Class<?>, List<Method>> scanCache = new ConcurrentHashMap<>();

    private static MethodHandle bind(Object owner, Method method) {
        try {
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(method.getDeclaringClass(), MethodHandles.lookup());
            return lookup.unreflect(method).bindTo(owner).asType(HANDLER_TYPE);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Cannot access " + method, e);
        }
    }

    /**
     * Registers every {@link Subscribe} method of {@code owner}. Registering twice is a no-op.
     */
    public void subscribe(Object owner) {
        synchronized (byOwner) {
            if (byOwner.containsKey(owner)) return;
            List<Handler> created = new ArrayList<>();
            for (Method method : scan(owner.getClass())) {
                Subscribe sub = method.getAnnotation(Subscribe.class);
                Handler handler = new Handler(owner, owner.getClass().getSimpleName() + "#" + method.getName(),
                        bind(owner, method), sub.priority(), sub.receiveCancelled(), new AtomicInteger());
                CopyOnWriteArrayList<Handler> list = handlers.computeIfAbsent(method.getParameterTypes()[0],
                        k -> new CopyOnWriteArrayList<>());
                synchronized (list) {
                    int index = 0;
                    while (index < list.size() && list.get(index).priority() >= handler.priority()) index++;
                    list.add(index, handler);
                }
                created.add(handler);
            }
            byOwner.put(owner, created);
        }
    }

    public void unsubscribe(Object owner) {
        synchronized (byOwner) {
            List<Handler> removed = byOwner.remove(owner);
            if (removed == null) return;
            for (CopyOnWriteArrayList<Handler> list : handlers.values()) list.removeAll(removed);
        }
    }

    public boolean isSubscribed(Object owner) {
        return byOwner.containsKey(owner);
    }

    /**
     * Posts {@code event} to its handlers in priority order and returns it.
     */
    public <T extends Event> T post(T event) {
        List<Handler> list = handlersFor(event.getClass());
        if (list.isEmpty()) return event;
        CancellableEvent cancellable = event instanceof CancellableEvent c ? c : null;
        for (Handler handler : list) {
            if (cancellable != null && cancellable.isCancelled() && !handler.receiveCancelled()) continue;
            try {
                handler.handle().invokeExact((Event) event);
            } catch (Throwable t) {
                int count = handler.errors().incrementAndGet();
                if (count <= MAX_LOGGED_ERRORS) {
                    Client.LOGGER.error("Handler {} threw while handling {}{}", handler.name(),
                            event.getClass().getSimpleName(), count == MAX_LOGGED_ERRORS ? " (further errors hidden)" : "", t);
                }
            }
        }
        return event;
    }

    private List<Handler> handlersFor(Class<?> type) {
        List<Handler> single = null;
        List<Handler> merged = null;
        for (Class<?> c = type; c != null && Event.class.isAssignableFrom(c); c = c.getSuperclass()) {
            List<Handler> list = handlers.get(c);
            if (list == null || list.isEmpty()) continue;
            if (single == null) {
                single = list;
            } else {
                if (merged == null) merged = new ArrayList<>(single);
                merged.addAll(list);
            }
        }
        if (merged != null) {
            merged.sort((a, b) -> Integer.compare(b.priority(), a.priority()));
            return merged;
        }
        return single == null ? List.of() : single;
    }

    private List<Method> scan(Class<?> type) {
        return scanCache.computeIfAbsent(type, t -> {
            List<Method> out = new ArrayList<>();
            for (Class<?> c = t; c != null && c != Object.class; c = c.getSuperclass()) {
                for (Method m : c.getDeclaredMethods()) {
                    if (!m.isAnnotationPresent(Subscribe.class)) continue;
                    if (Modifier.isStatic(m.getModifiers()) || m.getParameterCount() != 1
                            || !Event.class.isAssignableFrom(m.getParameterTypes()[0])
                            || m.getReturnType() != void.class) {
                        throw new IllegalArgumentException("Invalid @Subscribe method " + m
                                + ": must be non-static, return void and take a single Event");
                    }
                    out.add(m);
                }
            }
            return List.copyOf(out);
        });
    }

    private record Handler(Object owner, String name, MethodHandle handle, int priority, boolean receiveCancelled,
                           AtomicInteger errors) {
    }
}
