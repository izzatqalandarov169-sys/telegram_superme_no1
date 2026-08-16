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
import android.widget.Toast;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.BackDrawable;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.LayoutHelper;

public class CustomGiftStoreActivity extends BaseFragment {
    private LinearLayout root;
    private SharedPreferences prefs;

    @Override
    public View createView(Context context) {
        actionBar.setTitle("Hadya sotib olish • 1200+");
        actionBar.setBackButtonDrawable(new BackDrawable(false));
        prefs = context.getSharedPreferences("local_admin_panel", Context.MODE_PRIVATE);
        ScrollView scroll = new ScrollView(context);
        root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(8), AndroidUtilities.dp(10), AndroidUtilities.dp(18));
        scroll.addView(root);

        TextView header = new TextView(context);
        header.setText("🎁 1200 ta standart gift\n⭐ 15–5000 Stars oralig'ida\n🆔 Har bir giftning o'z ID'si bor");
        header.setTextSize(16);
        header.setTypeface(null, Typeface.BOLD);
        header.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(8), AndroidUtilities.dp(10), AndroidUtilities.dp(12));
        root.addView(header);

        Button mine = new Button(context);
        mine.setText("🎁 Mening giftlarim");
        mine.setAllCaps(false);
        mine.setOnClickListener(v -> showOwnedGifts(context));
        root.addView(mine, LayoutHelper.createLinear(-1, AndroidUtilities.dp(48), 0, 0, 0, 6));

        Button catalog = new Button(context);
        catalog.setText("🛍️ Katalogni ko'rsatish");
        catalog.setAllCaps(false);
        catalog.setOnClickListener(v -> buildCatalog(context));
        root.addView(catalog, LayoutHelper.createLinear(-1, AndroidUtilities.dp(48), 0, 0, 0, 6));

        buildCatalog(context);
        fragmentView = scroll;
        return scroll;
    }

    private void buildCatalog(Context context) {
        while (root.getChildCount() > 3) root.removeViewAt(3);
        LinearLayout list = new LinearLayout(context);
        list.setOrientation(LinearLayout.VERTICAL);
        for (int id = 1; id <= GiftCatalog.COUNT; id++) addGiftCard(context, list, id);
        TextView footer = new TextView(context);
        footer.setText("\nJami: " + GiftCatalog.COUNT + " ta gift");
        footer.setTextSize(14);
        footer.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(12), AndroidUtilities.dp(10), AndroidUtilities.dp(20));
        list.addView(footer);
        root.addView(list, LayoutHelper.createLinear(-1, -2));
    }

    private void addGiftCard(Context context, LinearLayout list, int id) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(10), AndroidUtilities.dp(12), AndroidUtilities.dp(10));
        TextView gift = new TextView(context);
        gift.setText(GiftCatalog.emoji(id) + "\n" + GiftCatalog.name(id));
        gift.setTextSize(18);
        gift.setTypeface(null, Typeface.BOLD);
        card.addView(gift);
        TextView meta = new TextView(context);
        meta.setText("ID: #" + id + "\nNarxi: ⭐ " + GiftCatalog.price(id) + "\nRarity: " + GiftCatalog.rarity(id));
        meta.setTextSize(14);
        meta.setPadding(0, AndroidUtilities.dp(5), 0, AndroidUtilities.dp(5));
        card.addView(meta);
        Button buy = new Button(context);
        buy.setText("⭐ " + GiftCatalog.price(id) + " — Sotib olish");
        buy.setAllCaps(false);
        buy.setOnClickListener(v -> buyGift(context, id));
        card.addView(buy);
        list.addView(card, LayoutHelper.createLinear(-1, -2, 0, 0, 0, 4));
    }

    private void buyGift(Context context, int id) {
        long userId = UserConfig.getInstance(UserConfig.selectedAccount).getClientUserId();
        String starsKey = "u_" + userId + "_stars";
        long balance = prefs.getLong(starsKey, 0L);
        long price = GiftCatalog.price(id);
        if (balance < price) {
            Toast.makeText(context, "Stars yetarli emas. Kerak: " + price + ", bor: " + balance, Toast.LENGTH_LONG).show();
            return;
        }
        prefs.edit().putLong(starsKey, balance - price).apply();
        String key = "u_" + userId + "_owned_gifts";
        String row = id + "|" + GiftCatalog.name(id) + "|" + price;
        String old = prefs.getString(key, "");
        prefs.edit().putString(key, old.length() == 0 ? row : old + "\n" + row).apply();
        Toast.makeText(context, "🎁 " + GiftCatalog.name(id) + " olindi! -" + price + " Stars", Toast.LENGTH_SHORT).show();
    }

    private void showOwnedGifts(Context context) {
        long userId = UserConfig.getInstance(UserConfig.selectedAccount).getClientUserId();
        String gifts = prefs.getString("u_" + userId + "_owned_gifts", "");
        if (gifts.length() == 0) gifts = "Hozircha sizda gift yo'q.";
        new AlertDialog.Builder(context).setTitle("Mening giftlarim").setMessage(gifts).setPositiveButton("OK", null).show();
    }
}
