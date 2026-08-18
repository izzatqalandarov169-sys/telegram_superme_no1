package org.telegram.ui;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Stars.StarsIntroActivity;

/** Entry point to Telegram's real Stars screen and payment flow. */
public class SupermeStarsActivity extends BaseFragment {
    @Override
    public View createView(Context context) {
        actionBar.setTitle("Telegram Yulduzlar");

        FrameLayout root = new FrameLayout(context);
        TextView loading = new TextView(context);
        loading.setText("⭐ Telegram Yulduzlar yuklanmoqda…");
        loading.setGravity(Gravity.CENTER);
        loading.setTextSize(16);
        root.addView(loading, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        fragmentView = root;

        AndroidUtilities.runOnUIThread(() -> {
            if (getParentActivity() == null || fragmentView == null) return;
            presentFragment(new StarsIntroActivity());
        }, 80);
        return root;
    }
}
