package com.gathin.tvhome;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * The current weather for a fixed place (weather_latitude / weather_longitude in config.xml), from Open-Meteo,
 * which needs no API key. Adapted from the photo-screensaver add-on's Weather, but with no location services:
 * the coordinates are configured, so nothing about where the TV is leaves it, and no location permission is
 * needed. Only used while weather_enabled is on, and only on the background thread MainActivity schedules
 * (every {@link #REFRESH_MS}); the launcher otherwise touches the network for nothing.
 *
 * This file, the INTERNET permission and the header line it fills are a personal-fork addition: the upstream
 * launcher declares no INTERNET permission and does no network by design.
 */
final class Weather {
    /** How often MainActivity refreshes the line while Home is resumed. */
    static final long REFRESH_MS = 30 * 60_000L;
    private static final int TIMEOUT_MS = 8_000;

    private Weather() {}

    /** "12°  &#9728;" for the place, blocking: call it off the main thread only. */
    static String now(double latitude, double longitude) throws IOException, JSONException {
        // Two decimals is about 1 km, which is all the weather needs and already far more precise than a city.
        String url = String.format(Locale.ROOT, "https://api.open-meteo.com/v1/forecast?latitude=%.2f"
                + "&longitude=%.2f&current=temperature_2m,weather_code,is_day", latitude, longitude);
        JSONObject current = get(url).getJSONObject("current");
        int temp = (int) Math.round(current.getDouble("temperature_2m"));
        return temp + "\u00B0  " + symbol(current.getInt("weather_code"), current.optInt("is_day", 1) == 1);
    }

    /** A plain symbol for a WMO weather code, from characters every TV font has. */
    static String symbol(int code, boolean day) {
        if (code == 0 || code == 1) return day ? "\u2600" : "\u263E";   // sun, moon
        if (code == 2) return "\u26C5";                                  // sun behind cloud
        if (code == 3 || code == 45 || code == 48) return "\u2601";      // cloud (fog too)
        if (code >= 71 && code <= 77 || code == 85 || code == 86) return "\u2744";   // snow
        if (code >= 95) return "\u26A1";                                 // thunder
        return "\u2602";                                                 // drizzle, rain, showers
    }

    private static JSONObject get(String url) throws IOException, JSONException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(TIMEOUT_MS);
        connection.setReadTimeout(TIMEOUT_MS);
        try (InputStream in = connection.getInputStream()) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            for (int n; (n = in.read(buffer)) != -1; ) {
                out.write(buffer, 0, n);
                if (out.size() > 64 * 1024) throw new IOException("reply too large");
            }
            return new JSONObject(new String(out.toByteArray(), StandardCharsets.UTF_8));
        } finally {
            connection.disconnect();
        }
    }
}
