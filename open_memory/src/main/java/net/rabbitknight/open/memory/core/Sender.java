package net.rabbitknight.open.memory.core;

import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;

import net.rabbitknight.open.memory.ErrorCode;
import net.rabbitknight.open.memory.IMemoryCenter;
import net.rabbitknight.open.memory.OpenMemory;

import java.lang.ref.WeakReference;

import static net.rabbitknight.open.memory.C.KEY_HOLDER;

public class Sender implements IConnectListener {
    private static final String TAG = "Sender";
    private WeakReference<OpenMemory> openMemoryWeakReference = null;
    private String key;
    private int size;

    private IMemoryCenter memoryCenter;
    private MemoryFileHolder fileHolder;

    private volatile boolean connected = false;

    /**
     * 创建发送器
     *
     * @param memory 上下文环境
     * @param key    共享内存
     * @param size   大小
     */
    public Sender(OpenMemory memory, String key, int size) {
        this.openMemoryWeakReference = new WeakReference<>(memory);
        this.key = key;
        this.size = size;
    }

    /**
     * 发送数据
     *
     * @return {@link ErrorCode}
     */
    public int send(byte[] payload, int offset, int length, long timestamp) {
        if (!connected) {
            return ErrorCode.ERROR;
        }
        MemoryFileHolder fileHolder = this.fileHolder;
        IMemoryCenter remoteBinder = memoryCenter;
        if (remoteBinder == null) {
            return ErrorCode.ERROR;
        }
        if (fileHolder == null) {
            return ErrorCode.ERROR;
        }
        // 写文件
        boolean write = fileHolder.writeBytes(payload, offset, length);
        if (!write) {
            return ErrorCode.ERROR;
        }
        Bundle args = new Bundle();
        // 通知
        boolean call = false;
        try {
            int rst = remoteBinder.call(key, offset, length, timestamp, args);
            if (rst == ErrorCode.SUCCESS) {
                call = true;
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        if (!call) {
            return ErrorCode.ERROR;
        }
        return ErrorCode.SUCCESS;
    }

    @Override
    public void onServiceConnected() {
        connected = true;
        OpenMemory memory = openMemoryWeakReference.get();
        if (memory == null) {
            Log.w(TAG, "onServiceConnected: memory is null!!");
            return;
        }
        memoryCenter = memory.getRemoteBinder();
        // 链接
        Bundle args = new Bundle();
        try {
            memoryCenter.open(key, size, args);
        } catch (RemoteException e) {
            e.printStackTrace();
            Log.w(TAG, "onServiceConnected: ", e);
        }
        args.setClassLoader(MemoryFileHolder.class.getClassLoader());
        MemoryFileHolder holder = args.getParcelable(KEY_HOLDER);
        if (holder != null) {
            this.fileHolder = holder;
        }
    }

    @Override
    public void onServiceDisconnected() {
        connected = false;
        memoryCenter = null;
        fileHolder = null;
    }
}
