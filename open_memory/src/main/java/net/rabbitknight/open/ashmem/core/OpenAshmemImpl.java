package net.rabbitknight.open.ashmem.core;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import net.rabbitknight.open.ashmem.IMemoryCenter;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;

public class OpenAshmemImpl {
    private static final String TAG = "OpenAshmemImpl";

    private WeakReference<Context> contextWeakReference = null;
    private final Set<IConnectListener> connectListeners = new HashSet<>();
    private ComponentName componentName = null;
    private IMemoryCenter remoteBinder = null;

    private volatile boolean connected = false;

    public OpenAshmemImpl(Context context, ComponentName componentName) {
        this.contextWeakReference = new WeakReference<>(context);
        this.componentName = componentName;
    }

    public void bind() {
        Context context = contextWeakReference.get();
        if (context == null) {
            throw new IllegalStateException("context is null");
        }
        Intent intent = new Intent();
        intent.setComponent(componentName);
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
                        Log.w(TAG, "onServiceConnected: listener = " + listener + " Exception", e);
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
