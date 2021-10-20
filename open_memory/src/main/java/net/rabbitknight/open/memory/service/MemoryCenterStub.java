package net.rabbitknight.open.memory.service;

import android.os.Bundle;
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

    private Map<String, MemoryFileHolder> fileMap = new HashMap<>();
    private Map<String, IMemoryCallback> callbackMap = new HashMap<>();

    @Override
    public int open(String key, int size, Bundle args) throws RemoteException {
        Log.d(TAG, "open() called with: key = [" + key + "], size = [" + size + "], args = [" + args + "]");
        MemoryFileHolder holder = fileMap.get(key);
        if (holder == null) {
            holder = new MemoryFileHolder(key, size);
        }
        if (size != holder.getSize()) {
            return -1;
        }
        args.setClassLoader(MemoryFileHolder.class.getClassLoader());
        args.putParcelable(KEY_HOLDER, holder);
        return 0;
    }

    @Override
    public int call(String key, int offset, int length, long ts, Bundle args) throws RemoteException {
        Log.d(TAG, "call() called with: key = [" + key + "], offset = [" + offset + "], length = [" + length + "], ts = [" + ts + "], args = [" + args + "]");
        IMemoryCallback callback = callbackMap.get(key);
        if (callback == null) {
            Log.w(TAG, "call: callback is null");
            return -1;
        }
        boolean alive = callback.asBinder().isBinderAlive();
        if (!alive) {
            Log.w(TAG, "call: callback not alive!");
            return -1;
        }
        boolean success = false;
        try {
            callback.onCall(key, offset, length, ts, args);
            success = true;
        } catch (Exception e) {
            Log.w(TAG, "call: Exception", e);
        }
        if (!success) {
            return -1;
        }
        return 0;
    }

    @Override
    public int listen(String key, Bundle args) throws RemoteException {
        Log.d(TAG, "listen() called with: key = [" + key + "], args = [" + args + "]");
        if (callbackMap.containsKey(key)) {
            return -1;
        }
        args.setClassLoader(IMemoryCallback.class.getClassLoader());
        IMemoryCallback.Stub callback = (IMemoryCallback.Stub) args.getBinder(KEY_CALLBACK);
        if (callback == null) {
            Log.w(TAG, "listen() called with: callback == null,key = [" + key + "], args = [" + args + "]");
            return -1;
        }
        callbackMap.put(key, callback);
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
