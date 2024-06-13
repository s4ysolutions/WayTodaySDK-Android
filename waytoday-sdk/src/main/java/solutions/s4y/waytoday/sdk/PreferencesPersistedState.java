package solutions.s4y.waytoday.sdk;

import android.content.Context;

import androidx.annotation.NonNull;

 class PreferencesPersistedState implements IPersistedState {
    final private Context context;
    private String trackerID;

    public PreferencesPersistedState(Context context) {
        this.context = context;
        trackerID = context
                .getSharedPreferences("solutions.s4y.waytoday.sdk", Context.MODE_PRIVATE)
                .getString("tid", "");
    }

    @NonNull
    @Override
    public String getTrackerId() {
        return trackerID;
    }

    @Override
    public void setTrackerId(@NonNull String trackerID) {
        this.trackerID = trackerID;
        context
                .getSharedPreferences("solutions.s4y.waytoday.sdk", Context.MODE_PRIVATE)
                .edit()
                .putString("tid", trackerID)
                .apply();
    }

     @Override
     public boolean hasTrackerId() {
         return !this.trackerID.isEmpty();
    }
}
