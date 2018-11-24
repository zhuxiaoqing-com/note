一、Tomcat和 SpringBoot 集成的源码

首先 Tomcat 是通过

​	EmbeddedServletContainerAutoConfiguration 进行注入的

```java
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@Configuration
@ConditionalOnWebApplication
@Import(BeanPostProcessorsRegistrar.class)
public class EmbeddedServletContainerAutoConfiguration {

	@Configuration
    // 如果有  Servlet.class, Tomcat.class 就注入
	@ConditionalOnClass({ Servlet.class, Tomcat.class })
    // 如果没有 EmbeddedServletContainerFactory 就注入
	@ConditionalOnMissingBean(value = EmbeddedServletContainerFactory.class, search = SearchStrategy.CURRENT)
	public static class EmbeddedTomcat {
		// 注入 TomcatEmbeddedServletContainerFactory 工厂
		@Bean
		public TomcatEmbeddedServletContainerFactory tomcatEmbeddedServletContainerFactory() {
			return new TomcatEmbeddedServletContainerFactory();
		}

	}

	public static class BeanPostProcessorsRegistrar
			implements ImportBeanDefinitionRegistrar, BeanFactoryAware {

		private ConfigurableListableBeanFactory beanFactory;

		@Override
		public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
			if (beanFactory instanceof ConfigurableListableBeanFactory) {
				this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
			}
		}

		@Override
		public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
				BeanDefinitionRegistry registry) {
			if (this.beanFactory == null) {
				return;
			}
			registerSyntheticBeanIfMissing(registry,
					"embeddedServletContainerCustomizerBeanPostProcessor",
					EmbeddedServletContainerCustomizerBeanPostProcessor.class);
			registerSyntheticBeanIfMissing(registry,
					"errorPageRegistrarBeanPostProcessor",
					ErrorPageRegistrarBeanPostProcessor.class);
		}

		private void registerSyntheticBeanIfMissing(BeanDefinitionRegistry registry,
				String name, Class<?> beanClass) {
			if (ObjectUtils.isEmpty(
					this.beanFactory.getBeanNamesForType(beanClass, true, false))) {
				RootBeanDefinition beanDefinition = new RootBeanDefinition(beanClass);
				beanDefinition.setSynthetic(true);
				registry.registerBeanDefinition(name, beanDefinition);
			}
		}

	}

}
```



```java
public class TomcatEmbeddedServletContainerFactory
		extends AbstractEmbeddedServletContainerFactory implements ResourceLoaderAware {

	@Override
	public EmbeddedServletContainer getEmbeddedServletContainer(
			ServletContextInitializer... initializers) {
        // 创建一个 Tomcat 
		Tomcat tomcat = new Tomcat();
        // C:\Users\a\AppData\Local\Temp\tomcat.3108396118394218520.8080
        // 应该是 Tomcat 的缓存目录
		File baseDir = (this.baseDirectory != null ? this.baseDirectory
				: createTempDir("tomcat"));
        // 设置
		tomcat.setBaseDir(baseDir.getAbsolutePath());
		Connector connector = new Connector(this.protocol);
		tomcat.getService().addConnector(connector);
		customizeConnector(connector);
		tomcat.setConnector(connector);
		tomcat.getHost().setAutoDeploy(false);
		configureEngine(tomcat.getEngine());
		for (Connector additionalConnector : this.additionalTomcatConnectors) {
			tomcat.getService().addConnector(additionalConnector);
		}
		prepareContext(tomcat.getHost(), initializers);
        // 主要看这里
		return getTomcatEmbeddedServletContainer(tomcat);
	}

    
    protected TomcatEmbeddedServletContainer getTomcatEmbeddedServletContainer(
			Tomcat tomcat) {
         // 如果 getPort 大于 0 就创建 TomcatEmbeddedServletContainer
		return new TomcatEmbeddedServletContainer(tomcat, getPort() >= 0);
	}
    
    org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainer#TomcatEmbeddedServletContainer(org.apache.catalina.startup.Tomcat, boolean)
        
      public TomcatEmbeddedServletContainer(Tomcat tomcat, boolean autoStart) {
		Assert.notNull(tomcat, "Tomcat Server must not be null");
		this.tomcat = tomcat;
		this.autoStart = autoStart;
        // 进行初始化
		initialize();
	}
  
    private void initialize() throws EmbeddedServletContainerException {
		TomcatEmbeddedServletContainer.logger
				.info("Tomcat initialized with port(s): " + getPortsDescription(false));
		synchronized (this.monitor) {
			try {
				addInstanceIdToEngineName();
				try {
					// Remove service connectors to that protocol binding doesn't happen
					// yet
					removeServiceConnectors();

					// Start the server to trigger initialization listeners
                    // 启动 tomcat 
					this.tomcat.start();

					// We can re-throw failure exception directly in the main thread
					rethrowDeferredStartupExceptions();

					Context context = findContext();
					try {
						ContextBindings.bindClassLoader(context, getNamingToken(context),
								getClass().getClassLoader());
					}
					catch (NamingException ex) {
						// Naming is not enabled. Continue
					}

					// Unlike Jetty, all Tomcat threads are daemon threads. We create a
					// blocking non-daemon to stop immediate shutdown
					startDaemonAwaitThread();
				}
				catch (Exception ex) {
					containerCounter.decrementAndGet();
					throw ex;
				}
			}
			catch (Exception ex) {
				throw new EmbeddedServletContainerException(
						"Unable to start embedded Tomcat", ex);
			}
		}
	}
    
```





我们再来看下 SpringBoot  怎么启动的



```java
@SpringBootApplication
public class SpringBoot04WebRestfulcrudApplication {

	public static void main(String[] args) {
        // 进入
		SpringApplication.run(SpringBoot04WebRestfulcrudApplication.class, args);
	}
}

org.springframework.context.ConfigurableApplicationContext
public static ConfigurableApplicationContext run(Object source, String... args) {
		return run(new Object[] { source }, args);
	}
	
public static ConfigurableApplicationContext run(Object[] sources, String[] args) {
		return new SpringApplication(sources).run(args);
	}

```

```java
	public ConfigurableApplicationContext run(String... args) {
     // 简单的秒表，允许执行一些任务的时间，为每个指定的任务暴露总运行时间和运行时间。
	// 隐藏使用system.currenttimemillis（），提高应用程序代码的可读性，减少计算错误的可能性。
	// 注意，这个对象不是设计为线程安全的，并且不使用同步。
	// 这个类通常用于验证概念验证和开发期间的性能，而不是作为生产应用程序的一部分
		StopWatch stopWatch = new StopWatch();
        // 本质就是 system.currenttimemillis()
		stopWatch.start();
		ConfigurableApplicationContext context = null;
		FailureAnalyzers analyzers = null;
        /* SYSTEM_PROPERTY_JAVA_AWT_HEADLESS = "java.awt.headless";
        	private void configureHeadlessProperty() {
                System.setProperty(SYSTEM_PROPERTY_JAVA_AWT_HEADLESS, System.getProperty(
                        SYSTEM_PROPERTY_JAVA_AWT_HEADLESS, 					 
                        Boolean.toString(this.headless)));
            }
        */
        // 为 系统设置一些属性
		configureHeadlessProperty();
        // 创建 Listeners SpringAppliction 监听器
		SpringApplicationRunListeners listeners = getRunListeners(args);
		listeners.starting();
		try {
			ApplicationArguments applicationArguments = new DefaultApplicationArguments(
					args);
            // 环境变量
			ConfigurableEnvironment environment = prepareEnvironment(listeners,
					applicationArguments);
			Banner printedBanner = printBanner(environment);
            // "org.springframework."
			+ "boot.context.embedded.AnnotationConfigEmbeddedWebApplicationContext";
            // 强转成 ConfigurableApplicationContext 返回
			context = createApplicationContext();
			analyzers = new FailureAnalyzers(context);
            // 预处理  Context 环境上下文
			prepareContext(context, environment, listeners, applicationArguments,
					printedBanner);
            // 刷新 refreshContext
			refreshContext(context);
            // 进入 这里就调用了 以前的方法
			afterRefresh(context, applicationArguments);
			listeners.finished(context, null);
			stopWatch.stop();
			if (this.logStartupInfo) {
				new StartupInfoLogger(this.mainApplicationClass)
						.logStarted(getApplicationLog(), stopWatch);
			}
			return context;
		}
		catch (Throwable ex) {
			handleRunFailure(context, listeners, analyzers, ex);
			throw new IllegalStateException(ex);
		}
	}

private void refreshContext(ConfigurableApplicationContext context) {
		refresh(context);
		if (this.registerShutdownHook) {
			try {
				context.registerShutdownHook();
			}
			catch (AccessControlException ex) {
				// Not allowed in some environments.
			}
		}
	}

protected void refresh(ApplicationContext applicationContext) {
		Assert.isInstanceOf(AbstractApplicationContext.class, applicationContext);
		((AbstractApplicationContext) applicationContext).refresh();
	}


org.springframework.context.support.AbstractApplicationContext#refresh

public void refresh() throws BeansException, IllegalStateException {
		synchronized (this.startupShutdownMonitor) {
			// Prepare this context for refreshing.
			prepareRefresh();

			// Tell the subclass to refresh the internal bean factory.
			ConfigurableListableBeanFactory beanFactory = obtainFreshBeanFactory();

			// Prepare the bean factory for use in this context.
			prepareBeanFactory(beanFactory);

			try {
				// Allows post-processing of the bean factory in context subclasses.
				postProcessBeanFactory(beanFactory);
				// 首先 SpringBoot 大部分的设置都是通过 BeanFactoryPostProcessor 来进行 Registry
				// Invoke factory processors registered as beans in the context.
				invokeBeanFactoryPostProcessors(beanFactory);

				// Register bean processors that intercept bean creation.
				registerBeanPostProcessors(beanFactory);

				// Initialize message source for this context.
				initMessageSource();

				// Initialize event multicaster for this context.
				initApplicationEventMulticaster();

				// Initialize other special beans in specific context subclasses.
				// 而我们的 TOmcat 是通过这里进行初始化的 进入
				onRefresh();

				// Check for listener beans and register them.
				registerListeners();

				// Instantiate all remaining (non-lazy-init) singletons.
				finishBeanFactoryInitialization(beanFactory);

				// Last step: publish corresponding event.
				finishRefresh();
			}
	}
	
	org.springframework.boot.context.embedded.EmbeddedWebApplicationContext#onRefresh
	
	protected void onRefresh() {
		super.onRefresh();
		try {
			// 创建 EmbeddedServletContainer
			createEmbeddedServletContainer();
		}
		catch (Throwable ex) {
			throw new ApplicationContextException("Unable to start embedded container",
					ex);
		}
	}
	
	private void createEmbeddedServletContainer() {
 		// null this.embeddedServletContainer
		EmbeddedServletContainer localContainer = this.embeddedServletContainer;
		// null  this.servletContext;
		ServletContext localServletContext = getServletContext();
		if (localContainer == null && localServletContext == null) {
		// 通过 getEmbeddedServletContainerFactory 来获取 就来到了上面的 Tomcat 初始化了
			EmbeddedServletContainerFactory containerFactory = getEmbeddedServletContainerFactory();
			this.embeddedServletContainer = containerFactory
					.getEmbeddedServletContainer(getSelfInitializer());
		}
		else if (localServletContext != null) {
			try {
				getSelfInitializer().onStartup(localServletContext);
			}
			catch (ServletException ex) {
				throw new ApplicationContextException("Cannot initialize servlet context",
						ex);
			}
		}
		initPropertySources();
	}
```







我们再来看下 Tomcat 的属性是怎么配置上去的

```java
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@Configuration
@ConditionalOnWebApplication
// 注入了 BeanPostProcessorsRegistrar注册器
@Import(BeanPostProcessorsRegistrar.class)
public class EmbeddedServletContainerAutoConfiguration {

	@Configuration
    // 如果有  Servlet.class, Tomcat.class 就注入
	@ConditionalOnClass({ Servlet.class, Tomcat.class })
    // 如果没有 EmbeddedServletContainerFactory 就注入
	@ConditionalOnMissingBean(value = EmbeddedServletContainerFactory.class, search = SearchStrategy.CURRENT)
	public static class EmbeddedTomcat {
		// 注入 TomcatEmbeddedServletContainerFactory 工厂
		@Bean
		public TomcatEmbeddedServletContainerFactory tomcatEmbeddedServletContainerFactory() {
			return new TomcatEmbeddedServletContainerFactory();
		}

	}
	// 在这里 继承了 ImportBeanDefinitionRegistrar 所以是在 
	public static class BeanPostProcessorsRegistrar
			implements ImportBeanDefinitionRegistrar, BeanFactoryAware {

		private ConfigurableListableBeanFactory beanFactory;

		@Override
		public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
			if (beanFactory instanceof ConfigurableListableBeanFactory) {
				this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
			}
		}

       // 调用 registerBeanDefinitions 方法
		@Override
		public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
				BeanDefinitionRegistry registry) {
			if (this.beanFactory == null) {
				return;
			}
            // 注册这个 EmbeddedServletContainerCustomizerBeanPostProcessor
			registerSyntheticBeanIfMissing(registry,
					"embeddedServletContainerCustomizerBeanPostProcessor",
					EmbeddedServletContainerCustomizerBeanPostProcessor.class);
			registerSyntheticBeanIfMissing(registry,
					"errorPageRegistrarBeanPostProcessor",
					ErrorPageRegistrarBeanPostProcessor.class);
		}

		private void registerSyntheticBeanIfMissing(BeanDefinitionRegistry registry,
				String name, Class<?> beanClass) {
			if (ObjectUtils.isEmpty(
					this.beanFactory.getBeanNamesForType(beanClass, true, false))) {
				RootBeanDefinition beanDefinition = new RootBeanDefinition(beanClass);
				beanDefinition.setSynthetic(true);
				registry.registerBeanDefinition(name, beanDefinition);
			}
		}

	}

}
```

EmbeddedServletContainerCustomizerBeanPostProcessor

```java
public class EmbeddedServletContainerCustomizerBeanPostProcessor
		implements BeanPostProcessor, BeanFactoryAware {

	private ListableBeanFactory beanFactory;

	private List<EmbeddedServletContainerCustomizer> customizers;

	// 后置处理器的第一个方法
	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName)
			throws BeansException {
        // 如果实例化好的 bean 是 ConfigurableEmbeddedServletContainer类型的
        // TomcatEmbeddedServletContainer 就是 ConfigurableEmbeddedServletContainer类型的 
		if (bean instanceof ConfigurableEmbeddedServletContainer) {
            // 进入这里
			postProcessBeforeInitialization((ConfigurableEmbeddedServletContainer) bean);
		}
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName)
			throws BeansException {
		return bean;
	}
	// 进入这里
	private void postProcessBeforeInitialization(
			ConfigurableEmbeddedServletContainer bean) {
        	// getCustomizers() 就是从 beanFactory 获取所有的 EmbeddedServletContainerCustomizer.class,
        // 类型的，所有的 EmbeddedServletContainerCustomizer 都是 Tomcat 的配置类
		for (EmbeddedServletContainerCustomizer customizer : getCustomizers()) {
            // 运行方法，传入 ConfigurableEmbeddedServletContainer 
			// 然后就会调用 EmbeddedServletContainerCustomizer 的方法运行我们自定义的属性和
            // 主要就是
            /*
            public class ServerProperties
		implements EmbeddedServletContainerCustomizer, EnvironmentAware, Ordered {}类
            */
            // ConfigurableEmbeddedServletContainer 会调用 ConfigurableEmbeddedServletContainer  相应的方法为其赋值
            
			customizer.customize(bean);
		}
	}

	private Collection<EmbeddedServletContainerCustomizer> getCustomizers() {
		if (this.customizers == null) {
			// Look up does not include the parent context
			this.customizers = new ArrayList<EmbeddedServletContainerCustomizer>(
					this.beanFactory
							.getBeansOfType(EmbeddedServletContainerCustomizer.class,
									false, false)
							.values());
			Collections.sort(this.customizers, AnnotationAwareOrderComparator.INSTANCE);
			this.customizers = Collections.unmodifiableList(this.customizers);
		}
		return this.customizers;
	}

}

```





