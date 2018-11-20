package com.sh.mh.web.commom;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.apache.poi.ss.formula.functions.T;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class ObjectToHttpClient {
    public static<T> List<NameValuePair> objectToHttpClient(T t) {
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        BeanInfo beanInfo = null;
        try {
            beanInfo = Introspector.getBeanInfo(t.getClass());

            PropertyDescriptor[] descriptors = beanInfo.getPropertyDescriptors();
            for(PropertyDescriptor descriptor : descriptors) {
                Method readMethod = descriptor.getReadMethod();
                String methodName = readMethod.getName();
                if(methodName.equals("getClass")) {
                    continue;
                }
                Object invoke = readMethod.invoke(t);

                if(invoke != null) {
                    // 将其去除 get 和首字母小写 以及排除 is
                    if (methodName.startsWith("g")) {
                        String substring = methodName.substring(3, methodName.length());
                        char[] chars = substring.toCharArray();
                        chars[0] += 32;
                        substring = String.valueOf(chars);
                        BasicNameValuePair pair = new BasicNameValuePair(substring, invoke.toString());
                        nvps.add(pair);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return nvps;
    }

}
