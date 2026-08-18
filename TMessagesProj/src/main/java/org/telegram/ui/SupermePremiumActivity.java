package org.telegram.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.LayoutHelper;

/** Premium storefront backed by Superme order/transaction infrastructure. */
public class SupermePremiumActivity extends BaseFragment {
    @Override
    public View createView(Context context) {
        actionBar.setTitle("Premium");

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(40), AndroidUtilities.dp(24), AndroidUtilities.dp(24));

        TextView title = new TextView(context);
        title.setText("⭐ Premium");
        title.setTextSize(28);
        title.setTypeface(null, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        root.addView(title, LayoutHelper.createLinear(-1, AndroidUtilities.dp(60)));

        TextView info = new TextView(context);
        info.setText("Superme orqali xarid qilinadi. Telegram to‘lovi ishlatilmaydi.");
        info.setGravity(Gravity.CENTER);
        info.setTextSize(15);
        root.addView(info, LayoutHelper.createLinear(-1, AndroidUtilities.dp(60)));

        Button monthly = new Button(context);
        monthly.setText("Oyiga 15 000 so‘m");
        root.addView(monthly, LayoutHelper.createLinear(-1, AndroidUtilities.dp(52), 0, 0, 0, 12));

        Button yearly = new Button(context);
        yearly.setText("Yiliga 45 000 so‘m");
        root.addView(yearly, LayoutHelper.createLinear(-1, AndroidUtilities.dp(52)));

        TextView status = new TextView(context);
        status.setGravity(Gravity.CENTER);
        status.setTextSize(13);
        root.addView(status, LayoutHelper.createLinear(-1, AndroidUtilities.dp(70), 0, AndroidUtilities.dp(20), 0, 0));

        monthly.setOnClickListener(v -> createOrder("premium_month", status));
        yearly.setOnClickListener(v -> createOrder("premium_year", status));

        fragmentView = root;
        return root;
    }

    private void createOrder(String productId, TextView status) {
        status.setText("Buyurtma yaratilmoqda…");
        new Thread(() -> {
            try {
                JSONObject result = CustomGiftApi.createSubscriptionOrder("premium", productId);
                String orderId = result.optString("order_id", "");
                AndroidUtilities.runOnUIThread(() -> status.setText(orderId.isEmpty()
                        ? "Buyurtma yaratildi, lekin ID olinmadi"
                        : "Superme buyurtmasi: " + orderId + "\nStatus: pending"));
            } catch (Exception e) {
                AndroidUtilities.runOnUIThread(() -> status.setText("Superme buyurtmasi xatosi: " + e.getMessage()));
            }
        }).start();
    }
}
