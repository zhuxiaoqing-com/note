package com.sh.mh.web.commom;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * 没有校验 json 的正确性，请传入合法的 json
 */
public class JsonToMapOrList {

    /**
     * 对 json 进行处理
     * 将" 替换掉
     * @param json
     * @eturn
     */
    private Object JsonToMapOrList(String json) {

        json = json.replace("\"", "");
        Object jsonObj = parseJson(json);
        return jsonObj;
    }

    /**
     * 递归将 json 解析
     * @param json
     * @return
     */
    private Object parseJson(String json) {
        String key;
        Object value;
        char i = json.charAt(0);
        if (i == '{') {
            HashMap<String, Object> map = new HashMap<>();
            json = json.substring(1, json.length() - 1);

            ArrayList<String> arrayList = splitJson(json, new ArrayList());

            //String[] split = substring.split(",");
            for (String subSplit : arrayList) {
                int i1 = subSplit.indexOf(":");
                // 获取 : 的左边
                key = subSplit.substring(0, i1);
                // 获取 : 的右边
                json = subSplit.substring(i1 + 1);

                char i3 = json.charAt(0);
                if (i3 == '{' || i3 == '[') {
                    value = parseJson(json);
                } else {
                    value = json;
                }

                map.put(key, value);
            }
            return map;
        }
        if (i == '[') {
            ArrayList<Object> list = new ArrayList<>();
            HashMap<String, Object> map = new HashMap<>();
            json = json.substring(1, json.length() - 1);

            ArrayList<String> arrayList = splitJson(json, new ArrayList());

            for (String subSplit : arrayList) {

                char i3 = subSplit.charAt(0);
                if (i3 == '{' || i3 == '[') {
                    list.add(parseJson(subSplit));
                } else {
                    list.add(subSplit);
                }
            }
            return list;
        }
        return "";
    }


    /**
     * 用来进行将 json 变成 list 返回， 你需要自己将外壳 去除
     *  将 json 切割成合理的样子
     * @param json
     * @param list
     * @return
     */
    private ArrayList splitJson(String json, ArrayList list) {
        //ArrayList<Object> list = new ArrayList<>();
        Integer end = 0;
        Integer index = 0;

        String[] split = json.split(",");
        if (split.length >= 1) {
            for (String subSplit : split) {
                boolean matches = subSplit.matches("\\w*:*\\w*:*\\w*");
                if (matches) {
                    list.add(subSplit);
                    if (subSplit.length() + 1 >= json.length()) {
                        return list;
                    }
                    json = json.substring(subSplit.length() + 1);
                } else {
                    int start = -1;
                    int index1 = subSplit.indexOf("{");
                    int index2 = subSplit.indexOf("[");
                    if (index1 != -1 && index2 != -1){
                        if (index1 < index2) {
                            start = index1;
                        } else if (index2 != -1) {
                            start = index2;
                        }
                    } else {
                        start = index1==-1 ? index2 : index1;
                    }

                    end = getIndex(json, start);
                    String substring = json.substring(0, end + 1);
                    list.add(substring);
                    if (substring.length() + 1 >= json.length()) {
                        return list;
                    } else {
                        json = json.substring(substring.length() + 1);
                        return splitJson(json, list);
                    }

                }
            }

        } else {
            return list;
        }
        return null;
    }

    /**
     * 取到 { ... } 对角的 下标 或 [ ... ] 对角的下标
     * @param a
     * @param index
     * @return
     */
    private Integer getIndex(String a, Integer index) {
        // 加 2 就是 反方向
        int sum = 1;
        char c = a.charAt(index);
        while (true) {
            index = a.indexOf(c, index + 1);
            if (index == -1) break;
            sum++;
        }
        index = -1;
        while (true) {
            index = a.indexOf(c + 2, index + 1);
            if (index == -1) break;
            sum--;
            if (sum == 0) break;
        }
        return index;
    }


}
