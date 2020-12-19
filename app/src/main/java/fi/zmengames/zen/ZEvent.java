package fi.zmengames.zen;

import androidx.annotation.NonNull;

public class ZEvent {
    private final @NonNull  State       state;
    private final String text;
    private final int intExtra;

    public ZEvent(@NonNull State state, String stringExtra, int intExtra) {
        this.state                = state;
        this.text = stringExtra;
        this.intExtra = intExtra;
    }

    public enum State {
        // Normal states
        GOOGLE_SIGNIN,
        GOOGLE_SIGNOUT,
        LOAD_OVER,
        FULL_LOAD_OVER,
        SHOW_TOAST,
        BADGE_COUNT,
        HANDLE_PENDING_EVENTS,
        INTERNAL_EVENT,
        RELOAD_APPS,
        ENABLE_DEVICE_ADMIN
    }

    public ZEvent(@NonNull State state)
    {
        this.state                = state;
        text = null;
        this.intExtra = 0;
    }
    public ZEvent(@NonNull State state, String text)
    {
        this.state                = state;
        this.text = text;
        this.intExtra = 0;
    }
    public @NonNull State getState() {
        return state;
    }
    public String getText() {
        return text;
    }
    public int getIntExtra() {return intExtra;}
}
