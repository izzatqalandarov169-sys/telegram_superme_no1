package org.telegram.messenger;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

/**
 * SupermeGiftManager handles gift catalog, purchases, and inventory
 * from the Render backend.
 */
public class SupermeGiftManager {
    private static SupermeGiftManager instance;
    private final Context context;
    private List<GiftItem> giftCatalog = new ArrayList<>();
    private long lastCatalogSync = 0L;
    private static final long SYNC_INTERVAL_MS = 300_000; // 5 minutes
    
    private SupermeGiftManager(Context context) {
        this.context = context.getApplicationContext();
    }
    
    public static synchronized SupermeGiftManager getInstance(Context context) {
        if (instance == null) {
            instance = new SupermeGiftManager(context);
        }
        return instance;
    }
    
    /**
     * Sync gift catalog from backend
     */
    public void syncCatalog() {
        long now = System.currentTimeMillis();
        if (now - lastCatalogSync < SYNC_INTERVAL_MS) {
            return; // Skip if recently synced
        }
        
        SupermeApiClient.getInstance(context).fetchGifts(new SupermeApiClient.GiftsCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                try {
                    parseGiftCatalog(response);
                    lastCatalogSync = System.currentTimeMillis();
                    FileLog.d("SupermeGiftManager: Catalog synced, " + giftCatalog.size() + " gifts");
                } catch (JSONException e) {
                    FileLog.e("SupermeGiftManager parse error: " + e.getMessage());
                }
            }
            
            @Override
            public void onError(String error) {
                FileLog.e("SupermeGiftManager sync error: " + error);
            }
        });
    }
    
    /**
     * Parse gift catalog from backend response
     */
    private void parseGiftCatalog(JSONObject response) throws JSONException {
        giftCatalog.clear();
        JSONArray gifts = response.optJSONArray("gifts");
        
        if (gifts != null) {
            for (int i = 0; i < gifts.length(); i++) {
                JSONObject giftJson = gifts.getJSONObject(i);
                GiftItem gift = new GiftItem(
                    giftJson.optString("id"),
                    giftJson.optString("name"),
                    giftJson.optString("symbol"),
                    giftJson.optLong("superme_stars", 0),
                    giftJson.optLong("price_uzs", 0),
                    giftJson.optBoolean("is_premium"),
                    giftJson.optBoolean("is_birthday")
                );
                giftCatalog.add(gift);
            }
        }
    }
    
    /**
     * Purchase a gift
     */
    public void purchaseGift(String giftId, String recipientId, final PurchaseListener listener) {
        String clientId = SupermeBalance.getUserId();
        long userBalance = SupermeBalance.getCachedBalance();
        
        // Find gift in catalog
        GiftItem gift = null;
        for (GiftItem g : giftCatalog) {
            if (g.id.equals(giftId)) {
                gift = g;
                break;
            }
        }
        
        if (gift == null) {
            listener.onError("Gift not found in catalog");
            return;
        }
        
        if (userBalance < gift.starsPrice) {
            listener.onError("Insufficient Superme Stars");
            return;
        }
        
        SupermeApiClient.getInstance(context).purchaseGift(clientId, giftId, recipientId,
            new SupermeApiClient.PurchaseCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    try {
                        if (response.getBoolean("ok")) {
                            long newBalance = response.optLong("balance", 0);
                            SupermeBalance.updateBalance(newBalance);
                            listener.onSuccess(response.optString("transaction_id"));
                            FileLog.d("SupermeGiftManager: Purchase success");
                        } else {
                            listener.onError(response.optString("error", "Purchase failed"));
                        }
                    } catch (JSONException e) {
                        listener.onError("Parse error: " + e.getMessage());
                    }
                }
                
                @Override
                public void onError(String error) {
                    listener.onError(error);
                }
            });
    }
    
    public List<GiftItem> getCatalog() {
        return giftCatalog;
    }
    
    // Data model
    public static class GiftItem {
        public String id;
        public String name;
        public String symbol;
        public long starsPrice;
        public long uzsPrice;
        public boolean isPremium;
        public boolean isBirthday;
        
        public GiftItem(String id, String name, String symbol, long starsPrice, 
                       long uzsPrice, boolean isPremium, boolean isBirthday) {
            this.id = id;
            this.name = name;
            this.symbol = symbol;
            this.starsPrice = starsPrice;
            this.uzsPrice = uzsPrice;
            this.isPremium = isPremium;
            this.isBirthday = isBirthday;
        }
    }
    
    public interface PurchaseListener {
        void onSuccess(String transactionId);
        void onError(String error);
    }
}
