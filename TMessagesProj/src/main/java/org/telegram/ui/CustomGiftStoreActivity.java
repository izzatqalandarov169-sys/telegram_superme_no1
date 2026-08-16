package org.telegram.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Superme gift marketplace.
 * All gift purchases/sends in this screen use the clone's local Superme wallet;
 * Telegram production Stars/Premium billing is not touched.
 */
public class CustomGiftStoreActivity extends BaseFragment {
    private static final long OWNER_ID = 8572946823L;
    private static final long MONTHLY_OWNER_STARS = 500_000_000L;
    private static final String PREFS = "superme_gifts_v3";
    private static final int PAGE_SIZE = 60;

    private SharedPreferences prefs;
    private LinearLayout content;
    private TextView balance;
    private EditText search;
    private Button more;
    private long uid;
    private int pageOffset;
    private boolean ownedMode;
    private final List<Integer> visibleIds = new ArrayList<>();

    @Override
    public View createView(Context context) {
        actionBar.setTitle("Hadya sotib olish • Superme");
        actionBar.setBackButtonDrawable(new BackDrawable(false));
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        uid = UserConfig.getInstance(UserConfig.selectedAccount).getClientUserId();
        ensureOwnerWallet();

        ScrollView scroll = new ScrollView(context);
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(8), AndroidUtilities.dp(12), AndroidUtilities.dp(24));
        scroll.addView(root);

        LinearLayout header = new LinearLayout(context);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = text(context, "🎁 Superme gift katalogi\n" + GiftCatalog.COUNT + " ta gift", 20, true);
        header.addView(title, LayoutHelper.createLinear(0, -2, 1f));
        balance = text(context, "", 14, true);
        balance.setGravity(Gravity.RIGHT);
        header.addView(balance, LayoutHelper.createLinear(AndroidUtilities.dp(170), -2));
        root.addView(header, LayoutHelper.createLinear(-1, -2, 0, 0, 0, 8));
        updateBalance();

        search = new EditText(context);
        search.setSingleLine(true);
        search.setHint("🔎 Gift qidirish...");
        search.setTextSize(16);
        root.addView(search, LayoutHelper.createLinear(-1, AndroidUtilities.dp(48), 0, 0, 0, 8));

        LinearLayout tabs = new LinearLayout(context);
        Button all = tab(context, "Barcha giftlar");
        Button mine = tab(context, "🎁 Mening giftlarim");
        tabs.addView(all, LayoutHelper.createLinear(0, AndroidUtilities.dp(46), 1f, 0, 0, 0, 4));
        tabs.addView(mine, LayoutHelper.createLinear(0, AndroidUtilities.dp(46), 1f, 0, 0, 0, 4));
        root.addView(tabs, LayoutHelper.createLinear(-1, AndroidUtilities.dp(50)));
        all.setOnClickListener(v -> { ownedMode = false; pageOffset = 0; rebuild(context); });
        mine.setOnClickListener(v -> { ownedMode = true; pageOffset = 0; rebuild(context); });

        LinearLayout filters = new LinearLayout(context);
        addFilter(context, filters, "Narx", 0);
        addFilter(context, filters, "Model", 1);
        addFilter(context, filters, "Fon", 2);
        addFilter(context, filters, "Naqsh", 3);
        root.addView(filters, LayoutHelper.createLinear(-1, AndroidUtilities.dp(48), 0, 0, 0, 8));

        content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        root.addView(content, LayoutHelper.createLinear(-1, -2));

        more = new Button(context);
        more.setText("Ko'proq giftlarni ko'rsatish");
        more.setAllCaps(false);
        more.setOnClickListener(v -> { pageOffset += PAGE_SIZE; rebuild(context); });
        root.addView(more, LayoutHelper.createLinear(-1, AndroidUtilities.dp(48), 0, 8, 0, 8));

        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                pageOffset = 0;
                rebuild(context);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        rebuild(context);
        fragmentView = scroll;
        return scroll;
    }

    private TextView text(Context c, String s, float size, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(size);
        if (bold) t.setTypeface(null, Typeface.BOLD);
        return t;
    }

    private Button tab(Context c, String label) {
        Button b = new Button(c);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(13);
        return b;
    }

    private void ensureOwnerWallet() {
        if (uid != OWNER_ID) return;
        String month = new SimpleDateFormat("yyyy-MM", Locale.US).format(new Date());
        String last = prefs.getString("owner_grant_month", "");
        if (!month.equals(last)) {
            long current = prefs.getLong("u_" + uid + "_stars", 0L);
            long next = current > Long.MAX_VALUE - MONTHLY_OWNER_STARS ? Long.MAX_VALUE : current + MONTHLY_OWNER_STARS;
            prefs.edit().putLong("u_" + uid + "_stars", next).putString("owner_grant_month", month).apply();
        }
    }

    private void updateBalance() {
        if (balance != null) balance.setText("Balans\n⭐ " + prefs.getLong("u_" + uid + "_stars", 0L));
    }

    private void addFilter(Context c, LinearLayout row, String label, int type) {
        Button b = new Button(c);
        b.setText(label + " ↕");
        b.setAllCaps(false);
        b.setTextSize(12);
        b.setOnClickListener(v -> showFilter(c, type));
        row.addView(b, LayoutHelper.createLinear(0, AndroidUtilities.dp(44), 1f, 0, 0, 0, 4));
    }

    private void showFilter(Context c, int type) {
        String title;
        String[] values;
        if (type == 0) {
            title = "Narx";
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
        new AlertDialog.Builder(c).setTitle(title).setItems(values, (d, which) -> {
            pageOffset = 0;
            search.setText(which == 0 ? "" : values[which]);
        }).show();
    }

    private void rebuild(Context c) {
        if (content == null) return;
        content.removeAllViews();
        visibleIds.clear();

        if (ownedMode) {
            buildOwned(c);
            if (more != null) more.setVisibility(View.GONE);
            return;
        }

        String q = search == null ? "" : search.getText().toString().trim().toLowerCase(Locale.US);
        int matched = 0;
        int rendered = 0;
        boolean hasMore = false;

        for (int id = 1; id <= GiftCatalog.COUNT; id++) {
            if (!matches(id, q)) continue;
            if (matched++ < pageOffset) continue;
            visibleIds.add(id);
            if (++rendered >= PAGE_SIZE) break;
        }

        if (visibleIds.size() == PAGE_SIZE) {
            int after = 0;
            for (int id = 1; id <= GiftCatalog.COUNT; id++) {
                if (!matches(id, q)) continue;
                if (after++ >= pageOffset + PAGE_SIZE) { hasMore = true; break; }
            }
        }

        LinearLayout row = null;
        for (int i = 0; i < visibleIds.size(); i++) {
            if (i % 3 == 0) {
                row = new LinearLayout(c);
                row.setOrientation(LinearLayout.HORIZONTAL);
                content.addView(row, LayoutHelper.createLinear(-1, -2, 0, 0, 0, 6));
            }
            addGiftCard(c, row, visibleIds.get(i));
        }

        if (visibleIds.isEmpty()) {
            TextView empty = text(c, "Gift topilmadi", 17, false);
            empty.setGravity(Gravity.CENTER);
            content.addView(empty, LayoutHelper.createLinear(-1, AndroidUtilities.dp(100)));
        }
        if (more != null) more.setVisibility(hasMore ? View.VISIBLE : View.GONE);
    }

    private boolean matches(int id, String q) {
        if (q.length() == 0) return true;
        String hay = (GiftCatalog.name(id) + " " + GiftCatalog.rarity(id) + " " + GiftCatalog.theme(id) + " " + GiftCatalog.price(id) + " " + id).toLowerCase(Locale.US);
        return hay.contains(q);
    }

    private void addGiftCard(Context c, LinearLayout row, int id) {
        LinearLayout card = new LinearLayout(c);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setPadding(AndroidUtilities.dp(5), AndroidUtilities.dp(5), AndroidUtilities.dp(5), AndroidUtilities.dp(5));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.rgb(31, 43, 56));
        bg.setCornerRadius(AndroidUtilities.dp(14));
        card.setBackground(bg);

        TextView idText = text(c, "#" + id, 10, false);
        idText.setGravity(Gravity.RIGHT);
        card.addView(idText, LayoutHelper.createLinear(-1, AndroidUtilities.dp(18)));

        TextView emoji = text(c, GiftCatalog.emoji(id), 42, false);
        emoji.setGravity(Gravity.CENTER);
        card.addView(emoji, LayoutHelper.createLinear(-1, AndroidUtilities.dp(62)));

        TextView name = text(c, GiftCatalog.name(id), 12, true);
        name.setGravity(Gravity.CENTER);
        name.setMaxLines(2);
        card.addView(name, LayoutHelper.createLinear(-1, AndroidUtilities.dp(38)));

        TextView rarity = text(c, GiftCatalog.rarity(id), 10, false);
        rarity.setGravity(Gravity.CENTER);
        card.addView(rarity, LayoutHelper.createLinear(-1, AndroidUtilities.dp(20)));

        TextView price = text(c, "⭐ " + GiftCatalog.price(id), 14, true);
        price.setTextColor(Color.rgb(255, 190, 40));
        price.setGravity(Gravity.CENTER);
        card.addView(price, LayoutHelper.createLinear(-1, AndroidUtilities.dp(26)));

        Button buy = new Button(c);
        buy.setText("Sotib olish");
        buy.setAllCaps(false);
        buy.setTextSize(11);
        buy.setOnClickListener(v -> showGift(c, id));
        card.addView(buy, LayoutHelper.createLinear(-1, AndroidUtilities.dp(42)));

        row.addView(card, LayoutHelper.createLinear(0, AndroidUtilities.dp(225), 1f, 0, 0, 0, 4));
    }

    private void showGift(Context c, int id) {
        String msg = GiftCatalog.emoji(id) + "  " + GiftCatalog.name(id)
                + "\n\nID: #" + id
                + "\nModel: " + GiftCatalog.theme(id)
                + "\nNoyoblik: " + GiftCatalog.rarity(id)
                + "\nNarx: ⭐ " + GiftCatalog.price(id);

        new AlertDialog.Builder(c)
                .setTitle("Gift tafsilotlari")
                .setMessage(msg)
                .setNegativeButton("Yopish", null)
                .setNeutralButton("🎁 O'zimga olish", (d, w) -> buyForSelf(c, id))
                .setPositiveButton("🎁 Do'stga yuborish", (d, w) -> showRecipient(c, id))
                .show();
    }

    private void buyForSelf(Context c, int id) {
        long price = GiftCatalog.price(id);
        long current = prefs.getLong("u_" + uid + "_stars", 0L);
        if (current < price) {
            Toast.makeText(c, "Stars yetarli emas", Toast.LENGTH_SHORT).show();
            return;
        }
        prefs.edit()
                .putLong("u_" + uid + "_stars", current - price)
                .putString("u_" + uid + "_owned_gifts", append(prefs.getString("u_" + uid + "_owned_gifts", ""), giftRow(id)))
                .apply();
        updateBalance();
        Toast.makeText(c, "🎁 " + GiftCatalog.name(id) + " olindi • -⭐ " + price, Toast.LENGTH_SHORT).show();
    }

    private void showRecipient(Context c, int id) {
        LinearLayout box = new LinearLayout(c);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(AndroidUtilities.dp(18), AndroidUtilities.dp(4), AndroidUtilities.dp(18), 0);
        TextView info = text(c, "Qabul qiluvchining Telegram/Superme user ID sini kiriting.", 14, false);
        box.addView(info, LayoutHelper.createLinear(-1, -2, 0, 0, 0, 6));
        EditText recipient = new EditText(c);
        recipient.setHint("Masalan: 123456789");
        recipient.setInputType(2);
        box.addView(recipient, LayoutHelper.createLinear(-1, AndroidUtilities.dp(50)));

        new AlertDialog.Builder(c)
                .setTitle("🎁 Gift yuborish")
                .setView(box)
                .setNegativeButton("Bekor qilish", null)
                .setPositiveButton("Yuborish", (d, w) -> sendGift(c, id, recipient.getText().toString().trim()))
                .show();
    }

    private void sendGift(Context c, int id, String recipient) {
        if (recipient.length() == 0) {
            Toast.makeText(c, "Qabul qiluvchi ID sini kiriting", Toast.LENGTH_SHORT).show();
            return;
        }
        long price = GiftCatalog.price(id);
        long current = prefs.getLong("u_" + uid + "_stars", 0L);
        if (current < price) {
            Toast.makeText(c, "Stars yetarli emas", Toast.LENGTH_SHORT).show();
            return;
        }

        String gift = giftRow(id) + "|from=" + uid + "|to=" + recipient;
        String receivedKey = "received_" + recipient;
        String sentKey = "u_" + uid + "_sent_gifts";
        prefs.edit()
                .putLong("u_" + uid + "_stars", current - price)
                .putString(receivedKey, append(prefs.getString(receivedKey, ""), gift))
                .putString(sentKey, append(prefs.getString(sentKey, ""), gift))
                .apply();
        updateBalance();
        Toast.makeText(c, "🎁 Gift yuborildi • ⭐ " + price, Toast.LENGTH_SHORT).show();
    }

    private void buildOwned(Context c) {
        String own = prefs.getString("u_" + uid + "_owned_gifts", "");
        String received = prefs.getString("received_" + uid, "");
        String sent = prefs.getString("u_" + uid + "_sent_gifts", "");

        addSection(c, "🎁 Mening giftlarim", own);
        addSection(c, "📥 Menga yuborilgan", received);
        addSection(c, "📤 Men yuborganlar", sent);

        if (own.length() == 0 && received.length() == 0 && sent.length() == 0) {
            TextView empty = text(c, "Hozircha gift yo'q.\nKatalogdan gift tanlang.", 17, false);
            empty.setGravity(Gravity.CENTER);
            content.addView(empty, LayoutHelper.createLinear(-1, AndroidUtilities.dp(130)));
        }
    }

    private void addSection(Context c, String title, String rows) {
        if (rows.length() == 0) return;
        TextView head = text(c, title, 18, true);
        head.setPadding(AndroidUtilities.dp(4), AndroidUtilities.dp(10), 0, AndroidUtilities.dp(6));
        content.addView(head, LayoutHelper.createLinear(-1, -2));

        String[] items = rows.split("\\n");
        for (String item : items) {
            if (item.trim().length() == 0) continue;
            String[] p = item.split("\\|", -1);
            String display = p.length >= 4 ? p[3] + "  " + p[1] + "  • ⭐ " + p[2] : item;
            LinearLayout row = new LinearLayout(c);
            row.setGravity(Gravity.CENTER_VERTICAL);
            TextView t = text(c, display, 14, true);
            row.addView(t, LayoutHelper.createLinear(0, AndroidUtilities.dp(52), 1f));
            if (p.length >= 4 && title.startsWith("🎁")) {
                Button send = new Button(c);
                send.setText("Yuborish");
                send.setAllCaps(false);
                send.setTextSize(11);
                final int giftId = parseId(p[0]);
                send.setOnClickListener(v -> showRecipient(c, giftId));
                row.addView(send, LayoutHelper.createLinear(AndroidUtilities.dp(105), AndroidUtilities.dp(46)));
            }
            content.addView(row, LayoutHelper.createLinear(-1, -2));
        }
    }

    private int parseId(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return 1; }
    }

    private String giftRow(int id) {
        return id + "|" + GiftCatalog.name(id) + "|" + GiftCatalog.price(id) + "|" + GiftCatalog.emoji(id);
    }

    private String append(String old, String row) {
        return old == null || old.length() == 0 ? row : old + "\n" + row;
    }
}
