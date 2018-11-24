# 1、JUCAPI

```
java1.8采用了 用 CAS + synchronized 控制并发操作
```

```
CopyOnWriteArrayList 和 CopyOnWriteArraySet;

当期望许多线程访问一个给定 collection 时，ConcurrentHashMap 优于 HashMap,
ConcurrentSkipListMap 优于 TreeMap

当期望的读数和遍历远远大于列表的更新数时， CopyOnWriteArrayList 优于同步的 ArrayList.
原理是 每次写入时都复制一份全新的
```



# 2、CountDownLatch

```
CountDownLatch 一个同步辅助类，在完成一组正在其他线程中执行的操作之前，它允许一个或者多个线程一直等待。

闭锁可以延迟线程的进度直到其到达终止状态，闭锁可以用来确保某些活动直到其他活动都完成才继续执行。

确保某个计算在其需要的所有资源都被初始化之后才继续执行
确保某个服务在其依赖的所有其他服务都已经启动之后才启动
等待直到某个操作所有参与者都准备就绪再继续执行
```

```java
public class TestCountDownLatch {
    
    public static void main(String[] args) {
        final CountDownLatch countDownCount = new CountDownLatch(5);
        LatchDemo latchDemo = new LatchDemo(countDownCount);
        for (int i = 0; i < 10;i++) {
            new Thread(latchDemo).start();
        }
    }
}

class LatchDemo implements Runnable {

    private CountDownLatch latch;
    public LatchDemo(CountDownLatch latch) {
        this.latch = latch;
    }
    
    @Override
    public void run() {
        try {
        for (int i = 0; i < 50000; i++) {
            if(i % 2 ==0) {
                System.out.println(i);
            }
        }} finally {
            latch.countDown();
        }
        
    }
    
}
```

```
final CountDownLatch countDownCount = new CountDownLatch(5);
latch.countDown();
```

