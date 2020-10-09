package solutions.s4y.waytoday.sdk.wsse;

import android.util.Base64;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Date;

import solutions.s4y.waytoday.sdk.errors.ErrorsObservable;

public class Wsse {
    static private final Charset UTF8_CHARSET = StandardCharsets.UTF_8;

    static private String sha1(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA");
            md.update(text.getBytes(UTF8_CHARSET));
            byte[] digest = md.digest();
            return Base64.encodeToString(digest, Base64.NO_WRAP);
        } catch (Exception e) {
            ErrorsObservable.notify(e, true);
            return "";
        }
    }

    private static String digest(String password, String nonce, String created) {
        String text = nonce + created + password;
        return sha1(text);
    }

    public static String getToken(String secret) {
        String nonce = String.valueOf(Math.random());
        String created = new Date().toString();
        String digest = digest(secret, nonce, created);
        return "Username=\"s4y.itag\"," +
                "PasswordDigest=\"" + digest + "\"," +
                "nonce=\"" + nonce + "\"," +
                "Created=\"" + created + "\"";
    }
}
