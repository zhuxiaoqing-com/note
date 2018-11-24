分析

mv = ha.handle(processedRequest, response, mappedHandler.getHandler());

```java
org.springframework.web.servlet.mvc.method.AbstractHandlerMethodAdapter#handle
@Override
	public final ModelAndView handle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {

		return handleInternal(request, response, (HandlerMethod) handler);
	}
```

```java
org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter#handleInternal
protected ModelAndView handleInternal(HttpServletRequest request,
			HttpServletResponse response, HandlerMethod handlerMethod) throws Exception {

		ModelAndView mav;
		checkRequest(request);

		// Execute invokeHandlerMethod in synchronized block if required.
    // 默认为 false
		if (this.synchronizeOnSession) {
			HttpSession session = request.getSession(false);
			if (session != null) {
				Object mutex = WebUtils.getSessionMutex(session);
				synchronized (mutex) {
					mav = invokeHandlerMethod(request, response, handlerMethod);
				}
			}
			else {
				// No HttpSession available -> no mutex necessary
				mav = invokeHandlerMethod(request, response, handlerMethod);
			}
		}
     // 来到这里
		else {
			// No synchronization on session demanded at all...
            // 进入
			mav = invokeHandlerMethod(request, response, handlerMethod);
		}

		if (!response.containsHeader(HEADER_CACHE_CONTROL)) {
			if (getSessionAttributesHandler(handlerMethod).hasSessionAttributes()) {
				applyCacheSeconds(response, this.cacheSecondsForSessionAttributeHandlers);
			}
			else {
				prepareResponse(response);
			}
		}

		return mav;
	}
```

```java
org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter#invokeHandlerMethod
protected ModelAndView invokeHandlerMethod(HttpServletRequest request,
			HttpServletResponse response, HandlerMethod handlerMethod) throws Exception {
		// 通过 reequest 和 response
		ServletWebRequest webRequest = new ServletWebRequest(request, response);
		try {
			WebDataBinderFactory binderFactory = getDataBinderFactory(handlerMethod);
			ModelFactory modelFactory = getModelFactory(handlerMethod, binderFactory);

			ServletInvocableHandlerMethod invocableMethod = createInvocableHandlerMethod(handlerMethod);
			invocableMethod.setHandlerMethodArgumentResolvers(this.argumentResolvers);
			invocableMethod.setHandlerMethodReturnValueHandlers(this.returnValueHandlers);
			invocableMethod.setDataBinderFactory(binderFactory);
			invocableMethod.setParameterNameDiscoverer(this.parameterNameDiscoverer);
			// 创建一个 ModelAndViewContainer 容器
			ModelAndViewContainer mavContainer = new ModelAndViewContainer();
			mavContainer.addAllAttributes(RequestContextUtils.getInputFlashMap(request));
			modelFactory.initModel(webRequest, mavContainer, invocableMethod);
			mavContainer.setIgnoreDefaultModelOnRedirect(this.ignoreDefaultModelOnRedirect);

			AsyncWebRequest asyncWebRequest = WebAsyncUtils.createAsyncWebRequest(request, response);
			asyncWebRequest.setTimeout(this.asyncRequestTimeout);

			WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(request);
			asyncManager.setTaskExecutor(this.taskExecutor);
			asyncManager.setAsyncWebRequest(asyncWebRequest);
			asyncManager.registerCallableInterceptors(this.callableInterceptors);
			asyncManager.registerDeferredResultInterceptors(this.deferredResultInterceptors);
			// false
			if (asyncManager.hasConcurrentResult()) {
				Object result = asyncManager.getConcurrentResult();
				mavContainer = (ModelAndViewContainer) asyncManager.getConcurrentResultContext()[0];
				asyncManager.clearConcurrentResult();
				if (logger.isDebugEnabled()) {
					logger.debug("Found concurrent result value [" + result + "]");
				}
				invocableMethod = invocableMethod.wrapConcurrentResult(result);
			}
			// 这里调用目标方法  ~>>1
			invocableMethod.invokeAndHandle(webRequest, mavContainer);
			if (asyncManager.isConcurrentHandlingStarted()) {
				return null;
			}
            // 获取 modelAndView ~>>2
			return getModelAndView(mavContainer, modelFactory, webRequest);
		}
		finally {
			webRequest.requestCompleted();
		}
	}
```

# 1、调用目标方法。以及处理返回值

~>>1

```java
org.springframework.web.servlet.mvc.method.annotation.ServletInvocableHandlerMethod#invokeAndHandle
public void invokeAndHandle(ServletWebRequest webRequest, ModelAndViewContainer mavContainer,
			Object... providedArgs) throws Exception {
		// 执行目标方法下面没什么好看的就不看了
		Object returnValue = invokeForRequest(webRequest, mavContainer, providedArgs);
		setResponseStatus(webRequest);
		// 判断返回值是否为 null
		if (returnValue == null) {
			if (isRequestNotModified(webRequest) || getResponseStatus() != null || mavContainer.isRequestHandled()) {
				mavContainer.setRequestHandled(true);
                // 为 Null 的话直接返回
				return;
			}
		}
    	// 为 Null false
		else if (StringUtils.hasText(getResponseStatusReason())) {
			mavContainer.setRequestHandled(true);
			return;
		}
	// 设置 this.requestHandled = false;
		mavContainer.setRequestHandled(false);
		try {
            // 执行 handlerMethodReturnValueHandler.handleReturnValue()方法
			this.returnValueHandlers.handleReturnValue(
					returnValue, getReturnValueType(returnValue), mavContainer, webRequest);
		}
		catch (Exception ex) {
			if (logger.isTraceEnabled()) {
				logger.trace(getReturnValueHandlingErrorMessage("Error handling return value", returnValue), ex);
			}
			throw ex;
		}
	}
```

```java
org.springframework.web.method.support.HandlerMethodReturnValueHandlerComposite#handleReturnValue
public void handleReturnValue(Object returnValue, MethodParameter returnType,
			ModelAndViewContainer mavContainer, NativeWebRequest webRequest) throws Exception {
		// 查找合适的 handler 进入
		HandlerMethodReturnValueHandler handler = selectHandler(returnValue, returnType);
		if (handler == null) {
			throw new IllegalArgumentException("Unknown return value type: " + returnType.getParameterType().getName());
		}
    	// 调用 handleReturnValue
		handler.handleReturnValue(returnValue, returnType, mavContainer, webRequest);
	}


	private HandlerMethodReturnValueHandler selectHandler(Object value, MethodParameter returnType) {
        // 应该是判断是否是 Async 方法
		boolean isAsyncValue = isAsyncReturnValue(value, returnType);
		for (HandlerMethodReturnValueHandler handler : this.returnValueHandlers) {
			if (isAsyncValue && !(handler instanceof AsyncHandlerMethodReturnValueHandler)) {
				continue;
			}
			if (handler.supportsReturnType(returnType)) {
				return handler;
			}
		}
		return null;
	}

/*
	主要有 
	ViewNameMethodReturnValueHandler 
		这个 handler 主要是把 
		mavContainer.setViewName(viewName);
		this.view = viewName; 给设置进去
	
	RequestResponseBodyMethodProcessor
		
	
	
*/
```

# 2、创建 ModelAndView

~>>2

return getModelAndView(mavContainer, modelFactory, webRequest);

```java
org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter#getModelAndView
private ModelAndView getModelAndView(ModelAndViewContainer mavContainer,
			ModelFactory modelFactory, NativeWebRequest webRequest) throws Exception {
		// 什么都没干。
		modelFactory.updateModel(webRequest, mavContainer);
    	// 为 false 前面设置了。如果返回值为 null  就设置 RequestHandled 为 true
		if (mavContainer.isRequestHandled()) {
			return null;
		}
    	// 从 mavContainerd 里面获取 model.因为我们没有往 model modleAndView map 里面放数据。就没有
		ModelMap model = mavContainerd.getModel();
    // 创建 ModelAndView  将 ViewName model 传入 返回
		ModelAndView mav = new ModelAndView(mavContainer.getViewName(), model, mavContainer.getStatus());
		if (!mavContainer.isViewReference()) {
			mav.setView((View) mavContainer.getView());
		}
		if (model instanceof RedirectAttributes) {
			Map<String, ?> flashAttributes = ((RedirectAttributes) model).getFlashAttributes();
			HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
			RequestContextUtils.getOutputFlashMap(request).putAll(flashAttributes);
		}
    // 返回
		return mav;
	}
```

# 3、继续下面的 doDispach

```java
protected void doDispatch(HttpServletRequest request, HttpServletResponse response) throws Exception {
		HttpServletRequest processedRequest = request;
		HandlerExecutionChain mappedHandler = null;
		boolean multipartRequestParsed = false;

		WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(request);

		try {
			ModelAndView mv = null;
			Exception dispatchException = null;

			try {
				processedRequest = checkMultipart(request);
				multipartRequestParsed = (processedRequest != request);

				// Determine handler for the current request. handler 是 methodHandler 类型的
				mappedHandler = getHandler(processedRequest);
				if (mappedHandler == null || mappedHandler.getHandler() == null) {
					noHandlerFound(processedRequest, response);
					return;
				}

				// Determine handler adapter for the current request.
				HandlerAdapter ha = getHandlerAdapter(mappedHandler.getHandler());

				// Process last-modified header, if supported by the handler.
				String method = request.getMethod();
				boolean isGet = "GET".equals(method);
				if (isGet || "HEAD".equals(method)) {
					long lastModified = ha.getLastModified(request, mappedHandler.getHandler());
					if (logger.isDebugEnabled()) {
						logger.debug("Last-Modified value for [" + getRequestUri(request) + "] is: " + lastModified);
					}
					if (new ServletWebRequest(request, response).checkNotModified(lastModified) && isGet) {
						return;
					}
				}

				if (!mappedHandler.applyPreHandle(processedRequest, response)) {
					return;
				}

				// Actually invoke the handler.
				mv = ha.handle(processedRequest, response, mappedHandler.getHandler());
            ///////////////// 这里开始  //////////////////////////

				if (asyncManager.isConcurrentHandlingStarted()) {
					return;
				}
			 // 如果 ModelAndView 的 View 对象为 null.为其设置一个默认的 viewName 需要我们自己配置
				applyDefaultViewName(processedRequest, mv);
                // 执行 interceptor 的 postHandle 方法
				mappedHandler.applyPostHandle(processedRequest, response, mv);
			}
			catch (Exception ex) {
				dispatchException = ex;
			}
			catch (Throwable err) {
				// As of 4.3, we're processing Errors thrown from handler methods as well,
				// making them available for @ExceptionHandler methods and other scenarios.
                // 出现异常就将其包装，赋值给 dispatchException
				dispatchException = new NestedServletException("Handler dispatch failed", err);
			}
            // 处理加工 dispatcheResult  进入 将 dispatchException 传入
			processDispatchResult(processedRequest, response, mappedHandler, mv, dispatchException);
		}
		catch (Exception ex) {
			triggerAfterCompletion(processedRequest, response, mappedHandler, ex);
		}
		catch (Throwable err) {
            // 调用其 interceptor afterCompletion
			triggerAfterCompletion(processedRequest, response, mappedHandler,
					new NestedServletException("Handler processing failed", err));
		}
		finally {
			if (asyncManager.isConcurrentHandlingStarted()) {
				// Instead of postHandle and afterCompletion
				if (mappedHandler != null) {
					mappedHandler.applyAfterConcurrentHandlingStarted(processedRequest, response);
				}
			}
			else {
				// Clean up any resources used by a multipart request.
				if (multipartRequestParsed) {
					cleanupMultipart(processedRequest);
				}
			}
		}
	}
```

## 1、processDispatchResult();

```java
org.springframework.web.servlet.DispatcherServlet#processDispatchResult
private void processDispatchResult(HttpServletRequest request, HttpServletResponse response,
			HandlerExecutionChain mappedHandler, ModelAndView mv, Exception exception) throws Exception {

		boolean errorView = false;
		// 判断  exception 是否为 null
		if (exception != null) {
			if (exception instanceof ModelAndViewDefiningException) {
				logger.debug("ModelAndViewDefiningException encountered", exception);
				mv = ((ModelAndViewDefiningException) exception).getModelAndView();
			}
			else {
				Object handler = (mappedHandler != null ? mappedHandler.getHandler() : null);
                // 运行所有的 HandlerExceptionResolver.resolveException();
                // 这里面 如果没有 HandlerExceptionResoler 进行处理就会将 Excetpion throw
                // 下面都就都不会执行了
				mv = processHandlerException(request, response, handler, exception);
				errorView = (mv != null);
			}
		}

		// Did the handler return a view to render?
		if (mv != null && !mv.wasCleared()) {
            // 进行渲染 ~>>1
			render(mv, request, response);
			if (errorView) {
				WebUtils.clearErrorRequestAttributes(request);
			}
		}
		else {
			if (logger.isDebugEnabled()) {
				logger.debug("Null ModelAndView returned to DispatcherServlet with name '" + getServletName() +
						"': assuming HandlerAdapter completed request handling");
			}
		}

		if (WebAsyncUtils.getAsyncManager(request).isConcurrentHandlingStarted()) {
			// Concurrent handling started during a forward
			return;
		}

		if (mappedHandler != null) {
            // 调用其 interceptor 的 afterCompletion
			mappedHandler.triggerAfterCompletion(request, response, null);
		}
	}

```

processDispatchResult()分为三步

- 运行行所有的 HandlerExceptionResolver.resolverException();

- reander 通过 ViewResolver 获取  View 

- 调用 interceptor.afterCompletion();



### 1、 HandlerExceptionResolver

```java
	protected ModelAndView processHandlerException(HttpServletRequest request, HttpServletResponse response,
			Object handler, Exception ex) throws Exception {

		// Check registered HandlerExceptionResolvers...
		ModelAndView exMv = null;
		for (HandlerExceptionResolver handlerExceptionResolver : this.handlerExceptionResolvers) {
			exMv = handlerExceptionResolver.resolveException(request, response, handler, ex);
			if (exMv != null) {
				break;
			}
		}
		if (exMv != null) {
			if (exMv.isEmpty()) {
				request.setAttribute(EXCEPTION_ATTRIBUTE, ex);
				return null;
			}
			// We might still need view name translation for a plain error model...
			if (!exMv.hasView()) {
				exMv.setViewName(getDefaultViewName(request));
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Handler execution resulted in exception - forwarding to resolved error view: " + exMv, ex);
			}
			WebUtils.exposeErrorRequestAttributes(request, ex, getServletName());
			return exMv;
		}
	// 如果 没有 HandlerExceptionResolver 进行处理就抛出去
		throw ex;
	}

```



### 2、render(mv, request, response);

~>>2 render(mv, request, response);

```java
protected void render(ModelAndView mv, HttpServletRequest request, HttpServletResponse response) throws Exception {
		// Determine locale for request and apply it to the response.
    	// 将当前的国际化设置进去 可以重写 默认没有实现
		Locale locale = this.localeResolver.resolveLocale(request);
		response.setLocale(locale);

		View view;
    	// this.view instanceof String 判断 ModelAndView 的 view 是不是 String 类型的
		if (mv.isReference()) {
			// We need to resolve the view name.
            // 通过 viewResolver 获取  进入 ~>>1
			view = resolveViewName(mv.getViewName(), mv.getModelInternal(), locale, request);
			if (view == null) {
				throw new ServletException("Could not resolve view with name '" + mv.getViewName() +
						"' in servlet with name '" + getServletName() + "'");
			}
		}
		else {
			// No need to lookup: the ModelAndView object contains the actual View object.
            // 如果没有就 viewResolver 获取 view 对象
			view = mv.getView();
			if (view == null) {
				throw new ServletException("ModelAndView [" + mv + "] neither contains a view name nor a " +
						"View object in servlet with name '" + getServletName() + "'");
			}
		}

		// Delegate to the View object for rendering.
		if (logger.isDebugEnabled()) {
			logger.debug("Rendering view [" + view + "] in DispatcherServlet with name '" + getServletName() + "'");
		}
		try {
			if (mv.getStatus() != null) {
				response.setStatus(mv.getStatus().value());
			}
            // view 对象 调用 render 对 浏览器作出相应
			view.render(mv.getModelInternal(), request, response);
		}
		catch (Exception ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("Error rendering view [" + view + "] in DispatcherServlet with name '" +
						getServletName() + "'", ex);
			}
			throw ex;
		}
	}
```

 ~>>1

view = resolveViewName();

```java
protected View resolveViewName(String viewName, Map<String, Object> model, Locale locale,
			HttpServletRequest request) throws Exception {
		// 遍历获取符合条件的 ViewResolver 调用其 resolveViewName 方法
		for (ViewResolver viewResolver : this.viewResolvers) {
            // ContentNegotiatingViewResolver  用来解析模板的
			View view = viewResolver.resolveViewName(viewName, locale);
			if (view != null) {
				return view;
			}
		}
		return null;
	}
```



​    

# 4、RequestResponseBodyMethodProcessor



```java
org.springframework.web.servlet.mvc.method.annotation.RequestResponseBodyMethodProcessor

@Override
	public void handleReturnValue(Object returnValue, MethodParameter returnType,
			ModelAndViewContainer mavContainer, NativeWebRequest webRequest)
			throws IOException, HttpMediaTypeNotAcceptableException, HttpMessageNotWritableException {
		// 将 setRequestHandled 设置为 true this.requestHandled = true;
    	// 为 requestHandled true 的话就不会进行创建 ModelAndView 而是返回 null
		mavContainer.setRequestHandled(true);
    	// 返回 HttpRequest 将其获取包装起来
		ServletServerHttpRequest inputMessage = createInputMessage(webRequest);
	    // 返回 HttpResponse 将其获取包装起来
		ServletServerHttpResponse outputMessage = createOutputMessage(webRequest);

		// Try even with null return value. ResponseBodyAdvice could get involved.
 		//即使有null返回值也可以尝试。ResponseBodyAdvice可以参与进来。 
    	// returnType 将 returnType 包装起来了 进入
		writeWithMessageConverters(returnValue, returnType, inputMessage, outputMessage);
	}
```

```java
org.springframework.web.servlet.mvc.method.annotation.AbstractMessageConverterMethodProcessor#writeWithMessageConverters(T, org.springframework.core.MethodParameter, org.springframework.http.server.ServletServerHttpRequest, org.springframework.http.server.ServletServerHttpResponse)
    
protected <T> void writeWithMessageConverters(T value, MethodParameter returnType,
			ServletServerHttpRequest inputMessage, ServletServerHttpResponse outputMessage)
			throws IOException, HttpMediaTypeNotAcceptableException, HttpMessageNotWritableException {

		Object outputValue;
		Class<?> valueType;
		Type declaredType;
		// CharSequence String 是其子类
		if (value instanceof CharSequence) {
			outputValue = value.toString();
			valueType = String.class;
			declaredType = String.class;
		}
		else {
            // 将 对象 赋予  outputValue
			outputValue = value;
            // 这里 
            //return (value != null ? value.getClass() : returnType.getParameterType());
            // 将 对象的 类型获取过来了通过  value.getClass() 
            // 获取的是实际的类型
			valueType = getReturnValueType(outputValue, returnType);
            // method.getGenericReturnType(); 获取其 return 的类型
            // method 是当前你执行的 controller 方法的的 method
            // 这应该获取的是 返回的时候的类型。
			declaredType = getGenericType(returnType);
		}
		// 获取 request
		HttpServletRequest request = inputMessage.getServletRequest();
    	// 获取 request 的 Accept 就是浏览器可以接收的类型
    	// 通过 reqeust 的 Accept 获取类型，然后切割
		List<MediaType> requestedMediaTypes = getAcceptableMediaTypes(request);
    // 通过 valueType 获取可以用的类型 就是 application/json
		List<MediaType> producibleMediaTypes = getProducibleMediaTypes(request, valueType, declaredType);
		// 如果 value 不为 Null. 且没有匹配的类型就抛出异常
		if (outputValue != null && producibleMediaTypes.isEmpty()) {
			throw new IllegalArgumentException("No converter found for return value of type: " + valueType);
		}
	  // 创建一个存放 可以匹配的类型
		Set<MediaType> compatibleMediaTypes = new LinkedHashSet<MediaType>();
    	// request 可以接受的全部类型
		for (MediaType requestedType : requestedMediaTypes) {
            // 可以发送的全部类型
			for (MediaType producibleType : producibleMediaTypes) {
                // 进行匹配。符合的 add compatibleMediaTypes 中
				if (requestedType.isCompatibleWith(producibleType)) {
					compatibleMediaTypes.add(getMostSpecificMediaType(requestedType, producibleType));
				}
			}
		}
		if (compatibleMediaTypes.isEmpty()) {
			if (outputValue != null) {
				throw new HttpMediaTypeNotAcceptableException(producibleMediaTypes);
			}
			return;
		}

		List<MediaType> mediaTypes = new ArrayList<MediaType>(compatibleMediaTypes);
    	// 应该是根据 q 排序的
		MediaType.sortBySpecificityAndQuality(mediaTypes);

		MediaType selectedMediaType = null;
		for (MediaType mediaType : mediaTypes) {
            // 表示这种媒体类型是否是具体的，即无论类型还是子类型都不是通配符
			if (mediaType.isConcrete()) {
                // 是具体的就 break
				selectedMediaType = mediaType;
				break;
			}
			else if (mediaType.equals(MediaType.ALL) || mediaType.equals(MEDIA_TYPE_APPLICATION)) {
				selectedMediaType = MediaType.APPLICATION_OCTET_STREAM;
				break;
			}
		}

		if (selectedMediaType != null) {
            // 返回一个 Map  其本身的话就删除了 
  /*
         Map<String, String> params = new LinkedHashMap<String, String>(getParameters());
		params.remove(PARAM_QUALITY_FACTOR);
  */
			selectedMediaType = selectedMediaType.removeQualityValue();
			for (HttpMessageConverter<?> messageConverter : this.messageConverters) {
                // 是否是  GenericHttpMessageConverter 类型的
				if (messageConverter instanceof GenericHttpMessageConverter) {
                    // 是的话就匹配是否能进入 canWrite 匹配 主要是根据 application/json accept
					if (((GenericHttpMessageConverter) messageConverter).canWrite(
							declaredType, valueType, selectedMediaType)) {
                // 调用 beforeBodyWrite(); 反正就是把原来的方法返回了 预处理
						outputValue = (T) getAdvice().beforeBodyWrite(outputValue, returnType, selectedMediaType,
								(Class<? extends HttpMessageConverter<?>>) messageConverter.getClass(),
								inputMessage, outputMessage);
                      // 如果返回不为空
						if (outputValue != null) {
	
							addContentDispositionHeader(inputMessage, outputMessage);
                            	// 调用 write 进行写入
							((GenericHttpMessageConverter) messageConverter).write(
									outputValue, declaredType, selectedMediaType, outputMessage);
							if (logger.isDebugEnabled()) {
								logger.debug("Written [" + outputValue + "] as \"" + selectedMediaType +
										"\" using [" + messageConverter + "]");
							}
						}
						return;
					}
				}
                // 是否能进行处理 valueType 和 selectedMediaType
				else if (messageConverter.canWrite(valueType, selectedMediaType)) {
					outputValue = (T) getAdvice().beforeBodyWrite(outputValue, returnType, selectedMediaType,
							(Class<? extends HttpMessageConverter<?>>) messageConverter.getClass(),
							inputMessage, outputMessage);
					if (outputValue != null) {
						addContentDispositionHeader(inputMessage, outputMessage);
						((HttpMessageConverter) messageConverter).write(outputValue, selectedMediaType, outputMessage);
						if (logger.isDebugEnabled()) {
							logger.debug("Written [" + outputValue + "] as \"" + selectedMediaType +
									"\" using [" + messageConverter + "]");
						}
					}
					return;
				}
			}
		}

		if (outputValue != null) {
			throw new HttpMediaTypeNotAcceptableException(this.allSupportedMediaTypes);
		}
	}
```

最终还是没有看出什么来，还是太难了。以后再看

















