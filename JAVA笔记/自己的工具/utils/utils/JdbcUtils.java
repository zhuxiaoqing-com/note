package cn.zhu.utils.utils;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;


public class JdbcUtils {
/*
 * 加载mysql驱动
 */
	static Properties p;
 static {
	 // 加载properties流
	 try {
		InputStream is = JdbcUtils.class.getClassLoader().getResourceAsStream("jdbc-config.properties");
		 p = new Properties();
		 p.load(is);
		 Class.forName(p.getProperty("driverClassName"));
	} catch (ClassNotFoundException | IOException e) {
		throw new RuntimeException(e);
	}
 }
 
 public static Connection getConnection(){
	 try {
		return DriverManager.getConnection(p.getProperty("url"), p.getProperty("username"), p.getProperty("password"));
	} catch (SQLException e) {
		throw new RuntimeException(e);
	}
 }
}
