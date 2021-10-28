package net.rabbitknight.open.memory.service;

import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;

import net.rabbitknight.open.memory.C;
import net.rabbitknight.open.memory.IMemoryCallback;
import net.rabbitknight.open.memory.IMemoryCenter;
import net.rabbitknight.open.memory.IMemoryClient;
import net.rabbitknight.open.memory.core.MemoryFileHolder;

import java.util.HashMap;
import java.util.Map;

import static net.rabbitknight.open.memory.C.KEY_CALLBACK;
import static net.rabbitknight.open.memory.C.KEY_HOLDER;

public class MemoryCenterStub extends IMemoryCenter.Stub {
    private static final String TAG = "MemoryCenterStub";

    private final Map<String, MemoryHolder> fileMap = new HashMap<>();
    private final Map<String, RemoteCallbackList<IMemoryCallback>> callbackMap = new HashMap<>();

    /**
     * 开启共享内存
     *
     * @param key  共享内存文件名
     * @param size 共享内存尺寸
     * @param args 存储返回的序列化文件描述符
     * @return 0 for success
     * @throws RemoteException 远程调用异常
     */
    @Override
    public int open(final String key, int size, Bundle args) throws RemoteException {
        Log.d(TAG, "open() called with: key = [" + key + "], size = [" + size + "], args = [" + args + "]");
        MemoryHolder memoryHolder = null;
        synchronized (fileMap) {
            memoryHolder = fileMap.get(key);
            if (memoryHolder == null || memoryHolder.isClose()) {
                memoryHolder = new MemoryHolder(key, size);
                fileMap.put(key, memoryHolder);
            }
        }

        MemoryFileHolder fileHolder = memoryHolder.getFileHolder();
        if (size != fileHolder.getSize()) {
            return -1;
        }
        // get client
        args.setClassLoader(IMemoryClient.Stub.class.getClassLoader());
        IBinder binder = args.getBinder(C.KEY_CLIENT);

        if (binder == null) {
            return -1;
        }
        memoryHolder.linkTo(binder);
        args.setClassLoader(MemoryFileHolder.class.getClassLoader());
        args.putParcelable(KEY_HOLDER, fileHolder);
        return 0;
    }

    /**
     * 通知接收端 发送数据大小
     *
     * @param key
     * @param offset
     * @param length
     * @param ts
     * @param args
     * @return
     * @throws RemoteException
     */
    @Override
    public int call(String key, int offset, int length, long ts, Bundle args) throws RemoteException {
//        Log.d(TAG, "call() called with: key = [" + key + "], offset = [" + offset + "], length = [" + length + "], ts = [" + ts + "], args = [" + args + "]");
        RemoteCallbackList<IMemoryCallback> callbackList = callbackMap.get(key);
        if (callbackList == null) {
            Log.w(TAG, "call: callback is null");
            return -1;
        }
        // 加锁 保证原子性
        synchronized (callbackMap) {
            int count = callbackList.beginBroadcast();
            for (int i = 0; i < count; i++) {
                IMemoryCallback callback = callbackList.getBroadcastItem(i);
                try {
                    callback.onCall(key, offset, length, ts, args);
                } catch (Exception e) {
                    Log.w(TAG, "call() called with: key = [" + key + "], offset = [" + offset + "], length = [" + length + "], ts = [" + ts + "], args = [" + args + "] exception!", e);
                }
            }
            callbackList.finishBroadcast();
        }
        return 0;
    }

    /**
     * 注册共享文件的监听
     *
     * @param key  共享文件名
     * @param args 用来存储监听
     * @return
     * @throws RemoteException
     */
    @Override
    public int listen(String key, Bundle args) throws RemoteException {
        Log.d(TAG, "listen() called with: key = [" + key + "], args = [" + args + "]");
        args.setClassLoader(IMemoryCallback.Stub.class.getClassLoader());
        IBinder binder = args.getBinder(KEY_CALLBACK);
        IMemoryCallback callback = IMemoryCallback.Stub.asInterface(binder);
        if (callback == null) {
            Log.w(TAG, "listen() called with: callback == null,key = [" + key + "], args = [" + args + "]");
            return -1;
        }
        synchronized (callbackMap) {
            RemoteCallbackList<IMemoryCallback> callbackList = callbackMap.get(key);
            if (callbackList == null) {
                callbackList = new RemoteCallbackList<>();
                callbackMap.put(key, callbackList);
            }
            callbackList.register(callback);
        }
        return 0;
    }

    @Override
    public int close(String key, Bundle args) throws RemoteException {
        Log.d(TAG, "close() called with: key = [" + key + "], args = [" + args + "]");
        return 0;
    }
}
