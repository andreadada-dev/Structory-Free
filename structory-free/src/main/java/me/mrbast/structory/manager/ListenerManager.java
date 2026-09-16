package me.mrbast.structory.manager;

import me.mrbast.structory.event.Listener;
import me.mrbast.structory.event.StructureEvent;
import me.mrbast.structory.event.StructureEventHandler;
import me.mrbast.structory.event.StructureEventPriority;
import me.mrbast.structory.option.Option;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class ListenerManager {

    public static class Caller {
        private final Method method;
        private final Object target;

        public Caller(Method method, Object target) {
            this.method = method;
            this.target = target;
            this.method.setAccessible(true);
        }

        public void call(StructureEvent event) throws InvocationTargetException, IllegalAccessException {
            method.invoke(target, event);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof Caller)) return false;
            Caller caller = (Caller) other;
            return method.equals(caller.method) && target == caller.target;
        }

        @Override
        public int hashCode() {
            return 31 * method.hashCode() + System.identityHashCode(target);
        }
    }

    private final Map<Class<? extends StructureEvent>, TreeMap<StructureEventPriority, Set<Caller>>> prioritizedListeners = new HashMap<>();
    private final Map<Class<? extends StructureEvent>, Class<? extends StructureEvent>> willAlsoCall = new HashMap<>();

    private static final ListenerManager instance = new ListenerManager();

    public static ListenerManager getInstance() {
        return instance;
    }

    public void clear() {
        prioritizedListeners.clear();
        willAlsoCall.clear();
    }

    private void call(Class<? extends StructureEvent> clazz, StructureEvent event) {
        Class<? extends StructureEvent> superClazz = willAlsoCall.get(clazz);
        if (superClazz != null) call(superClazz, event);

        Map<StructureEventPriority, Set<Caller>> prioritySetMap = prioritizedListeners.get(clazz);
        if (prioritySetMap == null) return;

        prioritySetMap.forEach((priority, set) -> set.forEach(caller -> {
            try {
                caller.call(event);
            } catch (InvocationTargetException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }));
    }

    public void call(StructureEvent event) {
        if (!prioritizedListeners.containsKey(event.getClass())) registerSuperClasses(event.getClass());
        call(event.getClass(), event);
    }

    /**
     * Supports both modern handlers declared directly on the option and the legacy
     * pattern where an Option owns one or more fields implementing Listener.
     */
    public void subscribe(Option option) {
        subscribeAnnotatedMethods(option);
        subscribeListenerFields(option);
    }

    public void subscribe(Listener listener) {
        subscribeAnnotatedMethods(listener);
    }

    private void subscribeListenerFields(Object owner) {
        for (Field field : owner.getClass().getDeclaredFields()) {
            if (!Listener.class.isAssignableFrom(field.getType())) continue;
            field.setAccessible(true);
            try {
                Object value = field.get(owner);
                if (value instanceof Listener) subscribe((Listener) value);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void subscribeAnnotatedMethods(Object target) {
        for (Method method : target.getClass().getDeclaredMethods()) {
            if (!method.isAnnotationPresent(StructureEventHandler.class)) continue;

            Class<?>[] paramTypes = method.getParameterTypes();
            if (paramTypes.length != 1 || !StructureEvent.class.isAssignableFrom(paramTypes[0])) continue;

            StructureEventHandler annotation = method.getAnnotation(StructureEventHandler.class);
            register(method, target, annotation.priority(), (Class<? extends StructureEvent>) paramTypes[0]);
        }
    }

    private void register(Method method, Object target, StructureEventPriority priority,
                          Class<? extends StructureEvent> clazz) {
        Map<StructureEventPriority, Set<Caller>> map = prioritizedListeners.computeIfAbsent(
                clazz, ignored -> new TreeMap<>(Comparator.comparingInt(StructureEventPriority::ordinal)));
        map.computeIfAbsent(priority, ignored -> new HashSet<>()).add(new Caller(method, target));
        registerSuperClasses(clazz);
    }

    @SuppressWarnings("unchecked")
    private void registerSuperClasses(Class<? extends StructureEvent> clazz) {
        Class<? extends StructureEvent> subClass = clazz;
        Class<?> superclass = clazz.getSuperclass();
        while (superclass != null && StructureEvent.class.isAssignableFrom(superclass)) {
            willAlsoCall.put(subClass, (Class<? extends StructureEvent>) superclass);
            subClass = (Class<? extends StructureEvent>) superclass;
            superclass = superclass.getSuperclass();
        }
    }
}
