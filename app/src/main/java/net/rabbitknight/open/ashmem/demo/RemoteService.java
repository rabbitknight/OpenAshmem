package net.rabbitknight.open.ashmem.demo;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import net.rabbitknight.open.ashmem.OpenAshmem;
import net.rabbitknight.open.ashmem.core.Receiver;
import net.rabbitknight.open.ashmem.core.Sender;

public class RemoteService extends Service {
    private static final String TAG = "RemoteService";
    private OpenAshmem openAshmem = null;

    public RemoteService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        openAshmem = new OpenAshmem(this);
        openAshmem.bind();

        Sender sender = openAshmem.createSender("test2", 1024 * 1024);
        Receiver receiver = openAshmem.createReceiver("test1", 1024 * 1024);
        receiver.listen(new Receiver.Callback() {
            @Override
            public void onReceive(byte[] payload, int offset, int length, long timestamp) {
                String msg = new String(payload, offset, length);
                Log.d(TAG, "onReceive() called with: payload = [" + msg + "], offset = [" + offset + "], length = [" + length + "], timestamp = [" + timestamp + "]");
                sender.send(payload, offset, length, timestamp);
            }
        });

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        openAshmem.unbind();
    }
}