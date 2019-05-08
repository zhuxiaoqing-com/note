## 字符集和比较规则简介

### 字符集简介

```
我们知道在计算机中只能存储二进制数据，那该怎么存储字符串呢？当然是建立字符与二进制数据的映射关系了，建立这个关系最起码要搞清楚两件事儿：
	1、你要把那些字符映射成二进制数据？
	也就是界定清楚字符范围。
	2、怎么映射？
	将一个字符映射成一个二进制数据的过程也叫做编码，将一个二进制数据映射到一个字符的过程叫做解码。
	
	人们抽象出一个字符集的概念来描述某个字符范围的编码规则。
```

### 比较规则简介

```
在我们确定了字符集以后，怎么比较两个字符的大小呢？最容易想到的就是直接比较这两个字符对应的二进制编码的大小，比方说字符 'a' 的编码为 0x01 ，字符 'b' 的编码为 0x02, 所以 'a' 小于 'b'，这种简单的比较规则也可以被称为二进制比较规则，英文名为 binary collaction.
```

```
二进制比较规则是简单，但有时候并不符合现实需求，比如在很多场合对于英文字符我们都是不区分大小写的，也就是说 'a' 和 'A' 是相等的，在这种场合下就不能简单粗暴的使用二进制比较规则了，这时候我们可以这样指令比较规则： 
1、将两个大小写不同的字符全部转为大写或者小写
2、再比较两个字符对应的二进制数据
```

## Mysql 中支持的字符集和排序规则

### mysql中的utf-8和utf-8mb4

```
我们上边说utf8字符集表示一个字符需要使用1～4个字节，但是我们常用的一些字符使用1～3个字节就可以表示了。而在MySQL中字符集表示一个字符所用最大字节长度在某些方面会影响系统的存储和性能，所以设计MySQL的大叔偷偷的定义了两个概念：

utf8mb3：阉割过的utf8字符集，只使用1～3个字节表示字符。

utf8mb4：正宗的utf8字符集，使用1～4个字节表示字符。

有一点需要大家十分的注意，在MySQL中utf8是utf8mb3的别名，所以之后在MySQL中提到utf8就意味着使用1~3个字节来表示一个字符，如果大家有使用4字节编码一个字符的情况，比如存储一些emoji表情啥的，那请使用utf8mb4。
```

### 字符集的查看

```
查看设置系统变量
SHOW [SESSION|GLOBAL] VARIABLES [LIKE 匹配的模式]
SET [SESSION|GLOBAL] 系统变量名 = 值
SET [@@SESSION|GLOBAL.]var_name = 值

查看设置系统变量
SHOW [GLOBAL|SESSION] STATUS [LIKE 匹配的模式];
如果不选定 变量的系统范围 默认为 session 范围 就是一个连接的有效范围
			会话变量，影响某个客户端连接的操作，就是在这里链接内有效，设置的变量只在这个链接内生效
			
查看mysql支持的字符集
SHOW (CHARACTOR SET|CHARSET) [LIKE 匹配的模式];
```

```
其中 CHARACTOR SET 和 CHARSET 是同义词，用任意一个都可以
```

```
mysql> SHOW CHARSET;
+----------+---------------------------------+---------------------+--------+
| Charset  | Description                     | Default collation   | Maxlen |
+----------+---------------------------------+---------------------+--------+
| big5     | Big5 Traditional Chinese        | big5_chinese_ci     |      2 |
...
| latin1   | cp1252 West European            | latin1_swedish_ci   |      1 |
| latin2   | ISO 8859-2 Central European     | latin2_general_ci   |      1 |
...
| ascii    | US ASCII                        | ascii_general_ci    |      1 |
...
| gb2312   | GB2312 Simplified Chinese       | gb2312_chinese_ci   |      2 |
...
| gbk      | GBK Simplified Chinese          | gbk_chinese_ci      |      2 |
| latin5   | ISO 8859-9 Turkish              | latin5_turkish_ci   |      1 |
...
| utf8     | UTF-8 Unicode                   | utf8_general_ci     |      3 |
| ucs2     | UCS-2 Unicode                   | ucs2_general_ci     |      2 |
...
| latin7   | ISO 8859-13 Baltic              | latin7_general_ci   |      1 |
| utf8mb4  | UTF-8 Unicode                   | utf8mb4_general_ci  |      4 |
| utf16    | UTF-16 Unicode                  | utf16_general_ci    |      4 |
| utf16le  | UTF-16LE Unicode                | utf16le_general_ci  |      4 |
...
| utf32    | UTF-32 Unicode                  | utf32_general_ci    |      4 |
| binary   | Binary pseudo charset           | binary              |      1 |
...
| gb18030  | China National Standard GB18030 | gb18030_chinese_ci  |      4 |
+----------+---------------------------------+---------------------+--------+
41 rows in set (0.01 sec)
```

可以看到，我使用的这个`MySQL`版本一共支持`41`种字符集，其中的`Default collation`列表示这种字符集中一种默认的`比较规则`。大家注意返回结果中的最后一列`Maxlen`，它代表该种字符集表示一个字符最多需要几个字节。为了让大家的印象更深刻，我把几个常用到的字符集的`Maxlen`列摘抄下来，大家务必记住：

| 字符集名称 | Maxlen |
| :--------: | :----: |
|  `ascii`   |  `1`   |
|  `latin1`  |  `1`   |
|  `gb2312`  |  `2`   |
|   `gbk`    |  `2`   |
|   `utf8`   |  `3`   |
| `utf8mb4`  |  `4`   |

```
latin1 又叫 ISO-88591
```

























