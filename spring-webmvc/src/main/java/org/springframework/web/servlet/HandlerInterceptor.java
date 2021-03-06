/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.servlet;

import org.springframework.lang.Nullable;
import org.springframework.web.method.HandlerMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Workflow interface that allows for customized handler execution chains.
 * Applications can register any number of existing or custom interceptors
 * for certain groups of handlers, to add common preprocessing behavior
 * without needing to modify each handler implementation.
 *
 * <p>A HandlerInterceptor gets called before the appropriate HandlerAdapter
 * triggers the execution of the handler itself. This mechanism can be used
 * for a large field of preprocessing aspects, e.g. for authorization checks,
 * or common handler behavior like locale or theme changes. Its main purpose
 * is to allow for factoring out repetitive handler code.
 *
 * <p>In an asynchronous processing scenario, the handler may be executed in a
 * separate thread while the main thread exits without rendering or invoking the
 * {@code postHandle} and {@code afterCompletion} callbacks. When concurrent
 * handler execution completes, the request is dispatched back in order to
 * proceed with rendering the model and all methods of this contract are invoked
 * again. For further options and details see
 * {@code org.springframework.web.servlet.AsyncHandlerInterceptor}
 *
 * <p>Typically an interceptor chain is defined per HandlerMapping bean,
 * sharing its granularity. To be able to apply a certain interceptor chain
 * to a group of handlers, one needs to map the desired handlers via one
 * HandlerMapping bean. The interceptors themselves are defined as beans
 * in the application context, referenced by the mapping bean definition
 * via its "interceptors" property (in XML: a &lt;list&gt; of &lt;ref&gt;).
 *
 * <p>HandlerInterceptor is basically similar to a Servlet Filter, but in
 * contrast to the latter it just allows custom pre-processing with the option
 * of prohibiting the execution of the handler itself, and custom post-processing.
 * Filters are more powerful, for example they allow for exchanging the request
 * and response objects that are handed down the chain. Note that a filter
 * gets configured in web.xml, a HandlerInterceptor in the application context.
 *
 * <p>As a basic guideline, fine-grained handler-related preprocessing tasks are
 * candidates for HandlerInterceptor implementations, especially factored-out
 * common handler code and authorization checks. On the other hand, a Filter
 * is well-suited for request content and view content handling, like multipart
 * forms and GZIP compression. This typically shows when one needs to map the
 * filter to certain content types (e.g. images), or to all requests.
 *
 * @author Juergen Hoeller
 * @see HandlerExecutionChain#getInterceptors
 * @see org.springframework.web.servlet.handler.HandlerInterceptorAdapter
 * @see org.springframework.web.servlet.handler.AbstractHandlerMapping#setInterceptors
 * @see org.springframework.web.servlet.handler.UserRoleAuthorizationInterceptor
 * @see org.springframework.web.servlet.i18n.LocaleChangeInterceptor
 * @see org.springframework.web.servlet.theme.ThemeChangeInterceptor
 * @see javax.servlet.Filter
 * @since 20.06.2003
 */
public interface HandlerInterceptor {

	/**
	 * Intercept the execution of a handler. Called after HandlerMapping determined
	 * an appropriate handler object, but before HandlerAdapter invokes the handler.
	 * <p>DispatcherServlet processes a handler in an execution chain, consisting
	 * of any number of interceptors, with the handler itself at the end.
	 * With this method, each interceptor can decide to abort the execution chain,
	  typically sending a HTTP error or writing a custom response.
	 * <p><strong>Note:</strong> special considerations apply for asynchronous
	 * request processing. For more details see
	 * {@link org.springframework.web.servlet.AsyncHandlerInterceptor}.
	 * <p>The default implementation returns {@code true}.
	 *
	 * @param request  current HTTP request
	 * @param response current HTTP response
	 * @param handler  chosen handler to execute, for type and/or instance evaluation
	 * @return {@code true} if the execution chain should proceed with the
	 * next interceptor or the handler itself. Else, DispatcherServlet assumes
	 * that this interceptor has already dealt with the response itself.
	 * @throws Exception in case of errors
	 *
	 * 该方法在请求处理之前进行调用。Spring MVC 中的Interceptor是链式调用的，在一个应用中或者说是在一个请求中可以同时存在多个Interceptor。每个Interceptor的调用会依据它的声明顺序依次执行，而且最先执行的都是Interceptor中的preHandle方法，所以可以在这个方法中进行一些前置初始化操作或者是对当前请求做一个预处理，也可以在这个方法中进行一些判断来决定请求是否要继续进行下去。该方法的返回值是布尔（Boolean）类型的，当它返回为false时，表示请求结束，后续的Interceptor和控制器（Controller）都不会再执行；当返回值为true时，就会继续调用下一个Interceptor的preHandle方法，如果已经是最后一个Interceptor的时候，就会是调用当前请求的控制器中的方法.
	 */
	default boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {

		return true;
	}

	/**
	 * Intercept the execution of a handler. Called after HandlerAdapter actually
	 * invoked the handler, but before the DispatcherServlet renders the view.
	 * Can expose additional model objects to the view via the given ModelAndView.
	 * <p>DispatcherServlet processes a handler in an execution chain, consisting
	 * of any number of interceptors, with the handler itself at the end.
	 * With this method, each interceptor can post-process an execution,
	 * getting applied in inverse order of the execution chain.
	 * <p><strong>Note:</strong> special considerations apply for asynchronous
	 * request processing. For more details see
	 * {@link org.springframework.web.servlet.AsyncHandlerInterceptor}.
	 * <p>The default implementation is empty.
	 *
	 * @param request      current HTTP request
	 * @param response     current HTTP response
	 * @param handler      handler (or {@link HandlerMethod}) that started asynchronous
	 *                     execution, for type and/or instance examination
	 * @param modelAndView the {@code ModelAndView} that the handler returned
	 *                     (can also be {@code null})
	 * @throws Exception in case of errors
	 * 我们知道这个方法包括后面要说到的afterCompletion方法都只能在当前所属的Interceptor的preHandle方法的返回值为true的时候，才能被调用。postHandle方法在当前请求进行处理之后，也就是在控制器中的方法调用之后执行，但是它会在DispatcherServlet进行视图返回渲染之前被调用，所以我们可以在这个方法中对控制器处理之后的ModelAndView对象进行操作。postHandle方法被调用的方向跟preHandle是相反的，也就是说，先声明的Interceptor的postHandle方法反而会后执行。这和 Struts2 里面的Interceptor的执行过程有点类似，Struts2 里面的Interceptor的执行过程也是链式的，只是在 Struts2里面需要手动调用ActionInvocation的invoke方法来触发对下一个Interceptor或者是action的调用，然后每一个Interceptor中在invoke方法调用之前的内容都是按照声明顺序执行的，而invoke方法之后的内容就是反向的。.
	 */
	default void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
							@Nullable ModelAndView modelAndView) throws Exception {
	}

	/**
	 * Callback after completion of request processing, that is, after rendering
	 * the view. Will be called on any outcome of handler execution, thus allows
	 * for proper resource cleanup.
	 * <p>Note: Will only be called if this interceptor's {@code preHandle}
	 * method has successfully completed and returned {@code true}!
	 * <p>As with the {@code postHandle} method, the method will be invoked on each
	 * interceptor in the chain in reverse order, so the first interceptor will be
	 * the last to be invoked.
	 * <p><strong>Note:</strong> special considerations apply for asynchronous
	 * request processing. For more details see
	 * {@link org.springframework.web.servlet.AsyncHandlerInterceptor}.
	 * <p>The default implementation is empty.
	 *
	 * @param request  current HTTP request
	 * @param response current HTTP response
	 * @param handler  handler (or {@link HandlerMethod}) that started asynchronous
	 *                 execution, for type and/or instance examination
	 * @param ex       exception thrown on handler execution, if any
	 * @throws Exception in case of errors
	 * 也是需要当前对应的Interceptor的preHandle方法的返回值为true时才会执行。因此，该方法将在整个请求结束之后，也就是在DispatcherServlet渲染了对应的视图之后执行，这个方法的主要作用是用于进行资源清理的工作
	 *
	 */
	default void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
								 @Nullable Exception ex) throws Exception {
	}

}
