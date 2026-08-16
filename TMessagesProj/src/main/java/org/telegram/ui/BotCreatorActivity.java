package org.telegram.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
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

import java.util.UUID;

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
        info.setText("🤖 Bot yaratish bepul\nBotlar clone ilovasi ichida saqlanadi va ishga tushiriladi.");
        info.setTextSize(16);
        info.setTypeface(null, Typeface.BOLD);
        root.addView(info, LayoutHelper.createLinear(-1, -2));

        Button create = new Button(context);
        create.setText("➕ Yangi bot yaratish");
        create.setAllCaps(false);
        create.setOnClickListener(v -> showCreateDialog(context));
        root.addView(create, LayoutHelper.createLinear(-1, AndroidUtilities.dp(50), 0, 10, 0, 8));
        rebuildList(context);
        fragmentView = scroll;
        return scroll;
    }

    private void showCreateDialog(Context context) {
        LinearLayout box = new LinearLayout(context);
        box.setOrientation(LinearLayout.VERTICAL);
        EditText name = new EditText(context); name.setHint("Bot nomi"); box.addView(name);
        EditText username = new EditText(context); username.setHint("Username (mybot)"); box.addView(username);
        new AlertDialog.Builder(context).setTitle("Bepul bot yaratish").setView(box)
            .setPositiveButton("Yaratish", (d, w) -> {
                String n = name.getText().toString().trim();
                String u = username.getText().toString().trim().replace("@", "");
                if (n.length() == 0 || u.length() < 3) return;
                long owner = UserConfig.getInstance(UserConfig.selectedAccount).getClientUserId();
                String token = "sm_" + owner + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
                String key = "bot_" + System.currentTimeMillis();
                String value = owner + "|" + n.replace("|", "") + "|" + u.replace("|", "") + "|" + token + "|STOPPED";
                prefs.edit().putString(key, value).apply();
                rebuildList(context);
            }).setNegativeButton("Bekor", null).show();
    }

    private void rebuildList(Context context) {
        while (root.getChildCount() > 2) root.removeViewAt(2);
        long me = UserConfig.getInstance(UserConfig.selectedAccount).getClientUserId();
        for (String key : prefs.getAll().keySet()) {
            if (!key.startsWith("bot_")) continue;
            String value = prefs.getString(key, "");
            String[] p = value.split("\\|", -1);
            if (p.length < 5) continue;
            long owner;
            try { owner = Long.parseLong(p[0]); } catch (Exception e) { continue; }
            if (owner != me) continue;
            LinearLayout card = new LinearLayout(context); card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(10), AndroidUtilities.dp(10), AndroidUtilities.dp(10));
            TextView t = new TextView(context);
            t.setText("🤖 " + p[1] + "\n@" + p[2] + "\nToken: " + p[3] + "\nHolat: " + p[4]);
            t.setTextSize(14); card.addView(t);
            Button toggle = new Button(context);
            boolean running = "RUNNING".equals(p[4]);
            toggle.setText(running ? "⏹ To'xtatish" : "▶ Ishga tushirish"); toggle.setAllCaps(false);
            toggle.setOnClickListener(v -> { p[4] = running ? "STOPPED" : "RUNNING"; prefs.edit().putString(key, join(p)).apply(); rebuildList(context); });
            card.addView(toggle);
            Button delete = new Button(context); delete.setText("🗑 Botni o'chirish"); delete.setAllCaps(false);
            delete.setOnClickListener(v -> { prefs.edit().remove(key).apply(); rebuildList(context); }); card.addView(delete);
            root.addView(card, LayoutHelper.createLinear(-1, -2, 0, 0, 0, 8));
        }
    }

    private String join(String[] p) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < p.length; i++) { if (i > 0) s.append("|"); s.append(p[i]); }
        return s.toString();
    }
}
