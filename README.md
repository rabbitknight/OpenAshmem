# OpenMemory
跨进程 低延迟 低占用，数据传递，基于 Android 的 共享内存。

## TL;DR
使用Java层的MemoryFile作为共享内存实现。使用Binder作为同步的信号量。

## 使用方式
### 创建OpenMemory
1. 构造OpenMemory实例
```
OpenMemory openMemory = new OpenMemory(context);
```
2. 绑定服务与解绑
```
@Override
protected void onCreate(Bundle savedInstanceState) {
    // 通过bindService 绑定服务
    openMemory.bind();
}

@Override
protected void onDestroy() {
    // 通过unbindService 解除服务绑定
    openMemory.unbind();
}

```

### 创建发送器
1. 通过openMemory创建发送器
```
String key = "memory1"; // 指定共享内存的key
int size = 1024;        // 指定共享内存的尺寸
// 创建sender
Sender sender = openMemory.createSender(key,size);
```
2. 发送消息

```
// 消息
String msg = "Hello,World!";
// 时间戳
long timestamp = System.currentTimeMillis();
// 调用发送
int error = sender.send(msg.getBytes(), 0, msg.getBytes().length, timestamp);
```

### 创建接收器
1. 通过openMemory创建接收器
```
String key = "memory1"; // 指定共享内存的key
int size = 1024;        // 指定共享内存的尺寸
// 创建Receiver
Receiver receiver = openMemory.createReceiver(key,size);
```

2. 接收消息
```
receiver.listen(new Receiver.Callback() {
    @Override
    public void onReceive(byte[] payload, int offset, int length, long timestamp) {
        String msg = new String(payload, offset, length);
        Log.d(TAG, "onReceive() called with: payload = [" + msg + "], offset = [" + offset + "], length = [" + length + "], timestamp = [" + timestamp + "]");
    }
});
```

### 注意
1. 内部使用远程的Service进行链接，希望你明白这意味什么。
2. receiver/sender匹配关系只收key和size限制，因此sender和receiver是多对多的关系。