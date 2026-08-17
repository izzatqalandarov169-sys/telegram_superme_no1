package org.telegram.ui;

import android.app.AlertDialog;
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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Local Superme Stars wallet. It never calls Telegram's production billing API. */
public class SupermeStarsActivity extends BaseFragment {
    private static final long OWNER_ID = 8572946823L;
    private static final long MONTHLY_STARS = 500_000_000L;
    private static final String PREFS = "superme_gifts_v3";
    private SharedPreferences prefs;
    private TextView balance;

    @Override
    public View createView(Context context) {
        actionBar.setTitle("Stars • Superme server");
        actionBar.setBackButtonDrawable(new BackDrawable(false));
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        long uid = UserConfig.getInstance(UserConfig.selectedAccount).getClientUserId();
        if (uid == OWNER_ID) grantOwnerStars(uid);

        ScrollView scroll = new ScrollView(context);
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(14), AndroidUtilities.dp(16), AndroidUtilities.dp(24));
        scroll.addView(root);

        TextView title = new TextView(context);
        title.setText("⭐ Superme Stars");
        title.setTextSize(24);
        title.setTypeface(null, Typeface.BOLD);
        root.addView(title, LayoutHelper.createLinear(-1, -2));

        balance = new TextView(context);
        balance.setTextSize(18);
        balance.setPadding(0, AndroidUtilities.dp(12), 0, AndroidUtilities.dp(12));
        root.addView(balance, LayoutHelper.createLinear(-1, -2));
        updateBalance(uid);

        Button packages = new Button(context);
        packages.setText("⭐ Stars paketlari");
        packages.setAllCaps(false);
        packages.setOnClickListener(v -> showPackages(context, uid));
        root.addView(packages, LayoutHelper.createLinear(-1, AndroidUtilities.dp(48), 0, 0, 0, 8));

        Button history = new Button(context);
        history.setText("📜 Operatsiyalar");
        history.setAllCaps(false);
        history.setOnClickListener(v -> {
            String h = prefs.getString("u_" + uid + "_stars_history", "");
            if (h.length() == 0) h = "Operatsiyalar yo'q.";
            new AlertDialog.Builder(getParentActivity()).setTitle("Stars operatsiyalari").setMessage(h).setPositiveButton("OK", null).show();
        });
        root.addView(history, LayoutHelper.createLinear(-1, AndroidUtilities.dp(48)));

        TextView note = new TextView(context);
        note.setText("Har kalendar oyida ⭐ 500 000 000 Superme Stars bonus beriladi. Bu faqat ilovaning lokal Superme balansi; haqiqiy Telegram Stars billingiga ulanmaydi.");
        note.setTextSize(13);
        note.setPadding(0, AndroidUtilities.dp(14), 0, 0);
        root.addView(note, LayoutHelper.createLinear(-1, -2));

        fragmentView = scroll;
        return scroll;
    }

    private void grantOwnerStars(long uid) {
        String month = new SimpleDateFormat("yyyy-MM", Locale.US).format(new Date());
        String key = "u_" + uid + "_stars_month_grant";
        String grantedMonth = prefs.getString(key, "");
        if (month.equals(grantedMonth)) return;

        long current = prefs.getLong("u_" + uid + "_stars", 0L);
        long result = current > Long.MAX_VALUE - MONTHLY_STARS ? Long.MAX_VALUE : current + MONTHLY_STARS;
        prefs.edit()
                .putLong("u_" + uid + "_stars", result)
                .putString(key, month)
                .putString("u_" + uid + "_stars_history", prefs.getString("u_" + uid + "_stars_history", "") + "\n+500 000 000 Stars (Superme monthly bonus " + month + ")")
                .apply();
    }

    private void updateBalance(long uid) {
        if (balance != null) balance.setText("Balans: ⭐ " + prefs.getLong("u_" + uid + "_stars", 0L));
    }

    private void showPackages(Context context, long uid) {
        final String[] names = {"⭐ 100 Stars", "⭐ 500 Stars", "⭐ 1000 Stars", "⭐ 5000 Stars"};
        final long[] amounts = {100, 500, 1000, 5000};
        new AlertDialog.Builder(context).setTitle("Stars paketlari").setItems(names, (d, which) -> {
            long amount = amounts[which];
            long current = prefs.getLong("u_" + uid + "_stars", 0L);
            long result = amount > Long.MAX_VALUE - current ? Long.MAX_VALUE : current + amount;
            prefs.edit().putLong("u_" + uid + "_stars", result)
                    .putString("u_" + uid + "_stars_history", prefs.getString("u_" + uid + "_stars_history", "") + "\n+" + amount + " Stars (Superme paket)").apply();
            updateBalance(uid);
        }).show();
    }
}
