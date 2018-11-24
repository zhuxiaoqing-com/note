package cn.zhu.utils.utils;
/**
 * 仿dbutil的BeanProcessor
 * ClassName: MyBeanProcessor 
 * @Description: TODO
 * @author sm
 * @date 2018年2月6日
 */

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MyBeanProcessor {
    private int PROPERTY_NOT_FOUND = -1;
    /**
     * 
     */
    public<T> List<T> toBeanList(ResultSet resultSet, Class<T> type) {
        try {
            List<T> results = new ArrayList<T>();
            if (!resultSet.next()) {
                return results;
            }
            PropertyDescriptor[] props = this.propertyDescriptors(type);

            // 数据库元数据
            ResultSetMetaData rsmd = resultSet.getMetaData();
            /*
             * 建立ResultSet的column与bean属性对应的关系 column的下标是columnToProperty的下标
             * 元素是bean的描述符的下标
             */
            int[] columnToProperty = this.mapColumnsToProperties(rsmd, props);
            do {
                results.add(this.createBean(resultSet, type, props, columnToProperty));
            } while (resultSet.next());
            return results;
        } catch (Exception e) {
            throw new RuntimeException("获取数据库元数据出错了！" + e);
        }
    }
    /**
     * 将resultSet赋予Bean对象
     * 外界请勿移动ResultSet游标！！内部已经移动了
     */
    public<T> T toBean(ResultSet resultSet, Class<T> type) {
    	try {
			if(!resultSet.next()) {
				return null;
			}
		} catch (SQLException e2) {
			throw new RuntimeException("操作resultSet的时候出错了" + e2);
		}
        // 获取PropertyDescriptor[]
        PropertyDescriptor[] props = this.propertyDescriptors(type);
        
            // 数据库元数据
            ResultSetMetaData rsmd;
			try {
				rsmd = resultSet.getMetaData();
			} catch (SQLException e1) {
				throw new RuntimeException("获取元数据的时候出错了！" + e1);
			}
            /*
             * 建立ResultSet的column与bean属性对应的关系
             * column的下标是columnToProperty的下标
             * 元素是bean的描述符的下标
             */
            int[] columnToProperty;
			try {
				columnToProperty = this.mapColumnsToProperties(rsmd, props);
			} catch (SQLException e) {
				throw new RuntimeException("建立ResultSet的column与bean属性对应的关系时出错了" + e);
			}
            try {
				return this.createBean(resultSet, type, props, columnToProperty);
			} catch (InstantiationException | IllegalAccessException
					| SQLException e) {
				throw new RuntimeException("创建bean对象的时候出错了" + e);
			}      
    }
    /**
     * 创建bean对象
     * @throws IllegalAccessException 
     * @throws InstantiationException 
     * @throws SQLException 
     */
    private<T> T createBean(ResultSet resultSet, Class<T> type, PropertyDescriptor[] props, int[] columnToProperty) throws InstantiationException, IllegalAccessException, SQLException {
        T bean = type.newInstance();
        for (int i = 1; i < columnToProperty.length; i++) {
            if (columnToProperty[i] == PROPERTY_NOT_FOUND) {
                continue;
            }
            PropertyDescriptor prop = props[columnToProperty[i]];
            Class<?> propType = prop.getPropertyType();// 获取bean属性的类型Class
            /*
             * 根据bean属性的类型以及对应该属性的Column返回用户期望的值
             * 都使用getObject的话，null不能正确转换。比如 int类型的在数据库里面默认为null.使用
             * getObject会获得null.使用getInt会获得0;
             */
            Object value = this.processColumn(resultSet, i, propType);
            this.callSetter(bean, prop, value); //为bean的prop属性赋value值
        }
        return bean;
    }
    /**
     * 为bean的prop赋值
     */
    private<T> void callSetter(T bean, PropertyDescriptor prop, Object value) {
        Method method = prop.getWriteMethod();
        if(method == null) return;
        Class<?>[] params = method.getParameterTypes();// 获取形参类型
        /*
         * 处理sql时间类型
         * 判断bean形参属于什么类型的时间类型
         * 如果是sql类型的就将value转换成相应的bean形参类型
         */
        if (value instanceof java.util.Date) {                   //如果processColumn方法返回的值类型为java.sql.包中的一些类型，则需要对应进行特殊处理
            final String targetType = params[0].getName();         //获取参数类型对应的名字
            if ("java.sql.Date".equals(targetType)) {
                value = new java.sql.Date(((java.util.Date) value).getTime());
            } else
            if ("java.sql.Time".equals(targetType)) {
                value = new java.sql.Time(((java.util.Date) value).getTime());
            } else
            if ("java.sql.Timestamp".equals(targetType)) {
                value = new java.sql.Timestamp(((java.util.Date) value).getTime());
            }
        }
        /*
         * 判断形参类型和value值是否匹配
         * isCompatibleType方法来判断value与参数类型是否匹配，匹配则赋值，否则抛出异常
         */
        try {
            if(this.isCompatibleType(value, params[0])) {
                method.invoke(bean, new Object[]{value});
            } else {
                throw new RuntimeException("类型不匹配");
            }
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
           throw new RuntimeException(e);
        }
        
    }
    /**
     * 判断形参类型和value值是否匹配
     */
    private boolean isCompatibleType(Object value, Class<?> type) {
        /*
         *  如果value为null就不需要进行下面的判断了。
         *  isInstance判断value是否能强转或向下转型成type类型
         *  如果type为基本类型该方法返回false;
         *  
         */
       if(value == null || type.isInstance(value)) { 
           return true;
       } else if (type.equals(Integer.TYPE) && Integer.class.isInstance(value)) {
           return true;

       } else if (type.equals(Long.TYPE) && Long.class.isInstance(value)) {
           return true;

       } else if (type.equals(Double.TYPE) && Double.class.isInstance(value)) {
           return true;

       } else if (type.equals(Float.TYPE) && Float.class.isInstance(value)) {
           return true;

       } else if (type.equals(Short.TYPE) && Short.class.isInstance(value)) {
           return true;

       } else if (type.equals(Byte.TYPE) && Byte.class.isInstance(value)) {
           return true;

       } else if (type.equals(Character.TYPE) && Character.class.isInstance(value)) {
           return true;

       } else if (type.equals(Boolean.TYPE) && Boolean.class.isInstance(value)) {
           return true;

       }
       return false;

    }
    /**
     * 根据bean属性的类型来使用不同的获取column的值的方法。正确的将sql的类型转换为java的类型
     * 根据bean属性的类型以及对应该属性的Column返回用户期望的值
     * 都使用getObject的话，null不能正确转换。比如 int类型的在数据库里面默认为null.使用
     * getObject会获得null.使用getInt会获得0;
     * 这步处理是很有必要的，如果其实是int的值却返回null的话调用反射就会出错。将出现
     *  无法通过一个标识或基本扩展转换将指定值转换为基础数组的指定类型 异常；
     * @throws SQLException 
     */
    private Object processColumn(ResultSet rs, int index, Class<?> propType) throws SQLException {
       /*
        * 不是基本类型且sql的列的默认为null
        */
    	if(!propType.isPrimitive() && rs.getObject(index) == null) {
           return null;
       } 
       if(propType.equals(String.class)) {
           return rs.getString(index);
       } else if(propType.equals(Integer.TYPE) || propType.equals(Integer.class)) {
           return Integer.valueOf(rs.getInt(index));
       } else if (
               propType.equals(Boolean.TYPE) || propType.equals(Boolean.class)) {
               return Boolean.valueOf(rs.getBoolean(index));
           } else if (propType.equals(Long.TYPE) || propType.equals(Long.class)) {
               return Long.valueOf(rs.getLong(index));
           } else if (
               propType.equals(Double.TYPE) || propType.equals(Double.class)) {
               return Double.valueOf(rs.getDouble(index));
           } else if (
               propType.equals(Float.TYPE) || propType.equals(Float.class)) {
               return Float.valueOf(rs.getFloat(index));
           } else if (
               propType.equals(Short.TYPE) || propType.equals(Short.class)) {
               return Short.valueOf(rs.getShort(index));
           } else if (propType.equals(Byte.TYPE) || propType.equals(Byte.class)) {
               return Byte.valueOf(rs.getByte(index));
           } else if (propType.equals(Timestamp.class)) {
               return rs.getTimestamp(index);
           } else if (propType.equals(SQLXML.class)) {
               return rs.getSQLXML(index);
           } else {
               return rs.getObject(index);             //其他类型
           }
    }
    /**
     * 建立ResultSet的column与bean属性对应的关系
     * column的下标是columnToProperty的下标
     * 元素是bean的描述符的下标
     * @throws SQLException 
     */
  private int[] mapColumnsToProperties(ResultSetMetaData rsmd, PropertyDescriptor[] props) throws SQLException {
      int cols = rsmd.getColumnCount();// 获取集中列项目的个数
      int[] columnToProperty = new int[cols + 1]; //使其column的下标与数组的下标对应
      Arrays.fill(columnToProperty, PROPERTY_NOT_FOUND);// 使用-1充填数组
      for (int col = 1; col < columnToProperty.length; col++) {
        /*
         * 获取column对应的as别名  因为在表最终形成时有的列可能用的别名这样你只能用别名来获取，原本名没法获取
         */
          String columnName = rsmd.getColumnLabel(col);
          if(columnName == null || 0 == columnName.length()) {// 如果为Null或空的话说明没有使用别名
              /*
               * 获取原本列名
               */
              columnName = rsmd.getColumnName(col); 
          }
          /*
           * 循环遍历bean描述符数组，查找与column列名相等的bean属性名
           */
          for (int i = 0; i < props.length; i++) {
            if (columnName.equalsIgnoreCase(props[i].getName())) {
                columnToProperty[col] = i;
                break;
            }
        }
          
    }
      return columnToProperty;
    }
/**
   * 获取PropertyDescriptor[]通过Class
   */
    private PropertyDescriptor[] propertyDescriptors(Class<?> type) {
            try {
                BeanInfo beanInfo = Introspector.getBeanInfo(type);// 内省获得beanInfo提供了关其 bean 的方法、属性、事件等显式信息
                return beanInfo.getPropertyDescriptors();// 获取描述属性对象的数组
            } catch (IntrospectionException e) {
               throw new RuntimeException("获取bean描述对象数组出错了！" + e);
            }
    }
}
    

