package fi.zmengames.zlauncher;

import androidx.annotation.NonNull;

public class ZEvent {
    private final @NonNull  State       state;
    private final String text;

    public enum State {
        // Normal states
        GOOGLE_SIGNIN,
        GOOGLE_SIGNOUT, LOAD_OVER, FULL_LOAD_OVER, SHOW_TOAST;
    }

    public ZEvent(@NonNull State state)
    {
        this.state                = state;
        text = null;
    }
    public ZEvent(@NonNull State state, String text)
    {
        this.state                = state;
        this.text = text;
    }
    public @NonNull State getState() {
        return state;
    }
    public String getText() {
        return text;
    }
}
