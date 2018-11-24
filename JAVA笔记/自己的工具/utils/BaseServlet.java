package cn.zhu.utils;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public abstract class BaseServlet extends HttpServlet {

    /**
     * 有两种功能
     * 1.继承此类可以实现method=xxx  就可以调用子类的xxx方法，当然形参必须和service一样
     * 2.返回字符串可以自动帮其转发和重定向
     */
	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
	    // 设置请求和响应当中的字符的编码
	    request.setCharacterEncoding("utf-8");
	    response.setContentType("text/html;charset=utf-8");
		Method me = null;
		// 获取浏览器传递过来的method参数，并使用反射得到这个方法对象
		String method = request.getParameter("method");
		
		if (method == null) {
			throw new RuntimeException("您没有给出method参数！");
		}
		try {
		    // 使用期子类的this引用，获取其Class,然后获取方法对象
			me = this.getClass().getDeclaredMethod(method, HttpServletRequest.class, HttpServletResponse.class);
		} catch (Exception e) {
			throw new RuntimeException("您给出的"+method+"不存在");
		}
		String order = null;
        try {
            // 调用method传递过来的参数的方法，并获取返回的String
            order = (String) me.invoke(this, request, response);
        } catch (Exception e1) {
            throw new RuntimeException(e1);
        }
        /*
         *当order等于NULL或空时说明并不需要进行重定向和转发 
         */
        if (order != null && !order.trim().isEmpty() ){
		try {
		    /*
		     * 我们定义了一个规范，返回的字符串的 ":" 前面是其指令（暂时只有重定向和转发），
		     * ":"后面是地址，要重定向或转发的地址
		     * 
		     * */
		    // 使用":"分隔其地址,将其指令和指令要操作的地址分割开来
			if (order.contains(":")) {
				int endIndex = order.trim().indexOf(":");
				String start = order.trim().substring(0, endIndex);
				String end = order.trim().substring(endIndex+1);
				// 判断指令是重定向还是转发，然后执行响应的操作
				if (start.equalsIgnoreCase("forward")) {
					request.getRequestDispatcher(end).forward(request, response);
				}
				if (start.equalsIgnoreCase("redirect")) {
					System.out.println();
					response.sendRedirect(request.getContextPath()+end);
				}
			}else {
				request.getRequestDispatcher(order).forward(request, response);
			}
		} catch (Exception e) {
			throw new RuntimeException(e+"暂时不支持此版本或指令输入错误");
		}
		}
	}
	@Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // TODO Auto-generated method stub
System.out.println("dd");
    }

}
