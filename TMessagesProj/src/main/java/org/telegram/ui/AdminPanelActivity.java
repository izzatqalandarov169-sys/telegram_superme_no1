package org.telegram.ui;

import android.content.Context;
import android.view.View;

/**
 * Compatibility entry point kept so existing navigation does not break.
 * The old admin panel has been removed; this entry now opens Gift yaratish.
 */
public class AdminPanelActivity extends CustomGiftCreatorActivity {
    @Override
    public View createView(Context context) {
        return super.createView(context);
    }
}
