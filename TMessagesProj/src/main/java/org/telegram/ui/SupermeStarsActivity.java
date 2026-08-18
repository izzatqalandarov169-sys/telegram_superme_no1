package org.telegram.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.LayoutHelper;

/** Superme Stars screen. The displayed balance comes from the Superme backend. */
public class SupermeStarsActivity extends BaseFragment {
    @Override
    public View createView(Context context) {
        actionBar.setTitle("Stars");

        FrameLayout root = new FrameLayout(context);
        TextView balance = new TextView(context);
        balance.setGravity(Gravity.CENTER);
        balance.setText("⭐ Superme Stars\nYuklanmoqda…");
        balance.setTextSize(24);
        balance.setTypeface(null, Typeface.BOLD);
        balance.setTextColor(Color.WHITE);
        root.addView(balance, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));
        fragmentView = root;

        new Thread(() -> {
            try {
                JSONObject response = CustomGiftApi.getSupermeBalance();
                long stars = response.optLong("stars", 0L);
                AndroidUtilities.runOnUIThread(() -> balance.setText("⭐ Superme Stars\n" + String.format("%,d", stars)));
            } catch (Exception e) {
                AndroidUtilities.runOnUIThread(() -> balance.setText("⭐ Superme Stars\nBalansni yuklab bo‘lmadi"));
            }
        }).start();

        return root;
    }
}
