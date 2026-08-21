package org.telegram.ui;

import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import org.json.JSONObject;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_stars;

/** Routes regular Star Gift purchases to the Superme commerce backend. */
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
            finish(false, "INVALID_GIFT", whenDone);
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
            finish(false, "INVALID_GIFT", whenDone);
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
                // This endpoint is implemented by the production Superme commerce service.
                // It validates the Telegram catalog entry before charging the internal balance
                // and only records the transaction after Telegram confirms delivery.
                String response = CustomGiftApi.postJson("/api/purchase/gift", json);
                JSONObject object = new JSONObject(response == null || response.isEmpty() ? "{}" : response);
                ok = object.optBoolean("ok", false);
                if (!ok) error = object.optString("error", "PURCHASE_FAILED");
            } catch (Exception e) {
                error = e.getMessage() == null ? "BACKEND_ERROR" : e.getMessage();
            }

            final boolean result = ok;
            final String resultError = error;
            new Handler(Looper.getMainLooper()).post(() -> {
                if (!result) {
                    Toast.makeText(ApplicationLoader.applicationContext, friendlyError(resultError), Toast.LENGTH_LONG).show();
                }
                if (whenDone != null) whenDone.run(result, resultError);
            });
        }).start();
    }

    private static void finish(boolean ok, String error, Utilities.Callback2<Boolean, String> callback) {
        if (!ok) {
            Toast.makeText(ApplicationLoader.applicationContext, friendlyError(error), Toast.LENGTH_LONG).show();
        }
        if (callback != null) callback.run(ok, error);
    }

    private static String friendlyError(String error) {
        if (error == null || error.isEmpty()) return "Gift xaridi amalga oshmadi.";
        if (error.contains("INSUFFICIENT_SUPERME_STARS")) return "Superme Stars yetarli emas.";
        if (error.contains("GIFT_NOT_FOUND")) return "Bu gift Telegram katalogida hozir mavjud emas.";
        if (error.contains("GIFT_PRICE_UNAVAILABLE")) return "Gift narxi hozir mavjud emas.";
        if (error.contains("GIFT_EXPIRED") || error.contains("GIFT_SOLD_OUT")) return "Bu gift tugagan.";
        if (error.contains("TELEGRAM_BOT_TOKEN_NOT_CONFIGURED")) return "Gift xizmati serverda sozlanmagan.";
        if (error.contains("TELEGRAM_GIFT_DELIVERY_FAILED")) return "Telegram giftni yuborishning iloji bo‘lmadi.";
        if (error.contains("GIFT_DELIVERY_NOT_CONFIGURED")) return "Telegram gift yetkazib berish xizmati sozlanmagan.";
        if (error.contains("TELEGRAM_GIFTS_UNAVAILABLE")) return "Telegram gift katalogi hozircha mavjud emas.";
        if (error.contains("BACKEND_HTTP_")) return "Superme serverida vaqtinchalik xatolik.";
        if (error.contains("INVALID_GIFT")) return "Gift ma’lumotlari noto‘g‘ri.";
        return error;
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }
}
