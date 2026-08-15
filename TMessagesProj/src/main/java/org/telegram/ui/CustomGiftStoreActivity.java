package org.telegram.ui;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.BackDrawable;
import org.telegram.ui.Components.LayoutHelper;

public class CustomGiftStoreActivity extends BaseFragment {
    private LinearLayout root;
    @Override public View createView(Context context) {
        actionBar.setTitle("Gifts");
        actionBar.setBackButtonDrawable(new BackDrawable(false));
        root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(12), AndroidUtilities.dp(16), AndroidUtilities.dp(12));
        TextView info = new TextView(context);
        info.setText("Giftlar sizning serveringizdan olinadi.");
        root.addView(info, LayoutHelper.createLinear(-1, -2));
        TextView load = new TextView(context);
        load.setText("Giftlarni yuklash");
        load.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(18), AndroidUtilities.dp(16), AndroidUtilities.dp(18));
        load.setOnClickListener(v -> new Thread(() -> {
            try {
                String response = CustomGiftApi.get("/api/gifts");
                AndroidUtilities.runOnUIThread(() -> Toast.makeText(context, "Server javobi olindi", Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                AndroidUtilities.runOnUIThread(() -> Toast.makeText(context, "Gift serveri bilan aloqa xatosi", Toast.LENGTH_SHORT).show());
            }
        }).start());
        root.addView(load, LayoutHelper.createLinear(-1, -2));
        fragmentView = root;
        return root;
    }
}
