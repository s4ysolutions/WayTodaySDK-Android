package solutions.s4y.waytoday.sdk.locations;

/**
 * Callback interface for notification request to start location updates completed
 */
public interface IRequestListener {
    void onRequestResult(boolean success);
}
