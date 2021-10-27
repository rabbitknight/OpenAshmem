package net.rabbitknight.open.memory.service;

import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteCallbackList;

import net.rabbitknight.open.memory.IMemoryCallback;

public class MemoryMap implements IBinder.DeathRecipient {

    @Override
    public void binderDied() {

    }
}
