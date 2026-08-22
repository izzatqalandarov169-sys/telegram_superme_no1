package org.telegram.messenger;

import android.content.Context;
import org.json.JSONObject;

/**
 * SupermeBalance manages the local Superme wallet model.
 * This is separate from Telegram's official Stars balance.
 * Balance is fetched from the Render backend in real-time.
 */
public final class SupermeBalance {
    private static final long OWNER_INITIAL_BALANCE = 500_000_000L;
    private static final long OWNER_USER_ID = 8572946823L;
    
    private static long cachedBalance = 0L;
    private static long lastFetchTime = 0L;
    private static final long CACHE_DURATION_MS = 30_000; // 30 seconds

    private SupermeBalance() { }

    /**
     * Get current balance - fetches from backend if cache expired
     */
    public static long getBalance(Context context) {
        if (isOwner()) {
            return OWNER_INITIAL_BALANCE;
        }
        
        // Use cache if fresh
        long now = System.currentTimeMillis();
        if (now - lastFetchTime < CACHE_DURATION_MS && cachedBalance > 0) {
            return cachedBalance;
        }
        
        // Fetch from backend
        fetchBalanceFromBackend(context);
        return cachedBalance;
    }
    
    /**
     * Synchronously fetch balance from backend (blocking)
     */
    private static void fetchBalanceFromBackend(Context context) {
        String clientId = getUserId();
        SupermeApiClient.getInstance(context).fetchBalance(clientId, new SupermeApiClient.BalanceCallback() {
            @Override
            public void onSuccess(long balance) {
                cachedBalance = balance;
                lastFetchTime = System.currentTimeMillis();
                FileLog.d("SupermeBalance: Fetched balance from backend: " + balance);
            }
            
            @Override
            public void onError(String error) {
                FileLog.e("SupermeBalance fetch error: " + error);
                // Keep using cached value on error
            }
        });
    }
    
    /**
     * Get cached balance without fetching
     */
    public static long getCachedBalance() {
        if (isOwner()) {
            return OWNER_INITIAL_BALANCE;
        }
        return cachedBalance;
    }
    
    /**
     * Update balance after purchase
     */
    public static void updateBalance(long newBalance) {
        cachedBalance = newBalance;
        lastFetchTime = System.currentTimeMillis();
    }

    /**
     * Check if current user is the owner
     */
    public static boolean isOwner() {
        return UserConfig.getInstance(UserConfig.selectedAccount).getClientUserId() == OWNER_USER_ID;
    }
    
    /**
     * Get current user ID as string
     */
    public static String getUserId() {
        return String.valueOf(UserConfig.getInstance(UserConfig.selectedAccount).getClientUserId());
    }
}
