package net.rabbitknight.open.memory.demo;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import net.rabbitknight.open.memory.OpenMemory;
import net.rabbitknight.open.memory.core.Receiver;
import net.rabbitknight.open.memory.core.Sender;
import net.rabbitknight.open.memory.service.MemoryCenterService;

public class RemoteService extends Service {
    private static final String TAG = "RemoteService";
    private OpenMemory openMemory = new OpenMemory(this, MemoryCenterService.class);

    public RemoteService() {
        Sender sender = openMemory.createSender("test2", 1024);
        Receiver receiver = openMemory.createReceiver("test1", 1024);
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
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        openMemory.bind();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        openMemory.unbind();
    }
}