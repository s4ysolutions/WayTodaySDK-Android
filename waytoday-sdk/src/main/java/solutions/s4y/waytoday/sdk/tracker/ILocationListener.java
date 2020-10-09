package solutions.s4y.waytoday.sdk.tracker;

import android.location.Location;

import androidx.annotation.NonNull;

public interface ILocationListener {
    void onLocation(@NonNull Location location);
}
