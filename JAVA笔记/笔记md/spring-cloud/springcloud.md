# 一、springCloud面试题

1、什么是微服务

2、微服务之间是如何独立通讯的

3、springCloud 和 Dubbo 有哪些区别

4、SpringBoot和SpringCloud,请你谈谈对他们的理解

5、什么是服务熔断？什么是服务降级

6、微服务的优缺点分别是什么？说下你在项目开发中碰到的坑

7、你所知道的微服务技术栈有哪些？请列举一二

8、eureka 和 zookeeper 都可以提供服务注册与发现的功能，请说说两个的区别？



# 二、微服务概述

## 1、是什么

业界并没有一个统一的、标准的定义

但通常而言，微服务架构是一种架构模式或者说是一种架构风格，**它提倡将单一应用程序划分成一组小的服务**

，每个服务运行在其独立的自己的进程中，服务之间互相协调，互相配合，为用户提供最终价值。服务之间采用轻量级的通信机制互相沟通(通常是基于HTTP的RESTful API)。每个服务都围绕着具体的业务进行构建，并且能够被独立得部署到生产环境、类生产环境等。另外，应尽量避免统一的、集中式的服务管理机制，对具体的一个服务而言，应根据业务上下文，选择合适的语言、工具对其进行构建，可以有一个非常轻量级的集中式管理来协调这些服务，可以使用不同的语言来编写服务，也可以使用不同的数据存储

微服务化的核心就是将传统的一站式应用，根据业务拆分成一个一个的服务去耦合，每一个微服务提供单个业务功能的服务，一个服务做一件事，从技术角度看就是一种小而独立的处理过程，类似进程的概念，能够自行单独运行或销毁，拥有自己独立的数据库。

## 2、微服务与微服务架构

**微服务**

强调的是服务的大小，他关注的是某一个点，是具体解决某一个问题/提供落地对应服务的一个服务应用，狭义意的讲，可以看做 Eclipse 里面一个个微服务工程/或者Module

可以将其理解成 eclipse 工具里面用 maven 开发的一个个独立的小 module ,它具体是使用 springBoot 开发的一个小模块，专业的事情交给专业的模块来做，一个模块就做一件事，

​          **强调的是一个一个的个体，每个个体完成一个具体的任务或者功能**

**微服务架构**

是一个宏观的概念，一个个微服务连接起来就是一个微服务架构了

## 3、微服务优缺点

**优点：**

1、每个服务足够内聚，足够小，代码容易理解。 这样能聚焦一个指定的业务功能或业务需求。

2、开发简单、开发效率提高，一个服务可能就是专一的只干一件事。

3、能使用不同的语言开发

4、然后前后端分离

5、可以有自己的数据库，也可以有统一的数据库



**缺点：**

1、开发人员要处理分布式系统的复杂性

2、多服务运维难度，随着服务的增加，运维的压力也在增大

3、系统部署依赖

4、服务间通信成本

5、数据一致性

6、系统集成测试

7、性能监控



## 4、微服务的技术栈有哪些

| 微服务条目                             | 微服务实现                              |
| -------------------------------------- | --------------------------------------- |
| 服务开发                               | Springboot                              |
| 服务配置与管理                         | Netflix公司的 Archaius、阿里的Diamond等 |
| 服务注册与发现                         | Eureka、Consul、Zookeeper 等            |
| 服务调用                               | REST、RPC                               |
| 服务熔断器                             | Hystrix、Envoy等                        |
| 负载均衡                               | Ribbon、Nginx等                         |
| 服务接口调用(客户端调用服务的简化工具) | Feign等                                 |
| 消息队列                               | Kafka、RabbitMQ、ActiveMQ               |
| 服务配置中心管理                       | SpringCloudConfig、Chef等               |
| 服务路由(API网关)                      | Zuul等                                  |
| 服务监控                               | Zabbix、Nagios、Metrics、Spectator      |
| 全链路追踪                             | Zipkin、Brave、Dapper 等                |
| 服务部署                               | Docker、OpenStack、Kubernetes等         |
| 数据流操作开发包                       | SpringCloud Stream                      |
| 事件消息总线                           | SpringCloud Bus                         |
| ...                                    |                                         |

## 5、SpringCloud

​	是一个分布式微服务架构下的一站式解决方案，是各个微服务架构落地技术的集合体

## 6、SpringCloud 和 SpringBoot 是什么关系

**SpringBoot** 专注于快速方便的开发单个个体微服务。

**SpringCloud** 是关注全局的微服务协调整理治理框架，它将 SpringBoot 开发的一个个单体微服务整个并管理起来，为各个微服务之间提供，配置管理、服务发现、断路器、路由、微服务、事件总线、全局锁、决策竞选、分布式会话等集成服务



SpringBoot 可以离开 SpringCloud 独立开发项目，但是 SpringCloud 离不开 SpringBoot,属于依赖的关系。



SpringBoot专注于快速、方便的开发单个微服务个体，SpringCloud 关注全局的服务治理框架。

## 7、SpringCloud 和 Dubbo 的区别

SpringCloud 抛弃了 Dubbo 的 RPC 通信，采用的是基于 HTTP 的 REST 方式。

总的来说，这两种方式各有优劣。虽然从一定的程度上来说，后者牺牲了服务调用的性能，但是也避免了原生的RPC 带来的问题。而且REST相比RPC 更为灵活，服务提供方和调用方的依赖只依靠一纸契约，不存在代码级别的强依赖，这在强调快速烟花的微服务环境下，显得更加合适。

社区支持和更新力度

SpringCloud有一系列Spring系列的分布式工具的支持，而Dubbo 你必须拼装使用第三方

springCloud 中文文档  https://springcloud.cc/spring-cloud-dalston.html

springCloud 中文社区  http://springcloud.cn/

springCloud 中文网      https://springcloud.cc/



# 二、练习小项目

lombok 一个可以帮你自动生成 getset 方法的注解

```pom
<dependency>
  		<groupId>org.projectlombok</groupId>
  		<artifactId>lombok</artifactId>
  		<version>1.16.18</version>
  	</dependency>
```

```java
@AllArgsConstructor
@NoArgsConstructor
@Data // 生成setter getter
@Accessors(chain=true)//chain开启链式写法 链式写法就是 a.seta().sets(); 这样的
public class Dept implements Serializable {
    private Long deptno;
    private String dname;
    private String db_source;// 来自哪个数据库，因为微服务架构可以一个服务对应一个数据库，同一个星星被存储到不同数据库
}
```

微服务主要有四大步骤工程

1、整体父工程 Project  pom

2、公共子模块 Module 放 entity  还可以有 公共的 utils 模块  都是 jar

3、部门微服务提供者 Module

4、部门微服务消费者 Module



之前做的商城项目，是

1、整体父工程 Project pom

2、common jar 继承  整体父工程   是一些通用的工具 自定义的jedis  pojo  utils

3、然后就是 order pom  服务提供方

​	1、 order service  war

​	2、pojo、interface、dao     jar

4、order-web war 服务调用方



```yaml
server:
  port: 8001
  
mybatis:
  config-location: classpath:mybatis/mybatis.cfg.xml        # mybatis配置文件所在路径
  type-aliases-package: cn.zhu.springcloud.entities    # 所有Entity别名类所在包 开启别名
  mapper-locations:
  - classpath:mybatis/mapper/**/*.xml                       # mapper映射文件
    
spring:
   application:
    name: microservicecloud-dept 
   datasource:
    type: com.alibaba.druid.pool.DruidDataSource            # 当前数据源操作类型
    driver-class-name: org.gjt.mm.mysql.Driver              # mysql驱动包
    url: jdbc:mysql://localhost:3306/cloudDB01              # 数据库名称
    username: root
    password: 123456
    dbcp2:
      min-idle: 5                                           # 数据库连接池的最小维持连接数
      initial-size: 5                                       # 初始化连接数
      max-total: 5                                          # 最大连接数
      max-wait-millis: 200                                  # 等待连接获取的最大超时时间
```

```java
<insert id="addDept" >
  	INSERT INTO dept(dname,db_source) VALUES(#{dname},DATABASE());
</insert>

DATABASE(); 代表当前数据库的名称
```

Consumer 的写法

```java
@Configuration
public class ConfigBean {

    @Bean 
    public RestTemplate getRestTemplate() {
        return new RestTemplate();
    } 
}

```

RestTemplate

​	RestTemplate 提供了多种便捷访问远程 Http 服务的方法，是一种简单便捷的访问 Restful 服务模板类，是spring 提供的用于访问 Rest 服务的客户端模板工具集

有点像 Httpclient

使用

​	使用 restTemplate 访问 restful 接口非常的简单粗暴无脑。

(url,requestMap,ResponseBean.class)这三个参数分别代表

Rest请求地址，请求参数，Http响应转换被转换成的对象类型(provider)返回的类型

// provider 的返回值是什么类型的 第三个参数就写什么

```java
@RestController
public class DeptController_Consumer {
    
    private static final String REST_URL_PREFIX="http://localhost:8001";

   @Autowired
   private RestTemplate restTemptate; 
   
   @RequestMapping(value="/consumer/dept/add")
   public boolean add(Dept dapt) {
       // provider 的返回值是什么类型的 第三个参数就写什么
       return restTemptate.postForObject(REST_URL_PREFIX+"/dept/add", dapt, Boolean.class);
   }
   @RequestMapping(value="/consumer/dept/get/{id}")
   public Dept get(@PathVariable("id") Long id) {
       return restTemptate.getForObject(REST_URL_PREFIX+"/dept/get/"+ id, Dept.class);
   }
   @SuppressWarnings("unchecked")
@RequestMapping(value="/consumer/dept/list")
   public List<Dept> list(Dept dapt) {
       return restTemptate.getForObject(REST_URL_PREFIX+"/dept/list",List.class);
   }
}
```

# 三、Eureka 服务注册与发现

## 1、Eureka 是什么？

Eureka 是 Netflix 的一个子模块，也是核心模块之一。Eureka 是一个基于 REST 的服务，用于定位服务，以实现云端中间层服务发现和故障转移。服务注册与发现对于微服务架构来说是非常重要的，有了服务发现与注册，只需要使用服务的标识符，就可以访问到服务，而不需要修改服务调用的配置文件了。功能类似于 dubbo 的注册中心，比如 Zookeeper

Netflix 在设计 Eureka 时遵守的就是 AP 原则 可用性 和 分区容忍性

## 2、Eureka 的基本架构

SpringCloud 封装了 Netflix 公司开发的 Eureka 模块来 实现服务注册和发现(请对比 Zookeeper)

Eureka 采用了 C-S  的设计架构。Eureka Server 作为服务注册功能的服务器，它是服务注册中心



而系统中的其他微服务，使用 Eureka 的客户端连接到 Eureka Server 并维持心跳连接。这样系统的维护人员就可以通过 Eureka Server 来监控系统中各个微服务是否正常运行。SpringCloud 的一些其他模块(比如Zuul)就可以通过 Eureka Server 来发现系统中的其他微服务，并执行相关的逻辑。



Eureka 包含两个组件: **Eureka Server** 和 **Eurake Client**

Eureka Server 提供服务 注册服务

各个节点启动后，会在 EurekaSever 中进行注册，这样EurekaServer 中的服务注册表中将会存储所有可用服务节点的信息，服务节点中的信息可在界面中直观的看到

EurekaClient 是一个 java 客户端，用于简化 Eureka Server 的交互，客户端同时也具备一个内置的、使用轮询(round-robin)负载算法的负载均衡器。在应用启动后，将会向 Eureka Server 发送心跳(默认周期为 30 秒)。如果 EurekaServer 在多个心跳周期内没有接收到某个节点的心跳，EurekaServer将会从服务注册表中吧这个服务节点移除(默认90秒)

