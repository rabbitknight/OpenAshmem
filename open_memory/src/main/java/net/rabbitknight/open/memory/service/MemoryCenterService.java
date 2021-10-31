package net.rabbitknight.open.memory.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import net.rabbitknight.open.memory.IMemoryCenter;

/**
 * 中心服务
 * 提供Binder实例
 */
public class MemoryCenterService extends Service {
    private final IMemoryCenter memoryCenter = new MemoryCenterStub();

    public MemoryCenterService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return memoryCenter.asBinder();
    }
}