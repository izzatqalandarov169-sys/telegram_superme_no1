package org.telegram.ui;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.BackDrawable;
import org.telegram.ui.Components.LayoutHelper;

public class CustomGiftCreatorActivity extends BaseFragment {
    private Uri videoUri;
    private TextView selected;
    private EditText name;
    private EditText price;

    @Override public View createView(Context context) {
        actionBar.setTitle("Gift yaratish");
        actionBar.setBackButtonDrawable(new BackDrawable(false));
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(12), AndroidUtilities.dp(16), AndroidUtilities.dp(16));

        TextView limit = new TextView(context);
        limit.setText("Har bir foydalanuvchi ko'pi bilan 10 ta gift yaratishi mumkin.");
        root.addView(limit, LayoutHelper.createLinear(-1, -2));

        name = new EditText(context);
        name.setHint("Gift nomi");
        root.addView(name, LayoutHelper.createLinear(-1, -2));

        price = new EditText(context);
        price.setHint("Gift narxi — Stars");
        price.setInputType(2);
        root.addView(price, LayoutHelper.createLinear(-1, -2));

        Button pick = new Button(context);
        pick.setText("1–20 soniyalik gift videosini tanlash");
        pick.setAllCaps(false);
        pick.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.setType("video/*");
            i.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(i, 7001);
        });
        root.addView(pick, LayoutHelper.createLinear(-1, -2));

        selected = new TextView(context);
        selected.setText("Video tanlanmagan");
        root.addView(selected, LayoutHelper.createLinear(-1, -2));

        Button create = new Button(context);
        create.setText("Giftni yaratish");
        create.setAllCaps(false);
        create.setOnClickListener(v -> createGift());
        root.addView(create, LayoutHelper.createLinear(-1, -2));

        TextView note = new TextView(context);
        note.setText("Kanal va promo talablarini Admin paneldan sozlash mumkin. Kanalga majburan a'zo qilish o'rniga foydalanuvchiga kanalga kirish havolasi beriladi va backend mavjud bo'lsa a'zolik tekshiriladi.");
        root.addView(note, LayoutHelper.createLinear(-1, -2));

        fragmentView = root;
        return root;
    }

    @Override
    public void onActivityResultFragment(int requestCode, int resultCode, Intent data) {
        super.onActivityResultFragment(requestCode, resultCode, data);
        if (requestCode != 7001 || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        long seconds = getVideoDuration(uri);
        if (seconds < 1 || seconds > 20) {
            Toast.makeText(getParentActivity(), "Video 1 dan 20 soniyagacha bo'lishi kerak", Toast.LENGTH_LONG).show();
            return;
        }
        videoUri = uri;
        selected.setText("Tanlandi: " + getFileName(uri) + " — " + seconds + " soniya");
    }

    private void createGift() {
        if (videoUri == null) { toast("Avval video tanlang"); return; }
        String giftName = name.getText().toString().trim();
        long stars = parse(price.getText().toString());
        if (giftName.length() == 0 || stars <= 0) { toast("Gift nomi va Stars narxini kiriting"); return; }

        String userId = String.valueOf(UserConfig.getInstance(UserConfig.selectedAccount).getClientUserId());
        new Thread(() -> {
            try {
                String countJson = CustomGiftApi.get("/api/gifts/count?user_id=" + userId);
                int count = extractCount(countJson);
                if (count >= 10) {
                    AndroidUtilities.runOnUIThread(() -> toast("Siz 10 ta gift limitiga yetdingiz"));
                    return;
                }
                String safeName = giftName.replace("\\", "\\\\").replace("\"", "\\\"");
                String json = "{\"user_id\":\"" + userId + "\",\"name\":\"" + safeName + "\",\"stars\":" + stars + ",\"duration\":" + getVideoDuration(videoUri) + ",\"video_name\":\"" + getFileName(videoUri).replace("\"", "") + "\"}";
                CustomGiftApi.postJson("/api/gifts", json);
                AndroidUtilities.runOnUIThread(() -> toast("Gift yaratildi va serverga yuborildi"));
            } catch (Exception e) {
                AndroidUtilities.runOnUIThread(() -> toast("Gift yaratishda server xatosi"));
            }
        }).start();
    }

    private int extractCount(String json) {
        try {
            int p = json.indexOf("count");
            if (p >= 0) {
                String s = json.substring(p).replaceAll("[^0-9]", "");
                return s.length() == 0 ? 0 : Integer.parseInt(s);
            }
        } catch (Exception ignored) {}
        return 0;
    }

    private long getVideoDuration(Uri uri) {
        MediaMetadataRetriever r = new MediaMetadataRetriever();
        try {
            r.setDataSource(getParentActivity(), uri);
            String d = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            return Math.round(Long.parseLong(d == null ? "0" : d) / 1000.0);
        } catch (Exception e) { return 0; }
        finally { try { r.release(); } catch (Exception ignored) {} }
    }

    private String getFileName(Uri uri) {
        Cursor c = null;
        try {
            c = getParentActivity().getContentResolver().query(uri, null, null, null, null);
            if (c != null && c.moveToFirst()) {
                int i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (i >= 0) return c.getString(i);
            }
        } catch (Exception ignored) {} finally { if (c != null) c.close(); }
        return "video";
    }

    private long parse(String s) { try { return Math.max(0, Long.parseLong(s.trim())); } catch (Exception e) { return 0; } }
    private void toast(String s) { Toast.makeText(getParentActivity(), s, Toast.LENGTH_SHORT).show(); }
}
