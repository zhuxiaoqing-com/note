## 浅析MySQL中exists与in的使用

这里的  exists 和 in 都是指的是 两张表之间的

in 的具体使用

```sql
SELECT
    *
FROM
    `user`
WHERE
    `user`.id IN (
        SELECT
            `order`.user_id
        FROM
            `order`
    )
```

exists

```sql
SELECT
    `user`.*
FROM
    `user`
WHERE
    EXISTS (
        SELECT
            `order`.user_id
        FROM
            `order`
        WHERE
            `user`.id = `order`.user_id
    )
    可以使用 外表的属性
```



**Exists**

exists 对外表用loop逐条查询，每次查询都会查看exists的条件语句，当exists里面的条件语句能够返回记录行时(无论记录行多少，只要能返回)，条件就为真，返回当前loop到的这条记录，反之如果exists里的条件不能返回记录行，则当前loop到的这条记录被丢弃，exists的条件就像一个bool条件，当能返回结果集则为true,不能返回结果集则为false

例子1:

```sql
select * from user where exists (select 1); 
select 1 返回 记录为 1  列名为 1
```

对 user 表的记录逐条取出，由于子条件中的 select 1永远能返回记录行，那么user表的所有的记录都将被加入结果集，所以和 select * from user; 是一样的

例子2：

```sql
select * from user where exists (select * from user where userId=0);
```

可以知道对user表进行loop时，检查条件语句(select * from user where userId=0),由于userId永远不为0，所以条件所以条件语句永远返回空集，条件永远为false，那么user表的所有记录都将被丢弃

not exists与exists相反，也就是当exists条件有结果集返回时，loop到的记录将被丢弃，否则将loop到的记录加入结果集

总的来说，就是遍历外表，遍历的时候判断内表，如果内表返回了记录，不管几条，只要有记录，就返回 true,将外表的当前条记录返回，一直遍历，直到遍历完毕，每遍历出一条记录，就判断一次。

因为不管怎么样 外表都是要全表扫描的，建不建索引都是一样的，但是内表可以使用 索引，因为每次循环都是一个查询，因为外表需要全表扫描，所以记录最好少一点，内表的有索引记录可以多一点



**In**

不管怎么样，外表都是全扫描，但是内表不是，内表可以走索引

in查询相当于多个or条件的叠加，这个比较好理解，比如下面的查询】

```sql
select * from user where userId in (1,2,3);
```

等效于

```sql
select * from user where userId=1 or userId=2 or userId=3;
```

not in 与 in 相反，如下

```sql
select * from user where userId!=1 and userId != 2 and userId != 3;
```

总的来说，in查询就是先将子查询条件的记录全都查出来，假设结果集为B，共有m条记录，然后在将子查询条件的结果集分解成m个，再进行m次查询 



值得一提的是，in查询的子条件返回结果必须只有一个字段，例如

select * from user where userId in (select id from B);

而不能是

select * from user where userId in (select id, age from B);

而exists就没有这个限制

mysql中的in语句是把外表和内表作hash 连接，而exists语句是对外表作loop循环，每次loop循环再对内表进行查询。一直大家都认为exists比in语句的效率要高，这种说法其实是不准确的。这个是要区分环境的。 



not in 和not exists如果查询语句使用了not in 那么内外表都进行全表扫描，没有用到索引；而not extsts 的子查询依然能用到表上的索引。**所以无论那个表大，用not exists都比not in要快**。 

反正就是 in 和 exists 不管怎么样都是要有一个表全表扫描的

in 的话 innodb in 是不能使用索引的。