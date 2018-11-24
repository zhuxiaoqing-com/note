package cn.zhu.utils;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import com.mchange.v2.c3p0.ComboPooledDataSource;

public class JdbcUtils {
	/*因为现在还不能处理并发，而且加锁会严重影响性能。所有我们现在要使用ThreadLocal线程池
	 * 将其涉及到并发处理的connection加入到线程池。
	 * */
	
	private static ThreadLocal<Connection> threadLocal = new ThreadLocal<Connection>();
	// 配置文件的默认配置！要求你给出一个c3p0-config.xml！！！
	private static ComboPooledDataSource  dataSource = new ComboPooledDataSource();
	/**
	 *  使用连接池返回一个连接池对象
	 */
	public static Connection getConnection() throws SQLException{
		if (threadLocal.get() != null) return threadLocal.get();
		return dataSource.getConnection();
	}
	/**
	 * 返回连接池对象
	 */
	public static DataSource getDataSource(){
		return dataSource;
	}
	/*
	 * 开启事务
	 */
	public static void beginTransaction() throws SQLException {
		if (threadLocal.get() != null) throw new SQLException("您的事务已经存在！请勿重复开启！");
		threadLocal.set(JdbcUtils.getConnection());
		threadLocal.get().setAutoCommit(false);
	}
	
	/*
	 * 提交事务
	 */
	public static void commitTransaction() throws SQLException{
		if (threadLocal.get() == null) throw new SQLException("您的事务还未开启或已提交，请勿提交");
		threadLocal.get().commit();
		threadLocal.get().close();
		threadLocal.remove();
	}
	/*
	 * 回滚事务
	 */
	public static void rollbackTransaction() throws SQLException{
		if (threadLocal.get() == null) throw new SQLException("您的事务已提交或不存在，请勿回滚");
		threadLocal.get().rollback();
		threadLocal.get().close();
		threadLocal.remove();
	}
	/*
	 * 释放连接
	 */
	public static void releaseConnection(Connection conn) throws SQLException{
	    /*
	     * 判断是不是事务连接，事务连接会自动关闭
	     * 如果threadLocal当前线程没有连接connection，说明没有开启事务，将其关闭
	     * 如果threadLocal里面有connection，但是传进来的连接不是threadLocal里面的连接
	     * 也将其关闭
	     */
		if (threadLocal.get() == null) {
		    conn.close();
		    return;
		}
		if (conn != threadLocal.get()) conn.close();
	}
}
