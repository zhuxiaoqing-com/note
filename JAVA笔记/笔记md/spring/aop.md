# 1、注册 AnnotationAwareAspectAutoProxyCreator

aop 的本质还要从  

@EnableAspectJAutoProxy 看起

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
// 注册了 AspectJAutoProxyRegistrar 
@Import(AspectJAutoProxyRegistrar.class)
public @interface EnableAspectJAutoProxy {
	boolean proxyTargetClass() default false;
	boolean exposeProxy() default false;
}
```

## 1、AspectJAutoProxyRegistrar 

让我们看下  AspectJAutoProxyRegistrar 

```java
class AspectJAutoProxyRegistrar implements ImportBeanDefinitionRegistrar {
	@Override
	public void registerBeanDefinitions(
			AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
		// 注册 aspect 如果有必要的话，从名字我们可以看出来这里注册 Bean
		AopConfigUtils.registerAspectJAnnotationAutoProxyCreatorIfNecessary(registry);

		AnnotationAttributes enableAspectJAutoProxy =
				AnnotationConfigUtils.attributesFor(importingClassMetadata, EnableAspectJAutoProxy.class);
		if (enableAspectJAutoProxy.getBoolean("proxyTargetClass")) {
			AopConfigUtils.forceAutoProxyCreatorToUseClassProxying(registry);
		}
		if (enableAspectJAutoProxy.getBoolean("exposeProxy")) {
			AopConfigUtils.forceAutoProxyCreatorToExposeProxy(registry);
		}
	}
}

public static BeanDefinition registerAspectJAnnotationAutoProxyCreatorIfNecessary(BeanDefinitionRegistry registry) {
		return registerAspectJAnnotationAutoProxyCreatorIfNecessary(registry, null);
	}

	public static BeanDefinition registerAspectJAnnotationAutoProxyCreatorIfNecessary(BeanDefinitionRegistry registry, Object source) {
        // 这里可以看到将  AnnotationAwareAspectJAutoProxyCreator 传入
		return registerOrEscalateApcAsRequired(AnnotationAwareAspectJAutoProxyCreator.class, registry, source);
	}
```

## 2、AopConfigUtils

org.springframework.aop.config.AopConfigUtils

// 这里讲 AnnotationAwareAspectAutoProxyCreator.class 传入 后注册

```java
private static BeanDefinition registerOrEscalateApcAsRequired(Class<?> cls, BeanDefinitionRegistry registry, Object source) {
		Assert.notNull(registry, "BeanDefinitionRegistry must not be null");
    //AUTO_PROXY_CREATOR_BEAN_NAME = org.springframework.aop.config.internalAutoProxyCreator
    // 判断 internalAutoProxyCreator 是否已经注册在工厂里面 没有
		if (registry.containsBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME)) {
			...
			return null;
		}
    // 创建 AnnotationAwareAspectProxyCreater 的 bean 的 definition (定义信息)
		RootBeanDefinition beanDefinition = new RootBeanDefinition(cls);
    // source = null
		beanDefinition.setSource(source);
		beanDefinition.getPropertyValues().add("order", Ordered.HIGHEST_PRECEDENCE);
		beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
    // 将 org.springframework.aop.config.internalAutoProxyCreator 作为 bean 的名称
    // 类型为 AnnotationAwareAspectAutoProxyCreator 注册到 bean 容器中
		registry.registerBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME, beanDefinition);
		return beanDefinition;
	}
```





# 2、创建 proxy 

 **AnnotationAwareAspectAutoProxyCreator**

```java
AnnotationAwareAspectAutoProxyCreator 
	-c AspectJAwareAdvisorAutoProxyCreator
		-a AbstractAdvisorAutoProxyCreator
		-a  AbstractAutoProxyCreator
			-i 	implements SmartInstantiationAwareBeanPostProcessor, BeanFactoryAware
```

## 0、AbstractAutoProxyCreator 

分析  AbstractAutoProxyCreator 主要是这里创建了其 这里重写了其 BeanPostProcess 的 postProcessAfterInitialization 方法

主要调用了一个方法

```java
org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator.postProcessAfterInitialization(Object, String)
@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if (bean != null) {
            // 是 null
			Object cacheKey = getCacheKey(bean.getClass(), beanName);
			if (!this.earlyProxyReferences.contains(cacheKey)) {
                // 如果有需要的话 包装这个对象； 进入
				return wrapIfNecessary(bean, beanName, cacheKey);
			}
		}
		return bean;
	}

protected Object wrapIfNecessary(Object bean, String beanName, Object cacheKey) {
		if (beanName != null && this.targetSourcedBeans.contains(beanName)) {
			return bean;
		}
		if (Boolean.FALSE.equals(this.advisedBeans.get(cacheKey))) {
			return bean;
		}
		if (isInfrastructureClass(bean.getClass()) || shouldSkip(bean.getClass(), beanName)) {
			this.advisedBeans.put(cacheKey, Boolean.FALSE);
			return bean;
		}

    	// 创建代理如果有 
		// Create proxy if we have advice(就是增强方法，比如 before). 进入 
		Object[] specificInterceptors = getAdvicesAndAdvisorsForBean(bean.getClass(), beanName, null);
		if (specificInterceptors != DO_NOT_PROXY) {
			this.advisedBeans.put(cacheKey, Boolean.TRUE);
			Object proxy = createProxy(
					bean.getClass(), beanName, specificInterceptors, new SingletonTargetSource(bean));
			this.proxyTypes.put(cacheKey, proxy.getClass());
			return proxy;
		}

		this.advisedBeans.put(cacheKey, Boolean.FALSE);
		return bean;
	}
```

## 1、获取  Advisor

### 1、AbstractAdvisorAutoProxyCreator

```java
org.springframework.aop.framework.autoproxy.AbstractAdvisorAutoProxyCreator.getAdvicesAndAdvisorsForBean(Class<?>, String, TargetSource)

   // Advices 通知增强方法，比如 before
   // Advisors 将增强方法和切入点(JoinPoint) 适配的增强器 和 Aspect 一样 
    @Override
	protected Object[] getAdvicesAndAdvisorsForBean(Class<?> beanClass, String beanName, TargetSource targetSource) {
		// 查询 合格的 advisors
		List<Advisor> advisors = findEligibleAdvisors(beanClass, beanName);
		if (advisors.isEmpty()) {
			return DO_NOT_PROXY;
		}
		return advisors.toArray();
	}

protected List<Advisor> findEligibleAdvisors(Class<?> beanClass, String beanName) {
// 这里我在 spring原理扩展哪里分析过了就不分析了 简单总结一下
// 1、首先你从 beanFactory 获取所有的 bean。遍历查找标注了 Aspect 注解的 bean
// 2、然后获取标注了 Aspect 注解的 bean 的所有 method。遍历他们是不是 advice 
// 3、如果是 advice 将其 表达式解析出来，封装成一个类。
// 4、然后将相应的 advice 的 method 和 pointCut表达式封装成一个 advisor 
// 5、保存在一个  this.advisorsCache 里面，以 beanName 作为 Key 保存起来
// 6、所有的 Aspect 的 bean 全部放在了 this.aspectBeanNames 里面
		// 这个方法获取了所有的 Advisor
		List<Advisor> candidateAdvisors = findCandidateAdvisors();
    // 从方法的名称来看，是查找匹配 该 bean 的 advisors 进入
		List<Advisor> eligibleAdvisors = findAdvisorsThatCanApply(candidateAdvisors, beanClass, beanName);
    // 这里 主要干了 	advisors.add(0, ExposeInvocationInterceptor.ADVISOR); 
    // 将 ExposeInvocationInterceptor 保存进去了
		extendAdvisors(eligibleAdvisors);
		if (!eligibleAdvisors.isEmpty()) {
			eligibleAdvisors = sortAdvisors(eligibleAdvisors);
		}
		return eligibleAdvisors;
	}

protected List<Advisor> findAdvisorsThatCanApply(
			List<Advisor> candidateAdvisors, Class<?> beanClass, String beanName) {

		ProxyCreationContext.setCurrentProxiedBeanName(beanName);
		try {
            // 看这里 step into
			return AopUtils.findAdvisorsThatCanApply(candidateAdvisors, beanClass);
		}
		finally {
			ProxyCreationContext.setCurrentProxiedBeanName(null);
		}
	}
```

我们先知道 Advisor 是什么类型的

返回的 advisor 是
InstantiationModelAwarePointcutAdvisorImpl 类型的 
    -i InstantiationModelAwarePointcutAdvisor
        -i PointcutAdvisor
            -i Advisor

-i AspectJPrecedenceInformation

```java
org.springframework.aop.support.AopUtils.findAdvisorsThatCanApply(List<Advisor>, Class<?>)
public static List<Advisor> findAdvisorsThatCanApply(List<Advisor> candidateAdvisors, Class<?> clazz) {
    	// 当然我们不是 null 也不为空
		if (candidateAdvisors.isEmpty()) {
			return candidateAdvisors;
		}
    	// 用来存储可以用的 advisor
		List<Advisor> eligibleAdvisors = new LinkedList<Advisor>();
    //	是 InstantiationModelAwarePointcutAdvisor 类型的 false
		for (Advisor candidate : candidateAdvisors) {
			if (candidate instanceof IntroductionAdvisor && canApply(candidate, clazz)) {
				eligibleAdvisors.add(candidate);
			}
		}
    	// 如果 eligibleAdvisors 为空就 为 false
		boolean hasIntroductions = !eligibleAdvisors.isEmpty();
    //	是 InstantiationModelAwarePointcutAdvisor 类型的 false
		for (Advisor candidate : candidateAdvisors) {
			if (candidate instanceof IntroductionAdvisor) {
				// already processed
				continue;
			}
            // 进入
			if (canApply(candidate, clazz, hasIntroductions)) {
                // 如果符合规则，存入
				eligibleAdvisors.add(candidate);
			}
		}
    	// 返回
		return eligibleAdvisors;
	}

public static boolean canApply(Advisor advisor, Class<?> targetClass, boolean hasIntroductions) {
		if (advisor instanceof IntroductionAdvisor) {
			return ((IntroductionAdvisor) advisor).getClassFilter().matches(targetClass);
		}
    // 是 PointcutAdvisor 类型的
		else if (advisor instanceof PointcutAdvisor) {
			PointcutAdvisor pca = (PointcutAdvisor) advisor
              // 将 pointCut 传入，目标 class 还有 hasIntroductions false
              //进入
			return canApply(pca.getPointcut(), targetClass, hasIntroductions);
		}
		else {
			// It doesn't have a pointcut so we assume it applies.
			return true;
		}
	}



public static boolean canApply(Pointcut pc, Class<?> targetClass, boolean hasIntroductions) {
		Assert.notNull(pc, "Pointcut must not be null");
    	// 匹配类。如果类匹配了 继续，不匹配就返回
		if (!pc.getClassFilter().matches(targetClass)) {
			return false;
		}
		// 获取 方法匹配
		MethodMatcher methodMatcher = pc.getMethodMatcher();
    	// false
		if (methodMatcher == MethodMatcher.TRUE) {
			// No need to iterate the methods if we're matching any method anyway...
			return true;
		}

		IntroductionAwareMethodMatcher introductionAwareMethodMatcher = null;
		if (methodMatcher instanceof IntroductionAwareMethodMatcher) {
			introductionAwareMethodMatcher = (IntroductionAwareMethodMatcher) methodMatcher;
		}

		Set<Class<?>> classes = new LinkedHashSet<Class<?>>(ClassUtils.getAllInterfacesForClassAsSet(targetClass));
		classes.add(targetClass);
		for (Class<?> clazz : classes) {
            // 获取类及祖先类和 interface 里面的所有方法
			Method[] methods = ReflectionUtils.getAllDeclaredMethods(clazz);
			for (Method method : methods) {
                // 应该是进行看看是不是匹配 method matches(应该是都是执行一个方法)
				if ((introductionAwareMethodMatcher != null &&
						introductionAwareMethodMatcher.matches(method, targetClass, hasIntroductions)) ||
						methodMatcher.matches(method, targetClass)) {
					return true;
				}
			}
		}

		return false;
	}
```

至此 匹配结束

先判断 类是否匹配，再看 method 是否匹配



## 2、创建 proxy

主要调用了一个方法

```java
org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator.postProcessAfterInitialization(Object, String)
@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if (bean != null) {
            // 是 null
			Object cacheKey = getCacheKey(bean.getClass(), beanName);
			if (!this.earlyProxyReferences.contains(cacheKey)) {
                // 如果有需要的话 包装这个对象； 进入
				return wrapIfNecessary(bean, beanName, cacheKey);
			}
		}
		return bean;
	}

protected Object wrapIfNecessary(Object bean, String beanName, Object cacheKey) {
		if (beanName != null && this.targetSourcedBeans.contains(beanName)) {
			return bean;
		}
		if (Boolean.FALSE.equals(this.advisedBeans.get(cacheKey))) {
			return bean;
		}
		if (isInfrastructureClass(bean.getClass()) || shouldSkip(bean.getClass(), beanName)) {
			this.advisedBeans.put(cacheKey, Boolean.FALSE);
			return bean;
		}

    	// 创建代理如果有 
		// Create proxy if we have advice(就是增强方法，比如 before). 进入 
    	// 返回了 List<Advisor>
		Object[] specificInterceptors = getAdvicesAndAdvisorsForBean(bean.getClass(), beanName, null);
    -------------------------------------------------------------
        // DO_NOT_PROXY = null true
		if (specificInterceptors != DO_NOT_PROXY) {
            //如果有 proxy  将 beanName 存储起来 标记为 true
			this.advisedBeans.put(cacheKey, Boolean.TRUE);
			Object proxy = createProxy(
					bean.getClass(), beanName, specificInterceptors, new SingletonTargetSource(bean));
			this.proxyTypes.put(cacheKey, proxy.getClass());
			return proxy;
		}
		// 如果没有 proxy 将其缓存为 FALSE
		this.advisedBeans.put(cacheKey, Boolean.FALSE);
		return bean;
	}
```

```java
org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator.createProxy(Class<?>, String, Object[], TargetSource)
    
protected Object createProxy(
			Class<?> beanClass, String beanName, Object[] specificInterceptors, TargetSource targetSource) {

		if (this.beanFactory instanceof ConfigurableListableBeanFactory) {
			AutoProxyUtils.exposeTargetClass((ConfigurableListableBeanFactory) this.beanFactory, beanName, beanClass);
		}
		// 创建 ProxyFactory
		ProxyFactory proxyFactory = new ProxyFactory();
		proxyFactory.copyFrom(this);

		if (!proxyFactory.isProxyTargetClass()) {
			if (shouldProxyTargetClass(beanClass, beanName)) {
				proxyFactory.setProxyTargetClass(true);
			}
			else {
                // 如果没有接口将 this.proxyTargetClass = true;
                // 如果有接口将接口添加到 this.interfaces 中
				evaluateProxyInterfaces(beanClass, proxyFactory);
			}
		}
		// 因为我们是 Advisor 类型的，所有什么都没干。。原样返回了 
		Advisor[] advisors = buildAdvisors(beanName, specificInterceptors);
    	// 将 Advisors 设置进去了
		proxyFactory.addAdvisors(advisors);
    	// 将 targetSource  spring 的类
		proxyFactory.setTargetSource(targetSource);
		customizeProxyFactory(proxyFactory);

		proxyFactory.setFrozen(this.freezeProxy);
		if (advisorsPreFiltered()) {
			proxyFactory.setPreFiltered(true);
		}
		// 创建对象返回 进入
		return proxyFactory.getProxy(getProxyClassLoader());
	}
```

```java
org.springframework.aop.framework.ProxyFactory.getProxy(ClassLoader)
    
public Object getProxy(ClassLoader classLoader) {
		return createAopProxy().getProxy(classLoader);
	}

	protected final synchronized AopProxy createAopProxy() {
		if (!this.active) {
			activate();
		}
        // 进入
		return getAopProxyFactory().createAopProxy(this);
	}

```

```java
org.springframework.aop.framework.DefaultAopProxyFactory	
@Override
	public AopProxy createAopProxy(AdvisedSupport config) throws AopConfigException {
	// 因为 config.isProxyTargetClass()  为 false
		if (config.isOptimize() || config.isProxyTargetClass() || hasNoUserSuppliedProxyInterfaces(config)) {
			Class<?> targetClass = config.getTargetClass();
			if (targetClass == null) {
				throw new AopConfigException("TargetSource cannot determine target class: " +
						"Either an interface or a target is required for proxy creation.");
			}
			// targetClass.isInterface() 也是有值的
			if (targetClass.isInterface() || Proxy.isProxyClass(targetClass)) {
				return new JdkDynamicAopProxy(config);
			}
			return new ObjenesisCglibAopProxy(config);
		}
		// 所以运行了 这里创建了 JdkDynamicAopProxy 返回
		else {
			return new JdkDynamicAopProxy(config);
		}
	}

```



# 3、执行

首先因为我们是有接口的所以来到了  

org.springframework.aop.framework.JdkDynamicAopProxy.invoke(Object, Method, Object[])

```java
	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		MethodInvocation invocation;
		Object oldProxy = null;
		boolean setProxyContext = false;

		TargetSource targetSource = this.advised.targetSource;
		Class<?> targetClass = null;
		Object target = null;

		try {
			if (!this.equalsDefined && AopUtils.isEqualsMethod(method)) {
				// The target does not implement the equals(Object) method itself.
                // 目标没有实现 equals 本身
				return equals(args[0]);
			}
			else if (!this.hashCodeDefined && AopUtils.isHashCodeMethod(method)) {
				// The target does not implement the hashCode() method itself.
                // 目标没有实现 hashCode 本身
				return hashCode();
			}
			else if (method.getDeclaringClass() == DecoratingProxy.class) {
				// There is only getDecoratedClass() declared -> dispatch to proxy config.
				return AopProxyUtils.ultimateTargetClass(this.advised);
			}
			else if (!this.advised.opaque && method.getDeclaringClass().isInterface() &&
					method.getDeclaringClass().isAssignableFrom(Advised.class)) {
               //使用代理配置在ProxyConfig上的服务调用。
				// Service invocations on ProxyConfig with the proxy config...
				return AopUtils.invokeJoinpointUsingReflection(this.advised, method, args);
			}

			Object retVal;

			if (this.advised.exposeProxy) {
				// Make invocation available if necessary.
				oldProxy = AopContext.setCurrentProxy(proxy);
				setProxyContext = true;
			}

			// May be null. Get as late as possible to minimize the time we "own" the target,
			// in case it comes from a pool.
			target = targetSource.getTarget();
			if (target != null) {
                // 不为 null
				targetClass = target.getClass();
			}
// 前面的反正都没有进入 好像都没有什么软用
        // this.advised 
 /*
   // Config used to configure this proxy 
	private final AdvisedSupport advised;
	public JdkDynamicAopProxy(AdvisedSupport config) throws AopConfigException { 
		...		
		this.advised = config;
	}
	经过分析 advised 就是 ProxyFactory 是 AdvisedSupport 的子类

 */
            
			// Get the interception chain for this method.
            // 获取执行链 进入  >>1
			List<Object> chain = this.advised.getInterceptorsAndDynamicInterceptionAdvice(method, targetClass);

			// Check whether we have any advice. If we don't, we can fallback on direct
			// reflective invocation of the target, and avoid creating a MethodInvocation.
			if (chain.isEmpty()) {
				// We can skip creating a MethodInvocation: just invoke the target directly
				// Note that the final invoker must be an InvokerInterceptor so we know it does
				// nothing but a reflective operation on the target, and no hot swapping or fancy proxying.
				Object[] argsToUse = AopProxyUtils.adaptArgumentsIfNecessary(method, args);
				retVal = AopUtils.invokeJoinpointUsingReflection(target, method, argsToUse);
			}
			else {
				// We need to create a method invocation...
				invocation = new ReflectiveMethodInvocation(proxy, target, method, args, targetClass, chain);
				// Proceed to the joinpoint through the interceptor chain.
				retVal = invocation.proceed();
			}

			// Massage return value if necessary.
			Class<?> returnType = method.getReturnType();
			if (retVal != null && retVal == target &&
					returnType != Object.class && returnType.isInstance(proxy) &&
					!RawTargetAccess.class.isAssignableFrom(method.getDeclaringClass())) {
				// Special case: it returned "this" and the return type of the method
				// is type-compatible. Note that we can't help if the target sets
				// a reference to itself in another returned object.
				retVal = proxy;
			}
			else if (retVal == null && returnType != Void.TYPE && returnType.isPrimitive()) {
				throw new AopInvocationException(
						"Null return value from advice does not match primitive return type for: " + method);
			}
			return retVal;
		}
		finally {
			if (target != null && !targetSource.isStatic()) {
				// Must have come from TargetSource.
				targetSource.releaseTarget(target);
			}
			if (setProxyContext) {
				// Restore old proxy.
				AopContext.setCurrentProxy(oldProxy);
			}
		}
	}

```

## 1、 获取执行链

List<Object> chain = this.advised.getInterceptorsAndDynamicInterceptionAdvice(method, targetClass);

```java
org.springframework.aop.framework.AdvisedSupport.getInterceptorsAndDynamicInterceptionAdvice(Method, Class<?>)
public List<Object> getInterceptorsAndDynamicInterceptionAdvice(Method method, Class<?> targetClass) {
    	// 创键一个  MethodCacheKey
		MethodCacheKey cacheKey = neSw MethodCacheKey(method);
    /** Cache with Method as key and advisor chain List as value */
	// private transient Map<MethodCacheKey, List<Object>> methodCache;
    //	this.methodCache 是一个 以 MethodCacheKey 为key,
    // method 拥有的 advisor chain List 作为缓存
		List<Object> cached = this.methodCache.get(cacheKey);
    // 如果没有获取到的话
		if (cached == null) {
           // 进入
			cached = this.advisorChainFactory.getInterceptorsAndDynamicInterceptionAdvice(
					this, method, targetClass);
            // 将获取的 interceptor 缓存起来
			this.methodCache.put(cacheKey, cached);
		}
    	// 返回
		return cached;
	}
```

我们先知道 Advisor 是什么类型的

返回的 advisor 是
InstantiationModelAwarePointcutAdvisorImpl 类型的 
    -i InstantiationModelAwarePointcutAdvisor
        -i PointcutAdvisor
            -i Advisor

-i AspectJPrecedenceInformation

```java
org.springframework.aop.framework.DefaultAdvisorChainFactory.getInterceptorsAndDynamicInterceptionAdvice(Advised, Method, Class<?>)
@Override
	public List<Object> getInterceptorsAndDynamicInterceptionAdvice(
			Advised config, Method method, Class<?> targetClass) {

		// This is somewhat tricky... We have to process introductions first,
		// but we need to preserve order in the ultimate list.
    // 创建一个list 来缓存 interceptor
		List<Object> interceptorList = new ArrayList<Object>(config.getAdvisors().length);
		Class<?> actualClass = (targetClass != null ? targetClass : method.getDeclaringClass());
    // 判断 advisor 是否是 IntroductionAdvisor 类型的 不是 false
		boolean hasIntroductions = hasMatchingIntroductions(config, actualClass);
    // 创建一个 advisor 的适配注册器
		AdvisorAdapterRegistry registry = GlobalAdvisorAdapterRegistry.getInstance();
		// 获取所有的 getAdvisors 
		for (Advisor advisor : config.getAdvisors()) {
			if (advisor instanceof PointcutAdvisor) {
				// Add it conditionally.
				PointcutAdvisor pointcutAdvisor = (PointcutAdvisor) advisor;
  // config.isPreFiltered() 是否匹配代理的目标类 true
  // pointcutAdvisor.getPointcut().getClassFilter().matches(actualClass)
  // 匹配
			if (config.isPreFiltered() || pointcutAdvisor.getPointcut().getClassFilter().matches(actualClass)) {
 // 通过 registry.getInterceptors(advisor); 来获取 进入 ~>>1
	MethodInterceptor[] interceptors = registry.getInterceptors(advisor);
	MethodMatcher mm = pointcutAdvisor.getPointcut().getMethodMatcher();
					if (MethodMatchers.matches(mm, method, actualClass, hasIntroductions)) {
                        // false
						if (mm.isRuntime()) {
							// Creating a new object instance in the getInterceptors() method
							// isn't a problem as we normally cache created chains.
							for (MethodInterceptor interceptor : interceptors) {
								interceptorList.add(new InterceptorAndDynamicMethodMatcher(interceptor, mm));
							}
						}
						else {
                            // 存入 interceptorList 里面
							interceptorList.addAll(Arrays.asList(interceptors));
						}
					}
				}
			}
            // 不会进入
			else if (advisor instanceof IntroductionAdvisor) {
				IntroductionAdvisor ia = (IntroductionAdvisor) advisor;
				if (config.isPreFiltered() || ia.getClassFilter().matches(actualClass)) {
					Interceptor[] interceptors = registry.getInterceptors(advisor);
					interceptorList.addAll(Arrays.asList(interceptors));
				}
			}
			else {
				Interceptor[] interceptors = registry.getInterceptors(advisor);
				interceptorList.addAll(Arrays.asList(interceptors));
			}
		}
·		// 将 list 返回
		return interceptorList;
	}

```

~>>1

MethodInterceptor[] interceptors = registry.getInterceptors(advisor);

```java
org.springframework.aop.framework.adapter.DefaultAdvisorAdapterRegistry.getInterceptors(Advisor)
public MethodInterceptor[] getInterceptors(Advisor advisor) throws UnknownAdviceTypeException {
		List<MethodInterceptor> interceptors = new ArrayList<MethodInterceptor>(3);
    	// 进入分析
		Advice advice = advisor.getAdvice();
    	// spring 默认的拦截器是 MethodInterceptor 类型的  ExposeInvocationInterceptor
		if (advice instanceof MethodInterceptor) {
			interceptors.add((MethodInterceptor) advice);
		}
    /*
    	this.adapters 是下面三种
    	MethodBeforeAdviceAdapter
    	AfterReturningAdviceAdapter
    	ThrowsAdviceAdapter 	
    */
		for (AdvisorAdapter adapter : this.adapters) {
			if (adapter.supportsAdvice(advice)) {
                // 将其包装成相应的 MethodInterceptor 返回
				interceptors.add(adapter.getInterceptor(advisor));
			}
		}
		if (interceptors.isEmpty()) {
			throw new UnknownAdviceTypeException(advisor.getAdvice());
		}
    	// new 一个 MethodInterceptor[] 将数据放入  返回
		return interceptors.toArray(new MethodInterceptor[interceptors.size()]);
	}
```



## 2、 分析 getAdvice 怎么完成的

Advice advice = advisor.getAdvice();

```java
org.springframework.aop.aspectj.annotation.InstantiationModelAwarePointcutAdvisorImpl.getAdvice()
@Override
	public synchronized Advice getAdvice() {
		if (this.instantiatedAdvice == null) {
			this.instantiatedAdvice = instantiateAdvice(this.declaredPointcut);
		}
		return this.instantiatedAdvice;
	}

private Advice instantiateAdvice(AspectJExpressionPointcut pcut) {
		return this.aspectJAdvisorFactory.getAdvice(this.aspectJAdviceMethod, pcut,
				this.aspectInstanceFactory, this.declarationOrder, this.aspectName);
	}


```

```java
org.springframework.aop.aspectj.annotation.ReflectiveAspectJAdvisorFactory.getAdvice(Method, AspectJExpressionPointcut, MetadataAwareAspectInstanceFactory, int, String)
@Override
	public Advice getAdvice(Method candidateAdviceMethod, AspectJExpressionPointcut expressionPointcut,
			MetadataAwareAspectInstanceFactory aspectInstanceFactory, int declarationOrder, String aspectName) {

		Class<?> candidateAspectClass = aspectInstanceFactory.getAspectMetadata().getAspectClass();
		validate(candidateAspectClass);

		AspectJAnnotation<?> aspectJAnnotation =
				AbstractAspectJAdvisorFactory.findAspectJAnnotationOnMethod(candidateAdviceMethod);
		if (aspectJAnnotation == null) {
			return null;
		}

		// If we get here, we know we have an AspectJ method.
		// Check that it's an AspectJ-annotated class
		if (!isAspect(candidateAspectClass)) {
			throw new AopConfigException("Advice must be declared inside an aspect type: " +
					"Offending method '" + candidateAdviceMethod + "' in class [" +
					candidateAspectClass.getName() + "]");
		}

		if (logger.isDebugEnabled()) {
			logger.debug("Found AspectJ method: " + candidateAdviceMethod);
		}

		AbstractAspectJAdvice springAdvice;
	// 可以非常明显的看见，根据不同的 advice 返回不同的 对象
		switch (aspectJAnnotation.getAnnotationType()) {
			case AtBefore:
				springAdvice = new AspectJMethodBeforeAdvice(
						candidateAdviceMethod, expressionPointcut, aspectInstanceFactory);
				break;
			case AtAfter:
				springAdvice = new AspectJAfterAdvice(
						candidateAdviceMethod, expressionPointcut, aspectInstanceFactory);
				break;
			case AtAfterReturning:
				springAdvice = new AspectJAfterReturningAdvice(
						candidateAdviceMethod, expressionPointcut, aspectInstanceFactory);
				AfterReturning afterReturningAnnotation = (AfterReturning) aspectJAnnotation.getAnnotation();
				if (StringUtils.hasText(afterReturningAnnotation.returning())) {
					springAdvice.setReturningName(afterReturningAnnotation.returning());
				}
				break;
			case AtAfterThrowing:
				springAdvice = new AspectJAfterThrowingAdvice(
						candidateAdviceMethod, expressionPointcut, aspectInstanceFactory);
				AfterThrowing afterThrowingAnnotation = (AfterThrowing) aspectJAnnotation.getAnnotation();
				if (StringUtils.hasText(afterThrowingAnnotation.throwing())) {
					springAdvice.setThrowingName(afterThrowingAnnotation.throwing());
				}
				break;
			case AtAround:
				springAdvice = new AspectJAroundAdvice(
						candidateAdviceMethod, expressionPointcut, aspectInstanceFactory);
				break;
			case AtPointcut:
				if (logger.isDebugEnabled()) {
					logger.debug("Processing pointcut '" + candidateAdviceMethod.getName() + "'");
				}
				return null;
			default:
				throw new UnsupportedOperationException(
						"Unsupported advice type on method: " + candidateAdviceMethod);
		}

		// Now to configure the advice...
		springAdvice.setAspectName(aspectName);
		springAdvice.setDeclarationOrder(declarationOrder);
		String[] argNames = this.parameterNameDiscoverer.getParameterNames(candidateAdviceMethod);
		if (argNames != null) {
			springAdvice.setArgumentNamesFromStringArray(argNames);
		}
		springAdvice.calculateArgumentBindings();
		return springAdvice;
	}


```

| advice 类型          | 返回的类                          | 其主要父类或接口     |
| -------------------- | --------------------------------- | -------------------- |
| **AtBefore**         | **AspectJMethodBeforeAdvice**     | MethodBeforeAdvice   |
| **AtAfter**          | **AspectJAfterAdvice**            | MethodInterceptor    |
| **AtAfterReturning** | **AspectJAfterReturningAdvice**   | AfterReturningAdvice |
| **AtAfterThrowing**  | **AspectJAfterThrowingAdvice**    | MethodInterceptor    |
| **AtAround**         | **AspectJAroundAdvice**           | MethodInterceptor    |
| **AtPointcut**       | **不做处理**                      |                      |
| **上面的都不是**     | **UnsupportedOperationException** | 抛异常。             |

从所有的 advisor 里面获取 advice 

将其全部转换成 methodIntceptor



## 3、执行执行链

```java
@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		MethodInvocation invocation;
		Object oldProxy = null;
		boolean setProxyContext = false;

		TargetSource targetSource = this.advised.targetSource;
		Class<?> targetClass = null;
		Object target = null;

		try {
			if (!this.equalsDefined && AopUtils.isEqualsMethod(method)) {
				// The target does not implement the equals(Object) method itself.
                // 如果是 equals 的话直接执行 比较其是否是 proxy
                // 因为是接口没有相应的方法在这样执行的吧
				return equals(args[0]);
			}
			else if (!this.hashCodeDefined && AopUtils.isHashCodeMethod(method)) {
				// The target does not implement the hashCode() method itself.
                // 直接执行其hashCode
				return hashCode();
			}
			else if (method.getDeclaringClass() == DecoratingProxy.class) {
				// There is only getDecoratedClass() declared -> dispatch to proxy config.
				return AopProxyUtils.ultimateTargetClass(this.advised);
			}
			else if (!this.advised.opaque && method.getDeclaringClass().isInterface() &&
					method.getDeclaringClass().isAssignableFrom(Advised.class)) {
				// Service invocations on ProxyConfig with the proxy config...
				return AopUtils.invokeJoinpointUsingReflection(this.advised, method, args);
			}

			Object retVal;

			if (this.advised.exposeProxy) {
				// Make invocation available if necessary.
				oldProxy = AopContext.setCurrentProxy(proxy);
				setProxyContext = true;
			}

			// May be null. Get as late as possible to minimize the time we "own" the target,
			// in case it comes from a pool.
			target = targetSource.getTarget();
			if (target != null) {
				targetClass = target.getClass();
			}

			// Get the interception chain for this method.
			List<Object> chain = this.advised.getInterceptorsAndDynamicInterceptionAdvice(method, targetClass);
             // 上面将所有的 advice 转换成了 methodInterceptor 并返回
                ---------------------------------------------------------------------

			// Check whether we have any advice. If we don't, we can fallback on direct
			// reflective invocation of the target, and avoid creating a MethodInvocation.
              // 不为空
			if (chain.isEmpty()) {
				// We can skip creating a MethodInvocation: just invoke the target directly
				// Note that the final invoker must be an InvokerInterceptor so we know it does
				// nothing but a reflective operation on the target, and no hot swapping or fancy proxying.
				Object[] argsToUse = AopProxyUtils.adaptArgumentsIfNecessary(method, args);
				retVal = AopUtils.invokeJoinpointUsingReflection(target, method, argsToUse);
			}
            // 来到这里
			else {
				// We need to create a method invocation...
                // 创建一个方法调用类
				invocation = new ReflectiveMethodInvocation(proxy, target, method, args, targetClass, chain);
               
				// Proceed to the joinpoint through the interceptor chain.
                // 开始调用   进入
				retVal = invocation.proceed();
			}

			// Massage return value if necessary.
			Class<?> returnType = method.getReturnType();
			if (retVal != null && retVal == target &&
					returnType != Object.class && returnType.isInstance(proxy) &&
					!RawTargetAccess.class.isAssignableFrom(method.getDeclaringClass())) {
				// Special case: it returned "this" and the return type of the method
				// is type-compatible. Note that we can't help if the target sets
				// a reference to itself in another returned object.
				retVal = proxy;
			}
			else if (retVal == null && returnType != Void.TYPE && returnType.isPrimitive()) {
				throw new AopInvocationException(
						"Null return value from advice does not match primitive return type for: " + method);
			}
            // 将其返回
			return retVal;
		}
		finally {
			if (target != null && !targetSource.isStatic()) {
				// Must have come from TargetSource.
				targetSource.releaseTarget(target);
			}
			if (setProxyContext) {
				// Restore old proxy.
				AopContext.setCurrentProxy(oldProxy);
			}
		}
	}
```



开始调用执行链

```java
org.springframework.aop.framework.ReflectiveMethodInvocation.proceed()
public Object proceed() throws Throwable {
		//	We start with an index of -1 and increment early.
    // 默认 this.currentInterceptorIndex = -1 
		if (this.currentInterceptorIndex == this.interceptorsAndDynamicMethodMatchers.size() - 1) {
			return invokeJoinpoint();
		}
		// 获取相应下标的 拦截连
		Object interceptorOrInterceptionAdvice =
			this.interceptorsAndDynamicMethodMatchers.get(++this.currentInterceptorIndex);
    	// 不是这种类型的
		if (interceptorOrInterceptionAdvice instanceof InterceptorAndDynamicMethodMatcher) {
			// Evaluate dynamic method matcher here: static part will already have
			// been evaluated and found to match.
			InterceptorAndDynamicMethodMatcher dm =
					(InterceptorAndDynamicMethodMatcher) interceptorOrInterceptionAdvice;
			if (dm.methodMatcher.matches(this.method, this.targetClass, this.arguments)) {
				return dm.interceptor.invoke(this);
			}
			else {
				// Dynamic matching failed.
				// Skip this interceptor and invoke the next in the chain.
				return proceed();
			}
		}
		else {
			// It's an interceptor, so we just invoke it: The pointcut will have
			// been evaluated statically before this object was constructed.
            // 进入这里
			return ((MethodInterceptor) interceptorOrInterceptionAdvice).invoke(this);
		}
	}
```

```java
 0 ExposeInvocationInterceptor 
       1 AspectJAfterThrowingAdvice
       2 AfterReturningAdviceInterceptor
       3 AspectJAfterAdvice
       4 MethodBeforeAdviceInterceptor
       
 就不跟着调了 头会大的
 
前置通知-》目标方法-》后置通知-》返回通知
前置通知-》目标方法-》后置通知-》异常通知
```

