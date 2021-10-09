package net.rabbitknight.open.memory.demo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import net.rabbitknight.open.memory.core.MemoryFileHolder;

public class MainActivity extends AppCompatActivity {
    private Button sendBtn;
    private MemoryFileHolder memoryFileHolder;
    private int count = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initMemoryFile();
        initService();
        initView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Intent intent = new Intent(this, MyService.class);
        this.stopService(intent);
    }

    private void initView() {
        sendBtn = findViewById(R.id.btn_send);
        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String msg = "hello";
                memoryFileHolder.writeBytes(msg.getBytes(), 0, msg.getBytes().length);
                Intent intent = new Intent(MainActivity.this, MyService.class);
                startService(intent);
            }
        });
    }

    private void initService() {
        Intent intent = new Intent(this, MyService.class);
        intent.putExtra("mem-file", memoryFileHolder);
        startService(intent);
    }

    private void initMemoryFile() {
        memoryFileHolder = new MemoryFileHolder("test", 1024);
    }
}