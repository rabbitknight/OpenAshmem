package net.rabbitknight.open.memory;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import net.rabbitknight.open.memory.core.IConnectListener;
import net.rabbitknight.open.memory.core.Receiver;
import net.rabbitknight.open.memory.core.Sender;
import net.rabbitknight.open.memory.service.MemoryCenterService;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;

public class OpenMemory {

    private WeakReference<Context> contextWeakReference = null;
    private Set<IConnectListener> connectListeners = new HashSet<>();
    private Class<?> serviceClazz = MemoryCenterService.class;

    private IMemoryCenter remoteBinder = null;

    public OpenMemory(Context context, Class<?> service) {
        this.contextWeakReference = new WeakReference<>(context);
        if (service != null)
            this.serviceClazz = service;
    }

    public void bind() {
        Context context = contextWeakReference.get();
        if (context == null) {
            throw new IllegalStateException("context is null");
        }
        Intent intent = new Intent(context, serviceClazz);
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    public void unbind() {
        Context context = contextWeakReference.get();
        if (context == null) {
            throw new IllegalStateException("context is null");
        }
        context.unbindService(serviceConnection);
    }

    public IMemoryCenter getRemoteBinder() {
        return remoteBinder;
    }

    public Sender createSender(String key, int size) {
        Sender sender = new Sender(this, key, size);
        connectListeners.add(sender);
        return sender;
    }

    public Receiver createReceiver(String key, int size) {
        Receiver receiver = new Receiver(this, key, size);
        connectListeners.add(receiver);
        return receiver;
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // 创建远程服务
            remoteBinder = IMemoryCenter.Stub.asInterface(service);
            // 通知已经链接
            for (IConnectListener listener : connectListeners) {
                listener.onServiceConnected();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            // 通知已经关闭
            for (IConnectListener listener : connectListeners) {
                listener.onServiceDisconnected();
            }
        }
    };
}
