package net.rabbitknight.open.ashmem;

import android.content.ComponentName;
import android.content.Context;

import net.rabbitknight.open.ashmem.core.OpenAshmemImpl;
import net.rabbitknight.open.ashmem.core.Receiver;
import net.rabbitknight.open.ashmem.core.Sender;
import net.rabbitknight.open.ashmem.service.MemoryCenterService;

/**
 * 外观类
 */
public class OpenAshmem {
    private final OpenAshmemImpl innerOpenAshmem;

    /**
     * 创建共享内存
     *
     * @param context 上下文
     */
    public OpenAshmem(Context context) {
        this(context, new ComponentName(context, MemoryCenterService.class));
    }

    /**
     * 创建共享内存
     *
     * @param context       上下文
     * @param componentName 访问包名
     */
    public OpenAshmem(Context context, ComponentName componentName) {
        innerOpenAshmem = new OpenAshmemImpl(context, componentName);
    }

    /**
     * 绑定服务
     */
    public void bind() {
        innerOpenAshmem.bind();
    }

    /**
     * 解除绑定
     */
    public void unbind() {
        innerOpenAshmem.unbind();
    }

    /**
     * 创建发送器
     *
     * @param key  key
     * @param size 内存大小
     * @return {@link Sender}
     */
    public Sender createSender(String key, int size) {
        return innerOpenAshmem.createSender(key, size);
    }

    /**
     * 创建接收器
     *
     * @param key  key
     * @param size 内存大小
     * @return {@link Receiver}
     */
    public Receiver createReceiver(String key, int size) {
        return innerOpenAshmem.createReceiver(key, size);
    }
}
