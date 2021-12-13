package net.rabbitknight.open.ashmem.core;

import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;

import net.rabbitknight.open.ashmem.C;
import net.rabbitknight.open.ashmem.ErrorCode;
import net.rabbitknight.open.ashmem.IMemoryCenter;
import net.rabbitknight.open.ashmem.IMemoryClient;

import java.lang.ref.WeakReference;

import static net.rabbitknight.open.ashmem.C.KEY_HOLDER;
import static net.rabbitknight.open.ashmem.ErrorCode.ERROR_SENDER_FILE_CALL;
import static net.rabbitknight.open.ashmem.ErrorCode.ERROR_SENDER_FILE_LOSS;
import static net.rabbitknight.open.ashmem.ErrorCode.ERROR_SENDER_FILE_WRITE;
import static net.rabbitknight.open.ashmem.ErrorCode.ERROR_SENDER_SERVICE_API_LOSS;
import static net.rabbitknight.open.ashmem.ErrorCode.ERROR_SENDER_SERVICE_NOT_CONNECT;

public class Sender extends IMemoryClient.Stub {
    private static final String TAG = "Sender";
    private WeakReference<OpenAshmemImpl> openAshmemWeakReference = null;
    private final String key;
    private final int size;

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
    Sender(OpenAshmemImpl memory, String key, int size) {
        this.openAshmemWeakReference = new WeakReference<>(memory);
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
            return ERROR_SENDER_SERVICE_NOT_CONNECT;
        }
        MemoryFileHolder fileHolder = this.fileHolder;
        IMemoryCenter remoteBinder = memoryCenter;
        if (remoteBinder == null) {
            return ERROR_SENDER_SERVICE_API_LOSS;
        }
        if (fileHolder == null) {
            return ERROR_SENDER_FILE_LOSS;
        }
        Bundle args = new Bundle();
        boolean write = false;
        synchronized (this) {
            // 写文件
            write = fileHolder.writeBytes(payload, offset, length);
            if (!write) {
                return ERROR_SENDER_FILE_WRITE;
            }
            // 通知
            try {
                return remoteBinder.call(key, offset, length, timestamp, args);
            } catch (Exception e) {
                e.printStackTrace();
                Log.w(TAG, "send: remoteBinder.call", e);
            }
        }

        return ERROR_SENDER_FILE_CALL;
    }

    /**
     * 关闭链接
     */
    public void close() {
        if (connected) {
            IMemoryCenter memoryCenter = this.memoryCenter;
            if (memoryCenter != null) {
                Bundle args = new Bundle();
                args.putBinder(C.KEY_CLIENT, this.asBinder());
                try {
                    memoryCenter.close(key, args);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
        // 关闭
        OpenAshmemImpl openAshmem = openAshmemWeakReference.get();
        if (openAshmem != null) {
            openAshmem.close(this);
        }
    }

    public IConnectListener getConnection() {
        return connection;
    }

    private final IConnectListener connection = new IConnectListener() {

        @Override
        public void onServiceConnected() {
            connected = true;
            OpenAshmemImpl memory = openAshmemWeakReference.get();
            if (memory == null) {
                Log.w(TAG, "onServiceConnected: memory is null!!");
                return;
            }
            memoryCenter = memory.getRemoteBinder();
            // 链接
            Bundle args = new Bundle();
            try {
                args.putBinder(C.KEY_CLIENT, Sender.this.asBinder());
                memoryCenter.open(key, size, args);
            } catch (RemoteException e) {
                e.printStackTrace();
                Log.w(TAG, "onServiceConnected: ", e);
            }
            args.setClassLoader(MemoryFileHolder.class.getClassLoader());
            MemoryFileHolder holder = args.getParcelable(KEY_HOLDER);
            if (holder != null) {
                fileHolder = holder;
            }
        }

        @Override
        public void onServiceDisconnected() {
            connected = false;
            memoryCenter = null;
            fileHolder = null;
        }
    };
}
