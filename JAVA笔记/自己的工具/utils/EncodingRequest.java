package cn.zhu.utils;

import java.io.UnsupportedEncodingException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
/**
 *这个类的作用是
 *  将其get请求里面的参数全部帮其转换成UTF-8，默认是使用ISO-8859-1的
 *  当然必须是URL的Get请求在客户端的编码也是UTF-8，如果是直接在URL地址栏输入的参数没用
 *    因为地址栏里面的参数默认是使用系统默认的编码的也就是GBK的，具体还得看浏览器设置，不同浏览器默认不同
 *    如果想把URL里面的编码也统一的话就得使用 JS来手动处理
 *    
 *    这个类的核心就是将其getParameter的方法获取过来然后将其转换成UTF-8，返回。这样就不会乱码了
 */
public class EncodingRequest extends HttpServletRequestWrapper{
    private HttpServletRequest request;
    public EncodingRequest(HttpServletRequest request) {
        super(request);
        this.request = request;    
    }
    @Override
    public String getParameter(String name) {
       
         String na = request.getParameter(name);
         /**
          * 现在name是在内存中存在的是ISO-8859-1解码后的乱码，将其按照对照表转化成
          * Unicode码的二进制码，所以必须使用ISO-8859-1将其乱码编码成二进制，然后由UTF-8对二进制从新解码
          * 虽然都是显示的二进制码，但是在进内存的时候总会被转换成unicode,所以必须将其显示的转换成bytes数组，
          * 然后再用UTF-8编码，这样就把乱码转换了
          */
         try {
            na = new String(na.getBytes("ISO-8859-1"),"utf-8");
            return na;
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
    
}
