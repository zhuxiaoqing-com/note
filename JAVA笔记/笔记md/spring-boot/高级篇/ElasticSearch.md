ElasticSearch

# 1、概念简介

以员工文档的形式存储为例，一个文档代表一个员工数据。存储数据到 ElasticSearch 的行为叫做索引，

但在索引一个文档之前，需要确定将文档存储在哪里。

一个 ElasticSearch 集群可以包含多个索引，相应的每个索引可以包含多个类型，这些不同的类型存储着

多个文档，每个文档又有多个属性。

类似关系

索引 --  数据库

类型 -- 表

id -- 行

属性 -- 列



ElasticSearch  采用的是 RESTful 风格



PUT 存储或修改

POST/GET 获取

DELETE 删除

HEAD 数据是否存在，没有 消息体      存在返回状态码 : 200      不存在返回状态码 : 404 



# 2、基本搜索

## 1、轻量搜索 Query-string 

```http
查询 megacorp 索引下的 employee 类型的所有数据
所有数据放在 hits 中，一个搜索默认返回十条结果
GET /megacorp/employee/_search

{
   "took":      6,
   "timed_out": false,
   "_shards": { ... },
   "hits": {
      "total":      3,
      "max_score":  1, // score 表示匹配度，因为没有加条件所以都是 1.最大就是 1.
      "hits": [
         ...
      ]
   }
}
```

带条件搜索

```http
GET /megacorp/employee/_search?q=last_name:Smith
```



## 2、使用查询表达式搜索

Query-string 搜索通过命令非常方便地进行临时性的即席搜索 ，但它有自身的局限性（参见 [*轻量* 搜索](https://www.elastic.co/guide/cn/elasticsearch/guide/current/search-lite.html) ）。Elasticsearch 提供一个丰富灵活的查询语言叫做 *查询表达式* ， 它支持构建更加复杂和健壮的查询。

*领域特定语言* （DSL）， 指定了使用一个 JSON 请求。我们可以像这样重写之前的查询所有 Smith 的搜索 ：

```http
Get /megacorp/employee/_search
{
    "query":{
        "match": {
            "last_name" : "Smith"
        }
    }
}
```

返回的结果与之前的查询一样，但还是可以看到有一些变化。其中之一是，不再使用 query-string 参数，而是一个请求体替代。这个请求使用 JSON 构造，并使用了一个 match 查询 (属于查询类型之一)



## 3、更复杂的查询  filter

```http
GET /megacorp/employee/_search
{
    "query": {
        "match":{
            "last_name":"Smith"
        }
    },
    "filter":{
        "range":{
            "age":{"gt":30}
        }
    }
}

#我们使用了 一个 range 过滤器 ，它能找到年龄大于 30 的文档，其中 gt 表示_大于(_great than)。
```

## 4、全文搜索

```http
GET /megacorp/employee/_search
{
    "query":{
        "match":{
            "about":"rock climbing"
        }
    }
}

#会采用模糊查询
```

全文属性上搜索返回相关性最强的结果。ElasticSearch 中的相关性概念非常重要，也是完全区别于传统关系型数据库的一个概念，数据库中的一条记录要么匹配要么不匹配。

## 5、短语搜索

找出一个属性中的独立单词是没有问题的，但有时候想要精确匹配一系列单词或者短语，比如， 我们想执行这样一个查询，仅匹配同时包含 “rock” *和* “climbing” ，*并且* 二者以短语 “rock climbing” 的形式紧挨着的雇员记录。 

为此对  match 查询稍作调整，使用一个叫做 match_phrase

```http
GET /megacorp/employee/_search
{
    "query" : {
        "match_phrase" : {
            "about" : "rock climbing"
        }
    }
}

#只会返回有 rock climbing 单词，并且紧挨着的文档
```

## 6、高亮搜索

许多应用都倾向于在每个搜索结果中高亮部分文本片段，以便让用户知道为何该文档符合查询条件。在 Elasticsearch 中检索出高亮片段也很容易。

再次执行前面的查询，并增加一个新的 `highlight` 参数：

```http
GET /megacorp/employee/_search
{
    "query" : {
        "match_phrase" : {
            "about" : "rock climbing"
        }
    },
    "highlight" : {
        
    }
}
```

## 7、分析

 Elasticsearch 有一个功能叫聚合（aggregations），允许我们基于数据生成一些精细的分析结果。聚合与 SQL 中的 `GROUP BY` 类似但更强大。 

```http
GET /megacorp/employee/_search
{
    "aggs":{
        "all_interests" : {
            "terms" : {"field", "interests"}
        }
    }
}
 all_...  所有 
 avg_... 平均值
 "aggs":{
        "all_..." : {
            "terms" : {"field", "interests"}
        }
    }
    
    
 
 结果
 
 {
   ...
   "hits": { ... },
   "aggregations": {
      "all_interests": {
         "buckets": [
            {
               "key":       "music",
               "doc_count": 2
            },
            {
               "key":       "forestry",
               "doc_count": 1
            },
            {
               "key":       "sports",
               "doc_count": 1
            }
         ]
      }
   }
}
```



# 3、结构化搜索

## 1、精确至值查找

当进行精确值查找时， 我们会使用过滤器（filters）。过滤器很重要，因为它们执行速度非常快，不会计算相关度（直接跳过了整个评分阶段）而且很容易被缓存。我们会在本章后面的 [过滤器缓存](https://www.elastic.co/guide/cn/elasticsearch/guide/current/filter-caching.html) 中讨论过滤器的性能优势，不过现在只要记住：请尽可能多的使用过滤式查询。



### 1、term 查询数字 

例子

```http
POST /my_store/products/_bulk
{ "index": { "_id": 1 }}
{ "price" : 10, "productID" : "XHDK-A-1293-#fJ3" }
{ "index": { "_id": 2 }}
{ "price" : 20, "productID" : "KDKE-B-9947-#kL5" }
{ "index": { "_id": 3 }}
{ "price" : 30, "productID" : "JODL-X-1937-#pV7" }
{ "index": { "_id": 4 }}
{ "price" : 30, "productID" : "QQPX-R-3956-#aD8" }

#_bulk : 批量操作 批量添加
```

响应的 sql

```sql
SELECT document
FROM   products
WHERE  price = 20
```

可以转换成 

```json
{
    "term" : {
        "price" : 20
    }
}
```

通常当查找一个精确值的时候，我们不需要对查询进行评分计算，只希望对文档进行包括或排除的计算，所以我们会使用 constant_score  查询以非评分模式来执行 term 查询并以一作为统一评分。

最终组合的结果是一个 `constant_score` 查询，它包含一个 `term` 查询： 

```http
GET/POST /my_store/products/_search
{
    "query" : {
        "constant_score" : { 
            "filter" : {
                "term" : { 
                    "price" : 20
                }
            }
        }
    }
}
```

我们用 `constant_score` 将 `term` 查询转化成为过滤器 

查询置于 `filter` 语句内不进行评分或相关度的计算，所以所有的结果都会返回一个默认评分 `1` 。 

### 2、trem 查询文本

如本部分开始处提到过的一样 ，使用 `term` 查询匹配字符串和匹配数字一样容易。但是注意 term 查询 text

会被分词 

我们需要 

```http
DELETE /my_store  // 删除索引，数据也会被删除

PUT /my_store 
{
    "mappings" : {
        "products" : {// 类型
            "properties" : {
                "productID" : {// 字段
                    "type" : "string",
                     // 这里我们告诉 Elasticsearch ，我们不想对 productID 做任何分析。
                    "index" : "not_analyzed" 
                }
            }
        }
    }

}
```



## 2、组合过滤器

SQL

 在实际应用中，我们很有可能会过滤多个值或字段。比方说，怎样用 Elasticsearch 来表达下面的 SQL ？ 

```sql
SELECT product
FROM   products
WHERE  (price = 20 OR productID = "XHDK-A-1293-#fJ3")
  AND  (price != 30)
```

这种情况下，我们需要 `bool` （布尔）过滤器。 这是个 *复合过滤器（compound filter）* ，它可以接受多个其他过滤器作为参数，并将这些过滤器结合成各式各样的布尔（逻辑）组合。 

### 1、布尔过滤器

一个 `bool` 过滤器由三部分组成：

```JSON
{
   "bool" : {
      "must" :     [],
      "should" :   [],
      "must_not" : [],
   }
}
```

- `must`

  所有的语句都 *必须（must）* 匹配，与 `AND` 等价。

- `must_not`

  所有的语句都 *不能（must not）* 匹配，与 `NOT` 等价。

- `should`

  至少有一个语句要匹配，与 `OR` 等价。

就这么简单！ 当我们需要多个过滤器时，只须将它们置入 `bool` 过滤器的不同部分即可。



一个 `bool` 过滤器的每个部分都是可选的（例如，我们可以只有一个 `must` 语句），而且每个部分内部可以只有一个或一组过滤器。 

```http
{
   "query" : {
            "bool" : {
              "should" : [
                { "term" : {"productID" : "KDKE-B-9947-#kL5"}}, 
                { "bool" : { 
                  "must" : [
                    { "term" : {"productID" : "JODL-X-1937-#pV7"}}, 
                    { "term" : {"price" : 30}} 
                  ]
                }}
              ]
           }
         }
}
```

## 3、查找多个精确值

`term` 查询对于查找单个值非常有用，但通常我们可能想搜索多个值。 

不需要使用多个 `term` 查询，我们只要用单个 `terms` 查询（注意末尾的 *s* ）， `terms` 查询好比是 `term` 查询的复数形式（以英语名词的单复数做比）。 

```java
{
    "terms" : {
        "price" : [20, 30]
    }
}
```

### 1、包含而不是相等

一定要了解 `term` 和 `terms` 是 *包含（contains）* 操作，而非 *等值（equals）* （判断）。 如何理解这句话呢？ 

### 2、范围

sql

```sql
SELECT document
FROM   products
WHERE  price BETWEEN 20 AND 40
```

可以转换为

```http
"range" : {
    "price" : {
        "gte" : 20,
        "lte" : 40
    }
}
```

`range` 查询可同时提供包含（inclusive）和不包含（exclusive）这两种范围表达式，可供组合的选项如下： 

- `gt`: `>` 大于（greater than）
- `lt`: `<` 小于（less than）
- `gte`: `>=` 大于或等于（greater than or equal to）
- `lte`: `<=` 小于或等于（less than or equal to）

**日期范围**

```http
"range" : {
    "timestamp" : {
        "gt" : "2014-01-01 00:00:00",
        "lt" : "2014-01-07 00:00:00"
    }
}
```

当使用它处理日期字段时， `range` 查询支持对 *日期计算（date math）* 进行操作，比方说，如果我们想查找时间戳在过去一小时内的所有文档： 



### 3、处理 Null 值

如何将某个不存在的字段存储在这个数据结构中呢？无法做到！简单的说，一个倒排索引只是一个 token 列表和与之相关的文档信息，如果字段不存在，那么它也不会持有任何 token，也就无法在倒排索引结构中表现。

最终，这也就意味着 ，`null`, `[]` （空数组）和 `[null]` 所有这些都是等价的，它们无法存于倒排索引中。

显然，世界并不简单，数据往往会有缺失字段，或有显式的空值或空数组。为了应对这些状况，Elasticsearch 提供了一些工具来处理空或缺失值。

#### 1、查询存在

第一件武器就是 `exists` 存在查询。 这个查询会返回那些在指定字段有任何值的文档

```http
POST /my_index/posts/_bulk
{ "index": { "_id": "1"              }}
{ "tags" : ["search"]                }  1
{ "index": { "_id": "2"              }}
{ "tags" : ["search", "open_source"] }  2
{ "index": { "_id": "3"              }}
{ "other_field" : "some data"        }  3
{ "index": { "_id": "4"              }}
{ "tags" : null                      }  4
{ "index": { "_id": "5"              }}
{ "tags" : ["search", null]          }  5
```

们的目标是找到那些被设置过标签字段的文档，并不关心标签的具体内容。只要它存在于文档中即可，用 SQL 的话就是用 `IS NOT NULL` 非空进行查询： 

```sql
SELECT tags
FROM   posts
WHERE  tags IS NOT NULL
```

```http
GET /my_index/posts/_search
{
    "query" : {
        "constant_score" : {
            "filter" : {
                "exists" : { "field" : "tags" }
            }
        }
    }
}
```

只要 `tags` 字段存在项（term）的文档都会命中并作为结果返回



#### 2、缺失查询



```sql
SELECT tags
FROM   posts
WHERE  tags IS NULL
```

`exists` 查询换成 `missing` 查询 

```http
GET /my_index/posts/_search
{
    "query" : {
        "constant_score" : {
            "filter": {
                "missing" : { "field" : "tags" }
            }
        }
    }
}
```

只要 `tags` 字段存在项（term）的文档都会命中并作为结果返回



#### 3、**当 null 的意思是 null** 

有时候我们需要区分一个字段是没有值，还是它已被显式的设置成了 `null` 。在之前例子中，我们看到的默认的行为是无法做到这点的；数据被丢失了。不过幸运的是，我们可以选择将显式的 `null` 值替换成我们指定 *占位符（placeholder）* 。

在为字符串（string）、数字（numeric）、布尔值（Boolean）或日期（date）字段指定映射时，同样可以为之设置 `null_value` 空值，用以处理显式 `null` 值的情况。不过即使如此，还是会将一个没有值的字段从倒排索引中排除。

当选择合适的 `null_value` 空值的时候，需要保证以下几点：

- 它会匹配字段的类型，我们不能为一个 `date` 日期字段设置字符串类型的 `null_value` 。
- 它必须与普通值不一样，这可以避免把实际值当成 `null` 空的情况。



#### 4、对象的存在于缺失

```json
{
   "name" : {
      "first" : "John",
      "last" :  "Smith"
   }
}
```

我们不仅可以检查 `name.first` 和 `name.last` 的存在性，也可以检查 `name` ，不过在 [映射](https://www.elastic.co/guide/cn/elasticsearch/guide/current/mapping.html) 中，如上对象的内部是个扁平的字段与值（field-value）的简单键值结构，类似下面这样： 

```
{
   "name.first" : "John",
   "name.last"  : "Smith"
}
```

那么我们如何用 `exists` 或 `missing` 查询 `name` 字段呢？ `name` 字段并不真实存在于倒排索引中。

原因是当我们执行下面这个过滤的时候：

```http
{
    "exists" : { "field" : "name" }
}
```

实际执行的是：

```http
{
    "bool": {
        "should": [
            { "exists": { "field": "name.first" }},
            { "exists": { "field": "name.last" }}
        ]
    }
}
```

这也就意味着，如果 `first` 和 `last` 都是空，那么 `name` 这个命名空间才会被认为不存在



# 4、关于缓存 

其核心实际是采用一个 bitset 记录与过滤器匹配的文档。Elasticsearch 积极地把这些 bitset 缓存起来以备随后使用。一旦缓存成功，bitset 可以复用 *任何* 已使用过的相同过滤器，而无需再次计算整个过滤器。

这些 bitsets 缓存是“智能”的：它们以增量方式更新。当我们索引新文档时，只需将那些新文档加入已有 bitset，而不是对整个缓存一遍又一遍的重复计算。和系统其他部分一样，过滤器是实时的，我们无需担心缓存过期问题。



# 5、全文搜索

## 基于词项与基于全文

所有查询会或多或少的执行相关度计算，但不是所有查询都有分析阶段。 和一些特殊的完全不会对文本进行操作的查询（如 `bool` 或 `function_score` ）不同，文本查询可以划分成两大家族：

**1、基于词项的查询**

基于词项的查询如 `term` 或 `fuzzy` 这样的底层查询不需要分析阶段，它们对单个词项进行操作。用 `term` 查询词项 `Foo` 只要在倒排索引中查找 *准确词项* ，并且用 TF/IDF 算法为每个包含该词项的文档计算相关度评分 `_score` 。

记住 `term` 查询只对倒排索引的词项精确匹配，这点很重要，它不会对词的多样性进行处理（如， `foo`或 `FOO` ）。这里，无须考虑词项是如何存入索引的。如果是将 `["Foo","Bar"]` 索引存入一个不分析的（ `not_analyzed` ）包含精确值的字段，或者将 `Foo Bar` 索引到一个带有 `whitespace` 空格分析器的字段，两者的结果都会是在倒排索引中有 `Foo` 和 `Bar` 这两个词。

**2、基于全文的查询** 

像 `match` 或 `query_string` 这样的查询是高层查询，它们了解字段映射的信息：

- 如果查询 `日期（date）` 或 `整数（integer）` 字段，它们会将查询字符串分别作为日期或整数对待。
- 如果查询一个（ `not_analyzed` ）未分析的精确值字符串字段， 它们会将整个查询字符串作为单个词项对待。
- 但如果要查询一个（ `analyzed` ）已分析的全文字段， 它们会先将查询字符串传递到一个合适的分析器，然后生成一个供查询的词项列表。

一旦组成了词项列表，这个查询会对每个词项逐一执行底层的查询，再将结果合并，然后为每个文档生成一个最终的相关度评分。

我们将会在随后章节中详细讨论这个过程。

先不看了  我好像弄偏了 应该直接 springboot 上手