package fi.zmengames.zlauncher;

import android.support.annotation.NonNull;

public class ZEvent {
    private final @NonNull  State       state;

    public enum State {
        // Normal states
        GOOGLE_SIGNIN,
        GOOGLE_SIGNOUT, LOAD_OVER, FULL_LOAD_OVER
    }

    public ZEvent(@NonNull State state)
    {
        this.state                = state;
    }
    public @NonNull State getState() {
        return state;
    }
}
