package solutions.s4y.waytoday.sdk.tracker;

import androidx.annotation.NonNull;

public interface ITrackingStateListener {
    void onStateChange(@NonNull TrackerState state);
}
