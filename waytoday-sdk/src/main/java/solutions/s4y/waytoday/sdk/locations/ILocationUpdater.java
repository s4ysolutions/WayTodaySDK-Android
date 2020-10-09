package solutions.s4y.waytoday.sdk.locations;

import android.location.LocationListener;

import androidx.annotation.NonNull;

/**
 * Low level location updater (provider) to be passed to LocationTracker
 * An interface to be implemented by concrete implementation of location providers
 * The SDK contains Android GPS sensor implementation @see LocationGPSUpdater
 */
public interface ILocationUpdater {
    void requestLocationUpdates(
            @NonNull LocationListener locationListener,
            @NonNull IRequestListener requestListener,
            int frequency);

    void cancelLocationUpdates();
}
