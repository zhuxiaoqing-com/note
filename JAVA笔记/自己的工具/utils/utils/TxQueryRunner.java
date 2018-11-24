package cn.zhu.utils.utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;


public class TxQueryRunner {
	MyBeanProcessor mbp = new MyBeanProcessor();
	/**
	 * 查询单行记录
	 */
	public<T> T query(String sql,Class<T> clazz, Object... params) {
		Connection connection = null;
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;
		try {
			connection = JdbcUtils.getConnection();
			preparedStatement = connection.prepareStatement(sql);
			/*
			 * 循环数组，将其参数赋值
			 */
			for (int i = 0; i < params.length; i++) {
				preparedStatement.setObject(i + 1, params[i]);
			}
			
			resultSet = preparedStatement.executeQuery();
			return mbp.toBean(resultSet, clazz);
		/*	try {
				if(resultSet.next()) {
					
					 * 通过反射来获取对象的构造函数
					 
					Constructor constructor = clazz.getConstructor(ResultSet.class);
					return constructor.newInstance(resultSet);
				}
				return null;
			} catch (NoSuchMethodException | SecurityException
					| InstantiationException | IllegalAccessException
					| IllegalArgumentException | InvocationTargetException e) {
				throw new RuntimeException(e);
			}*/
		} catch (SQLException e) {
			throw new RuntimeException(e);
		} finally {
			try {
				if(connection != null) connection.close();
				if(preparedStatement != null) preparedStatement.close();
				if(resultSet != null) resultSet.close();
			} catch (SQLException e) {}
			
		}
	}
	/**
	 * 
	 * @Description: querys方法返回多条记录
	 * 	需要提供sql语句和参数
	 * @param @param sql
	 * @param @param params
	 * @param @return   
	 * @return List<Student>  
	 * @throws
	 */
	public<T> List<T> queryList(String sql,Class<T> clazz, Object... params) {
		Connection connection = null;
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;
		try {
			connection = JdbcUtils.getConnection();
			preparedStatement = connection.prepareStatement(sql);
			/*
			 * 循环数组，将其参数赋值
			 */
			for (int i = 0; i < params.length; i++) {
				preparedStatement.setObject(i + 1, params[i]);
			}
			
			resultSet = preparedStatement.executeQuery();
			return mbp.toBeanList(resultSet, clazz);
		  /*List list = new ArrayList();
			try {
				while(resultSet.next()) {
					
					 * 通过反射来获取对象的构造函数
					 
					Constructor constructor = clazz.getConstructor(ResultSet.class);
					list.add(constructor.newInstance(resultSet));
				}
			} catch (NoSuchMethodException | SecurityException
					| InstantiationException | IllegalAccessException
					| IllegalArgumentException | InvocationTargetException e) {
				throw new RuntimeException(e);
			}
			return list;*/
		} catch (SQLException e) {
			throw new RuntimeException(e);
		} finally {
			try {
				if(connection != null) connection.close();
				if(preparedStatement != null) preparedStatement.close();
				if(resultSet != null) resultSet.close();
			} catch (SQLException e) {}
			
		}
	}
	/**
	 * 
	 * @Description 可以更新数据添加或者删除
	 * @param @param sql
	 * @param @param params   
	 * @return void  
	 * @throws
	 */
	public void update(String sql, Object... params) {
		Connection connection = null;
		PreparedStatement preparedStatement = null;
		try {
			connection = JdbcUtils.getConnection();
			preparedStatement = connection.prepareStatement(sql);
			/*
			 * 循环数组，将其参数赋值
			 */
			for (int i = 0; i < params.length; i++) {
				preparedStatement.setObject(i + 1, params[i]);
			}
			preparedStatement.executeUpdate();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		} finally {
			try {
				if(connection != null) connection.close();
				if(preparedStatement != null) preparedStatement.close();
			} catch (SQLException e) {}
			
		}
	}
	/**
	 * 用来统计行数，显示单行单列
	 */
	public int querySingle(String sql) {
		Connection connection = null;
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;
		try {
			connection =JdbcUtils.getConnection();
			preparedStatement = connection.prepareStatement(sql);
			resultSet = preparedStatement.executeQuery();
			if(resultSet.next()) {
				return resultSet.getInt(1);
			}
			return 0;
		} catch (SQLException e) {
		throw new RuntimeException(e);
		} finally {
			try {
				if(connection != null) connection.close();
				if(preparedStatement != null) preparedStatement.close();
				if(resultSet != null) resultSet.close();
			} catch (SQLException e) {}
		}
	}
	

}
