
package com.example.javacalculator;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ApiClient {

    private static final String BASE_URL =
            "http://10.0.2.2:3000";

    private static final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private static final Handler mainHandler =
            new Handler(Looper.getMainLooper());

    public interface Callback {
        void onSuccess(JSONObject response);
        void onError(String message);
    }

    public interface HistoryCallback {
        void onSuccess(JSONArray history);
        void onError(String message);
    }

    public static void calculate(
            double a, String operator, double b,
            Callback callback) {

        executor.execute(() -> {
            HttpURLConnection connection = null;

            try {
                connection = (HttpURLConnection)
                        new URL(BASE_URL + "/api/calculations")
                                .openConnection();

                connection.setRequestMethod("POST");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                connection.setDoOutput(true);
                connection.setRequestProperty(
                        "Content-Type", "application/json");
                connection.setRequestProperty(
                        "Accept", "application/json");

                JSONObject body = new JSONObject();
                body.put("a", a);
                body.put("operator", operator);
                body.put("b", b);

                try (OutputStream out =
                             connection.getOutputStream()) {
                    out.write(body.toString().getBytes(
                            StandardCharsets.UTF_8));
                }

                int status = connection.getResponseCode();
                String responseText = readResponse(
                        connection, status);

                if (status < 200 || status >= 300) {
                    JSONObject error = new JSONObject(
                            responseText);

                    throw new ApiException(
                            error.optString(
                                    "error", "HTTP " + status));
                }

                JSONObject response = new JSONObject(
                        responseText);

                mainHandler.post(() ->
                        callback.onSuccess(response));

            } catch (Exception e) {
                String message = e instanceof ApiException
                        ? e.getMessage()
                        : "Cannot reach the server. Check the API URL.";

                mainHandler.post(() ->
                        callback.onError(message));
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    public static void getHistory(
            HistoryCallback callback) {

        executor.execute(() -> {
            HttpURLConnection connection = null;

            try {
                connection = (HttpURLConnection)
                        new URL(BASE_URL + "/api/calculations")
                                .openConnection();

                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);

                int status = connection.getResponseCode();
                String responseText = readResponse(
                        connection, status);

                if (status < 200 || status >= 300) {
                    throw new ApiException(
                            "Could not load history. HTTP "
                                    + status);
                }

                JSONArray history = new JSONArray(
                        responseText);

                mainHandler.post(() ->
                        callback.onSuccess(history));

            } catch (Exception e) {
                mainHandler.post(() ->
                        callback.onError(e.getMessage() == null
                                ? "Could not load history."
                                : e.getMessage()));
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    private static String readResponse(
            HttpURLConnection connection, int status)
            throws Exception {

        InputStream stream = status >= 200 && status < 300
                ? connection.getInputStream()
                : connection.getErrorStream();

        if (stream == null) {
            return "";
        }

        StringBuilder text = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        stream, StandardCharsets.UTF_8))) {
            String line;

            while ((line = reader.readLine()) != null) {
                text.append(line);
            }
        }

        return text.toString();
    }

    private static class ApiException extends Exception {
        ApiException(String message) {
            super(message);
        }
    }
}
