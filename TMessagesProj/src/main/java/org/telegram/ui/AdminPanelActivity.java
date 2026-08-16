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

        addHeader("Target foydalanuvchi");
        target = new EditText(context);
        target.setHint("User ID");
        target.setInputType(InputType.TYPE_CLASS_NUMBER);
        target.setText(prefs.getString("target", ""));
        root.addView(target, LayoutHelper.createLinear(-1, -2));

        addHeader("Stars va Premium");
        addAction("Stars qo'shish", v -> changeStars(1));
        addAction("Stars ayirish", v -> changeStars(-1));
        addAction("Premium berish / olish", v -> togglePremium());
        addAction("Premium narxini belgilash", v -> setGlobalLong("premium_price", "Premium narxi"));
        addAction("Stars narxini belgilash", v -> setGlobalLong("stars_price", "Stars narxi"));

        addHeader("Karta va Giftlar");
        addAction("Karta qo'shish", v -> setGlobalText("card", "Karta nomi"));
        addAction("Gift yaratish", v -> createGift());
        addAction("Gift IDlarini ko'rish", v -> showValue("gifts", "Giftlar"));
        addAction("Gift yuborish", v -> toast("Gift yuborish: demo/local amal bajarildi"));

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

    private void changeStars(final int sign) {
        final String key = key("stars");
        if (key == null) return;
        promptNumber("Stars miqdori", value -> {
            long current = prefs.getLong(key, 0L);
            long amount = parseLong(value);
            long result = sign > 0 ? current + amount : Math.max(0L, current - amount);
            prefs.edit().putLong(key, result).apply();
            toast("Stars: " + result);
        });
    }

    private void togglePremium() {
        String key = key("premium");
        if (key == null) return;
        boolean value = !prefs.getBoolean(key, false);
        prefs.edit().putBoolean(key, value).apply();
        toast(value ? "Premium berildi" : "Premium olindi");
    }

    private void toggleFlag(String name, String title) {
        String key = key(name);
        if (key == null) return;
        boolean value = !prefs.getBoolean(key, false);
        prefs.edit().putBoolean(key, value).apply();
        toast(title + (value ? " yoqildi" : " o'chirildi"));
    }

    private void setGlobalLong(String name, String title) {
        promptNumber(title, value -> prefs.edit().putLong(name, parseLong(value)).apply());
    }

    private void setGlobalText(String name, String title) {
        promptText(title, value -> prefs.edit().putString(name, value).apply());
    }

    private void createGift() {
        final EditText input = new EditText(getParentActivity());
        input.setHint("Gift nomi");
        new AlertDialog.Builder(getParentActivity())
                .setTitle("Gift yaratish")
                .setView(input)
                .setPositiveButton("Saqlash", (d, w) -> {
                    String name = input.getText().toString().trim();
                    if (name.length() == 0) return;
                    String old = prefs.getString("gifts", "");
                    String next = old.length() == 0 ? name : old + "\n" + name;
                    prefs.edit().putString("gifts", next).apply();
                    toast("Gift yaratildi");
                })
                .setNegativeButton("Bekor qilish", null)
                .show();
    }

    private void showValue(String name, String title) {
        String value = prefs.getString(name, "Hozircha yo'q");
        new AlertDialog.Builder(getParentActivity()).setTitle(title).setMessage(value).setPositiveButton("OK", null).show();
    }

    private void showStatus() {
        String id = uid();
        if (id == null) return;
        String message = "User: " + id
                + "\nStars: " + prefs.getLong("u_" + id + "_stars", 0L)
                + "\nPremium: " + prefs.getBoolean("u_" + id + "_premium", false)
                + "\nBan: " + prefs.getBoolean("u_" + id + "_banned", false)
                + "\nSpam: " + prefs.getBoolean("u_" + id + "_spam", false)
                + "\nMute: " + prefs.getBoolean("u_" + id + "_muted", false)
                + "\nPremium narxi: " + prefs.getLong("premium_price", 0L)
                + "\nStars narxi: " + prefs.getLong("stars_price", 0L)
                + "\nKarta: " + prefs.getString("card", "qo'shilmagan");
        new AlertDialog.Builder(getParentActivity()).setTitle("Holat").setMessage(message).setPositiveButton("OK", null).show();
    }

    private long parseLong(String value) {
        try { return Math.max(0L, Long.parseLong(value)); } catch (Exception e) { return 0L; }
    }

    private void promptNumber(String title, final ValueCallback callback) {
        final EditText input = new EditText(getParentActivity());
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("0");
        new AlertDialog.Builder(getParentActivity()).setTitle(title).setView(input)
                .setPositiveButton("Saqlash", (d, w) -> callback.onValue(input.getText().toString()))
                .setNegativeButton("Bekor qilish", null).show();
    }

    private void promptText(String title, final ValueCallback callback) {
        final EditText input = new EditText(getParentActivity());
        new AlertDialog.Builder(getParentActivity()).setTitle(title).setView(input)
                .setPositiveButton("Saqlash", (d, w) -> callback.onValue(input.getText().toString()))
                .setNegativeButton("Bekor qilish", null).show();
    }

    private void addHeader(String text) {
        TextView h = new TextView(getParentActivity());
        h.setText(text);
        h.setTextSize(14);
        h.setTypeface(null, android.graphics.Typeface.BOLD);
        h.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(16), AndroidUtilities.dp(12), AndroidUtilities.dp(7));
        root.addView(h, LayoutHelper.createLinear(-1, -2));
    }

    private void addAction(String text, View.OnClickListener listener) {
        Button b = new Button(getParentActivity());
        b.setText(text);
        b.setAllCaps(false);
        b.setOnClickListener(listener);
        root.addView(b, LayoutHelper.createLinear(-1, AndroidUtilities.dp(52), 0, 0, 0, 2));
    }

    private void toast(String text) {
        Toast.makeText(getParentActivity(), text, Toast.LENGTH_SHORT).show();
    }

    private interface ValueCallback { void onValue(String value); }
}
