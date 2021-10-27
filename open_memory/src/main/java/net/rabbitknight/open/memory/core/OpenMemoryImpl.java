package net.rabbitknight.open.memory.core;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import net.rabbitknight.open.memory.IMemoryCenter;
import net.rabbitknight.open.memory.service.MemoryCenterService;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;

public class OpenMemoryImpl {

    private WeakReference<Context> contextWeakReference = null;
    private final Set<IConnectListener> connectListeners = new HashSet<>();
    private Class<?> serviceClazz = MemoryCenterService.class;
    private IMemoryCenter remoteBinder = null;

    private volatile boolean connected = false;

    public OpenMemoryImpl(Context context, Class<?> service) {
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

    IMemoryCenter getRemoteBinder() {
        return remoteBinder;
    }

    public Sender createSender(String key, int size) {
        Sender sender = new Sender(this, key, size);
        synchronized (connectListeners) {
            connectListeners.add(sender.getConnection());
            // 如果已经链接 直接请求
            if (connected) {
                sender.getConnection().onServiceConnected();
            }
        }
        return sender;
    }

    public Receiver createReceiver(String key, int size) {
        Receiver receiver = new Receiver(this, key, size);
        synchronized (connectListeners) {
            connectListeners.add(receiver.getConnection());
            // 如果已经链接 直接请求
            if (connected) {
                receiver.getConnection().onServiceConnected();
            }
        }
        return receiver;
    }

    void close(Sender sender) {
        synchronized (connectListeners) {
            connectListeners.remove(sender.getConnection());
        }
    }

    void close(Receiver receiver) {
        synchronized (connectListeners) {
            connectListeners.remove(receiver.getConnection());
        }
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            // 创建远程服务
            remoteBinder = IMemoryCenter.Stub.asInterface(service);
            // 已链接
            connected = true;

            synchronized (connectListeners) {
                // 通知已经链接
                for (IConnectListener listener : connectListeners) {
                    try {
                        listener.onServiceConnected();
                    } catch (Exception e) {
                    }
                }
            }

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            // 断开
            connected = false;
            synchronized (connectListeners) {
                // 通知已经关闭
                for (IConnectListener listener : connectListeners) {
                    try {
                        listener.onServiceDisconnected();
                    } catch (Exception e) {
                    }
                }
            }

        }
    };
}
