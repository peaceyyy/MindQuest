package com.mindquest.controller;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;


public class NavigationManager {
    private final Deque<MenuEntry> navigationStack;

    public NavigationManager() {
        this.navigationStack = new ArrayDeque<>();
    }

    /**
     * Push a new menu onto the navigation stack.
     * @param menuId The menu identifier
     * @param state Optional state object associated with this menu (can be null)
     */
    public void push(MenuId menuId, Object state) {
        navigationStack.push(new MenuEntry(menuId, state));
    }

    /**
     * Pop the current menu from the stack (navigate back one level).
     * @return The menu entry that was popped, or empty if stack is empty
     */
    public Optional<MenuEntry> pop() {
        if (navigationStack.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(navigationStack.pop());
    }

    /**
     * Get the current menu without removing it from the stack.
     * @return The current menu entry, or empty if stack is empty
     */
    public Optional<MenuEntry> current() {
        return Optional.ofNullable(navigationStack.peek());
    }

    /**
     * Navigate back to the previous menu.
     * Pops the current menu and returns the new current menu.
     * @return The previous menu (now current), or empty if we're at the root
     */
    public Optional<MenuEntry> goBack() {
        pop();
        return current();
    }

    /**
     * Clear the entire navigation stack (used when returning to main menu).
     */
    public void clearToRoot() {
        navigationStack.clear();
    }

    /**
     * Check if the navigation stack is empty (at root level).
     */
    public boolean isEmpty() {
        return navigationStack.isEmpty();
    }

    /**
     * Get the size of the navigation stack.
     */
    public int size() {
        return navigationStack.size();
    }

    /**
     * Represents a single entry in the navigation stack.
     */
    public static class MenuEntry {
        private final MenuId menuId;
        private final Object state;

        public MenuEntry(MenuId menuId, Object state) {
            this.menuId = menuId;
            this.state = state;
        }

        public MenuId getMenuId() {
            return menuId;
        }

        public Object getState() {
            return state;
        }

        @SuppressWarnings("unchecked")
        public <T> Optional<T> getStateAs(Class<T> type) {
            if (state != null && type.isInstance(state)) {
                return Optional.of((T) state);
            }
            return Optional.empty();
        }
    }
}
