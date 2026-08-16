package org.telegram.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.BackDrawable;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.LayoutHelper;

/** Local Superme Premium state. This does not change Telegram production billing/account state. */
public class SupermePremiumActivity extends BaseFragment {
    private static final long OWNER_ID = 8572946823L;
    private static final int OWNER_FREE_MONTHS = Integer.MAX_VALUE;
    private SharedPreferences prefs;
    private TextView status;

    @Override
    public View createView(Context context) {
        actionBar.setTitle("Premium • Superme server");
        actionBar.setBackButtonDrawable(new BackDrawable(false));
        prefs = context.getSharedPreferences("local_admin_panel", Context.MODE_PRIVATE);
        long uid = UserConfig.getInstance(UserConfig.selectedAccount).getClientUserId();
        if (uid == OWNER_ID) {
            prefs.edit().putInt("u_" + uid + "_premium_months", OWNER_FREE_MONTHS).apply();
        }

        ScrollView scroll = new ScrollView(context);
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(14), AndroidUtilities.dp(16), AndroidUtilities.dp(24));
        scroll.addView(root);

        TextView title = new TextView(context);
        title.setText("⭐ Superme Premium");
        title.setTextSize(24);
        title.setTypeface(null, Typeface.BOLD);
        root.addView(title, LayoutHelper.createLinear(-1, -2));

        status = new TextView(context);
        status.setTextSize(18);
        status.setPadding(0, AndroidUtilities.dp(14), 0, AndroidUtilities.dp(14));
        root.addView(status, LayoutHelper.createLinear(-1, -2));
        update(uid);

        if (uid != OWNER_ID) {
            addButton(context, root, "Premium +1 oy", 1, uid);
            addButton(context, root, "Premium +3 oy", 3, uid);
            addButton(context, root, "Premium +6 oy", 6, uid);
            addButton(context, root, "Premium +12 oy", 12, uid);
        }

        TextView note = new TextView(context);
        note.setText(uid == OWNER_ID
                ? "👑 Owner: Premium cheksiz. Har oy qayta aktiv bo'lib turadi."
                : "Bu Premium holati Superme ilovasi/backendi uchun. Telegram production akkauntining Premium holatini o'zgartirmaydi.");
        note.setTextSize(13);
        note.setPadding(0, AndroidUtilities.dp(14), 0, 0);
        root.addView(note, LayoutHelper.createLinear(-1, -2));

        fragmentView = scroll;
        return scroll;
    }

    private void addButton(Context context, LinearLayout root, String text, int months, long uid) {
        Button button = new Button(context);
        button.setText(text);
        button.setAllCaps(false);
        button.setOnClickListener(v -> {
            String key = "u_" + uid + "_premium_months";
            int current = prefs.getInt(key, 0);
            long next = (long) current + months;
            prefs.edit().putInt(key, next > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) next).apply();
            update(uid);
        });
        root.addView(button, LayoutHelper.createLinear(-1, AndroidUtilities.dp(48), 0, 0, 0, 6));
    }

    private void update(long uid) {
        if (status == null) return;
        if (uid == OWNER_ID) {
            status.setText("Premium faol • ♾️ cheksiz");
        } else {
            int months = prefs.getInt("u_" + uid + "_premium_months", 0);
            status.setText(months > 0 ? "Premium faol • qolgan: " + months + " oy" : "Premium faol emas");
        }
    }
}
