package org.telegram.ui;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_stars;
import org.telegram.messenger.Utilities;

/** Routes Star Gift purchases to the Superme backend instead of Telegram's payment flow. */
public final class SupermePurchaseApi {
    private SupermePurchaseApi() { }

    public static void purchaseStarGift(
            int account,
            TL_stars.StarGift gift,
            boolean anonymous,
            boolean upgraded,
            long dialogId,
            TLRPC.TL_textWithEntities message,
            Utilities.Callback2<Boolean, String> whenDone
    ) {
        if (gift == null) {
            if (whenDone != null) whenDone.run(false, "INVALID_GIFT");
            return;
        }

        final String messageText = message == null || message.text == null ? "" : message.text;
        final String safeTitle = gift.title == null ? "Gift" : gift.title;
        final String json = "{"
                + "\"gift_id\":" + gift.id + ","
                + "\"stars\":" + gift.stars + ","
                + "\"recipient_id\":" + dialogId + ","
                + "\"anonymous\":" + anonymous + ","
                + "\"upgraded\":" + upgraded + ","
                + "\"gift_title\":\"" + escape(safeTitle) + "\","
                + "\"message\":\"" + escape(messageText) + "\""
                + "}";

        new Thread(() -> {
            boolean ok = false;
            String error = null;
            try {
                String response = CustomGiftApi.postJson("/superme/external/gift", json);
                JSONObject object = new JSONObject(response == null || response.isEmpty() ? "{}" : response);
                ok = object.optBoolean("ok", false);
                if (!ok) error = object.optString("error", "PURCHASE_FAILED");
            } catch (Exception e) {
                error = e.getMessage() == null ? "BACKEND_ERROR" : e.getMessage();
            }

            final boolean result = ok;
            final String resultError = error;
            new Handler(Looper.getMainLooper()).post(() -> {
                if (whenDone != null) whenDone.run(result, resultError);
            });
        }).start();
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }
}
