package cn.zhu.httpclient;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.Test;

public class HttpClient {
    /**
     * 使用 httpclient 来获取 json
     * @throws IOException 
     * @throws ClientProtocolException 
     */
    @Test
    public void fun1() throws ClientProtocolException, IOException {
        // 使用 HttpClients 创建一个默认的请求客户端
        CloseableHttpClient httpClient = HttpClients.createDefault();
        // 创建一个HTTPGET 连接方法实例，并传入请求连接池
        HttpGet get = new HttpGet("http://192.168.2.126:8080/ssm_demo/user/userjson?id=10011");
        // 使用请求客户端执行连接方法的实例
        HttpResponse response = httpClient.execute(get);
        // 获取 response 的响应
        HttpEntity entity = response.getEntity();
        // 根据 response 的 StatusCode 进行请求结果判断
        boolean status = response.getStatusLine().getStatusCode() == HttpStatus.SC_OK;
        if(status) {
            String data = new String(EntityUtils.toByteArray(entity), "UTF-8");
            System.out.println(data);
            // 防止 TCP 状态一直保持在 CLOSE_WAIT 状态，在请求头中进行 connection 配置
            get.setHeader("Connection", "close");
            // 使用 EntityUtils 关闭 InputStream 流
            EntityUtils.consume(entity);
            // 使用 httpGet 实例中的 abort  方法终止连接实例
            get.abort();
            // 使用 CloseableHttpClient 中的 close 方法关闭连接
            httpClient.close();
        }
    }
}





















