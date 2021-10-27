package net.rabbitknight.open.memory.service;

import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import net.rabbitknight.open.memory.core.MemoryFileHolder;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 用来控制memfile的自动关闭
 */
public class MemoryHolder implements IBinder.DeathRecipient {
    private static final String TAG = "MemoryHolder";
    private final AtomicInteger count = new AtomicInteger(0);
    private final MemoryFileHolder holder;
    private final OnClearListener onClearListener;

    public MemoryHolder(MemoryFileHolder fileHolder, OnClearListener clearListener) {
        this.holder = fileHolder;
        this.onClearListener = clearListener;
    }

    public void linkTo(final IBinder binder) {
        DeathRecipient deathRecipient = new DeathRecipient(binder);
        try {
            binder.linkToDeath(deathRecipient, 0);
            count.incrementAndGet();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public MemoryFileHolder getFileHolder() {
        return holder;
    }

    @Override
    public void binderDied() {
        if (count.decrementAndGet() != 0) {
            Log.i(TAG, "binderDied: count = " + count.get());
            return;
        }
        // 通知
        OnClearListener clearListener = MemoryHolder.this.onClearListener;
        if (clearListener != null) {
            clearListener.onClear();
        }
        // 关闭
        MemoryFileHolder holder = MemoryHolder.this.holder;
        if (holder != null) {
            holder.close();
        }
    }


    public interface OnClearListener {
        void onClear();
    }

    class DeathRecipient implements IBinder.DeathRecipient {
        private final IBinder binder;

        public DeathRecipient(IBinder binder) {
            this.binder = binder;
        }

        @Override
        public void binderDied() {
            Log.d(TAG, "binderDied() called");
            binder.unlinkToDeath(this, 0);

            MemoryHolder.this.binderDied();
        }
    }
}
