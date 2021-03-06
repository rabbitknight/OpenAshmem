package net.rabbitknight.open.ashmem.service;

import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;

import net.rabbitknight.open.ashmem.C;
import net.rabbitknight.open.ashmem.ErrorCode;
import net.rabbitknight.open.ashmem.IMemoryCallback;
import net.rabbitknight.open.ashmem.IMemoryCenter;
import net.rabbitknight.open.ashmem.IMemoryClient;
import net.rabbitknight.open.ashmem.core.MemoryFileHolder;

import java.util.HashMap;
import java.util.Map;

import static net.rabbitknight.open.ashmem.C.KEY_CALLBACK;
import static net.rabbitknight.open.ashmem.C.KEY_HOLDER;
import static net.rabbitknight.open.ashmem.ErrorCode.ERROR_CLIENT_CALLBACK_LOSS;
import static net.rabbitknight.open.ashmem.ErrorCode.ERROR_SERVER_CALLBACK_LOSS;
import static net.rabbitknight.open.ashmem.ErrorCode.SUCCESS;

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
        // get client
        args.setClassLoader(IMemoryClient.Stub.class.getClassLoader());
        IBinder binder = args.getBinder(C.KEY_CLIENT);
        if (binder == null) {
            return ErrorCode.ERROR_CLIENT_CLIENT_LOSS;
        }
        MemoryHolder memoryHolder = null;
        synchronized (fileMap) {
            memoryHolder = fileMap.get(key);
            if (memoryHolder == null || memoryHolder.isClose()) {
                memoryHolder = new MemoryHolder(key, size);
                fileMap.put(key, memoryHolder);
            }
            memoryHolder.linkTo(binder);
        }

        MemoryFileHolder fileHolder = memoryHolder.getFileHolder();
        if (size != fileHolder.getSize()) {
            return ErrorCode.ERROR_CLIENT_SIZE_NOT_MATCH;
        }

        args.setClassLoader(MemoryFileHolder.class.getClassLoader());
        args.putParcelable(KEY_HOLDER, fileHolder);

        return SUCCESS;
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
            return ERROR_SERVER_CALLBACK_LOSS;
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
        return SUCCESS;
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
            return ERROR_CLIENT_CALLBACK_LOSS;
        }
        synchronized (callbackMap) {
            RemoteCallbackList<IMemoryCallback> callbackList = callbackMap.get(key);
            if (callbackList == null) {
                callbackList = new RemoteCallbackList<>();
                callbackMap.put(key, callbackList);
            }
            callbackList.register(callback);
        }
        return SUCCESS;
    }

    @Override
    public int close(String key, Bundle args) throws RemoteException {
        Log.d(TAG, "close() called with: key = [" + key + "], args = [" + args + "]");
        return SUCCESS;
    }
}
