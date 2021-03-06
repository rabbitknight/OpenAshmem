package net.rabbitknight.open.ashmem.utils;

import android.os.Build;
import android.os.MemoryFile;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.FileDescriptor;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;

public class MemoryFileUtils {
    private static final String TAG = "MemoryFileUtils";

    /**
     * 创建共享内存对象
     *
     * @param name   描述共享内存文件名称
     * @param length 用于指定创建多大的共享内存对象
     * @return MemoryFile 描述共享内存对象
     */
    public static MemoryFile createMemoryFile(String name, int length) {
        try {
            return new MemoryFile(name, length);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static MemoryFile openAshmemFile(ParcelFileDescriptor pfd, int length, int mode) {
        if (pfd == null) {
            throw new IllegalArgumentException("ParcelFileDescriptor is null");
        }
        FileDescriptor fd = pfd.getFileDescriptor();
        return openAshmemFile(fd, length, mode);
    }

    private static final int PROT_READ = 0x1;
    private static final int PROT_WRITE = 0x2;
    public static final int OPEN_READONLY = PROT_READ;
    public static final int OPEN_READWRITE = PROT_READ | PROT_WRITE;

    /**
     * 打开共享内存，一般是一个地方创建了一块共享内存
     * 另一个地方持有描述这块共享内存的文件描述符，调用
     * 此方法即可获得一个描述那块共享内存的MemoryFile
     * 对象
     *
     * @param fd     文件描述
     * @param length 共享内存的大小
     * @param mode   PROT_READ = 0x1只读方式打开,
     *               PROT_WRITE = 0x2可写方式打开，
     *               PROT_WRITE|PROT_READ可读可写方式打开
     * @return MemoryFile
     */
    public static MemoryFile openAshmemFile(FileDescriptor fd, int length, int mode) {
        if (mode != OPEN_READONLY && mode != OPEN_READWRITE)
            throw new IllegalArgumentException("invalid mode, only support OPEN_READONLY and OPEN_READWRITE");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            return openAshmemFileV27(fd, mode);
        }
        return openAshmemFileV26(fd, length, mode);
    }

    private static MemoryFile openAshmemFileV27(FileDescriptor fd, int mode) {
        MemoryFile memoryFile = null;
        try {
            memoryFile = new MemoryFile("service.remote", 1);
            memoryFile.close();
            Class<?> c = Class.forName("android.os.SharedMemory");
            Object sharedMemory = InvokeUtil.newInstanceOrThrow(c, fd);
            ByteBuffer mapping = null;
            if (mode == OPEN_READONLY) {
                mapping = (ByteBuffer) InvokeUtil.invokeMethod(sharedMemory, "mapReadOnly");
            } else {
                mapping = (ByteBuffer) InvokeUtil.invokeMethod(sharedMemory, "mapReadWrite");
            }

            InvokeUtil.setValueOfField(memoryFile, "mSharedMemory", sharedMemory);
            InvokeUtil.setValueOfField(memoryFile, "mMapping", mapping);
            return memoryFile;
        } catch (Exception e) {
            throw new RuntimeException("openAshmemFile failed!", e);
        }

    }

    public static MemoryFile openAshmemFileV26(FileDescriptor fd, int length, int mode) {
        MemoryFile memoryFile = null;
        try {
            memoryFile = new MemoryFile("service.remote", 1);
            memoryFile.close();
            Class<?> c = MemoryFile.class;
            InvokeUtil.setValueOfField(memoryFile, "mFD", fd);
            InvokeUtil.setValueOfField(memoryFile, "mLength", length);
            long address = (long) InvokeUtil.invokeStaticMethod(c, "native_mmap", fd, length, mode);
            InvokeUtil.setValueOfField(memoryFile, "mAddress", address);

        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "openAshmemFileV26: error ", e);
        }

        return memoryFile;
    }

    /**
     * 获取memoryFile的ParcelFileDescriptor
     *
     * @param memoryFile 描述一块共享内存
     * @return ParcelFileDescriptor
     */
    public static ParcelFileDescriptor getParcelFileDescriptor(MemoryFile memoryFile) {
        if (memoryFile == null) {
            throw new IllegalArgumentException("memoryFile is null");
        }
        ParcelFileDescriptor pfd = null;

        try {
            FileDescriptor fd = getFileDescriptor(memoryFile);
            pfd = (ParcelFileDescriptor) InvokeUtil.newInstanceOrThrow(ParcelFileDescriptor.class, fd);
            return pfd;
        } catch (Exception e) {
            throw new RuntimeException("InvokeUtil.newInstanceOrThrow failed", e);
        }
    }

    /**
     * 获取memoryFile的FileDescriptor
     *
     * @param memoryFile 描述一块共享内存
     * @return 这块共享内存对应的文件描述符
     */
    public static FileDescriptor getFileDescriptor(MemoryFile memoryFile) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        if (memoryFile == null) {
            throw new IllegalArgumentException("memoryFile is null");
        }
        FileDescriptor fd;
        fd = (FileDescriptor) InvokeUtil.invokeMethod(memoryFile, "getFileDescriptor");
        return fd;
    }
}