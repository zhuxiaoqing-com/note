package cn.zhu.utils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

public class StaticResponse extends HttpServletResponseWrapper {
/**
 * 将response里面的printwriter调包，让其输出到我们指定的页面
 * @param response
 */
    PrintWriter pw;     
    public StaticResponse(HttpServletResponse response, String path) throws FileNotFoundException, UnsupportedEncodingException {
        super(response);
        // 创建一个自己的PrintWriter，然后自己选定目录输出
        pw = new PrintWriter(path, "utf-8");
    }
    @Override
        public PrintWriter getWriter() throws IOException {
            
            return pw;
        }

}
