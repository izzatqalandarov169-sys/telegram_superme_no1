package org.telegram.ui;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.BackDrawable;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.LayoutHelper;

public class AdminPanelActivity extends BaseFragment {
    private LinearLayout root;

    @Override
    public View createView(Context context) {
        actionBar.setTitle("Admin panel");
        actionBar.setBackButtonDrawable(new BackDrawable(false));
        ScrollView scroll = new ScrollView(context);
        root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(8), AndroidUtilities.dp(12), AndroidUtilities.dp(24));
        scroll.addView(root);

        addHeader("Boshqaruv");
        addButton("Stars qo'shish");
        addButton("Stars ayirish");
        addButton("Premium berish");
        addButton("Premium narxini o'zgartirish");
        addButton("Stars narxini o'zgartirish");

        addHeader("Giftlar");
        addButton("Gift qo'shish");
        addButton("Gift IDlarini ko'rish");
        addButton("Gift kanalini yaratish");
        addButton("Giftlar oynasini ochish");

        addHeader("Foydalanuvchi boshqaruvi");
        addButton("Ban");
        addButton("Unban");
        addButton("Spam");
        addButton("Unspam");
        addButton("Mute");
        addButton("Unmute");

        fragmentView = scroll;
        return scroll;
    }

    private void addHeader(String text) {
        TextView h = new TextView(getParentActivity());
        h.setText(text);
        h.setTextSize(14);
        h.setTypeface(null, android.graphics.Typeface.BOLD);
        h.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp (16), AndroidUtilities.dp(12), AndroidUtilities.dp (7));
        root.addView(h, LayoutHelper.createLinear(-1, -2));
    }

    private void addButton(String text) {
        Button b = new Button(getParentActivity());
        b.setText(text);
        b.setAllCaps(false);
        root.addView(b, LayoutHelper.createLinear(-1, AndroidUtilities.dp(52), 0, 0, 0, 2));
    }
}
