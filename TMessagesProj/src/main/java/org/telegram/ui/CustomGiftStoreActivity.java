package org.telegram.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.BackDrawable;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.LayoutHelper;

import java.util.ArrayList;
import java.util.List;

/** Local Superme gift store. Purchases are local to this clone and never charge Telegram. */
public class CustomGiftStoreActivity extends BaseFragment {
    private static final long OWNER_ID = 8572946823L;
    private static final long OWNER_FREE_STARS = 999_000_000_000_000L;

    private SharedPreferences prefs;
    private LinearLayout grid;
    private TextView balance;
    private EditText search;
    private long uid;
    private final List<Integer> visibleIds = new ArrayList<>();

    @Override
    public View createView(Context context) {
        actionBar.setTitle("Hadya sotib olish");
        actionBar.setBackButtonDrawable(new BackDrawable(false));
        prefs = context.getSharedPreferences("local_admin_panel", Context.MODE_PRIVATE);
        uid = UserConfig.getInstance(UserConfig.selectedAccount).getClientUserId();
        ensureOwnerWallet();

        ScrollView scroll = new ScrollView(context);
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(8), AndroidUtilities.dp(10), AndroidUtilities.dp(20));
        scroll.addView(root);

        LinearLayout top = new LinearLayout(context);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(android.view.Gravity.CENTER_VERTICAL);
        TextView title = new TextView(context);
        title.setText("🎁  Hadya\n" + GiftCatalog.COUNT + " ta gift");
        title.setTextSize(20);
        title.setTypeface(null, Typeface.BOLD);
        top.addView(title, LayoutHelper.createLinear(0, -2, 1f));
        balance = new TextView(context);
        balance.setGravity(android.view.Gravity.RIGHT);
        balance.setTextSize(15);
        balance.setTypeface(null, Typeface.BOLD);
        top.addView(balance, LayoutHelper.createLinear(AndroidUtilities.dp(170), -2));
        root.addView(top, LayoutHelper.createLinear(-1, -2, 0, 0, 0, 8));
        updateBalance();

        search = new EditText(context);
        search.setSingleLine(true);
        search.setHint("🔎 Gift qidirish...");
        search.setTextSize(16);
        root.addView(search, LayoutHelper.createLinear(-1, AndroidUtilities.dp(50), 0, 0, 0, 8));

        LinearLayout filters = new LinearLayout(context);
        filters.setOrientation(LinearLayout.HORIZONTAL);
        addFilterButton(context, filters, "Narx", 0);
        addFilterButton(context, filters, "Model", 1);
        addFilterButton(context, filters, "Fon", 2);
        addFilterButton(context, filters, "Naqsh", 3);
        root.addView(filters, LayoutHelper.createLinear(-1, AndroidUtilities.dp(48), 0, 0, 0, 8));

        Button mine = new Button(context);
        mine.setText("🎁 Mening giftlarim");
        mine.setAllCaps(false);
        mine.setOnClickListener(v -> showOwnedGifts(context));
        root.addView(mine, LayoutHelper.createLinear(-1, AndroidUtilities.dp(46), 0, 0, 0, 8));

        grid = new LinearLayout(context);
        grid.setOrientation(LinearLayout.VERTICAL);
        root.addView(grid, LayoutHelper.createLinear(-1, -2));

        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { rebuildGrid(context); }
            @Override public void afterTextChanged(Editable s) {}
        });
        rebuildGrid(context);
        fragmentView = scroll;
        return scroll;
    }

    private void ensureOwnerWallet() {
        if (uid == OWNER_ID && prefs.getLong("u_" + uid + "_stars", 0L) < OWNER_FREE_STARS) {
            prefs.edit().putLong("u_" + uid + "_stars", OWNER_FREE_STARS).apply();
        }
    }

    private void updateBalance() {
        if (balance != null) {
            balance.setText("Balans\n⭐ " + prefs.getLong("u_" + uid + "_stars", 0L));
        }
    }

    private void addFilterButton(Context context, LinearLayout row, String label, int type) {
        Button b = new Button(context);
        b.setText(label + " ↕");
        b.setAllCaps(false);
        b.setTextSize(13);
        b.setOnClickListener(v -> showFilter(context, type));
        row.addView(b, LayoutHelper.createLinear(0, AndroidUtilities.dp(44), 1f, 0, 0, 0, 4));
    }

    private void showFilter(Context context, int type) {
        String title;
        String[] values;
        if (type == 0) {
            title = "Narx bo'yicha";
            values = new String[]{"Hammasi", "0–100", "101–500", "501–5 000", "5 001+"};
        } else if (type == 1) {
            title = "Model";
            values = new String[]{"Hammasi", "Classic", "Holiday", "Love", "Galaxy", "Nature", "Fantasy", "Premium"};
        } else if (type == 2) {
            title = "Fon";
            values = new String[]{"Hammasi", "Ocean", "Forest", "Lunar", "Solar", "Magic", "Rare"};
        } else {
            title = "Naqsh";
            values = new String[]{"Hammasi", "Classic", "Holiday", "Love", "Galaxy", "Nature", "Festival"};
        }
        new AlertDialog.Builder(context).setTitle(title).setItems(values, (d, which) -> search.setText(which == 0 ? "" : values[which])).show();
    }

    private void rebuildGrid(Context context) {
        if (grid == null) return;
        grid.removeAllViews();
        visibleIds.clear();
        String q = search == null ? "" : search.getText().toString().trim().toLowerCase();
        for (int id = 1; id <= GiftCatalog.COUNT; id++) {
            String hay = (GiftCatalog.name(id) + " " + GiftCatalog.rarity(id) + " " + GiftCatalog.theme(id)).toLowerCase();
            if (q.length() == 0 || hay.contains(q) || String.valueOf(id).equals(q)) visibleIds.add(id);
        }
        LinearLayout row = null;
        for (int i = 0; i < visibleIds.size(); i++) {
            if (i % 3 == 0) {
                row = new LinearLayout(context);
                row.setOrientation(LinearLayout.HORIZONTAL);
                grid.addView(row, LayoutHelper.createLinear(-1, -2, 0, 0, 0, 6));
            }
            addGiftCard(context, row, visibleIds.get(i));
        }
        if (visibleIds.isEmpty()) {
            TextView empty = new TextView(context);
            empty.setText("Gift topilmadi");
            empty.setGravity(android.view.Gravity.CENTER);
            empty.setTextSize(17);
            grid.addView(empty, LayoutHelper.createLinear(-1, AndroidUtilities.dp(100)));
        }
    }

    private void addGiftCard(Context context, LinearLayout row, int id) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
        card.setPadding(AndroidUtilities.dp(6), AndroidUtilities.dp(8), AndroidUtilities.dp(6), AndroidUtilities.dp(8));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.rgb(31, 43, 56));
        bg.setCornerRadius(AndroidUtilities.dp(12));
        card.setBackground(bg);

        TextView ribbon = new TextView(context);
        ribbon.setText(GiftCatalog.rarity(id));
        ribbon.setTextSize(10);
        ribbon.setTextColor(Color.WHITE);
        ribbon.setGravity(android.view.Gravity.CENTER);
        card.addView(ribbon, LayoutHelper.createLinear(-1, AndroidUtilities.dp(20)));

        TextView emoji = new TextView(context);
        emoji.setText(GiftCatalog.emoji(id));
        emoji.setTextSize(38);
        emoji.setGravity(android.view.Gravity.CENTER);
        card.addView(emoji, LayoutHelper.createLinear(-1, AndroidUtilities.dp(62)));

        TextView name = new TextView(context);
        name.setText(GiftCatalog.name(id));
        name.setTextSize(12);
        name.setTypeface(null, Typeface.BOLD);
        name.setGravity(android.view.Gravity.CENTER);
        name.setMaxLines(2);
        card.addView(name, LayoutHelper.createLinear(-1, AndroidUtilities.dp(38)));

        TextView price = new TextView(context);
        price.setText("⭐ " + GiftCatalog.price(id));
        price.setTextSize(13);
        price.setTypeface(null, Typeface.BOLD);
        price.setTextColor(Color.rgb(255, 190, 40));
        price.setGravity(android.view.Gravity.CENTER);
        card.addView(price, LayoutHelper.createLinear(-1, AndroidUtilities.dp(28)));

        Button buy = new Button(context);
        buy.setText("Sotib olish");
        buy.setAllCaps(false);
        buy.setTextSize(12);
        buy.setOnClickListener(v -> buyGift(context, id));
        card.addView(buy, LayoutHelper.createLinear(-1, AndroidUtilities.dp(42)));
        row.addView(card, LayoutHelper.createLinear(0, AndroidUtilities.dp(220), 1f, 0, 0, 0, 4));
    }

    private void buyGift(Context context, int id) {
        ensureOwnerWallet();
        long price = GiftCatalog.price(id);
        long balanceNow = prefs.getLong("u_" + uid + "_stars", 0L);
        if (uid != OWNER_ID && balanceNow < price) {
            Toast.makeText(context, "Stars yetarli emas", Toast.LENGTH_SHORT).show();
            return;
        }
        // Owner balance stays at 999 trillion, so every gift is effectively unlimited for the owner.
        if (uid == OWNER_ID) {
            prefs.edit().putLong("u_" + uid + "_stars", OWNER_FREE_STARS).apply();
        } else {
            prefs.edit().putLong("u_" + uid + "_stars", balanceNow - price).apply();
        }

        String key = "u_" + uid + "_owned_gifts";
        String giftRow = id + "|" + GiftCatalog.name(id) + "|" + price + "|" + GiftCatalog.emoji(id);
        String old = prefs.getString(key, "");
        prefs.edit().putString(key, old.length() == 0 ? giftRow : old + "\n" + giftRow).apply();
        updateBalance();
        Toast.makeText(context, "🎁 " + GiftCatalog.name(id) + " olindi", Toast.LENGTH_SHORT).show();
    }

    private void showOwnedGifts(Context context) {
        String gifts = prefs.getString("u_" + uid + "_owned_gifts", "");
        if (gifts.length() == 0) gifts = "Hozircha sizda gift yo'q.";
        new AlertDialog.Builder(context).setTitle("Mening giftlarim").setMessage(gifts).setPositiveButton("OK", null).show();
    }
}
