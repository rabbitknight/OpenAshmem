package net.rabbitknight.open.memory;

import android.content.ComponentName;
import android.content.Context;

import net.rabbitknight.open.memory.core.OpenMemoryImpl;
import net.rabbitknight.open.memory.core.Receiver;
import net.rabbitknight.open.memory.core.Sender;
import net.rabbitknight.open.memory.service.MemoryCenterService;

/**
 * 外观类
 */
public class OpenMemory {
    private final OpenMemoryImpl innerOpenMemory;

    /**
     * 创建共享内存
     *
     * @param context 上下文
     */
    public OpenMemory(Context context) {
        this(context, new ComponentName(context, MemoryCenterService.class));
    }

    /**
     * 创建共享内存
     *
     * @param context       上下文
     * @param componentName 访问包名
     */
    public OpenMemory(Context context, ComponentName componentName) {
        innerOpenMemory = new OpenMemoryImpl(context, componentName);
    }

    /**
     * 绑定服务
     */
    public void bind() {
        innerOpenMemory.bind();
    }

    /**
     * 解除绑定
     */
    public void unbind() {
        innerOpenMemory.unbind();
    }

    /**
     * 创建发送器
     *
     * @param key  key
     * @param size 内存大小
     * @return {@link Sender}
     */
    public Sender createSender(String key, int size) {
        return innerOpenMemory.createSender(key, size);
    }

    /**
     * 创建接收器
     *
     * @param key  key
     * @param size 内存大小
     * @return {@link Receiver}
     */
    public Receiver createReceiver(String key, int size) {
        return innerOpenMemory.createReceiver(key, size);
    }
}
