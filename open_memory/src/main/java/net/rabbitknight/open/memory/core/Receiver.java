package net.rabbitknight.open.memory.core;

import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;

import net.rabbitknight.open.memory.C;
import net.rabbitknight.open.memory.ErrorCode;
import net.rabbitknight.open.memory.IMemoryCallback;
import net.rabbitknight.open.memory.IMemoryCenter;
import net.rabbitknight.open.memory.IMemoryClient;

import java.lang.ref.WeakReference;

import static net.rabbitknight.open.memory.C.KEY_HOLDER;

public class Receiver extends IMemoryClient.Stub {
    private static final String TAG = "Receiver";
    private WeakReference<OpenMemoryImpl> openMemoryWeakReference = null;
    private final String key;
    private final int size;

    private Callback callback = null;
    private IMemoryCenter memoryCenter;
    private MemoryFileHolder fileHolder;
    private byte[] cache = null;

    private volatile boolean connected = false;

    /**
     * 创建发送器
     *
     * @param memory 上下文环境
     * @param key    共享内存
     * @param size   大小
     */
    Receiver(OpenMemoryImpl memory, String key, int size) {
        this.openMemoryWeakReference = new WeakReference<>(memory);
        this.key = key;
        this.size = size;
        this.cache = new byte[size];
    }

    /**
     * 设置监听器
     *
     * @param callback
     */
    public void listen(Callback callback) {
        this.callback = callback;
    }

    public IConnectListener getConnection() {
        return connection;
    }

    private final IConnectListener connection = new IConnectListener() {

        @Override
        public void onServiceConnected() {
            connected = true;
            OpenMemoryImpl memory = openMemoryWeakReference.get();
            if (memory == null) {
                Log.w(TAG, "onServiceConnected: memory is null!!");
                return;
            }
            memoryCenter = memory.getRemoteBinder();
            // 获取MemoryFile
            Bundle args = new Bundle();
            try {
                args.putBinder(C.KEY_CLIENT, Receiver.this.asBinder());
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
            // 注册监听
            boolean listen = false;
            try {
                args.putBinder(C.KEY_CALLBACK, remoteCallback.asBinder());
                int rst = memoryCenter.listen(key, args);
                if (rst == ErrorCode.SUCCESS) {
                    listen = true;
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected() {
            connected = false;
            fileHolder = null;
            memoryCenter = null;
        }
    };

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
        OpenMemoryImpl openMemory = openMemoryWeakReference.get();
        if (openMemory != null) {
            openMemory.close(this);
        }
    }

    /**
     * 回调监听
     */
    public interface Callback {
        /**
         * 回调数据
         *
         * @param payload   接收到的数据
         * @param offset    偏移量
         * @param length    长度
         * @param timestamp 时间戳
         */
        void onReceive(byte[] payload, int offset, int length, long timestamp);
    }

    private final IMemoryCallback.Stub remoteCallback = new IMemoryCallback.Stub() {
        @Override
        public int onCall(String key, int offset, int length, long ts, Bundle args) throws RemoteException {
            if (!connected) {
                return ErrorCode.ERROR;
            }
            MemoryFileHolder fileHolder = Receiver.this.fileHolder;
            if (fileHolder == null) {
                return ErrorCode.ERROR;
            }
            boolean read = fileHolder.readBytes(cache, 0, length);
            if (!read) {
                return ErrorCode.ERROR;
            }
            Callback callback = Receiver.this.callback;
            if (null != callback) {
                callback.onReceive(cache, 0, length, ts);
            }
            return 0;
        }
    };
}
