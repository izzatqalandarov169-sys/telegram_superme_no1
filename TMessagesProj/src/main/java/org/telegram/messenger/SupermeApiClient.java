package org.telegram.messenger;

import android.content.Context;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * SupermeApiClient handles all communication with the Render backend at
 * https://messenger-clone-zbef.onrender.com
 */
public class SupermeApiClient {
    private static final String BACKEND_URL = "https://messenger-clone-zbef.onrender.com";
    private static final String BASE_API = BACKEND_URL + "/superme/external";
    
    private static SupermeApiClient instance;
    private final RequestQueue requestQueue;
    private final Context context;
    
    private SupermeApiClient(Context context) {
        this.context = context.getApplicationContext();
        this.requestQueue = Volley.newRequestQueue(this.context);
    }
    
    public static synchronized SupermeApiClient getInstance(Context context) {
        if (instance == null) {
            instance = new SupermeApiClient(context);
        }
        return instance;
    }
    
    /**
     * Fetch current user's Superme Stars balance from backend
     */
    public void fetchBalance(String clientId, final BalanceCallback callback) {
        String url = BASE_API + "/balance";
        
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
            new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    try {
                        if (response.getBoolean("ok")) {
                            long stars = response.optLong("stars", 0);
                            long balance = response.optLong("balance", 0);
                            callback.onSuccess(stars > 0 ? stars : balance);
                        } else {
                            callback.onError(response.optString("error", "Unknown error"));
                        }
                    } catch (JSONException e) {
                        callback.onError("JSON parse error: " + e.getMessage());
                    }
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    callback.onError("Network error: " + error.getMessage());
                }
            }) {
            @Override
            public java.util.Map<String, String> getHeaders() {
                java.util.Map<String, String> headers = new java.util.HashMap<>();
                headers.put("X-Client-Id", clientId);
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };
        
        requestQueue.add(request);
    }
    
    /**
     * Fetch available gifts from backend catalog
     */
    public void fetchGifts(final GiftsCallback callback) {
        String url = BASE_API + "/gifts";
        
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
            new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    try {
                        if (response.getBoolean("ok")) {
                            callback.onSuccess(response);
                        } else {
                            callback.onError(response.optString("error", "Failed to fetch gifts"));
                        }
                    } catch (JSONException e) {
                        callback.onError("JSON parse error: " + e.getMessage());
                    }
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    callback.onError("Network error: " + error.getMessage());
                }
            }
        );
        
        requestQueue.add(request);
    }
    
    /**
     * Purchase a gift from the marketplace
     */
    public void purchaseGift(String clientId, String giftId, String recipientId, 
                            final PurchaseCallback callback) {
        String url = BACKEND_URL + "/api/purchase/gift";
        
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("gift_id", giftId);
            requestBody.put("recipient_id", recipientId);
        } catch (JSONException e) {
            callback.onError("Request build error: " + e.getMessage());
            return;
        }
        
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, requestBody,
            new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    try {
                        if (response.getBoolean("ok")) {
                            callback.onSuccess(response);
                        } else {
                            callback.onError(response.optString("error", "Purchase failed"));
                        }
                    } catch (JSONException e) {
                        callback.onError("JSON parse error: " + e.getMessage());
                    }
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    callback.onError("Network error: " + error.getMessage());
                }
            }) {
            @Override
            public java.util.Map<String, String> getHeaders() {
                java.util.Map<String, String> headers = new java.util.HashMap<>();
                headers.put("X-Client-Id", clientId);
                headers.put("X-Request-Id", java.util.UUID.randomUUID().toString());
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };
        
        requestQueue.add(request);
    }
    
    /**
     * Check backend health and configuration
     */
    public void checkHealth(final HealthCallback callback) {
        String url = BACKEND_URL + "/health";
        
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
            new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    try {
                        callback.onSuccess(response.getBoolean("ok"));
                    } catch (JSONException e) {
                        callback.onError("Health check parse error");
                    }
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    callback.onError("Backend unreachable: " + error.getMessage());
                }
            }
        );
        
        requestQueue.add(request);
    }
    
    // Callback interfaces
    public interface BalanceCallback {
        void onSuccess(long balance);
        void onError(String error);
    }
    
    public interface GiftsCallback {
        void onSuccess(JSONObject response);
        void onError(String error);
    }
    
    public interface PurchaseCallback {
        void onSuccess(JSONObject response);
        void onError(String error);
    }
    
    public interface HealthCallback {
        void onSuccess(boolean healthy);
        void onError(String error);
    }
}
