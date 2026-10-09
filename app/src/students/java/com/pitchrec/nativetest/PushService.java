package com.pitchrec.nativetest;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

// Odbiera powiadomienia push z proxy. "avail" = ktos kliknal "Chcę porozmawiać" — pokazujemy
// powiadomienie od razu (te same zasady co przy sprawdzaniu w tle: bez siebie, bez nocy, bez duplikatow).
public class PushService extends FirebaseMessagingService {

    @Override
    public void onNewToken(String token) {
        L.init(this);
        Push.send(this, token);
    }

    @Override
    public void onMessageReceived(RemoteMessage msg) {
        L.init(this);
        Map<String, String> d = msg.getData();
        if (d == null) return;
        if ("avail".equals(d.get("type"))) AvailWatch.fromPush(this, d);
    }
}
