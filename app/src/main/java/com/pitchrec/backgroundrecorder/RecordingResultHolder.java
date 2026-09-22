package com.pitchrec.backgroundrecorder;

// Prosty, statyczny "callback holder" — zastępuje mechanizm PluginCall z Capacitora, którego
// tu nie używamy (aplikacja jest w pełni natywna, bez WebView). MainActivity ustawia
// listener przed startem/stopem, Service woła go po zakończeniu.
public class RecordingResultHolder {

    public interface Listener {
        void onSuccess(String base64, long durationMs, String mimeType);
        void onError(String code, String message);
    }

    private static Listener listener;

    public static void setListener(Listener l) {
        listener = l;
    }

    public static void resolveStop(String base64, long durationMs, String mimeType) {
        if (listener != null) listener.onSuccess(base64, durationMs, mimeType);
    }

    public static void rejectStop(String code, String message) {
        if (listener != null) listener.onError(code, message);
    }
}
