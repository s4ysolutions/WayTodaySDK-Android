package solutions.s4y.waytoday.sdk.tracker;

public class TrackerState {
    public final boolean isUpdating;
    public final boolean isSuspended;

    TrackerState(boolean isUpdating, boolean isSuspended) {
        this.isUpdating = isUpdating;
        this.isSuspended = isSuspended;
    }
}
