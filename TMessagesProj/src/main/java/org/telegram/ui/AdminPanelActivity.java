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
import org.telegram.ui.ActionBar.BackDrawable;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.LayoutHelper;

public class AdminPanelActivity extends BaseFragment {
    private LinearLayout root;
    private SharedPreferences prefs;
    private EditText target;

    @Override public View createView(Context context) {
        actionBar.setTitle("Admin panel");
        actionBar.setBackButtonDrawable(new BackDrawable(false));
        prefs = context.getSharedPreferences("local_admin_panel", Context.MODE_PRIVATE);
        ScrollView scroll = new ScrollView(context);
        root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(8), AndroidUtilities.dp(12), AndroidUtilities.dp(24));
        scroll.addView(root);

        addHeader("Target foydalanuvchi");
        target = new EditText(context);
        target.setHint("User ID");
        target.setInputType(InputType.TYPE_CLASS_NUMBER);
        root.addView(target, LayoutHelper.createLinear(-1, -2));

        addHeader("Stars va Premium");
        addAction("Stars qo'shish", v -> changeStars(1));
        addAction("Stars ayirish", v -> changeStars(-1));
        addAction("Premium 3 oy", v -> grantPremium(3));
        addAction("Premium 6 oy", v -> grantPremium(6));
        addAction("Premium 12 oy", v -> grantPremium(12));
        addAction("Premium narxini belgilash", v -> setGlobalLong("premium_price", "Premium narxi"));
        addAction("Stars narxini belgilash", v -> setGlobalLong("stars_price", "Stars narxi"));

        addHeader("Gift tizimi");
        addAction("Gift yaratish sozlamalari", v -> createGiftRule());
        addAction("Gift IDlarini ko'rish", v -> showValue("gifts", "Giftlar va IDlar"));
        addAction("Gift yuborish", v -> toast("Gift yuborish serverdagi gift endpointiga yuboriladi"));
        addAction("Gift olish / Stars qo'shish", v -> addPurchasedGiftStars());

        addHeader("Karta va to'lov");
        addAction("Karta qo'shish", v -> addCard());
        addAction("Kartalarni ko'rish", v -> showValue("cards", "Saqlangan kartalar"));
        addAction("To'lov ma'lumotlarini ko'rish", v -> showPaymentInfo());
        addAction("Chek / to'lov so'rovini qo'shish", v -> createPaymentRequest());
        addAction("Kutilayotgan to'lovlar", v -> showValue("payments", "To'lovlar"));
        addAction("To'lovni tasdiqlash", v -> approvePayment());
        addAction("To'lovni rad etish", v -> rejectPayment());

        addHeader("Kanallar");
        addAction("Promo kanalini ulash", v -> setGlobalText("promo_channel", "Promo kanal @username yoki URL"));
        addAction("Gift kanalini ulash", v -> setGlobalText("gift_channel", "Gift kanal @username yoki URL"));
        addAction("Kanal talabini yoqish/o'chirish", v -> toggleGlobal("channel_gate", "Kanal talabi"));

        addHeader("Promokodlar");
        addAction("Promokod qo'shish", v -> createPromoCode());
        addAction("Promokodlarni ko'rish", v -> showValue("promos", "Promokodlar"));

        addHeader("Foydalanuvchi boshqaruvi");
        addAction("Ban / Unban", v -> toggleFlag("banned", "Ban"));
        addAction("Spam / Unspam", v -> toggleFlag("spam", "Spam"));
        addAction("Mute / Unmute", v -> toggleFlag("muted", "Mute"));

        addHeader("Joriy holat");
        addAction("Holatni ko'rish", v -> showStatus());
        fragmentView = scroll;
        return scroll;
    }

    private String uid() {
        String id = target == null ? "" : target.getText().toString().trim();
        if (id.length() == 0) { toast("Avval User ID kiriting"); return null; }
        prefs.edit().putString("target", id).apply();
        return id;
    }
    private String key(String name) { String id = uid(); return id == null ? null : "u_" + id + "_" + name; }

    private void changeStars(final int sign) {
        final String k = key("stars"); if (k == null) return;
        promptNumber("Stars miqdori", value -> {
            long current = prefs.getLong(k, 0L), amount = parseLong(value);
            long result = sign > 0 ? current + amount : Math.max(0L, current - amount);
            prefs.edit().putLong(k, result).apply(); toast("Stars: " + result);
        });
    }

    private void grantPremium(int months) {
        String k = key("premium_months"); if (k == null) return;
        int old = prefs.getInt(k, 0);
        prefs.edit().putInt(k, old + months).apply();
        toast("Premium muddati: " + (old + months) + " oy");
    }

    private void addPurchasedGiftStars() {
        String k = key("stars"); if (k == null) return;
        promptNumber("Gift ID", idText -> {
            String gift = prefs.getString("gift_" + idText, "");
            if (gift.length() == 0) { toast("Gift ID topilmadi"); return; }
            String[] parts = gift.split("\\|");
            long price = parts.length > 2 ? parseLong(parts[2]) : 0;
            prefs.edit().putLong(k, prefs.getLong(k, 0) + price).apply();
            toast("Gift sotib olindi: +" + price + " Stars");
        });
    }

    private void createGiftRule() {
        LinearLayout box = new LinearLayout(getParentActivity());
        box.setOrientation(LinearLayout.VERTICAL);
        EditText name = new EditText(getParentActivity()); name.setHint("Gift nomi"); box.addView(name);
        EditText price = new EditText(getParentActivity()); price.setHint("Stars narxi"); price.setInputType(InputType.TYPE_CLASS_NUMBER); box.addView(price);
        new AlertDialog.Builder(getParentActivity()).setTitle("Gift yaratish").setView(box)
            .setPositiveButton("Saqlash", (d,w) -> {
                String n = name.getText().toString().trim(); long p = parseLong(price.getText().toString());
                if (n.length() == 0 || p <= 0) return;
                String id = String.valueOf(prefs.getInt("gift_seq", 1000) + 1);
                prefs.edit().putInt("gift_seq", Integer.parseInt(id)).putString("gift_" + id, id + "|" + n + "|" + p).apply();
                String old = prefs.getString("gifts", "");
                prefs.edit().putString("gifts", old.length() == 0 ? id + " | " + n + " | " + p + " Stars" : old + "\n" + id + " | " + n + " | " + p + " Stars").apply();
                toast("Gift yaratildi. ID: " + id);
            }).setNegativeButton("Bekor", null).show();
    }

    private void addCard() {
        LinearLayout box = new LinearLayout(getParentActivity());
        box.setOrientation(LinearLayout.VERTICAL);
        EditText holder = new EditText(getParentActivity()); holder.setHint("Karta egasi ism-familiyasi"); box.addView(holder);
        EditText number = new EditText(getParentActivity()); number.setHint("Uzcard karta raqami"); number.setInputType(InputType.TYPE_CLASS_NUMBER); box.addView(number);
        EditText status = new EditText(getParentActivity()); status.setHint("Holati (faol/nofaol)"); box.addView(status);
        new AlertDialog.Builder(getParentActivity()).setTitle("Karta qo'shish").setView(box)
            .setPositiveButton("Saqlash", (d,w) -> {
                String h = holder.getText().toString().trim(), num = number.getText().toString().trim(), s = status.getText().toString().trim();
                if (h.length() == 0 || num.length() == 0) { toast("Ism-familiya va karta raqami kerak"); return; }
                String row = h + " | " + num + " | " + (s.length() == 0 ? "faol" : s);
                String old = prefs.getString("cards", "");
                prefs.edit().putString("cards", old.length() == 0 ? row : old + "\n" + row).apply();
                prefs.edit().putString("payment_card_holder", h).putString("payment_card_number", num).apply();
                toast("Karta saqlandi");
            }).setNegativeButton("Bekor", null).show();
    }

    private void showPaymentInfo() {
        String holder = prefs.getString("payment_card_holder", "kiritilmagan");
        String number = prefs.getString("payment_card_number", "kiritilmagan");
        String text = "Karta egasi: " + holder + "\nKarta: " + number +
                "\n\nTo'lovni amalga oshirgach chekni yuboring.\nTasdiqlashdan keyin tanlangan Stars, Premium yoki Gift beriladi.\n\nMuammo bo'lsa: Admin bilan gaplashing.";
        new AlertDialog.Builder(getParentActivity()).setTitle("To'lov ma'lumotlari").setMessage(text).setPositiveButton("OK", null).show();
    }

    private void createPaymentRequest() {
        LinearLayout box = new LinearLayout(getParentActivity()); box.setOrientation(LinearLayout.VERTICAL);
        EditText user = new EditText(getParentActivity()); user.setHint("User ID"); user.setInputType(InputType.TYPE_CLASS_NUMBER); box.addView(user);
        EditText product = new EditText(getParentActivity()); product.setHint("Stars / Premium 3 oy / Premium 6 oy / Premium 12 oy / Gift ID"); box.addView(product);
        EditText amount = new EditText(getParentActivity()); amount.setHint("Miqdor (Stars yoki oy)"); amount.setInputType(InputType.TYPE_CLASS_NUMBER); box.addView(amount);
        EditText receipt = new EditText(getParentActivity()); receipt.setHint("Chek ID yoki izoh"); box.addView(receipt);
        new AlertDialog.Builder(getParentActivity()).setTitle("Chek / to'lov so'rovi").setView(box)
            .setPositiveButton("Kutilmoqda", (d,w) -> {
                String u = user.getText().toString().trim(), p = product.getText().toString().trim(), a = amount.getText().toString().trim(), r = receipt.getText().toString().trim();
                if (u.length() == 0 || p.length() == 0 || a.length() == 0) { toast("User, mahsulot va miqdor kerak"); return; }
                int id = prefs.getInt("payment_seq", 1000) + 1;
                prefs.edit().putInt("payment_seq", id).putString("payment_" + id, u + "|" + p + "|" + a + "|" + r + "|PENDING").apply();
                appendLine("payments", id + " | " + u + " | " + p + " | " + a + " | " + r + " | PENDING");
                toast("To'lov #" + id + " kutilmoqda");
            }).setNegativeButton("Bekor", null).show();
    }

    private void approvePayment() {
        promptNumber("Tasdiqlanadigan to'lov ID", value -> {
            int id = (int) parseLong(value);
            String raw = prefs.getString("payment_" + id, "");
            if (raw.length() == 0) { toast("To'lov topilmadi"); return; }
            String[] parts = raw.split("\\|", -1);
            if (parts.length < 5 || !"PENDING".equals(parts[4])) { toast("Bu to'lov allaqachon ko'rib chiqilgan"); return; }
            grantPayment(parts[0], parts[1], parseLong(parts[2]));
            prefs.edit().putString("payment_" + id, raw.replace("|PENDING", "|APPROVED")).apply();
            replacePaymentStatus(id, "APPROVED");
            toast("To'lov #" + id + " tasdiqlandi");
        });
    }

    private void rejectPayment() {
        promptNumber("Rad etiladigan to'lov ID", value -> {
            int id = (int) parseLong(value);
            String raw = prefs.getString("payment_" + id, "");
            if (raw.length() == 0) { toast("To'lov topilmadi"); return; }
            if (!raw.endsWith("|PENDING")) { toast("Bu to'lov allaqachon ko'rib chiqilgan"); return; }
            prefs.edit().putString("payment_" + id, raw.replace("|PENDING", "|REJECTED")).apply();
            replacePaymentStatus(id, "REJECTED");
            toast("To'lov #" + id + " rad etildi");
        });
    }

    private void grantPayment(String userId, String product, long amount) {
        String base = "u_" + userId + "_";
        String p = product.toLowerCase();
        if (p.startsWith("stars")) {
            prefs.edit().putLong(base + "stars", prefs.getLong(base + "stars", 0L) + amount).apply();
        } else if (p.contains("premium")) {
            prefs.edit().putInt(base + "premium_months", prefs.getInt(base + "premium_months", 0) + (int) amount).apply();
        } else if (p.startsWith("gift")) {
            prefs.edit().putString(base + "last_gift", product).apply();
        }
    }

    private void replacePaymentStatus(int id, String status) {
        String all = prefs.getString("payments", "");
        String[] lines = all.split("\\n", -1);
        StringBuilder out = new StringBuilder();
        for (String line : lines) {
            if (line.startsWith(id + " | ")) {
                String[] p = line.split(" \\| ", -1);
                if (p.length >= 6) line = p[0] + " | " + p[1] + " | " + p[2] + " | " + p[3] + " | " + p[4] + " | " + status;
            }
            if (out.length() > 0) out.append('\n');
            if (line.length() > 0) out.append(line);
        }
        prefs.edit().putString("payments", out.toString()).apply();
    }

    private void appendLine(String key, String line) {
        String old = prefs.getString(key, "");
        prefs.edit().putString(key, old.length() == 0 ? line : old + "\n" + line).apply();
    }

    private void createPromoCode() {
        LinearLayout box = new LinearLayout(getParentActivity()); box.setOrientation(LinearLayout.VERTICAL);
        EditText code = new EditText(getParentActivity()); code.setHint("Promokod"); box.addView(code);
        EditText value = new EditText(getParentActivity()); value.setHint("Stars miqdori yoki Gift ID"); box.addView(value);
        String[] types = {"Stars", "Premium 3 oy", "Premium 6 oy", "Premium 12 oy", "Gift"};
        final int[] selected = {0};
        new AlertDialog.Builder(getParentActivity()).setTitle("Promokod turi")
            .setSingleChoiceItems(types, 0, (d, which) -> selected[0] = which)
            .setView(box)
            .setPositiveButton("Saqlash", (d,w) -> {
                String c = code.getText().toString().trim(), v = value.getText().toString().trim();
                if (c.length() == 0 || v.length() == 0) return;
                String old = prefs.getString("promos", "");
                String row = c + " | " + types[selected[0]] + " | " + v;
                prefs.edit().putString("promos", old.length() == 0 ? row : old + "\n" + row).apply();
                toast("Promokod saqlandi");
            }).setNegativeButton("Bekor", null).show();
    }

    private void toggleGlobal(String name, String title) {
        boolean v = !prefs.getBoolean(name, false); prefs.edit().putBoolean(name, v).apply(); toast(title + (v ? " yoqildi" : " o'chirildi"));
    }
    private void toggleFlag(String name, String title) {
        String k = key(name); if (k == null) return;
        boolean v = !prefs.getBoolean(k, false); prefs.edit().putBoolean(k, v).apply(); toast(title + (v ? " yoqildi" : " o'chirildi"));
    }
    private void setGlobalLong(String name, String title) { promptNumber(title, v -> prefs.edit().putLong(name, parseLong(v)).apply()); }
    private void setGlobalText(String name, String title) { promptText(title, v -> prefs.edit().putString(name, v).apply()); }
    private void showValue(String name, String title) { new AlertDialog.Builder(getParentActivity()).setTitle(title).setMessage(prefs.getString(name, "Hozircha yo'q")).setPositiveButton("OK", null).show(); }

    private void showStatus() {
        String id = uid(); if (id == null) return;
        String m = "User: " + id + "\nStars: " + prefs.getLong("u_" + id + "_stars", 0L)
            + "\nPremium: " + prefs.getInt("u_" + id + "_premium_months", 0) + " oy"
            + "\nBan: " + prefs.getBoolean("u_" + id + "_banned", false)
            + "\nSpam: " + prefs.getBoolean("u_" + id + "_spam", false)
            + "\nMute: " + prefs.getBoolean("u_" + id + "_muted", false)
            + "\nPromo kanal: " + prefs.getString("promo_channel", "ulanmagan")
            + "\nGift kanal: " + prefs.getString("gift_channel", "ulanmagan")
            + "\nKanal talabi: " + prefs.getBoolean("channel_gate", false);
        new AlertDialog.Builder(getParentActivity()).setTitle("Holat").setMessage(m).setPositiveButton("OK", null).show();
    }
    private long parseLong(String v) { try { return Math.max(0L, Long.parseLong(v.trim())); } catch (Exception e) { return 0L; } }
    private void promptNumber(String title, final ValueCallback cb) { final EditText i = new EditText(getParentActivity()); i.setInputType(InputType.TYPE_CLASS_NUMBER); new AlertDialog.Builder(getParentActivity()).setTitle(title).setView(i).setPositiveButton("Saqlash", (d,w)->cb.onValue(i.getText().toString())).setNegativeButton("Bekor", null).show(); }
    private void promptText(String title, final ValueCallback cb) { final EditText i = new EditText(getParentActivity()); new AlertDialog.Builder(getParentActivity()).setTitle(title).setView(i).setPositiveButton("Saqlash", (d,w)->cb.onValue(i.getText().toString())).setNegativeButton("Bekor", null).show(); }
    private void addHeader(String text) { TextView h = new TextView(getParentActivity()); h.setText(text); h.setTextSize(14); h.setTypeface(null, android.graphics.Typeface.BOLD); h.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(16), AndroidUtilities.dp(12), AndroidUtilities.dp(7)); root.addView(h, LayoutHelper.createLinear(-1,-2)); }
    private void addAction(String text, View.OnClickListener l) { Button b = new Button(getParentActivity()); b.setText(text); b.setAllCaps(false); b.setOnClickListener(l); root.addView(b, LayoutHelper.createLinear(-1, AndroidUtilities.dp(52),0,0,0,2)); }
    private void toast(String s) { Toast.makeText(getParentActivity(), s, Toast.LENGTH_SHORT).show(); }
    private interface ValueCallback { void onValue(String value); }
}
