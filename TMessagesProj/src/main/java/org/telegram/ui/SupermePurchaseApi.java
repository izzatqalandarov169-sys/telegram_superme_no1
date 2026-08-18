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
        purchaseStarGift(
                String.valueOf(gift.id),
                gift.stars,
                gift.title == null ? "Gift" : gift.title,
                anonymous,
                upgraded,
                dialogId,
                message == null || message.text == null ? "" : message.text,
                whenDone
        );
    }

    /** Purchase bridge used by the Superme GiftSheet when upstream GiftSheet is unavailable. */
    public static void purchaseStarGift(
            String giftId,
            long stars,
            String title,
            boolean anonymous,
            boolean upgraded,
            long dialogId,
            String messageText,
            Utilities.Callback2<Boolean, String> whenDone
    ) {
        if (giftId == null || giftId.trim().isEmpty() || stars <= 0 || dialogId == 0) {
            if (whenDone != null) whenDone.run(false, "INVALID_GIFT");
            return;
        }

        final String safeTitle = title == null || title.isEmpty() ? "Gift" : title;
        final String json = "{"
                + "\"gift_id\":\"" + escape(giftId) + "\","
                + "\"stars\":" + stars + ","
                + "\"recipient_id\":\"" + dialogId + "\","
                + "\"anonymous\":" + anonymous + ","
                + "\"upgraded\":" + upgraded + ","
                + "\"gift_title\":\"" + escape(safeTitle) + "\","
                + "\"message\":\"" + escape(messageText == null ? "" : messageText) + "\""
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
        return value == null ? "" : value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }
}
