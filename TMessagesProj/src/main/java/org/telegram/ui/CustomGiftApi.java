package org.telegram.ui;

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
        if (code >= 400) throw new Exception("Backend HTTP " + code + ": " + out);
        return out.toString();
    }
}
