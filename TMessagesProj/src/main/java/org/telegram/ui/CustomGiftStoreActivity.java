package org.telegram.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.BackDrawable;
import org.telegram.ui.Components.LayoutHelper;

public class CustomGiftStoreActivity extends BaseFragment {
    private LinearLayout root;

    private static final String[] GIFT_CATALOG = {
            "Homemade Cake", "Jelly Bunny", "Spiced Wine", "Santa Hat", "Berry Box",
            "Astral Shard", "Bonded Ring", "Bunny Muffin", "Candy Cane", "Clover Pin",
            "Crystal Ball", "Cupid Charm", "Desk Calendar", "Diamond Ring", "Durov's Cap",
            "Electric Skull", "Eternal Rose", "Evil Eye", "Flying Broom", "Genie Lamp",
            "Ginger Cookie", "Heart Locket", "Heroic Helmet", "Hex Pot", "Honeymoon Pod",
            "Ion Gem", "Jack-in-the-Box", "Jester Hat", "Kissed Frog", "Lol Pop",
            "Lunar Snake", "Magic Potion", "Mini Oscar", "Neko Helmet", "Nail Bracelet",
            "Party Sparkler", "Perfume Bottle", "Precious Peach", "Record Player", "Restless Jar",
            "Sakura Flower", "Sharp Tongue", "Signet Ring", "Skull Flower", "Snoop Dogg",
            "Snoop Cigar", "Snow Globe", "Socks", "Star Notepad", "Swiss Watch",
            "Tama Gadget", "Top Hat", "Toy Bear", "Trapped Heart", "Valentine Box",
            "Vintage Cigar", "Voodoo Doll", "Whip Cupcake", "Winter Wreath", "Witch Hat"
    };

    @Override public View createView(Context context) {
        actionBar.setTitle("Gifts");
        actionBar.setBackButtonDrawable(new BackDrawable(false));
        root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(12), AndroidUtilities.dp(16), AndroidUtilities.dp(12));

        TextView info = new TextView(context);
        info.setText("Gift katalogi • Stars • Collectible Gifts • Crafting");
        info.setTextSize(16);
        info.setTypeface(null, Typeface.BOLD);
        root.addView(info, LayoutHelper.createLinear(-1, -2));

        TextView note = new TextView(context);
        note.setText("Narxlar va mavjudlik sizning serveringizdagi sozlamalar bilan boshqariladi. Telegramning yangi giftlari chiqqanda katalogni yangilash mumkin.");
        note.setPadding(0, AndroidUtilities.dp(8), 0, AndroidUtilities.dp(12));
        root.addView(note, LayoutHelper.createLinear(-1, -2));

        for (String gift : GIFT_CATALOG) {
            TextView item = new TextView(context);
            item.setText("🎁  " + gift);
            item.setTextSize(15);
            item.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(12), AndroidUtilities.dp(12), AndroidUtilities.dp(12));
            root.addView(item, LayoutHelper.createLinear(-1, -2));
        }

        TextView collectible = new TextView(context);
        collectible.setText("\n✨ Collectible / NFT\n• Upgrade gift\n• Rarity / attributes\n• Collections\n• Crafting (Uncommon / Rare / Epic / Legendary)\n• Transfer / resale / offers\n• Auction support");
        collectible.setTextSize(15);
        root.addView(collectible, LayoutHelper.createLinear(-1, -2));

        TextView load = new TextView(context);
        load.setText("\n↻ Serverdagi giftlarni yangilash");
        load.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(18), AndroidUtilities.dp(12), AndroidUtilities.dp(18));
        load.setOnClickListener(v -> new Thread(() -> {
            try {
                String response = CustomGiftApi.get("/api/gifts");
                AndroidUtilities.runOnUIThread(() -> Toast.makeText(context, "Server gift katalogi yangilandi", Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                AndroidUtilities.runOnUIThread(() -> Toast.makeText(context, "Gift serveri bilan aloqa xatosi", Toast.LENGTH_SHORT).show());
            }
        }).start());
        root.addView(load, LayoutHelper.createLinear(-1, -2));

        fragmentView = root;
        return root;
    }
}
