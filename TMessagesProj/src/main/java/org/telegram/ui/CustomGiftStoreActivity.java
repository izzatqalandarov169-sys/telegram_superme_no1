package org.telegram.ui;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Gifts.GiftSheet;

/**
 * Entry point for Telegram's real Star Gift catalog.
 * The actual catalog, stock state, profile gifts and Stars payment flow are
 * provided by Telegram's Gifts/Stars implementation from the upstream client.
 */
public class CustomGiftStoreActivity extends BaseFragment {

    @Override
    public View createView(Context context) {
        actionBar.setTitle("Hadya");

        FrameLayout root = new FrameLayout(context);
        TextView loading = new TextView(context);
        loading.setText("🎁 Telegram hadyalari yuklanmoqda…");
        loading.setGravity(Gravity.CENTER);
        loading.setTextSize(16);
        root.addView(loading, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        fragmentView = root;

        final int account = UserConfig.selectedAccount;
        final long userId = UserConfig.getInstance(account).getClientUserId();
        AndroidUtilities.runOnUIThread(() -> {
            if (getParentActivity() == null || fragmentView == null) {
                return;
            }
            new GiftSheet(context, account, userId, this::finishFragment).show();
        }, 80);

        return root;
    }
}
