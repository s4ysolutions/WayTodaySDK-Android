package solutions.s4y.waytoday.sdk.id;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.core.app.JobIntentService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;
import solutions.s4y.waytoday.sdk.errors.ErrorsObservable;
import solutions.s4y.waytoday.sdk.grpc.GRPCMetadataKeys;
import solutions.s4y.waytoday.sdk.grpc.TrackerGrpc;
import solutions.s4y.waytoday.sdk.grpc.TrackerOuterClass;
import solutions.s4y.waytoday.sdk.wsse.Wsse;
import solutions.s4y.waytoday.sdk.grpc.GRPCChannelProvider;

/**
 * Ready for use JobIntentService to request TrackIDs
 */
public class TrackIDJobService extends JobIntentService {
    private static String secret;

    private static final String EXTRA_PREVID = "previd";
    @SuppressWarnings({"FieldCanBeLocal", "unused", "RedundantSuppression"})
    private static boolean sFailed = false;
    private static boolean sProgress = false;

    private final GRPCChannelProvider grpcChannelProvider = GRPCChannelProvider.getInstance();
    private ManagedChannel ch = null;

    @VisibleForTesting
    public static final List<ITrackIDChangeListener> sListeners =
            new ArrayList<>(2);

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static synchronized boolean isProgress() {
        return sProgress;
    }

    private static synchronized void setProgress(boolean progress) {
        sProgress = progress;
    }

    /**
     * Must be called before the very first request to upload locations
     *
     * @param secret mandatory string to athorize the application against WayToday server.
     *               Currently it might be any characters string.
     */
    @SuppressWarnings({"unused", "RedundantSuppression"})
    public static void init(String secret) {
        TrackIDJobService.secret = secret;
    }

    /**
     * Enqueue the work unit requesting a new track id. The result will be send back
     * through ITrackIDChangeSeriviceListener. See @addOnTrackIDChangeListener
     * @param context a context used to create the intent to launch the service
     */
    @SuppressWarnings({"unused", "RedundantSuppression"})
    public static void enqueueRetrieveId(Context context) {
        if (!isProgress()) {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            String prevID = sp.getString("tid", "");
            enqueueRetrieveId(context, prevID);
        }
    }

    private static final int sJobID = 1000;
    /**
     * Enqueue the work unit requesting a new track id. The result will be send back
     * through ITrackIDChangeSeriviceListener. See @addOnTrackIDChangeListener
     * @param context a context used to create the intent to launch the service
     * @param prevID an id being used and needed to be released
     */
    public static void enqueueRetrieveId(Context context, String prevID) {
        if (!isProgress()) {
            Intent intent = new Intent(context, TrackIDJobService.class);
            intent.putExtra(EXTRA_PREVID, prevID);
            enqueueWork(context, TrackIDJobService.class, sJobID, intent);
        }
    }

    /**
     * Subscribes listener to the responses on the request to retrieve new track id
     * @param listener the implementation of ITrackIDChangeServiceListener to handle
     *                 the notification
     */
    public static void addOnTrackIDChangeListener(@NonNull ITrackIDChangeListener listener) {
        sListeners.add(listener);
    }

    /**
     * Unsubscribes listener from the responses on the request to retrieve new track id
     * @param listener the implementation of ITrackIDChangeServiceListener used
     *                 in the previous call of addOnTrackIDChangeListener
     */
    public static void removeOnTrackIDChangeListener(@NonNull ITrackIDChangeListener listener) {
        sListeners.remove(listener);
    }

    private static void notifyTrack(@NonNull String trackID) {
        for (ITrackIDChangeListener listener : new ArrayList<>(sListeners)) {
            listener.onTrackID(trackID);
        }
    }

    private void reportFail(Throwable e) {
        ErrorsObservable.notify(e);
    }

    protected boolean isConnected() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null &&
                activeNetwork.isConnected();
    }

    @VisibleForTesting()
    public TrackerGrpc.TrackerBlockingStub getGrpcStub() {
        return TrackerGrpc.newBlockingStub(ch);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        setProgress(true);
        sFailed = false;
        if (isConnected()) {
            String id = intent.getStringExtra(EXTRA_PREVID);

            try {
                // intent service can be handle work few times
                // without destory, te use the channel then
                if (ch == null)
                    ch = grpcChannelProvider.channel();
                TrackerGrpc.TrackerBlockingStub grpcStub = getGrpcStub();

                Metadata headers = new Metadata();
                Metadata.Key<String> key = GRPCMetadataKeys.wsseKey;
                String token = Wsse.getToken(secret);
                headers.put(key, token);
                grpcStub = MetadataUtils.attachHeaders(grpcStub, headers);

                TrackerOuterClass.GenerateTrackerIDRequest req = TrackerOuterClass.
                        GenerateTrackerIDRequest.
                        newBuilder()
                        .setPrevTid(id == null ? "" : id)
                        .build();

                TrackerOuterClass.GenerateTrackerIDResponse response = grpcStub.generateTrackerID(req);
                final String tid = response.getTid();
                notifyTrack(tid);
            } catch (final Exception e) {
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
                String oldtid = preferences.getString("tid", null);
                sFailed = oldtid != null;
                reportFail(e);
                notifyTrack(oldtid == null ? "" : oldtid);
            }
        } else {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            String oldtid = preferences.getString("tid", null);
            sFailed = oldtid != null;
        }
        setProgress(false);
    }

    @VisibleForTesting()
    public void destoryGrpcChannel() {
        if (ch != null) {
            try {
                ManagedChannel tmp = ch;
                ch = null;
                tmp.shutdown().awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onDestroy() {
        destoryGrpcChannel();
        super.onDestroy();
    }

}
