package cn.zhu.utils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.servlet.http.HttpServletRequest;

import sun.misc.BASE64Encoder;
/**
 * 
 * ClassName: DownUtils 
 * @Description: 处理下载的文件名乱码问题
 * @author sm
 * @date 2018年1月22日
 */
public class DownUtils {
   public static String filenameEncoding(HttpServletRequest request, String filename)
            throws UnsupportedEncodingException {
        String agent = request.getHeader("User-Agent"); // 获取浏览器
      if (agent.contains("Firefox")) {
          BASE64Encoder base64Encoder = new BASE64Encoder();
          filename ="=?utf-8?B?" + 
                  base64Encoder.encode(filename.getBytes("utf-8"))
                  + "?=";
          } else if (agent.contains("MSIE")) {
              filename = URLEncoder.encode(filename, "utf-8");
          } else {
              filename = URLEncoder.encode(filename, "utf-8");
          }
        return filename;
    }
}
