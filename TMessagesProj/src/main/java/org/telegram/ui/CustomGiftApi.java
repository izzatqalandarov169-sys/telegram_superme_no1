package org.telegram.ui;

import org.json.JSONObject;
import org.telegram.messenger.UserConfig;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

public final class CustomGiftApi {
    public static final String BASE_URL = "https://messenger-clone-zbef.onrender.com";
    private CustomGiftApi() {}

    public static String get(String path) throws Exception { return request("GET", path, null); }
    public static String postJson(String path, String json) throws Exception { return request("POST", path, json); }

    public static JSONObject getSupermeBalance() throws Exception {
        return new JSONObject(get("/superme/external/balance"));
    }

    public static JSONObject getExternalGifts() throws Exception {
        return new JSONObject(get("/superme/external/gifts"));
    }

    public static JSONObject getProfileGifts() throws Exception {
        return new JSONObject(get("/superme/external/profile/gifts"));
    }

    public static JSONObject createSubscriptionOrder(String productType, String productId) throws Exception {
        String json = "{\"product_type\":\"" + escape(productType) + "\",\"product_id\":\"" + escape(productId) + "\"}";
        return new JSONObject(postJson("/superme/external/subscription-order", json));
    }

    public static JSONObject purchaseSubscriptionWithStars(String productId) throws Exception {
        String json = "{\"product_id\":\"" + escape(productId) + "\"}";
        return new JSONObject(postJson("/superme/external/subscription-stars", json));
    }

    private static String request(String method, String path, String json) throws Exception {
        URL url = new URL(BASE_URL + (path.startsWith("/") ? path : "/" + path));
        HttpURLConnection c = (HttpURLConnection) url.openConnection();
        c.setRequestMethod(method);
        c.setConnectTimeout(15000);
        c.setReadTimeout(20000);
        c.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        c.setRequestProperty("X-Client-Id", String.valueOf(UserConfig.getInstance(UserConfig.selectedAccount).getClientUserId()));
        c.setRequestProperty("X-Request-Id", UUID.randomUUID().toString());
        if (json != null) {
            c.setDoOutput(true);
            c.getOutputStream().write(json.getBytes("UTF-8"));
        }
        int code = c.getResponseCode();
        InputStream in = code >= 400 ? c.getErrorStream() : c.getInputStream();
        if (in == null) return "";
        BufferedReader r = new BufferedReader(new InputStreamReader(in, "UTF-8"));
        StringBuilder out = new StringBuilder();
        String line;
        while ((line = r.readLine()) != null) out.append(line);
        r.close();
        if (code >= 400) {
            try {
                JSONObject error = new JSONObject(out.toString());
                String detail = error.optString("detail", "");
                if (!detail.isEmpty()) throw new Exception(detail);
            } catch (org.json.JSONException ignored) {
            }
            throw new Exception("BACKEND_HTTP_" + code);
        }
        return out.toString();
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
