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

    public MemoryHolder(String key, int size) {
        this.holder = new MemoryFileHolder(key, size);
    }

    public void linkTo(final IBinder binder) {
        synchronized (count) {
            DeathRecipient deathRecipient = new DeathRecipient(binder);
            try {
                binder.linkToDeath(deathRecipient, 0);
                count.incrementAndGet();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            Log.d(TAG, "linkTo() called with: fileHolder = [" + holder + "], count = " + count.get());
        }
    }

    public MemoryFileHolder getFileHolder() {
        return holder;
    }

    @Override
    public void binderDied() {
        synchronized (count) {
            if (count.decrementAndGet() != 0) {
                return;
            }
            Log.i(TAG, "binderDied: count = " + count.get());
            // 关闭
            MemoryFileHolder holder = MemoryHolder.this.holder;
            holder.close();
        }
    }

    public boolean isClose() {
        synchronized (count) {
            return count.get() == 0;
        }
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
