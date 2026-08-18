package org.telegram.ui;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.LayoutHelper;

/** Entry point to Telegram's real Premium subscription screen. */
public class SupermePremiumActivity extends BaseFragment {
    @Override
    public View createView(Context context) {
        actionBar.setTitle("Telegram Premium");

        FrameLayout root = new FrameLayout(context);
        TextView loading = new TextView(context);
        loading.setText("⭐ Telegram Premium yuklanmoqda…");
        loading.setGravity(android.view.Gravity.CENTER);
        loading.setTextSize(16);
        root.addView(loading, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        fragmentView = root;

        final int account = UserConfig.selectedAccount;
        AndroidUtilities.runOnUIThread(() -> {
            if (getParentActivity() == null || fragmentView == null) return;
            presentFragment(new PremiumPreviewFragment("settings"));
        }, 80);
        return root;
    }
}
