package org.telegram.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.Components.LayoutHelper;

/** Local Superme gifts shown inside the profile's Gifts tab. */
public class SupermeProfileGiftsView extends ScrollView {
    private final Context context;
    private final LinearLayout grid;
    private final SharedPreferences prefs;
    private final long uid;

    public SupermeProfileGiftsView(Context context) {
        super(context);
        this.context = context;
        setFillViewport(true);
        setVerticalScrollBarEnabled(false);
        setBackgroundColor(Color.TRANSPARENT);
        prefs = context.getSharedPreferences("superme_gifts_v3", Context.MODE_PRIVATE);
        uid = UserConfig.getInstance(UserConfig.selectedAccount).getClientUserId();

        grid = new LinearLayout(context);
        grid.setOrientation(LinearLayout.VERTICAL);
        grid.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(8), AndroidUtilities.dp(8), AndroidUtilities.dp(90));
        addView(grid, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        rebuild();
    }

    public void rebuild() {
        grid.removeAllViews();
        String rows = prefs.getString("received_" + uid, "");
        if (rows == null || rows.trim().isEmpty()) {
            TextView empty = label("🎁\nHozircha olingan giftlar yo'q", 16, false);
            empty.setGravity(Gravity.CENTER);
            grid.addView(empty, LayoutHelper.createLinear(-1, AndroidUtilities.dp(180)));
            return;
        }

        String[] items = rows.split("\\n");
        LinearLayout row = null;
        int count = 0;
        for (String item : items) {
            if (item.trim().isEmpty()) continue;
            String[] p = item.split("\\|", -1);
            if (p.length < 4) continue;
            if (count % 3 == 0) {
                row = new LinearLayout(context);
                row.setGravity(Gravity.TOP);
                grid.addView(row, LayoutHelper.createLinear(-1, AndroidUtilities.dp(146), 0, 0, 0, 6));
            }
            addGift(row, p, count);
            count++;
        }
    }

    private void addGift(LinearLayout row, String[] p, int index) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(AndroidUtilities.dp(4), AndroidUtilities.dp(4), AndroidUtilities.dp(4), AndroidUtilities.dp(4));

        int[] backgrounds = {
                Color.rgb(36, 52, 72), Color.rgb(56, 40, 72), Color.rgb(30, 67, 65),
                Color.rgb(72, 52, 36), Color.rgb(44, 44, 76), Color.rgb(67, 38, 57)
        };
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(backgrounds[index % backgrounds.length]);
        bg.setCornerRadius(AndroidUtilities.dp(15));
        card.setBackground(bg);

        TextView emoji = label(p[3], 43 + (index % 4), false);
        emoji.setGravity(Gravity.CENTER);
        card.addView(emoji, LayoutHelper.createLinear(-1, AndroidUtilities.dp(78)));

        TextView name = label(p[1], 11, true);
        name.setGravity(Gravity.CENTER);
        name.setMaxLines(1);
        card.addView(name, LayoutHelper.createLinear(-1, AndroidUtilities.dp(22)));

        TextView price = label("⭐ " + p[2], 10, true);
        price.setTextColor(Color.rgb(255, 193, 55));
        price.setGravity(Gravity.CENTER);
        card.addView(price, LayoutHelper.createLinear(-1, AndroidUtilities.dp(20)));

        AlphaAnimation glow = new AlphaAnimation(0.78f, 1.0f);
        glow.setDuration(700L + (index % 7) * 130L);
        glow.setRepeatMode(Animation.REVERSE);
        glow.setRepeatCount(Animation.INFINITE);
        emoji.startAnimation(glow);

        row.addView(card, LayoutHelper.createLinear(0, AndroidUtilities.dp(140), 1f, 0, 0, 0, 4));
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
