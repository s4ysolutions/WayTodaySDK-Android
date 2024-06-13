package solutions.s4y.waytoday.sdk;

import android.content.Context;

/** @noinspection unused*/
class PreferenceOn {
    public static boolean get(Context context) {
        return new PreferenceOn(context).get();
    }

    public static void set(Context context, boolean value) {
        new PreferenceOn(context).set(value);
    }

    private boolean on;
    private final Context context;
    public PreferenceOn(Context context) {
        this.context = context;
        on = context
                .getSharedPreferences("solutions.s4y.waytoday.sdk", Context.MODE_PRIVATE)
                .getBoolean("on", false);
    }

    public boolean get() {
        return on;
    }

    public void set(boolean on) {
        this.on = on;
        context
                .getSharedPreferences("solutions.s4y.waytoday.sdk", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("on", on)
                .apply();
    }
}
