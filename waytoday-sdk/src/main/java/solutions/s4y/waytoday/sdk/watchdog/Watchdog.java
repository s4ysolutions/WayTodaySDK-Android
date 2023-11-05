package solutions.s4y.waytoday.sdk.watchdog;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.SystemClock;

import androidx.preference.PreferenceManager;

import android.util.Log;

import solutions.s4y.waytoday.sdk.BuildConfig;
import solutions.s4y.waytoday.sdk.errors.ErrorsObservable;

public class Watchdog {
    private final static String LT = Watchdog.class.getSimpleName();
    private final static String ACTION_WATCHDOG = "solutions.s4y.waytoday.WATCHDOG";
    private PendingIntent wakeupPIntent;
    private BroadcastReceiver receiver = null;
    private long interval;

    private long getNextExpectedActivityTS(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getLong("solutions.s4y.waytoday.sdk.nt", 0);
    }

    @SuppressWarnings({"unused", "RedundantSuppression"})
    void setNextExpectedActivityTS(Context context, long ts) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putLong("solutions.s4y.waytoday.sdk.nt", ts).apply();
    }

    public class WatchdogBroadcastReceiver extends BroadcastReceiver {
        private final Runnable onWatchdog;

        WatchdogBroadcastReceiver(Runnable onWatchdog) {
            super();
            this.onWatchdog = onWatchdog;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(LT, "receiver");
            if (ACTION_WATCHDOG.equals(intent.getAction())) {
                Log.d(LT, "got watchdog");
                long nextTS = getNextExpectedActivityTS(context);
                if (nextTS > 0 && nextTS < System.currentTimeMillis()) {
                    onWatchdog.run();
                }
                if (isAlarmStarted()) {
                    startAlarm(context, Watchdog.this.interval);
                }
            }
        }
    }

    public synchronized void start(Context context, long interval, Runnable onWatchdog) {
        stop(context);
        unregister(context);
        register(context, new WatchdogBroadcastReceiver(onWatchdog));
        startAlarm(context, interval);
    }

    public synchronized void stop(Context context) {
        if (wakeupPIntent == null) return;
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(wakeupPIntent);
        } else {
            ErrorsObservable.notify(new Exception("alarmManager == null"), BuildConfig.DEBUG);
        }
        wakeupPIntent = null;
    }

    private synchronized void startAlarm(Context context, long interval) {
        this.interval = interval;
        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }
        if (wakeupPIntent != null) {
            alarmManager.cancel(wakeupPIntent);
        }
        this.wakeupPIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_WATCHDOG), PendingIntent.FLAG_IMMUTABLE);
        if (Build.VERSION.SDK_INT >= 23) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + Math.max(90 * 1000, interval), wakeupPIntent);
        } else {
            alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + Math.max(60 * 1000, interval), wakeupPIntent);
        }
        Log.d(LT, "Start alarm delay=" + interval);
    }

    private void register(Context context, BroadcastReceiver receiver) {
        if (receiver != null) {
            IntentFilter filter = new IntentFilter(ACTION_WATCHDOG);
            Log.d(LT, "will do registerReceiver");
            context.registerReceiver(receiver, filter);
            Log.d(LT, "done registerReceiver");
        }
        this.receiver = receiver;
    }

    private void unregister(Context context) {
        if (receiver != null) {
            try {
                Log.d(LT, "will do unregisterReceiver");
                context.unregisterReceiver(receiver);
                Log.d(LT, "done unregisterReceiver");
            } catch (IllegalArgumentException arg) {
                // TODO: must be fixed
                ErrorsObservable.notify(arg, BuildConfig.DEBUG);
            }
        }
        receiver = null;
    }

    private synchronized boolean isAlarmStarted() {
        return receiver != null;
    }

}
