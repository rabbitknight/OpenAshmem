package net.rabbitknight.open.memory;

import android.content.Context;

import net.rabbitknight.open.memory.core.OpenMemoryImpl;
import net.rabbitknight.open.memory.core.Receiver;
import net.rabbitknight.open.memory.core.Sender;

/**
 * 外观类
 */
public class OpenMemory {
    private OpenMemoryImpl innerOpenMemory;

    /**
     * 创建共享内存
     *
     * @param context 上下文
     * @param service 中心服务
     */
    public OpenMemory(Context context, Class<?> service) {
        innerOpenMemory = new OpenMemoryImpl(context, service);
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
     * @return
     */
    public Sender createSender(String key, int size) {
        return innerOpenMemory.createSender(key, size);
    }

    /**
     * 创建接收器
     *
     * @param key  key
     * @param size 内存大小
     * @return
     */
    public Receiver createReceiver(String key, int size) {
        return innerOpenMemory.createReceiver(key, size);
    }
}
