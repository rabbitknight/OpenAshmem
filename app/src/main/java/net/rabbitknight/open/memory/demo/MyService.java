package net.rabbitknight.open.memory.demo;

import android.app.IntentService;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.widget.Toast;

import androidx.annotation.Nullable;

import net.rabbitknight.open.memory.core.MemoryFileHolder;

public class MyService extends IntentService {
    private MemoryFileHolder memoryFileHolder = null;

    public MyService(String name) {
        super(name);
    }


    @Override
    public IBinder onBind(Intent intent) {
        return new Binder();
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (intent != null && intent.hasExtra("mem-file")) {
            memoryFileHolder = intent.getParcelableExtra("mem-file");
        } else {
            byte[] msg = new byte[1024];
            memoryFileHolder.readBytes(msg, 0, 5);
            Toast.makeText(this, new String(msg), Toast.LENGTH_SHORT).show();
        }
    }


}