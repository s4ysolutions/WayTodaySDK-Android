package solutions.s4y.waytoday.sdk;

import android.content.Context;

import androidx.annotation.NonNull;

public class PreferencesPersistedState implements IPersistedState {
    final private Context context;

    public PreferencesPersistedState(Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public String getTrackerId() {
        return context
                .getSharedPreferences("solutions.s4y.waytoday.sdk", Context.MODE_PRIVATE)
                .getString("tid", "");
    }

    @Override
    public void setTrackerId(@NonNull String trackerID) {
        context
                .getSharedPreferences("solutions.s4y.waytoday.sdk", Context.MODE_PRIVATE)
                .edit()
                .putString("tid", trackerID)
                .apply();
    }

    @Override
    public boolean isTrackerIDSet() {
        return !getTrackerId().isEmpty();
    }
}
