package org.telegram.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.InputType;
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

public class AdminPanelActivity extends BaseFragment {
    private LinearLayout root;
    private SharedPreferences prefs;
    private EditText target;

    @Override
    public View createView(Context context) {
        actionBar.setTitle("Admin panel");
        actionBar.setBackButtonDrawable(new BackDrawable(false));
        prefs = context.getSharedPreferences("local_admin_panel", Context.MODE_PRIVATE);

        ScrollView scroll = new ScrollView(context);
        root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(8), AndroidUtilities.dp(12), AndroidUtilities.dp(24));
        scroll.addView(root);

        addHeader("ID");
        addAction("Mening Telegram ID'im", v -> showMyId());
        target = new EditText(context);
        target.setHint("Target User ID");
        target.setInputType(InputType.TYPE_CLASS_NUMBER);
        root.addView(target, LayoutHelper.createLinear(-1, -2));
        addAction("Target ID holatini ko'rish", v -> showStatus());

        addHeader("Stars va Premium");
        addAction("Stars qo'shish", v -> changeStars(1));
        addAction("Stars ayirish", v -> changeStars(-1));
        addAction("Premium 3 oy", v -> grantPremium(3));
        addAction("Premium 6 oy", v -> grantPremium(6));
        addAction("Premium 12 oy", v -> grantPremium(12));
        addAction("Stars narxini belgilash", v -> setGlobalLong("stars_price", "Stars narxi"));
        addAction("Premium narxini belgilash", v -> setGlobalLong("premium_price", "Premium narxi"));

        addHeader("Giftlar");
        addAction("1200+ gift katalogi / IDlar", v -> showGiftCatalog());
        addAction("Gift yaratish sozlamalari", v -> createGiftRule());
        addAction("Gift yuborish", v -> sendGift());
        addAction("Gift sotib olish", v -> buyGift());
        addAction("Mening giftlarim", v -> showOwnedGifts());

        addHeader("Karta va to'lov");
        addAction("Uzcard qo'shish", v -> addCard());
        addAction("Kartalarni ko'rish", v -> showValue("cards", "Saqlangan kartalar"));
        addAction("To'lov ma'lumotlari", v -> showPaymentInfo());
        addAction("Chek / to'lov so'rovi", v -> createPaymentRequest());
        addAction("Kutilayotgan to'lovlar", v -> showValue("payments", "To'lovlar"));
        addAction("To'lovni tasdiqlash", v -> approvePayment());
        addAction("To'lovni rad etish", v -> rejectPayment());

        addHeader("Kanallar");
        addAction("Promo kanalini ulash", v -> setGlobalText("promo_channel", "Promo kanal"));
        addAction("Gift kanalini ulash", v -> setGlobalText("gift_channel", "Gift kanal"));
        addAction("Kanal talabini yoqish/o'chirish", v -> toggleGlobal("channel_gate", "Kanal talabi"));

        addHeader("Promokodlar");
        addAction("Promokod qo'shish", v -> createPromoCode());
        addAction("Promokodlarni ko'rish", v -> showValue("promos", "Promokodlar"));

        addHeader("Foydalanuvchi boshqaruvi");
        addAction("Ban / Unban", v -> toggleFlag("banned", "Ban"));
        addAction("Spam / Unspam", v -> toggleFlag("spam", "Spam"));
        addAction("Mute / Unmute", v -> toggleFlag("muted", "Mute"));

        addHeader("Botlar");
        addAction("Bot yaratish sozlamalari", v -> setGlobalText("bot_mode", "Bot rejimi"));
        addAction("Botlar ro'yxati", v -> showValue("bots", "Botlar"));

        addHeader("Joriy holat");
        addAction("Barcha sozlamalarni ko'rish", v -> showAllSettings());

        fragmentView = scroll;
        return scroll;
    }

    private String uid() {
        String id = target == null ? "" : target.getText().toString().trim();
        if (id.length() == 0) {
            toast("Avval User ID kiriting");
            return null;
        }
        prefs.edit().putString("target", id).apply();
        return id;
    }

    private String key(String name) {
        String id = uid();
        return id == null ? null : "u_" + id + "_" + name;
    }

    private void showMyId() {
        long id = UserConfig.getInstance(UserConfig.selectedAccount).getClientUserId();
        new AlertDialog.Builder(getParentActivity()).setTitle("Mening ID'im").setMessage(String.valueOf(id)).setPositiveButton("OK", null).show();
    }

    private void changeStars(final int sign) {
        final String k = key("stars");
        if (k == null) return;
        promptNumber("Stars miqdori", value -> {
            long amount = parseLong(value);
            if (amount <= 0) { toast("Miqdor noto'g'ri"); return; }
            long current = prefs.getLong(k, 0L);
            long result = sign > 0 ? safeAdd(current, amount) : Math.max(0L, current - amount);
            prefs.edit().putLong(k, result).apply();
            toast("Stars: " + result);
        });
    }

    private long safeAdd(long a, long b) { return b > Long.MAX_VALUE - a ? Long.MAX_VALUE : a + b; }

    private void grantPremium(int months) {
        String k = key("premium_months");
        if (k == null) return;
        int old = prefs.getInt(k, 0);
        prefs.edit().putInt(k, old + months).apply();
        toast("Premium: " + (old + months) + " oy");
    }

    private void showGiftCatalog() {
        final EditText search = new EditText(getParentActivity());
        search.setHint("Gift ID yoki nomi bo'yicha qidirish");
        search.setSingleLine(true);
        TextView list = new TextView(getParentActivity());
        list.setTextSize(13);
        list.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(8), AndroidUtilities.dp(8), AndroidUtilities.dp(8));
        updateGiftCatalogText(list, "");
        LinearLayout box = new LinearLayout(getParentActivity());
        box.setOrientation(LinearLayout.VERTICAL);
        box.addView(search, LayoutHelper.createLinear(-1, -2));
        ScrollView listScroll = new ScrollView(getParentActivity());
        listScroll.addView(list);
        box.addView(listScroll, LayoutHelper.createLinear(-1, AndroidUtilities.dp(520)));
        search.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { updateGiftCatalogText(list, s.toString().trim()); }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });
        new AlertDialog.Builder(getParentActivity()).setTitle("1200+ Giftlar • ID va narx").setView(box).setPositiveButton("OK", null).show();
    }

    private void updateGiftCatalogText(TextView view, String query) {
        String q = query.toLowerCase();
        StringBuilder out = new StringBuilder();
        int shown = 0;
        for (int id = 1; id <= GiftCatalog.COUNT; id++) {
            String line = GiftCatalog.line(id);
            if (q.length() == 0 || line.toLowerCase().contains(q)) {
                out.append(line).append('\n');
                shown++;
                if (shown >= 250 && q.length() == 0) { out.append("\n… Qidiruv orqali qolgan giftlarni toping."); break; }
            }
        }
        view.setText(out.length() == 0 ? "Gift topilmadi" : out.toString());
    }

    private void createGiftRule() {
        LinearLayout box = new LinearLayout(getParentActivity());
        box.setOrientation(LinearLayout.VERTICAL);
        EditText name = new EditText(getParentActivity()); name.setHint("Custom gift nomi"); box.addView(name);
        EditText price = new EditText(getParentActivity()); price.setHint("Stars narxi"); price.setInputType(InputType.TYPE_CLASS_NUMBER); box.addView(price);
        new AlertDialog.Builder(getParentActivity()).setTitle("Custom gift yaratish").setView(box)
            .setPositiveButton("Saqlash", (d, w) -> {
                String n = name.getText().toString().trim(); long p = parseLong(price.getText().toString());
                if (n.length() == 0 || p <= 0) { toast("Nom va to'g'ri narx kerak"); return; }
                int id = prefs.getInt("custom_gift_seq", 1200) + 1;
                prefs.edit().putInt("custom_gift_seq", id).apply();
                appendLine("custom_gifts", "#" + id + " | " + n + " | " + p + " Stars");
                toast("Custom gift #" + id + " yaratildi");
            }).setNegativeButton("Bekor", null).show();
    }

    private void sendGift() {
        LinearLayout box = new LinearLayout(getParentActivity()); box.setOrientation(LinearLayout.VERTICAL);
        EditText user = new EditText(getParentActivity()); user.setHint("Qabul qiluvchi User ID"); user.setInputType(InputType.TYPE_CLASS_NUMBER); box.addView(user);
        EditText gift = new EditText(getParentActivity()); gift.setHint("Gift ID (1-1200)"); gift.setInputType(InputType.TYPE_CLASS_NUMBER); box.addView(gift);
        new AlertDialog.Builder(getParentActivity()).setTitle("Gift yuborish").setView(box)
            .setPositiveButton("Yuborish", (d, w) -> {
                long userId = parseLong(user.getText().toString()); int giftId = (int) parseLong(gift.getText().toString());
                if (userId <= 0 || !GiftCatalog.isValid(giftId)) { toast("User ID yoki Gift ID noto'g'ri"); return; }
                appendLine("u_" + userId + "_owned_gifts", giftId + "|" + GiftCatalog.name(giftId) + "|" + GiftCatalog.price(giftId));
                toast("Gift #" + giftId + " yuborildi");
            }).setNegativeButton("Bekor", null).show();
    }

    private void buyGift() {
        final String id = uid(); if (id == null) return;
        promptNumber("Sotib olinadigan Gift ID (1-1200)", value -> {
            int giftId = (int) parseLong(value);
            if (!GiftCatalog.isValid(giftId)) { toast("Gift ID noto'g'ri"); return; }
            long price = GiftCatalog.price(giftId), balance = prefs.getLong("u_" + id + "_stars", 0L);
            if (balance < price) { toast("Stars yetarli emas: kerak " + price + ", bor " + balance); return; }
            prefs.edit().putLong("u_" + id + "_stars", balance - price).apply();
            appendLine("u_" + id + "_owned_gifts", giftId + "|" + GiftCatalog.name(giftId) + "|" + price);
            toast("Gift #" + giftId + " olindi. -" + price + " Stars");
        });
    }

    private void showOwnedGifts() { String id = uid(); if (id != null) showValue("u_" + id + "_owned_gifts", "User " + id + " giftlari"); }

    private void addCard() {
        LinearLayout box = new LinearLayout(getParentActivity()); box.setOrientation(LinearLayout.VERTICAL);
        EditText holder = new EditText(getParentActivity()); holder.setHint("Ism-familiya"); box.addView(holder);
        EditText number = new EditText(getParentActivity()); number.setHint("Uzcard raqami"); number.setInputType(InputType.TYPE_CLASS_NUMBER); box.addView(number);
        new AlertDialog.Builder(getParentActivity()).setTitle("Uzcard qo'shish").setView(box)
            .setPositiveButton("Saqlash", (d, w) -> {
                String h = holder.getText().toString().trim(), n = number.getText().toString().trim();
                if (h.length() == 0 || n.length() < 12) { toast("Ism-familiya va karta raqami kerak"); return; }
                prefs.edit().putString("payment_card_holder", h).putString("payment_card_number", n).apply();
                appendLine("cards", h + " | " + n + " | faol"); toast("Karta saqlandi");
            }).setNegativeButton("Bekor", null).show();
    }

    private void showPaymentInfo() {
        String holder = prefs.getString("payment_card_holder", "kiritilmagan"), number = prefs.getString("payment_card_number", "kiritilmagan");
        new AlertDialog.Builder(getParentActivity()).setTitle("To'lov ma'lumotlari").setMessage("Karta egasi: " + holder + "\nKarta: " + number + "\n\nBu clone ilovaning lokal to'lov ma'lumotlari.").setPositiveButton("OK", null).show();
    }

    private void createPaymentRequest() {
        LinearLayout box = new LinearLayout(getParentActivity()); box.setOrientation(LinearLayout.VERTICAL);
        EditText user = new EditText(getParentActivity()); user.setHint("User ID"); user.setInputType(InputType.TYPE_CLASS_NUMBER); box.addView(user);
        EditText product = new EditText(getParentActivity()); product.setHint("Stars / Premium / Gift ID"); box.addView(product);
        EditText amount = new EditText(getParentActivity()); amount.setHint("Miqdor"); amount.setInputType(InputType.TYPE_CLASS_NUMBER); box.addView(amount);
        EditText receipt = new EditText(getParentActivity()); receipt.setHint("Chek ID / izoh"); box.addView(receipt);
        new AlertDialog.Builder(getParentActivity()).setTitle("To'lov so'rovi").setView(box)
            .setPositiveButton("Kutilmoqda", (d, w) -> {
                String u = user.getText().toString().trim(), p = product.getText().toString().trim(), a = amount.getText().toString().trim(), r = receipt.getText().toString().trim();
                if (u.length() == 0 || p.length() == 0 || a.length() == 0) { toast("User, mahsulot va miqdor kerak"); return; }
                int id = prefs.getInt("payment_seq", 1000) + 1;
                prefs.edit().putInt("payment_seq", id).putString("payment_" + id, u + "|" + p + "|" + a + "|" + r + "|PENDING").apply();
                appendLine("payments", id + " | " + u + " | " + p + " | " + a + " | " + r + " | PENDING"); toast("To'lov #" + id + " kutilmoqda");
            }).setNegativeButton("Bekor", null).show();
    }

    private void approvePayment() { promptNumber("Tasdiqlanadigan to'lov ID", value -> changePayment(value, true)); }
    private void rejectPayment() { promptNumber("Rad etiladigan to'lov ID", value -> changePayment(value, false)); }

    private void changePayment(String value, boolean approve) {
        int id = (int) parseLong(value); String raw = prefs.getString("payment_" + id, "");
        if (raw.length() == 0) { toast("To'lov topilmadi"); return; }
        String[] parts = raw.split("\\|", -1);
        if (parts.length < 5 || !"PENDING".equals(parts[4])) { toast("To'lov allaqachon ko'rib chiqilgan"); return; }
        if (approve) grantPayment(parts[0], parts[1], parseLong(parts[2]));
        String status = approve ? "APPROVED" : "REJECTED";
        prefs.edit().putString("payment_" + id, raw.replace("|PENDING", "|" + status)).apply();
        replacePaymentStatus(id, status); toast("To'lov #" + id + ": " + status);
    }

    private void grantPayment(String userId, String product, long amount) {
        String base = "u_" + userId + "_", p = product.toLowerCase();
        if (p.startsWith("stars")) prefs.edit().putLong(base + "stars", safeAdd(prefs.getLong(base + "stars", 0L), amount)).apply();
        else if (p.contains("premium")) prefs.edit().putInt(base + "premium_months", prefs.getInt(base + "premium_months", 0) + (int) amount).apply();
        else if (p.startsWith("gift")) {
            int giftId = (int) parseLong(p.replaceAll("[^0-9]", ""));
            if (GiftCatalog.isValid(giftId)) appendLine(base + "owned_gifts", giftId + "|" + GiftCatalog.name(giftId) + "|" + GiftCatalog.price(giftId));
        }
    }

    private void replacePaymentStatus(int id, String status) {
        String all = prefs.getString("payments", ""); String[] lines = all.split("\\n", -1); StringBuilder out = new StringBuilder();
        for (String line : lines) {
            if (line.startsWith(id + " | ")) {
                String[] p = line.split(" \\| ", -1);
                if (p.length >= 6) line = p[0] + " | " + p[1] + " | " + p[2] + " | " + p[3] + " | " + p[4] + " | " + status;
            }
            if (line.length() > 0) { if (out.length() > 0) out.append('\n'); out.append(line); }
        }
        prefs.edit().putString("payments", out.toString()).apply();
    }

    private void createPromoCode() {
        LinearLayout box = new LinearLayout(getParentActivity()); box.setOrientation(LinearLayout.VERTICAL);
        EditText code = new EditText(getParentActivity()); code.setHint("Promokod"); box.addView(code);
        EditText value = new EditText(getParentActivity()); value.setHint("Stars miqdori yoki Gift ID"); box.addView(value);
        String[] types = {"Stars", "Premium 3 oy", "Premium 6 oy", "Premium 12 oy", "Gift"}; final int[] selected = {0};
        new AlertDialog.Builder(getParentActivity()).setTitle("Promokod turi").setSingleChoiceItems(types, 0, (d, which) -> selected[0] = which).setView(box)
            .setPositiveButton("Saqlash", (d, w) -> { String c = code.getText().toString().trim(), v = value.getText().toString().trim(); if (c.length() == 0 || v.length() == 0) { toast("Promokod va qiymat kerak"); return; } appendLine("promos", c + " | " + types[selected[0]] + " | " + v); toast("Promokod saqlandi"); })
            .setNegativeButton("Bekor", null).show();
    }

    private void toggleGlobal(String name, String title) { boolean value = !prefs.getBoolean(name, false); prefs.edit().putBoolean(name, value).apply(); toast(title + (value ? " yoqildi" : " o'chirildi")); }
    private void toggleFlag(String name, String title) { String k = key(name); if (k == null) return; boolean value = !prefs.getBoolean(k, false); prefs.edit().putBoolean(k, value).apply(); toast(title + (value ? " yoqildi" : " o'chirildi")); }

    private void setGlobalLong(String name, String title) {
        promptNumber(title, value -> { long n = parseLong(value); if (n <= 0) { toast("Narx noto'g'ri"); return; } prefs.edit().putLong(name, n).apply(); toast(title + ": " + n); });
    }
    private void setGlobalText(String name, String title) { promptText(title, value -> { if (value.trim().length() == 0) { toast("Qiymat bo'sh"); return; } prefs.edit().putString(name, value.trim()).apply(); toast(title + " saqlandi"); }); }
    private void showValue(String name, String title) { new AlertDialog.Builder(getParentActivity()).setTitle(title).setMessage(prefs.getString(name, "Hozircha yo'q")).setPositiveButton("OK", null).show(); }

    private void showStatus() {
        String id = uid(); if (id == null) return;
        String m = "User ID: " + id + "\nStars: " + prefs.getLong("u_" + id + "_stars", 0L) + "\nPremium: " + prefs.getInt("u_" + id + "_premium_months", 0) + " oy"
                + "\nBan: " + prefs.getBoolean("u_" + id + "_banned", false) + "\nSpam: " + prefs.getBoolean("u_" + id + "_spam", false) + "\nMute: " + prefs.getBoolean("u_" + id + "_muted", false)
                + "\nGiftlar: " + countLines(prefs.getString("u_" + id + "_owned_gifts", "")) + "\nPromo kanal: " + prefs.getString("promo_channel", "ulanmagan")
                + "\nGift kanal: " + prefs.getString("gift_channel", "ulanmagan") + "\nKanal talabi: " + prefs.getBoolean("channel_gate", false);
        new AlertDialog.Builder(getParentActivity()).setTitle("Foydalanuvchi holati").setMessage(m).setPositiveButton("OK", null).show();
    }

    private void showAllSettings() {
        String m = "Stars narxi: " + prefs.getLong("stars_price", 0) + "\nPremium narxi: " + prefs.getLong("premium_price", 0) + "\nPromo kanal: " + prefs.getString("promo_channel", "ulanmagan")
                + "\nGift kanal: " + prefs.getString("gift_channel", "ulanmagan") + "\nKanal talabi: " + prefs.getBoolean("channel_gate", false) + "\nBot rejimi: " + prefs.getString("bot_mode", "bepul")
                + "\nKartalar: " + countLines(prefs.getString("cards", "")) + "\nPromokodlar: " + countLines(prefs.getString("promos", ""));
        new AlertDialog.Builder(getParentActivity()).setTitle("Admin sozlamalari").setMessage(m).setPositiveButton("OK", null).show();
    }

    private int countLines(String text) { return text == null || text.trim().length() == 0 ? 0 : text.split("\\n").length; }
    private void appendLine(String key, String line) { String old = prefs.getString(key, ""); prefs.edit().putString(key, old.length() == 0 ? line : old + "\n" + line).apply(); }
    private long parseLong(String value) { try { return Math.max(0L, Long.parseLong(value.trim())); } catch (Exception e) { return 0L; } }
    private void promptNumber(String title, final ValueCallback callback) { final EditText input = new EditText(getParentActivity()); input.setInputType(InputType.TYPE_CLASS_NUMBER); new AlertDialog.Builder(getParentActivity()).setTitle(title).setView(input).setPositiveButton("Saqlash", (d, w) -> callback.onValue(input.getText().toString())).setNegativeButton("Bekor", null).show(); }
    private void promptText(String title, final ValueCallback callback) { final EditText input = new EditText(getParentActivity()); new AlertDialog.Builder(getParentActivity()).setTitle(title).setView(input).setPositiveButton("Saqlash", (d, w) -> callback.onValue(input.getText().toString())).setNegativeButton("Bekor", null).show(); }
    private void addHeader(String text) { TextView h = new TextView(getParentActivity()); h.setText(text); h.setTextSize(14); h.setTypeface(null, android.graphics.Typeface.BOLD); h.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(16), AndroidUtilities.dp(12), AndroidUtilities.dp(7)); root.addView(h, LayoutHelper.createLinear(-1, -2)); }
    private void addAction(String text, View.OnClickListener listener) { Button b = new Button(getParentActivity()); b.setText(text); b.setAllCaps(false); b.setOnClickListener(listener); root.addView(b, LayoutHelper.createLinear(-1, AndroidUtilities.dp(52), 0, 0, 0, 2)); }
    private void toast(String text) { Toast.makeText(getParentActivity(), text, Toast.LENGTH_SHORT).show(); }
    private interface ValueCallback { void onValue(String value); }
}
