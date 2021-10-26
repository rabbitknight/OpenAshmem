package net.rabbitknight.open.memory.demo;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import net.rabbitknight.open.memory.OpenMemory;
import net.rabbitknight.open.memory.core.Receiver;
import net.rabbitknight.open.memory.core.Sender;
import net.rabbitknight.open.memory.service.MemoryCenterService;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private Button sendBtn;
    private OpenMemory openMemory = new OpenMemory(this, MemoryCenterService.class);
    private Sender sender = null;
    private Receiver receiver = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        this.sender = openMemory.createSender("test1", 1024);

        this.receiver = openMemory.createReceiver("test2", 1024);
        this.receiver.listen(new Receiver.Callback() {
            @Override
            public void onReceive(byte[] payload, int offset, int length, long timestamp) {
                String msg = new String(payload, offset, length);
                Log.d(TAG, "onReceive() called with: payload = [" + msg + "], offset = [" + offset + "], length = [" + length + "], timestamp = [" + timestamp + "]");
            }
        });
        openMemory.bind();
        Intent intent = new Intent(this, RemoteService.class);
        startService(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        openMemory.unbind();
        Intent intent = new Intent(this, RemoteService.class);
        stopService(intent);
    }

    private void initView() {
        sendBtn = findViewById(R.id.btn_send);
        sendBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                new Thread(new Runnable() {
                    int count = 0;

                    @Override
                    public void run() {
                        for (int i = 0; i < 10000; i++) {
                            long time = System.currentTimeMillis();
                            String msg = "time:" + time + ",count:" + count++;
                            sender.send(msg.getBytes(), 0, msg.getBytes().length, time);
                        }
                    }
                }).start();
                new Thread(new Runnable() {
                    int aa = 0;

                    @Override
                    public void run() {
                        for (int i = 0; i < 10000; i++) {
                            long time = System.currentTimeMillis();
                            String msg = "time:" + time + ",count:" + aa++;
                            sender.send(msg.getBytes(), 0, msg.getBytes().length, time);
                        }
                    }
                }).start();

            }
        });
    }


}