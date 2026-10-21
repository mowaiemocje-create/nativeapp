package com.pitchrec.nativetest;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

// Po restarcie telefonu (albo aktualizacji aplikacji) budziki AlarmManagera znikają — ustawiamy je ponownie.
// Celowo NIC więcej: od Androida 15 odbiornik BOOT_COMPLETED nie może uruchamiać usług na pierwszym planie
// (mikrofon, odtwarzanie) — aplikacja by się wtedy zamknęła. Nagrywanie startuje wyłącznie z aplikacji.
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent intent) {
        String act = intent != null ? intent.getAction() : null;
        if (!Intent.ACTION_BOOT_COMPLETED.equals(act) && !Intent.ACTION_MY_PACKAGE_REPLACED.equals(act)) return;
        try { L.init(c); } catch (Throwable t) { }
        try { ReminderReceiver.schedule(c); } catch (Throwable t) { }
        try { TrainerWatch.schedule(c); } catch (Throwable t) { }
        try { AvailWatch.schedule(c); } catch (Throwable t) { }
        try { CallRecUi.watch(c); } catch (Throwable t) { } // wersja kursantów: tylko powiadomienie, bez uruchamiania usługi
    }
}
