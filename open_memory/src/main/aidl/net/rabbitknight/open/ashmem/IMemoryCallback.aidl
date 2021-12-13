// IMemoryCallback.aidl
package net.rabbitknight.open.ashmem;

// Declare any non-default types here with import statements

interface IMemoryCallback {
   /**
    * 通知接收端已经写入了
    **/
    int onCall(String key,int offset,int length,long ts,in Bundle args);
}