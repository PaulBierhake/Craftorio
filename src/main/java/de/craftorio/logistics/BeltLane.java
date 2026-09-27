package de.craftorio.logistics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiPredicate;

/**
 * One lane of a conveyor belt: single items at positions 0 (back edge) to 1 (front edge).
 * Items keep {@link #SPACING} apart and queue up behind a blocked front item, like Factorio belts.
 * Free of Minecraft types so the movement rules can be unit tested.
 *
 * @param <T> item type (an ItemStack of count 1 in game)
 */
public final class BeltLane<T> {
    public static final float SPACING = 0.25F;
    public static final int CAPACITY = 4;

    /** Front-most item first. */
    private final List<Entry<T>> entries = new ArrayList<>(CAPACITY);

    public List<Entry<T>> entries() {
        return Collections.unmodifiableList(entries);
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public void clear() {
        entries.clear();
    }

    /**
     * Moves every item forward by {@code speed}. An item reaching the front edge is offered to {@code handOff}
     * together with how far it overshot; if accepted it leaves the lane, otherwise it waits at the edge.
     *
     * @return true if an item left the lane
     */
    public boolean tick(float speed, BiPredicate<T, Float> handOff) {
        boolean removed = false;
        float limit = Float.MAX_VALUE;
        for (int i = 0; i < entries.size(); i++) {
            Entry<T> entry = entries.get(i);
            entry.previous = entry.progress;
            float target = entry.progress + speed;
            if (i == 0 && target >= 1.0F) {
                if (handOff.test(entry.item, target - 1.0F)) {
                    entries.remove(0);
                    i--;
                    removed = true;
                    continue;
                }
                target = 1.0F;
            }
            target = Math.min(target, limit);
            entry.progress = Math.max(entry.progress, target);
            limit = entry.progress - SPACING;
        }
        return removed;
    }

    /** Accepts an item arriving over the back edge, placed as far forward as {@code overshoot} and the queue allow. */
    public boolean acceptFromBehind(T item, float overshoot) {
        if (entries.size() >= CAPACITY) {
            return false;
        }
        float at = Math.min(overshoot, 1.0F);
        if (!entries.isEmpty()) {
            at = Math.min(at, entries.get(entries.size() - 1).progress - SPACING);
        }
        if (at < 0) {
            return false;
        }
        entries.add(new Entry<>(item, at));
        return true;
    }

    public boolean canInsertAt(float at) {
        if (entries.size() >= CAPACITY || at < 0 || at > 1) {
            return false;
        }
        for (Entry<T> entry : entries) {
            if (Math.abs(entry.progress - at) < SPACING) {
                return false;
            }
        }
        return true;
    }

    /** Inserts an item at an exact position, e.g. from the side. Fails if it would crowd a neighbour. */
    public boolean insertAt(T item, float at) {
        if (!canInsertAt(at)) {
            return false;
        }
        int index = 0;
        while (index < entries.size() && entries.get(index).progress > at) {
            index++;
        }
        entries.add(index, new Entry<>(item, at));
        return true;
    }

    /** Removes and returns the front-most item, or null. */
    public T removeFront() {
        return entries.isEmpty() ? null : entries.remove(0).item;
    }

    public T front() {
        return entries.isEmpty() ? null : entries.get(0).item;
    }

    /** Restores an item loaded from disk or received from the server; keeps front-first order. */
    public void restore(T item, float progress) {
        int index = 0;
        while (index < entries.size() && entries.get(index).progress > progress) {
            index++;
        }
        entries.add(index, new Entry<>(item, progress));
    }

    public static final class Entry<T> {
        private final T item;
        private float progress;
        private float previous;

        Entry(T item, float progress) {
            this.item = item;
            this.progress = progress;
            this.previous = progress;
        }

        public T item() {
            return item;
        }

        public float progress() {
            return progress;
        }

        /** Position interpolated between the last two ticks, for smooth rendering. */
        public float renderProgress(float partialTick) {
            return previous + (progress - previous) * partialTick;
        }
    }
}
