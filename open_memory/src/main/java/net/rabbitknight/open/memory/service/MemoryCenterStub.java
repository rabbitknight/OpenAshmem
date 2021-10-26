package net.rabbitknight.open.memory.service;

import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;

import net.rabbitknight.open.memory.IMemoryCallback;
import net.rabbitknight.open.memory.IMemoryCenter;
import net.rabbitknight.open.memory.core.MemoryFileHolder;

import java.util.HashMap;
import java.util.Map;

import static net.rabbitknight.open.memory.C.KEY_CALLBACK;
import static net.rabbitknight.open.memory.C.KEY_HOLDER;

public class MemoryCenterStub extends IMemoryCenter.Stub {
    private static final String TAG = "MemoryCenterStub";

    private final Map<String, MemoryFileHolder> fileMap = new HashMap<>();
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
    public int open(String key, int size, Bundle args) throws RemoteException {
        Log.d(TAG, "open() called with: key = [" + key + "], size = [" + size + "], args = [" + args + "]");
        MemoryFileHolder holder = fileMap.get(key);
        if (holder == null) {
            holder = new MemoryFileHolder(key, size);
            fileMap.put(key, holder);
        }
        if (size != holder.getSize()) {
            return -1;
        }
        args.setClassLoader(MemoryFileHolder.class.getClassLoader());
        args.putParcelable(KEY_HOLDER, holder);
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
        synchronized (this) {
            int count = callbackList.beginBroadcast();
            for (int i = 0; i < count; i++) {
                IMemoryCallback callback = callbackList.getBroadcastItem(i);
                callback.onCall(key, offset, length, ts, args);
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
        if (callbackMap.containsKey(key)) {
            return -1;
        }
        args.setClassLoader(IMemoryCallback.Stub.class.getClassLoader());
        IBinder binder = args.getBinder(KEY_CALLBACK);
        IMemoryCallback callback = IMemoryCallback.Stub.asInterface(binder);
        if (callback == null) {
            Log.w(TAG, "listen() called with: callback == null,key = [" + key + "], args = [" + args + "]");
            return -1;
        }
        RemoteCallbackList<IMemoryCallback> callbackList = callbackMap.get(key);
        if (callbackList == null) {
            callbackList = new RemoteCallbackList<>();
            callbackMap.put(key, callbackList);
        }
        callbackList.register(callback);
        return 0;
    }

    @Override
    public int close(String key) throws RemoteException {
        MemoryFileHolder holder = fileMap.remove(key);
        if (holder == null) {
            return -1;
        }
        holder.close();
        return 0;
    }
}
