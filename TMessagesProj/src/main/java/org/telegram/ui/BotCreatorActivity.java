package org.telegram.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.Uri;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.BackDrawable;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.LayoutHelper;

public class BotCreatorActivity extends BaseFragment {
    private SharedPreferences prefs;
    private LinearLayout root;

    @Override
    public View createView(Context context) {
        actionBar.setTitle("Bot yaratish");
        actionBar.setBackButtonDrawable(new BackDrawable(false));
        prefs = context.getSharedPreferences("local_admin_panel", Context.MODE_PRIVATE);

        ScrollView scroll = new ScrollView(context);
        root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(10), AndroidUtilities.dp(12), AndroidUtilities.dp(20));
        scroll.addView(root);

        TextView info = new TextView(context);
        info.setText("🤖 Haqiqiy Telegram bot yaratish\n\nBot Superme ichida tayyorlanadi, rasmiy Telegram BotFather orqali haqiqiy bot account yaratiladi. Soxta token yaratilmaydi.");
        info.setTextSize(16);
        info.setTypeface(null, Typeface.BOLD);
        root.addView(info, LayoutHelper.createLinear(-1, -2));

        Button create = new Button(context);
        create.setText("➕ Yangi bot yaratish");
        create.setAllCaps(false);
        create.setOnClickListener(v -> showCreateDialog(context));
        root.addView(create, LayoutHelper.createLinear(-1, AndroidUtilities.dp(50), 0, 12, 0, 8));

        Button openFather = new Button(context);
        openFather.setText("🤖 BotFather'ni ochish");
        openFather.setAllCaps(false);
        openFather.setOnClickListener(v -> openBotFather(context));
        root.addView(openFather, LayoutHelper.createLinear(-1, AndroidUtilities.dp(50), 0, 0, 0, 8));

        rebuildList(context);
        fragmentView = scroll;
        return scroll;
    }

    private void showCreateDialog(Context context) {
        LinearLayout box = new LinearLayout(context);
        box.setOrientation(LinearLayout.VERTICAL);
        EditText name = new EditText(context);
        name.setHint("Bot nomi");
        box.addView(name);
        EditText username = new EditText(context);
        username.setHint("Username (masalan: mysupermebot)");
        box.addView(username);

        new AlertDialog.Builder(context)
            .setTitle("Haqiqiy Telegram bot")
            .setMessage("1) Nom va username kiriting. 2) BotFather ochiladi. 3) /newbot orqali bot yarating. 4) BotFather bergan haqiqiy tokenni Superme'ga kiriting.")
            .setView(box)
            .setPositiveButton("BotFather'ni ochish", (d, w) -> {
                String n = name.getText().toString().trim();
                String u = username.getText().toString().trim().replace("@", "");
                if (n.length() == 0 || u.length() < 5) return;
                prefs.edit()
                    .putString("bot_draft_name", n.replace("|", ""))
                    .putString("bot_draft_username", u.replace("|", ""))
                    .apply();
                openBotFather(context);
                showTokenImportDialog(context, n, u);
            })
            .setNegativeButton("Bekor", null)
            .show();
    }

    private void openBotFather(Context context) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("tg://resolve?domain=BotFather"));
            context.startActivity(intent);
        } catch (Exception e) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/BotFather"));
            context.startActivity(intent);
        }
    }

    private void showTokenImportDialog(Context context, String name, String username) {
        EditText token = new EditText(context);
        token.setHint("123456789:AA...");
        token.setSingleLine(true);
        new AlertDialog.Builder(context)
            .setTitle("Bot tokenini ulash")
            .setMessage("BotFather /newbot orqali yaratib bergan HAQIQIY tokenni shu yerga kiriting. Superme o'zi token uydirmaydi.")
            .setView(token)
            .setPositiveButton("Saqlash", (d, w) -> {
                String t = token.getText().toString().trim();
                if (!isValidTelegramToken(t)) {
                    new AlertDialog.Builder(context).setTitle("Token noto'g'ri").setMessage("Telegram bot tokeni formati noto'g'ri.").setPositiveButton("OK", null).show();
                    return;
                }
                long owner = UserConfig.getInstance(UserConfig.selectedAccount).getClientUserId();
                String key = "bot_" + System.currentTimeMillis();
                String value = owner + "|" + name.replace("|", "") + "|" + username.replace("|", "") + "|" + t.replace("|", "") + "|STOPPED";
                prefs.edit().putString(key, value).remove("bot_draft_name").remove("bot_draft_username").apply();
                rebuildList(context);
            })
            .setNegativeButton("Keyinroq", null)
            .show();
    }

    private boolean isValidTelegramToken(String token) {
        return token.matches("\\d{5,}:\\S{20,}");
    }

    private void rebuildList(Context context) {
        while (root.getChildCount() > 3) root.removeViewAt(3);
        long me = UserConfig.getInstance(UserConfig.selectedAccount).getClientUserId();
        for (String key : prefs.getAll().keySet()) {
            if (!key.startsWith("bot_")) continue;
            String value = prefs.getString(key, "");
            String[] p = value.split("\\|", -1);
            if (p.length < 5) continue;
            long owner;
            try { owner = Long.parseLong(p[0]); } catch (Exception e) { continue; }
            if (owner != me) continue;

            LinearLayout card = new LinearLayout(context);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(10), AndroidUtilities.dp(10), AndroidUtilities.dp(10));
            TextView t = new TextView(context);
            t.setText("🤖 " + p[1] + "\n@" + p[2] + "\nToken: •••••••••••\nHolat: " + p[4]);
            t.setTextSize(14);
            card.addView(t);

            Button toggle = new Button(context);
            boolean running = "RUNNING".equals(p[4]);
            toggle.setText(running ? "⏹ To'xtatish" : "▶ Ishga tushirish");
            toggle.setAllCaps(false);
            toggle.setOnClickListener(v -> {
                p[4] = running ? "STOPPED" : "RUNNING";
                prefs.edit().putString(key, join(p)).apply();
                rebuildList(context);
            });
            card.addView(toggle);

            Button delete = new Button(context);
            delete.setText("🗑 Botni o'chirish");
            delete.setAllCaps(false);
            delete.setOnClickListener(v -> {
                prefs.edit().remove(key).apply();
                rebuildList(context);
            });
            card.addView(delete);
            root.addView(card, LayoutHelper.createLinear(-1, -2, 0, 0, 0, 8));
        }
    }

    private String join(String[] p) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < p.length; i++) {
            if (i > 0) s.append("|");
            s.append(p[i]);
        }
        return s.toString();
    }
}
