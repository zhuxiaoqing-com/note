package cn.zhu.utils.basedao;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.dbutils.QueryRunner;

import cn.zhu.utils.TxQueryRunner; 

public class BaseDao<T> {
    private QueryRunner qr = new TxQueryRunner();
    // 用来存储T的Class对象
    private Class<T> beanClass;
    // 用来存储Bean和数据库表之间的映射  键为Bean，值为数据库
    private Map<String, String> valueMap = new HashMap<String, String>();
    // 用来存储bean的主键  存的是map的键
    private String id;
    // 用来存储表名
    private String tableName;
    
    @SuppressWarnings("unchecked")
    public BaseDao() {
        /*
         * 1.获取Class对象
         */
        beanClass = (Class<T>) ((ParameterizedType)this.getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        /*
         * 获取其类注解，将其对应起来;
         */
        tableName = beanClass.getAnnotation(Table.class).value();
        /*
         * 获取其类属性注解，将其对应起来;
         */
        Field[] fields = beanClass.getDeclaredFields();
        // 循环遍历field获取注解剑气存入valueMap
        for (Field field : fields) {
            Column column = field.getAnnotation(Column.class);
            if(column == null) {
                Id id1 = field.getAnnotation(Id.class);
                valueMap.put(field.getName(), id1.value());
                id = field.getName();// 将map的键赋予id
            } else {
                valueMap.put(field.getName(), column.value());
            }
        }
        
    }

    public void add(T bean) {
        StringBuilder sql = new StringBuilder(" INSERT INTO " + tableName);
        StringBuilder table = new StringBuilder(" ( ");// 用来存储列名与bean对应
        StringBuilder values = new StringBuilder(" VALUES ( ");
        Object[] params = new Object[valueMap.size()]; // 用来存储bean参数
        /*
         * 将其values的占位符填上 有几个
         */
        for (int i = 0; i < valueMap.size(); i++) {
            if (i < valueMap.size() - 1) {
                values.append(" ?, ");
            } else {
                values.append("?)");
            }
        }
        /*
         * 为其table和bean对应起来
         */
        int i = 0;
        try {
            for (Entry<String, String> entry : valueMap.entrySet()) {
                table.append(entry.getValue() + ",");
                String oldValue = entry.getKey();
                String newValue = BeanUtils.getProperty(bean, oldValue);
                params[i] = newValue;
                i++;
            }
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e1) {
            throw new RuntimeException(e1);
        }
        // 处理table的最后一个,问题将最后一个,去掉，然后将其添加右括号
        table.deleteCharAt(table.length()-1);
        table.append(") ");
        /*
         * 拼凑sql语句
         */
        sql.append(table);
        sql.append(values);
        try {
            qr.update(sql.toString(), params);
        } catch (SQLException e) {
         throw new RuntimeException(e);
        }
    }
    public void update(T bean) {
        
    }
    public void delete(String uuid) {
        
    }
    public T findAll() {
        return null;
        
    }
}
