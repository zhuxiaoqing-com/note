原子性（CPU核\总线\周期\锁） 

如何查看 Linux 是多少位的CPU 

```
uname -an
```

## CPU 寻址

```
cpu 的位宽一般是一 min{ALU位宽，数据总线位宽}决定的！也就是说CPU由ALU、通过寄存器、数据总线三者之中最少的位宽决定！所以CPU位宽与其寻址能力并不是挂钩的！32位CPU指的是上述三个都是32位。和地址总线没关系。地址总线决定了寻址的范围，数据总线和寄存器（SI\DI等）决定了可寻址的能力。
```

```
32位 X86 包括 8个通用寄存器。64位 x64 处理器有 16 个寄存器

64 位处理器（以及为它们编写的应用程序和操作系统）可以更有效地处理数据，每个时钟周期可以移动更多信息。
```

## **时钟周期、总线周期和指令周期** 



**相关概念：** 时钟周期、总线周期和指令周期

 1.时钟周期：微处理器执行指令的最小时间单位，又称T状态。它通常与微机的主频有关。 



2.总线周期：CPU对存储器或I/O端口完成一次读/写操作所需的时间。如8086微处理器的基本**总线周期由四个时钟周期**T1～T4组成，80486微处理器的基本总线周期由T1和T2两个时钟周期组成。当外设速度较慢时，可插入等待周期Tw。 



3.指令周期：CPU执行一条指令所需要的时间。**指令周期由若干个总线周期组成**，不同指令执行的时间不同。同一功能的指令，在寻址方式不同时，所需要的时间也不同。 

```
从小到大来说：时钟周期，CPU周期，指令周期，CPU时间片

时钟周期：一个脉冲需要的时间，频率的倒数

CPU周期：读取一个指令节所需的时间

指令周期：读取并执行完一个指令所需的时间

CPU时间片：CPU分给每个进程的时间

指令周期是取出并执行一条指令的时间，指令周期常常有若干个CPU周期（也叫机器周期），CPU周期一般由12个时钟周期组成（时钟周期通常由晶振决定）。也就是说指令周期的通常大于cpu周期，指令周期的长短与执行的指令有关，有的指令需要花费更多的CPU周期。
```



锁的机制

```
锁其实就是锁总线。

在x86 平台上，CPU提供了在指令执行期间对总线加锁的手段。CPU芯片上有一条引线#HLOCK pin，如果汇编语言的程序中在一条指令前面加上前缀"LOCK"，经过汇编以后的机器代码就使CPU在执行这条指令的时候把#HLOCK pin的电位拉低，持续到这条指令结束时放开，从而把总线锁住，这样同一总线上别的CPU就暂时不能通过总线访问内存了，保证了这条指令在多处理器环境中的。

第二个机制是通过缓存锁定保证原子性。在同一时刻我们只需保证对某个内存地址的操作是原子性即可，但总线锁定把CPU和内存之间通信锁住了，这使得锁定期间，其他处理器不能操作其他内存地址的数据，所以总线锁定的开销比较大。

所谓“缓存锁定”就是如果缓存在处理器缓存行中内存区域在LOCK操作期间被锁定，当它执行锁操作回写内存时，处理器不在总线上声言LOCK＃信号，而是修改内部的内存地址，并允许它的缓存一致性机制来保证操作的原子性，因为缓存一致性机制会阻止同时修改被两个以上处理器缓存的内存区域数据，当其他处理器回写已被锁定的缓存行的数据时会起缓存行无效，在例1中，当CPU1修改缓存行中的i时使用缓存锁定，那么CPU2就不能同时缓存了i的缓存行。

但是有两种情况下处理器不会使用缓存锁定。第一种情况是：当操作的数据不能被缓存在处理器内部，或操作的数据跨多个缓存行（cache line），则处理器会调用总线锁定。第二种情况是：有些处理器不支持缓存锁定。对于Inter486和奔腾处理器,就算锁定的内存区域在处理器的缓存行中也会调用总线锁定。



```





























