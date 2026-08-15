package org.telegram.ui;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.BackDrawable;
import org.telegram.ui.Components.LayoutHelper;

public class CustomGiftCreatorActivity extends BaseFragment {
    @Override public View createView(Context context) {
        actionBar.setTitle("Create Gift");
        actionBar.setBackButtonDrawable(new BackDrawable(false));
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16));
        EditText name = new EditText(context);
        name.setHint("Gift nomi");
        root.addView(name, LayoutHelper.createLinear(-1, -2));
        EditText price = new EditText(context);
        price.setHint("Stars narxi");
        price.setInputType(2);
        root.addView(price, LayoutHelper.createLinear(-1, -2));
        Button create = new Button(context);
        create.setText("Serverga saqlash");
        create.setOnClickListener(v -> new Thread(() -> {
            try {
                String json = "{\"name\":\"" + name.getText().toString().replace("\"", "") + "\",\"price\":" + (price.getText().length() == 0 ? "0" : price.getText().toString()) + "}";
                CustomGiftApi.postJson("/api/gifts", json);
                AndroidUtilities.runOnUIThread(() -> Toast.makeText(context, "Gift serverga saqlandi", Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                AndroidUtilities.runOnUIThread(() -> Toast.makeText(context, "Saqlashda xato", Toast.LENGTH_SHORT).show());
            }
        }).start());
        root.addView(create, LayoutHelper.createLinear(-1, -2));
        fragmentView = root;
        return root;
    }
}
