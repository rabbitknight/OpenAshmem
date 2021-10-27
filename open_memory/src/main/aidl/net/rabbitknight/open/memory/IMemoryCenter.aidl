// IMemoryCenter.aidl
package net.rabbitknight.open.memory;

// Declare any non-default types here with import statements

interface IMemoryCenter {

    /**
    * 打开连接端口
    * @param args 存储返回的序列化文件描述符
    **/
    int open(String key,int size, inout Bundle args);

    /**
    * 通知服务已经写入了
    **/
    int call(String key,int offset,int length,long ts,in Bundle args);
    /**
    * 通知client 已经写入了
    **/
    int listen(String key,in Bundle args);
    /**
    * 关闭端口
    **/
    int close(String key,in Bundle args);

}