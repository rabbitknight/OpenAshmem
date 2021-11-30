package net.rabbitknight.open.memory.core;

import android.os.Build;
import android.os.MemoryFile;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;
import android.os.SharedMemory;
import android.system.ErrnoException;
import android.util.Log;

import androidx.annotation.RequiresApi;

import net.rabbitknight.open.memory.utils.MemoryFileUtils;

import java.io.IOException;
import java.nio.ByteBuffer;

import static net.rabbitknight.open.memory.utils.MemoryFileUtils.OPEN_READWRITE;

/**
 * 共享内存抽象类
 */
public class MemoryFileHolder implements Parcelable {
    private static final String TAG = MemoryFileHolder.class.getSimpleName();
    /**
     * 文件名
     */
    private String fileName;
    /**
     * 共享内存大小
     */
    private int size;

    /**
     * 共享内存
     */
    private MemoryFile memoryFile;
    /**
     * 文件描述符
     */
    private ParcelFileDescriptor parcelFileDescriptor;

    /**
     * 如果是AndroidP 直接使用SharedMemory
     */
    @RequiresApi(api = Build.VERSION_CODES.O_MR1)
    private SharedMemory sharedMemory;
    /**
     * 缓存数据
     */
    private ByteBuffer sharedBuffer;

    public byte[] cache;

    /**
     * 主动创建，为写入端
     *
     * @param fileName 文件名
     * @param size     大小
     */
    public MemoryFileHolder(String fileName, int size) {
        this.fileName = fileName;
        this.size = size;
        cache = new byte[size];

        // 如果是Android P及以上 使用SharedMemory
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            try {
                sharedMemory = SharedMemory.create(fileName, size);
                sharedBuffer = sharedMemory.mapReadWrite();
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "MemoryFileHolder: mapReadWrite", e);
            }
        } else {
            memoryFile = MemoryFileUtils.createMemoryFile(fileName, size);
            parcelFileDescriptor = MemoryFileUtils.getParcelFileDescriptor(memoryFile);
        }
    }

    /**
     * 序列化还原，为读取端
     *
     * @param in 序列化
     */
    protected MemoryFileHolder(Parcel in) {
        fileName = in.readString();
        size = in.readInt();
        cache = new byte[size];
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            sharedMemory = in.readParcelable(SharedMemory.class.getClassLoader());
            if (sharedMemory == null) {
                return;
            }
            try {
                sharedBuffer = sharedMemory.mapReadWrite();
            } catch (ErrnoException e) {
                e.printStackTrace();
            }
        } else {
            parcelFileDescriptor = in.readParcelable(ParcelFileDescriptor.class.getClassLoader());
            if (parcelFileDescriptor == null) {
                return;
            }
            memoryFile = MemoryFileUtils.openMemoryFile(parcelFileDescriptor, size, OPEN_READWRITE);
        }
    }

    /**
     * 写入共享内存
     *
     * @param data   数据
     * @param offset 偏移量
     * @param length 长度
     * @return
     */
    public boolean writeBytes(byte[] data, int offset, int length) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            if (sharedBuffer == null) {
                return false;
            }
            sharedBuffer.position(0);
            sharedBuffer.put(data, offset, length);
            return true;
        } else {
            if (memoryFile == null) {
                Log.w(TAG, "writeBytes: cannot get memoryFile [" + memoryFile + "]");
                return false;
            }
            try {
                memoryFile.writeBytes(data, offset, 0, length);
            } catch (Exception e) {
                Log.e(TAG, "writeBytes: Exception", e);
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    /**
     * 从共享内存中读取
     *
     * @param data   数据
     * @param offset 偏移量
     * @param length 长度
     * @return
     */
    public boolean readBytes(byte[] data, int offset, int length) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            if (sharedBuffer == null) {
                return false;
            }
            if (data.length < length) {
            }
            sharedBuffer.position(0);
            sharedBuffer.get(data, offset, length);
        } else {
            if (memoryFile == null) {
                Log.w(TAG, "readBytes: cannot get memoryFile [" + memoryFile + "]");
                return false;
            }
            try {
                memoryFile.readBytes(data, 0, offset, length);
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "readBytes: Exception", e);
                return false;
            }
        }
        return true;
    }

    public int getSize() {
        return size;
    }

    /**
     * 销毁方法
     * 释放资源占用
     */
    public void close() {
        if (memoryFile != null)
            memoryFile.close();
        if (parcelFileDescriptor != null) {
            try {
                parcelFileDescriptor.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            if (sharedMemory != null) {
                sharedMemory.close();
            }
        }
    }

    public static final Creator<MemoryFileHolder> CREATOR = new Creator<MemoryFileHolder>() {
        @Override
        public MemoryFileHolder createFromParcel(Parcel in) {
            return new MemoryFileHolder(in);
        }

        @Override
        public MemoryFileHolder[] newArray(int size) {
            return new MemoryFileHolder[size];
        }
    };


    /**
     * Describe the kinds of special objects contained in this Parcelable
     * instance's marshaled representation. For example, if the object will
     * include a file descriptor in the output of {@link #writeToParcel(Parcel, int)},
     * the return value of this method must include the
     * {@link #CONTENTS_FILE_DESCRIPTOR} bit.
     *
     * @return a bitmask indicating the set of special object types marshaled
     * by this Parcelable object instance.
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Flatten this object in to a Parcel.
     *
     * @param dest  The Parcel in which the object should be written.
     * @param flags Additional flags about how the object should be written.
     *              May be 0 or {@link #PARCELABLE_WRITE_RETURN_VALUE}.
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(fileName);
        dest.writeInt(size);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            dest.writeParcelable(sharedMemory, flags);
        } else {
            dest.writeParcelable(parcelFileDescriptor, flags);
        }

    }

    @Override
    public String toString() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            return "MemoryFileHolder{" +
                    "fileName='" + fileName + '\'' +
                    ", size=" + size +
                    ", sharedMemory=" + sharedMemory +
                    ", sharedBuffer=" + sharedBuffer +
                    ", cache=" + cache.length +
                    '}';
        } else {
            return "MemoryFileHolder{" +
                    "fileName='" + fileName + '\'' +
                    ", size=" + size +
                    ", memoryFile=" + memoryFile +
                    ", parcelFileDescriptor=" + parcelFileDescriptor +
                    ", cache=" + cache.length +
                    '}';
        }
    }
}
