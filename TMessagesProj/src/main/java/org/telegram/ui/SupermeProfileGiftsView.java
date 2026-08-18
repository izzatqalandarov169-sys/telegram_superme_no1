package org.telegram.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.Components.LayoutHelper;

public class SupermeProfileGiftsView extends ScrollView {
    private final Context context;
    private final LinearLayout grid;
    private final long uid;

    public SupermeProfileGiftsView(Context context) {
        super(context);
        this.context = context;
        setFillViewport(true);
        setVerticalScrollBarEnabled(false);
        uid = UserConfig.getInstance(UserConfig.selectedAccount).getClientUserId();

        grid = new LinearLayout(context);
        grid.setOrientation(LinearLayout.VERTICAL);
        grid.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(8), AndroidUtilities.dp(8), AndroidUtilities.dp(90));
        addView(grid, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        rebuild();
    }

    public void rebuild() {
        grid.removeAllViews();
        new Thread(() -> {
            try {
                JSONObject response = CustomGiftApi.getProfileGifts();
                JSONArray gifts = response.optJSONArray("gifts");
                final String[][] rows = new String[gifts == null ? 0 : gifts.length()][];
                for (int i = 0; gifts != null && i < gifts.length(); i++) {
                    JSONObject g = gifts.optJSONObject(i);
                    if (g == null) continue;
                    rows[i] = new String[] {
                            g.optString("gift_id", ""),
                            g.optString("gift_title", "Gift"),
                            String.valueOf(g.optLong("price_stars", 0)),
                            "🎁"
                    };
                }
                AndroidUtilities.runOnUIThread(() -> render(rows));
            } catch (Exception ignored) {
                AndroidUtilities.runOnUIThread(() -> render(new String[0][]));
            }
        }).start();
    }

    private void render(String[][] rows) {
        grid.removeAllViews();
        LinearLayout row = null;
        int count = 0;
        for (String[] item : rows) {
            if (item == null || item.length < 4) continue;
            if (count % 3 == 0) {
                row = new LinearLayout(context);
                row.setGravity(Gravity.TOP);
                grid.addView(row, LayoutHelper.createLinear(-1, AndroidUtilities.dp(146), 0, 0, 0, 6));
            }
            addGift(row, item, count);
            count++;
        }
    }

    private void addGift(LinearLayout row, String[] p, int index) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(AndroidUtilities.dp(4), AndroidUtilities.dp(4), AndroidUtilities.dp(4), AndroidUtilities.dp(4));

        int[] backgrounds = {
            Color.rgb(36,52,72), Color.rgb(56,40,72), Color.rgb(30,67,65), Color.rgb(72,52,36),
            Color.rgb(44,44,76), Color.rgb(67,38,57), Color.rgb(34,64,48), Color.rgb(66,48,76)
        };
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(backgrounds[index % backgrounds.length]);
        bg.setCornerRadius(AndroidUtilities.dp(15));
        bg.setStroke(AndroidUtilities.dp(1), Color.argb(90, 255, 255, 255));
        card.setBackground(bg);

        TextView emoji = label(p[3], 42 + (index % 5), false);
        emoji.setGravity(Gravity.CENTER);
        card.addView(emoji, LayoutHelper.createLinear(-1, AndroidUtilities.dp(74)));

        TextView name = label(p[1], 11, true);
        name.setGravity(Gravity.CENTER);
        name.setMaxLines(1);
        card.addView(name, LayoutHelper.createLinear(-1, AndroidUtilities.dp(22)));

        long price = parsePrice(p[2]);
        TextView priceView = label("⭐ " + price, 10, true);
        priceView.setTextColor(Color.rgb(255,193,55));
        priceView.setGravity(Gravity.CENTER);
        card.addView(priceView, LayoutHelper.createLinear(-1, AndroidUtilities.dp(20)));

        TextView rarity = label(rarityForPrice(price), 9, true);
        rarity.setGravity(Gravity.CENTER);
        card.addView(rarity, LayoutHelper.createLinear(-1, AndroidUtilities.dp(18)));

        AlphaAnimation glow = new AlphaAnimation(0.78f, 1.0f);
        glow.setDuration(650L + (index % 8) * 120L);
        glow.setRepeatMode(Animation.REVERSE);
        glow.setRepeatCount(Animation.INFINITE);
        emoji.startAnimation(glow);

        row.addView(card, LayoutHelper.createLinear(0, AndroidUtilities.dp(140), 1f, 0, 0, 0, 4));
    }

    private long parsePrice(String value) {
        try { return Long.parseLong(value); } catch (Exception e) { return 0L; }
    }

    private String rarityForPrice(long price) {
        if (price >= 100000) return "MYTHIC";
        if (price >= 25000) return "LEGENDARY";
        if (price >= 5000) return "EPIC";
        if (price >= 2500) return "RARE";
        return "COMMON";
    }

    private TextView label(String text, float size, boolean bold) {
        TextView t = new TextView(context);
        t.setText(text);
        t.setTextSize(size);
        if (bold) t.setTypeface(null, Typeface.BOLD);
        t.setTextColor(Color.WHITE);
        return t;
    }
}
