# 七、启动配置原理

几个重要的事件回调机制

配置在META-INF/spring.factories

**ApplicationContextInitializer**

**SpringApplicationRunListener**



只需要放在ioc容器中

**ApplicationRunner**

**CommandLineRunner**



启动流程：

```java
org.springframework.context.ConfigurableApplicationContext

public static ConfigurableApplicationContext run(Object source, String... args) {
        // 这里指将 source 封装成 一个数组，说明你可以传入多个配置类
		return run(new Object[] { source }, args);
	}
```

```java
org.springframework.context.ConfigurableApplicationContext

public static ConfigurableApplicationContext run(Object[] sources, String[] args) {
	    // 这里分两步 new 一个 SpringApplication 然后 调用其 run 方法。
        return new SpringApplication(sources).run(args);
	}
```

## 1、首先我们看 new SpringApplication 干了什么

```java
public SpringApplication(Object... sources) {
		initialize(sources);
	}

private void initialize(Object[] sources) {
		if (sources != null && sources.length > 0) {
            // 将配置类都添加到 sources 中
			this.sources.addAll(Arrays.asList(sources));
		}
    	// 判断是否是 web 环境 true
		this.webEnvironment = deduceWebEnvironment();
    	// 1. 从 META-INF/spring.factories 获取 SpringFactory  进入 传入 					ApplicationContextInitializer.class
     // 2. 存到 this.initializers  里面  获取所有 ApplicationContextInitializer 的 instances
      /*
    	public void setInitializers(
			Collection<? extends ApplicationContextInitializer<?>> initializers) {
		this.initializers = new ArrayList<ApplicationContextInitializer<?>>();
		this.initializers.addAll(initializers);
	}
    */
		setInitializers((Collection) getSpringFactoriesInstances(
				ApplicationContextInitializer.class));
    	
   		// 从 META-INF/spring.factories 获取所有的 ApplicationListener.class
    	// 存入 this.listeners 中
		setListeners((Collection) getSpringFactoriesInstances(ApplicationListener.class));
  		//从当前运行的堆栈中找到有main方法的主配置类
		this.mainApplicationClass = deduceMainApplicationClass();
    /*
    	private Class<?> deduceMainApplicationClass() {
		try {
			// 从 getStackTrace() 获取StackTraceElement[]数组，而这个StackTraceElement是ERROR的每一个cause by的信息。 可以获取 调用的方法的名称 调用的类的名称
			// 只要方法名 等于 main 就将其 class 实例化 返回 因为你传入的配置类可能有多个
			所以 main 方法所在的那个配置类就叫做主配置类
			StackTraceElement[] stackTrace = new RuntimeException().getStackTrace();
			for (StackTraceElement stackTraceElement : stackTrace) {
				if ("main".equals(stackTraceElement.getMethodName())) {
					return Class.forName(stackTraceElement.getClassName());
				}
			}
		}
		catch (ClassNotFoundException ex) {
			// Swallow and continue
		}
		return null;
	}
    */
	}



```



### 1、getSpringFactoriesInstances()

```java
private <T> Collection<? extends T> getSpringFactoriesInstances(Class<T> type) {
		return getSpringFactoriesInstances(type, new Class<?>[] {});
	}

private <T> Collection<? extends T> getSpringFactoriesInstances(Class<T> type,
			Class<?>[] parameterTypes, Object... args) {
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		// Use names and ensure unique to protect against duplicates
    	// 进入 SpringFactoriesLoader.loadFactoryNames(type, classLoader));
		Set<String> names = new LinkedHashSet<String>(
				SpringFactoriesLoader.loadFactoryNames(type, classLoader));
		List<T> instances = createSpringFactoriesInstances(type, parameterTypes,
				classLoader, args, names);
		AnnotationAwareOrderComparator.sort(instances);
		return instances;
	}
// factoryClass = ApplicationContextInitializer.class
public static List<String> loadFactoryNames(Class<?> factoryClass, ClassLoader classLoader) {		// 获取ApplicationContextInitializer.class 的 getName();
		String factoryClassName = factoryClass.getName();
		try {
            //  FACTORIES_RESOURCE_LOCATION = "META-INF/spring.factories";
			Enumeration<URL> urls = (classLoader != null ? classLoader.getResources(FACTORIES_RESOURCE_LOCATION) :
					ClassLoader.getSystemResources(FACTORIES_RESOURCE_LOCATION));
			List<String> result = new ArrayList<String>();
			while (urls.hasMoreElements()) {
				URL url = urls.nextElement();
                
				Properties properties = 
                    PropertiesLoaderUtils.loadProperties(new UrlResource(url));
                // 从 META-INF/spring.factories 中获取 ApplicationContextInitializer 
				String factoryClassNames = properties.getProperty(factoryClassName);
			
// 保存到 result 中
               result.addAll(Arrays.asList(StringUtils.commaDelimitedListToStringArray(factoryClassNames)));
			}
            // 将 result 返回
			return result;
		}
	}
```



### 2、总结 new SpringApplication

1. 将我们传入的配置类都添加到 this.sources 中 
2. this.webEnvironment =  true  判断是否是 web 环境
3. 从 META-INF/spring.factories 获取所有的 ApplicationContextInitializer.class 并将其实例化存放在  this.initializers
4. 从 META-INF/spring.factories 获取所有的 ApplicationListener.class 并将其实例化存放在 this.listeners
5. 从当前运行的堆栈中找到有main方法的主配置类 并将其保存到  this.mainApplicationClass  
6. this 代表 SpringApplication   
7. 我们知道了getSpringFactoriesInstances() 就是从 META-INF/spring.factories 获取相应class ，然后再将其实例化



## 2、然后我们再来看下  SpringApplication.run() 干了什么

```java
public static ConfigurableApplicationContext run(Object[] sources, String[] args) {
		return new SpringApplication(sources).run(args);
	}
```

```java
SpringApplication

public ConfigurableApplicationContext run(String... args) {
    	// 内部使用了 this.startTimeMillis = System.currentTimeMillis();
		StopWatch stopWatch = new StopWatch();
    	// 开始计时
		stopWatch.start();
		ConfigurableApplicationContext context = null;
		FailureAnalyzers analyzers = null;
    	// 配置 java.awt.headless 相关的 我们不在意
		configureHeadlessProperty();
    	// 我们进入看下
    /*
    	private SpringApplicationRunListeners getRunListeners(String[] args) {
		Class<?>[] types = new Class<?>[] { SpringApplication.class, String[].class };
		// 从 META-INF/spring.factories 获取 SpringApplicationRunListener.class 将其实例化
		// 将其存入 SpringApplicationRunListeners 的 this.listeners
		return new SpringApplicationRunListeners(logger, getSpringFactoriesInstances(
				SpringApplicationRunListener.class, types, this, args));
	}
    */
		SpringApplicationRunListeners listeners = getRunListeners(args);
		// 执行所有的 this.listeners 也就是 SpringApplicationRunListener.class 
		// 的  listener.starting(); 方法
		listeners.starting();
		try {
			//封装命令行参数
			ApplicationArguments applicationArguments = new DefaultApplicationArguments(
					args);
			//准备环境
			ConfigurableEnvironment environment = prepareEnvironment(listeners,
					applicationArguments);
					//创建环境完成后回调SpringApplicationRunListener.environmentPrepared()；表示环境准备完成
			// 就是我们的那个 图标。创建完成就会打印 SpringBoot 专属图标
			Banner printedBanner = printBanner(environment);
			// 根据 this.webEnvironment 判断是否是 web 环境 、
			// 根据是否是 web 环境返回不同的 ioc 对象
           // 返回 AnnotationConfigEmbeddedWebApplicationContext 实例
			context = createApplicationContext();
			// 故障分析器，在 method 的 catch 里面会用到
			analyzers = new FailureAnalyzers(context);
			// 调用 SpringApplicationRunListener.initialize()
			prepareContext(context, environment, listeners, applicationArguments,
					printedBanner);
			// 调用 ioc 的 refresh 开始初始化 beanFactory 什么的
			refreshContext(context);
			//从ioc容器中获取所有的ApplicationRunner和CommandLineRunner进行回调
       		//ApplicationRunner先回调，CommandLineRunner再回调
			afterRefresh(context, applicationArguments);
			// SpringApplicationRunListeners.finished(context, null);
			// //所有的SpringApplicationRunListener回调finished方法
			listeners.finished(context, null);
			// 计时停止
			stopWatch.stop();
			if (this.logStartupInfo) {
				new StartupInfoLogger(this.mainApplicationClass)
						.logStarted(getApplicationLog(), stopWatch);
			}
			// 将 ioc 容器返回
			return context;
		}
		catch (Throwable ex) {
			handleRunFailure(context, listeners, analyzers, ex);
			throw new IllegalStateException(ex);
		}
	}


```

### 1、prepareEnvironment();

```java
SpringApplication
private ConfigurableEnvironment prepareEnvironment(
			SpringApplicationRunListeners listeners,
			ApplicationArguments applicationArguments) {
		// Create and configure the environment
		ConfigurableEnvironment environment = getOrCreateEnvironment();
		configureEnvironment(environment, applicationArguments.getSourceArgs());
    	// 主要看这里  SpringApplicationRunListeners.environmentPrepared(environment);
		listeners.environmentPrepared(environment);
		if (!this.webEnvironment) {
			environment = new EnvironmentConverter(getClassLoader())
					.convertToStandardEnvironmentIfNecessary(environment);
		}
		return environment;
	}

public void environmentPrepared(ConfigurableEnvironment environment) {
		for (SpringApplicationRunListener listener : this.listeners) {
            // 执行所有的 this.listeners 也就是 SpringApplicationRunListener.class 
		// 的  SpringApplicationRunListener.environmentPrepared(environment); 方法
			listener.environmentPrepared(environment);
		}
	}
```

### 2、createApplicationContext()

```java
org.springframework.boot.SpringApplication#createApplicationContext

protected ConfigurableApplicationContext createApplicationContext() {
		Class<?> contextClass = this.applicationContextClass;
		if (contextClass == null) {
			try {
			// true 是 web 环境
			// DEFAULT_WEB_CONTEXT_CLASS = AnnotationConfigEmbeddedWebApplicationContext
			//  DEFAULT_CONTEXT_CLASS = AnnotationConfigApplicationContext
				contextClass = Class.forName(this.webEnvironment
						? DEFAULT_WEB_CONTEXT_CLASS : DEFAULT_CONTEXT_CLASS);
			}
			catch (ClassNotFoundException ex) {
				throw new IllegalStateException(
						"Unable create a default ApplicationContext, "
								+ "please specify an ApplicationContextClass",
						ex);
			}
		}
		// 将其实例化返回
		return (ConfigurableApplicationContext) BeanUtils.instantiate(contextClass);
	}
```

### 3、prepareContext();

```java
org.springframework.boot.SpringApplication#prepareContext

private void prepareContext(ConfigurableApplicationContext context,
			ConfigurableEnvironment environment, SpringApplicationRunListeners listeners,
			ApplicationArguments applicationArguments, Banner printedBanner) {
		context.setEnvironment(environment);
		postProcessApplicationContext(context);
    	// 主要这里 initializer.initialize(context);
    // 将 SpringApplication 的 this.initializers 循环调用 initialize():
    // 也就是 ApplicationContextInitializer.initialize()；
		applyInitializers(context);
    // 调用 SpringApplicationRunListeners 的 contextPrepared;
    // 内部 SpringApplicationRunListener.contextPrepared(context);
		listeners.contextPrepared(context);
		if (this.logStartupInfo) {
			logStartupInfo(context.getParent() == null);
			logStartupProfileInfo(context);
		}
    // Add boot specific singleton beans
		context.getBeanFactory().registerSingleton("springApplicationArguments",
				applicationArguments);
		if (printedBanner != null) {
			context.getBeanFactory().registerSingleton("springBootBanner", printedBanner);
		}

		// Load the sources
		Set<Object> sources = getSources();
		Assert.notEmpty(sources, "Sources must not be empty");
		load(context, sources.toArray(new Object[sources.size()]));
    	// 调用 SpringApplicationRunListener.contextLoaded();
		listeners.contextLoaded(context);
	}
    
    
    
    protected void applyInitializers(ConfigurableApplicationContext context) {
        //  this.initializers
		for (ApplicationContextInitializer initializer : getInitializers()) {
			Class<?> requiredType = GenericTypeResolver.resolveTypeArgument(
					initializer.getClass(), ApplicationContextInitializer.class);
			Assert.isInstanceOf(requiredType, context, "Unable to call initializer.");
            // ApplicationContextInitializer.initialize()；
			initializer.initialize(context);
		}
	}
    
    public void contextPrepared(ConfigurableApplicationContext context) {
		for (SpringApplicationRunListener listener : this.listeners) {
            // SpringApplicationRunListener.contextPrepared(context);
			listener.contextPrepared(context);
		}
	}
    
    public void contextLoaded(ConfigurableApplicationContext context) {
		for (SpringApplicationRunListener listener : this.listeners) {
            // SpringApplicationRunListener.contextLoaded();
			listener.contextLoaded(context);
		}
	}
```

### 4、afterRefresh();

```java
protected void afterRefresh(ConfigurableApplicationContext context,
			ApplicationArguments args) {
		callRunners(context, args);
	}

private void callRunners(ApplicationContext context, ApplicationArguments args) {
		List<Object> runners = new ArrayList<Object>();
    	// 从 ApplicationContext 里面获取 ApplicationRunner; CommandLineRunner
		runners.addAll(context.getBeansOfType(ApplicationRunner.class).values());
		runners.addAll(context.getBeansOfType(CommandLineRunner.class).values());
		AnnotationAwareOrderComparator.sort(runners);
		for (Object runner : new LinkedHashSet<Object>(runners)) {
            // 调用 ApplicationRunner.run(args);
			if (runner instanceof ApplicationRunner) {
				callRunner((ApplicationRunner) runner, args);
			}
			if (runner instanceof CommandLineRunner) {
                 // 调用 CommandLineRunner.run(args);
				callRunner((CommandLineRunner) runner, args);
			}
		}
	}
```

### 5、listeners.finished(context, null);

```java
public void finished(ConfigurableApplicationContext context, Throwable exception) {
		for (SpringApplicationRunListener listener : this.listeners) {
            // 调用 SpringApplicationRunListener.finished();
			callFinishedListener(listener, context, exception);
		}
	}
private void callFinishedListener(SpringApplicationRunListener listener,
			ConfigurableApplicationContext context, Throwable exception) {
		try {
			listener.finished(context, exception);
		}
	}
```

### 6、总结 SpringApplication.run();

```
1.stopWatch.start(); 开始计时 stopWatch.start();
2.getRunListeners(args); 从 META-INF/spring.factories 获取 		   
  SpringApplicationRunListener.class 将其实例化，
  并将其存入 SpringApplicationRunListeners 的 this.listeners
3. listeners.starting();执行 SpringApplicationRunListener.starting();方法
4. prepareEnvironment();准备环境,在其准备完成后调用 	
	SpringApplicationRunListener.environmentPrepared(environment); 
5. createApplicationContext(); 根据是否是 web 环境返回不同的 ioc 对象
6.prepareContext();
		调用 ApplicationContextInitializer.initialize()；
         调用 SpringApplicationRunListener.contextPrepared(context);
         调用 SpringApplicationRunListener.contextLoaded();
7. refreshContext(context); 调用 ioc 的 refresh 开始初始化
8. afterRefresh(); 从ioc容器中获取所有的ApplicationRunner和CommandLineRunner进行回调
        ApplicationRunner先回调，CommandLineRunner再回调
        ApplicationRunner.run(args);
        CommandLineRunner..run(args.getSourceArgs());

9. listeners.finished();调用 SpringApplicationRunListener 回调 finished方法	
10. stopWatch.stop(); 计时停止 
11.返回 ioc 容器

```

# 八、自定义starter

starter：

​	1、这个场景需要使用到的依赖是什么？

​	2、如何编写自动配置

```java
@Configuration  //指定这个类是一个配置类
@ConditionalOnXXX  //在指定条件成立的情况下自动配置类生效
@AutoConfigureAfter  //指定自动配置类的顺序
@Bean  //给容器中添加组件

@ConfigurationPropertie结合相关xxxProperties类来绑定相关的配置
@EnableConfigurationProperties //让xxxProperties生效加入到容器中

自动配置类要能加载
将需要启动就加载的自动配置类，配置在META-INF/spring.factories
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
org.springframework.boot.autoconfigure.admin.SpringApplicationAdminJmxAutoConfiguration,\
org.springframework.boot.autoconfigure.aop.AopAutoConfiguration,\
```

​	3、模式：

启动器只用来做依赖导入；

专门来写一个自动配置模块；

启动器依赖自动配置；别人只需要引入启动器（starter）

mybatis-spring-boot-starter；自定义启动器名-spring-boot-starter



步骤：

1）、启动器模块

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.atguigu.starter</groupId>
    <artifactId>atguigu-spring-boot-starter</artifactId>
    <version>1.0-SNAPSHOT</version>

    <!--启动器-->
    <dependencies>

        <!--引入自动配置模块-->
        <dependency>
            <groupId>com.atguigu.starter</groupId>
            <artifactId>atguigu-spring-boot-starter-autoconfigurer</artifactId>
            <version>0.0.1-SNAPSHOT</version>
        </dependency>
    </dependencies>

</project>
```

2）、自动配置模块

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
   xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
   <modelVersion>4.0.0</modelVersion>

   <groupId>com.atguigu.starter</groupId>
   <artifactId>atguigu-spring-boot-starter-autoconfigurer</artifactId>
   <version>0.0.1-SNAPSHOT</version>
   <packaging>jar</packaging>

   <name>atguigu-spring-boot-starter-autoconfigurer</name>
   <description>Demo project for Spring Boot</description>

   <parent>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-parent</artifactId>
      <version>1.5.10.RELEASE</version>
      <relativePath/> <!-- lookup parent from repository -->
   </parent>

   <properties>
      <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
      <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
      <java.version>1.8</java.version>
   </properties>

   <dependencies>

      <!--引入spring-boot-starter；所有starter的基本配置-->
      <dependency>
         <groupId>org.springframework.boot</groupId>
         <artifactId>spring-boot-starter</artifactId>
      </dependency>

   </dependencies>



</project>

```



```java
package com.atguigu.starter;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "atguigu.hello")
public class HelloProperties {

    private String prefix;
    private String suffix;

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }
}

```

```java
package com.atguigu.starter;

public class HelloService {

    HelloProperties helloProperties;

    public HelloProperties getHelloProperties() {
        return helloProperties;
    }

    public void setHelloProperties(HelloProperties helloProperties) {
        this.helloProperties = helloProperties;
    }

    public String sayHellAtguigu(String name){
        return helloProperties.getPrefix()+"-" +name + helloProperties.getSuffix();
    }
}

```

```java
package com.atguigu.starter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnWebApplication //web应用才生效
@EnableConfigurationProperties(HelloProperties.class)
public class HelloServiceAutoConfiguration {

    @Autowired
    HelloProperties helloProperties;
    @Bean
    public HelloService helloService(){
        HelloService service = new HelloService();
        service.setHelloProperties(helloProperties);
        return service;
    }
}

```

# 更多SpringBoot整合示例

https://github.com/spring-projects/spring-boot/tree/master/spring-boot-samples

