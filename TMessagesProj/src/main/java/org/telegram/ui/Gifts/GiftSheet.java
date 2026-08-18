package org.telegram.ui.Gifts;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.CustomGiftApi;
import org.telegram.ui.SupermePurchaseApi;

/**
 * Superme-compatible GiftSheet fallback.
 * Uses Telegram's live getAvailableGifts catalog through the Superme backend;
 * no local/fake 150k gift database is used.
 */
public class GiftSheet extends Dialog {
    private final int account;
    private final long dialogId;
    private final Runnable closeParentSheet;
    private LinearLayout list;
    private TextView status;

    public GiftSheet(Context context, int account, long userId, Runnable closeParentSheet) {
        super(context);
        this.account = account;
        this.dialogId = userId;
        this.closeParentSheet = closeParentSheet;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout root = new LinearLayout(getContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(14), dp(16), dp(18));
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.rgb(248, 248, 252));
        background.setCornerRadius(dp(22));
        root.setBackground(background);

        TextView title = text("🎁  Hadya yuborish", 21, true);
        title.setGravity(Gravity.CENTER);
        root.addView(title, lp(-1, -2));

        status = text("⭐ Superme Stars: yuklanmoqda…", 14, true);
        status.setTextColor(Color.rgb(95, 95, 105));
        status.setGravity(Gravity.CENTER);
        root.addView(status, lp(-1, -2));

        ScrollView scroll = new ScrollView(getContext());
        list = new LinearLayout(getContext());
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list, new ScrollView.LayoutParams(-1, -2));
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        setContentView(root);
        Window window = getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            WindowManager.LayoutParams p = window.getAttributes();
            p.width = WindowManager.LayoutParams.MATCH_PARENT;
            p.height = (int) (getContext().getResources().getDisplayMetrics().heightPixels * 0.86f);
            p.gravity = Gravity.BOTTOM;
            window.setAttributes(p);
        }

        loadBalance();
        loadGifts();
    }

    private void loadBalance() {
        new Thread(() -> {
            try {
                JSONObject response = CustomGiftApi.getSupermeBalance();
                long stars = response.optLong("stars", 0L);
                AndroidUtilities.runOnUIThread(() -> status.setText("⭐ Superme Stars: " + String.format("%,d", stars)));
            } catch (Exception e) {
                AndroidUtilities.runOnUIThread(() -> status.setText("⭐ Superme Stars: balans yuklanmadi"));
            }
        }).start();
    }

    private void loadGifts() {
        new Thread(() -> {
            try {
                JSONObject response = CustomGiftApi.getExternalGifts();
                JSONArray gifts = response.optJSONArray("gifts");
                AndroidUtilities.runOnUIThread(() -> render(gifts));
            } catch (Exception e) {
                AndroidUtilities.runOnUIThread(() -> {
                    list.removeAllViews();
                    TextView error = text("Giftlar yuklanmadi. Internet yoki Telegram gift xizmati mavjudligini tekshiring.", 15, false);
                    error.setGravity(Gravity.CENTER);
                    list.addView(error, lp(-1, -2));
                });
            }
        }).start();
    }

    private void render(JSONArray gifts) {
        list.removeAllViews();
        if (gifts == null || gifts.length() == 0) {
            TextView empty = text("Hozircha mavjud gift yo‘q.", 16, false);
            empty.setGravity(Gravity.CENTER);
            list.addView(empty, lp(-1, dp(80)));
            return;
        }

        for (int i = 0; i < gifts.length(); i++) {
            JSONObject gift = gifts.optJSONObject(i);
            if (gift == null) continue;
            addGiftCard(gift);
        }
    }

    private void addGiftCard(JSONObject gift) {
        String id = gift.optString("id", "");
        long stars = gift.optLong("stars", 0L);
        String emoji = gift.optString("emoji", "🎁");
        String title = gift.optString("title", emoji);
        int remaining = gift.optInt("remaining_count", -1);

        LinearLayout card = new LinearLayout(getContext());
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(14), dp(10), dp(14), dp(10));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setCornerRadius(dp(16));
        bg.setStroke(dp(1), Color.rgb(226, 226, 232));
        card.setBackground(bg);

        TextView icon = text(emoji, 34, false);
        icon.setGravity(Gravity.CENTER);
        card.addView(icon, lp(dp(56), dp(62)));

        LinearLayout info = new LinearLayout(getContext());
        info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(dp(10), 0, dp(8), 0);
        TextView name = text(title, 16, true);
        TextView price = text("⭐ " + String.format("%,d", stars), 14, true);
        price.setTextColor(Color.rgb(230, 160, 20));
        info.addView(name, lp(-1, -2));
        info.addView(price, lp(-1, -2));
        if (remaining >= 0) {
            TextView stock = text("Qoldi: " + remaining, 12, false);
            stock.setTextColor(Color.rgb(120, 120, 128));
            info.addView(stock, lp(-1, -2));
        }
        card.addView(info, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView buy = text("Sotib olish", 13, true);
        buy.setTextColor(Color.WHITE);
        buy.setGravity(Gravity.CENTER);
        GradientDrawable buyBg = new GradientDrawable();
        buyBg.setColor(Color.rgb(50, 130, 245));
        buyBg.setCornerRadius(dp(14));
        buy.setBackground(buyBg);
        card.addView(buy, lp(dp(108), dp(44)));

        card.setOnClickListener(v -> purchase(id, stars, title));
        buy.setOnClickListener(v -> purchase(id, stars, title));
        list.addView(card, lp(-1, -2));
        View spacer = new View(getContext());
        list.addView(spacer, lp(-1, dp(8)));
    }

    private void purchase(String id, long stars, String title) {
        if (stars <= 0 || id.isEmpty()) {
            showError("Gift narxi noto‘g‘ri.");
            return;
        }
        status.setText("⭐ Xarid tekshirilmoqda…");
        SupermePurchaseApi.purchaseStarGift(id, stars, title, false, false, dialogId, "", (ok, error) -> {
            if (ok) {
                status.setText("✅ Gift sotib olindi");
                new android.os.Handler().postDelayed(() -> {
                    if (closeParentSheet != null) closeParentSheet.run();
                    dismiss();
                }, 350);
            } else {
                status.setText("⭐ Xarid amalga oshmadi");
                String message = mapError(error);
                showError(message);
                loadBalance();
            }
        });
    }

    private String mapError(String error) {
        if (error == null) return "Xarid amalga oshmadi.";
        if (error.contains("INSUFFICIENT_SUPERME_STARS")) return "Superme Stars yetarli emas.";
        if (error.contains("GIFT_PRICE_MISMATCH")) return "Gift narxi o‘zgargan. Katalogni yangilang.";
        if (error.contains("TELEGRAM_BOT_TOKEN_NOT_CONFIGURED")) return "Telegram gift xizmati serverda sozlanmagan.";
        if (error.contains("TELEGRAM_GIFTS_UNAVAILABLE")) return "Telegram gift katalogi hozircha mavjud emas.";
        return error;
    }

    private void showError(String message) {
        new android.app.AlertDialog.Builder(getContext())
                .setTitle("Gift xaridi")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    private TextView text(String value, float size, boolean bold) {
        TextView view = new TextView(getContext());
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(Color.rgb(35, 35, 40));
        if (bold) view.setTypeface(null, android.graphics.Typeface.BOLD);
        return view;
    }

    private LinearLayout.LayoutParams lp(int width, int height) {
        return new LinearLayout.LayoutParams(width, height);
    }

    private int dp(float value) {
        return AndroidUtilities.dp(value);
    }
}
