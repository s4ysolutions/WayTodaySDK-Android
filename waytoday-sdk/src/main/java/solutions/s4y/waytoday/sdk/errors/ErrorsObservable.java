package solutions.s4y.waytoday.sdk.errors;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

public class ErrorsObservable {
    private static final List<IErrorListener> mIErrorListener =
            new ArrayList<>(2);

    public static void addErrorListener(IErrorListener listener) {
        mIErrorListener.add(listener);
    }

    public static void removeErrorListener(IErrorListener listener) {
        mIErrorListener.remove(listener);
    }

    private static void notifyListeners(ErrorNotification errorNotification) {
        for (IErrorListener listener : mIErrorListener) {
            listener.onError(errorNotification);
        }
    }

    private static void notify(ErrorNotification errorNotification) {
        notifyListeners(errorNotification);
    }

    public static void notify(Throwable throwable) {
        notify(new ErrorNotification(throwable));
    }

    private static void notify(int resourceID) {
        notify(new ErrorNotification(resourceID, true));
    }

    public static void notify(Throwable throwable, boolean toast) {
        notify(new ErrorNotification(throwable, toast));
    }

    public static void notify(String message, boolean toast) {
        notify(new ErrorNotification(message, toast));
    }

    public static void toast(Throwable throwable) {
        notify(throwable, true);
    }

    public static void toast(int resourceID) {
        notify(resourceID);
    }

    public interface IErrorListener {
        void onError(@Nonnull final ErrorNotification errorNotification);
    }

}