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

    private static final String[] BASE_GIFTS = {
            "Homemade Cake", "Jelly Bunny", "Spiced Wine", "Santa Hat", "Berry Box",
            "Astral Shard", "Bonded Ring", "Bunny Muffin", "Candy Cane", "Clover Pin",
            "Crystal Ball", "Cupid Charm", "Desk Calendar", "Diamond Ring", "Durov's Cap",
            "Electric Skull", "Eternal Rose", "Evil Eye", "Flying Broom", "Genie Lamp",
            "Ginger Cookie", "Heart Locket", "Heroic Helmet", "Hex Pot", "Honeymoon Pod",
            "Ion Gem", "Jack-in-the-Box", "Jester Hat", "Kissed Frog", "Lol Pop",
            "Lunar Snake", "Magic Potion", "Mini Oscar", "Neko Helmet", "Nail Bracelet",
            "Party Sparkler", "Perfume Bottle", "Precious Peach", "Record Player", "Restless Jar",
            "Sakura Flower", "Sharp Tongue", "Signet Ring", "Skull Flower", "Snow Globe",
            "Star Notepad", "Swiss Watch", "Tama Gadget", "Top Hat", "Toy Bear",
            "Trapped Heart", "Valentine Box", "Vintage Cigar", "Voodoo Doll", "Whip Cupcake",
            "Winter Wreath", "Witch Hat"
    };

    private static final String[] THEMES = {
            "Classic", "Royal", "Galaxy", "Neon", "Crystal", "Golden", "Diamond", "Rainbow",
            "Cyber", "Ocean", "Forest", "Desert", "Arctic", "Lunar", "Solar", "Magic",
            "Fantasy", "Retro", "Pixel", "Cosmic", "Legendary", "Cute", "Luxury", "Festival"
    };

    @Override public View createView(Context context) {
        actionBar.setTitle("Gifts • 1200+");
        actionBar.setBackButtonDrawable(new BackDrawable(false));
        root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(12), AndroidUtilities.dp(16), AndroidUtilities.dp(12));

        TextView info = new TextView(context);
        info.setText("🎁 1200+ custom gifts • Stars • Collectible Gifts • Crafting");
        info.setTextSize(16);
        info.setTypeface(null, Typeface.BOLD);
        root.addView(info, LayoutHelper.createLinear(-1, -2));

        TextView note = new TextView(context);
        note.setText("Bu katalog bizning custom ilovamiz uchun. Har bir giftning ID, nomi va Stars narxi admin paneldan boshqariladi.");
        note.setPadding(0, AndroidUtilities.dp(8), 0, AndroidUtilities.dp(12));
        root.addView(note, LayoutHelper.createLinear(-1, -2));

        int id = 1000;
        for (String gift : BASE_GIFTS) {
            addGift(context, id++, gift);
        }
        for (String theme : THEMES) {
            for (String gift : BASE_GIFTS) {
                if (id > 2200) break;
                addGift(context, id++, theme + " " + gift);
            }
        }

        TextView collectible = new TextView(context);
        collectible.setText("\n✨ Collectible / NFT\n• Upgrade gift\n• Rarity / attributes\n• Collections\n• Crafting: Uncommon / Rare / Epic / Legendary\n• Transfer / resale / offers\n• Auction support");
        collectible.setTextSize(15);
        root.addView(collectible, LayoutHelper.createLinear(-1, -2));

        TextView load = new TextView(context);
        load.setText("\n↻ Serverdagi giftlarni yangilash");
        load.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(18), AndroidUtilities.dp(12), AndroidUtilities.dp(18));
        load.setOnClickListener(v -> new Thread(() -> {
            try {
                CustomGiftApi.get("/api/gifts");
                AndroidUtilities.runOnUIThread(() -> Toast.makeText(context, "Server gift katalogi yangilandi", Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                AndroidUtilities.runOnUIThread(() -> Toast.makeText(context, "Gift serveri bilan aloqa xatosi", Toast.LENGTH_SHORT).show());
            }
        }).start());
        root.addView(load, LayoutHelper.createLinear(-1, -2));

        fragmentView = root;
        return root;
    }

    private void addGift(Context context, int id, String name) {
        TextView item = new TextView(context);
        item.setText("🎁  #" + id + "  " + name);
        item.setTextSize(15);
        item.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(10), AndroidUtilities.dp(12), AndroidUtilities.dp(10));
        item.setOnClickListener(v -> Toast.makeText(context, "Gift #" + id + " tanlandi", Toast.LENGTH_SHORT).show());
        root.addView(item, LayoutHelper.createLinear(-1, -2));
    }
}
