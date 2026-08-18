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

/** Premium storefront with both Superme Stars and so'm payment paths. */
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
        info.setText("Superme orqali xarid qilinadi");
        info.setGravity(Gravity.CENTER);
        info.setTextSize(15);
        root.addView(info, LayoutHelper.createLinear(-1, AndroidUtilities.dp(45)));

        Button starsMonthly = new Button(context);
        starsMonthly.setText("⭐ Oyiga 1 000 Stars");
        root.addView(starsMonthly, LayoutHelper.createLinear(-1, AndroidUtilities.dp(52), 0, 0, 0, 8));

        Button starsYearly = new Button(context);
        starsYearly.setText("⭐ Yiliga 500 Stars");
        root.addView(starsYearly, LayoutHelper.createLinear(-1, AndroidUtilities.dp(52), 0, 0, 0, 12));

        Button monthly = new Button(context);
        monthly.setText("💰 Oyiga 15 000 so‘m");
        root.addView(monthly, LayoutHelper.createLinear(-1, AndroidUtilities.dp(52), 0, 0, 0, 8));

        Button yearly = new Button(context);
        yearly.setText("💰 Yiliga 45 000 so‘m");
        root.addView(yearly, LayoutHelper.createLinear(-1, AndroidUtilities.dp(52)));

        TextView status = new TextView(context);
        status.setGravity(Gravity.CENTER);
        status.setTextSize(13);
        root.addView(status, LayoutHelper.createLinear(-1, AndroidUtilities.dp(80), 0, AndroidUtilities.dp(16), 0, 0));

        starsMonthly.setOnClickListener(v -> purchaseStars("premium_month", status));
        starsYearly.setOnClickListener(v -> purchaseStars("premium_year", status));
        monthly.setOnClickListener(v -> createOrder("premium_month", status));
        yearly.setOnClickListener(v -> createOrder("premium_year", status));

        fragmentView = root;
        return root;
    }

    private void purchaseStars(String productId, TextView status) {
        status.setText("⭐ Stars balans tekshirilmoqda…");
        new Thread(() -> {
            try {
                JSONObject result = CustomGiftApi.purchaseSubscriptionWithStars(productId);
                long balance = result.optLong("balance", -1);
                AndroidUtilities.runOnUIThread(() -> status.setText(
                        "✅ Premium faollashtirildi\nQolgan Stars: " + balance));
            } catch (Exception e) {
                AndroidUtilities.runOnUIThread(() -> status.setText("❌ Stars xaridi: " + e.getMessage()));
            }
        }).start();
    }

    private void createOrder(String productId, TextView status) {
        status.setText("So‘m buyurtmasi yaratilmoqda…");
        new Thread(() -> {
            try {
                JSONObject result = CustomGiftApi.createSubscriptionOrder("premium", productId);
                String orderId = result.optString("order_id", "");
                AndroidUtilities.runOnUIThread(() -> status.setText(orderId.isEmpty()
                        ? "Buyurtma yaratildi, lekin ID olinmadi"
                        : "So‘m buyurtmasi: " + orderId + "\nStatus: pending"));
            } catch (Exception e) {
                AndroidUtilities.runOnUIThread(() -> status.setText("❌ So‘m buyurtmasi: " + e.getMessage()));
            }
        }).start();
    }
}
