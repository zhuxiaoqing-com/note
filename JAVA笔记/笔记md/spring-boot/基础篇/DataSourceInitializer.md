```java
org.springframework.boot.autoconfigure.jdbc.DataSourceInitializer#init
@PostConstruct
	public void init() {
		if (!this.properties.isInitialize()) {
			logger.debug("Initialization disabled (not running DDL scripts)");
			return;
		}
		if (this.applicationContext.getBeanNamesForType(DataSource.class, false,
				false).length > 0) {
			this.dataSource = this.applicationContext.getBean(DataSource.class);
		}
		if (this.dataSource == null) {
			logger.debug("No DataSource found so not initializing");
			return;
		}
		runSchemaScripts();
	}
	
private void runSchemaScripts() {
    // 进入
    // org.springframework.boot.autoconfigure.jdbc.DataSourceProperties#schema
    // private List<String> schema; spring.datasource.schema
		List<Resource> scripts = getScripts("spring.datasource.schema",
				this.properties.getSchema(), "schema");
		if (!scripts.isEmpty()) {
			String username = this.properties.getSchemaUsername();
			String password = this.properties.getSchemaPassword();
            // 反正就是运行语句
			runScripts(scripts, username, password);
			try {
				this.applicationContext
						.publishEvent(new DataSourceInitializedEvent(this.dataSource));
				// The listener might not be registered yet, so don't rely on it.
				if (!this.initialized) {
					runDataScripts();
					this.initialized = true;
				}
			}
			catch (IllegalStateException ex) {
				logger.warn("Could not send event to complete DataSource initialization ("
						+ ex.getMessage() + ")");
			}
		}
	}


private List<Resource> getScripts(String propertyName, List<String> resources,
			String fallback) {
    // fallback = schema
     // 因为我们没有配置所有是 null
		if (resources != null) {
			return getResources(propertyName, resources, true);
		}
     	// private String platform = "all"; 也可以   spring.datasource.platform
		String platform = this.properties.getPlatform();
		List<String> fallbackResources = new ArrayList<String>();
    	
    // 从 classpath*: schema-all.sql 
		fallbackResources.add("classpath*:" + fallback + "-" + platform + ".sql");
    // 从 classpath*: schema.sql 
		fallbackResources.add("classpath*:" + fallback + ".sql");
    // 获取资源
		return getResources(propertyName, fallbackResources, false);
	}
```

runScripts(scripts, username, password);

```java
private void runScripts(List<Resource> resources, String username, String password) {
		if (resources.isEmpty()) {
			return;
		}
    	// 进行一系列处理
		ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
		populator.setContinueOnError(this.properties.isContinueOnError());
		populator.setSeparator(this.properties.getSeparator());
		if (this.properties.getSqlScriptEncoding() != null) {
			populator.setSqlScriptEncoding(this.properties.getSqlScriptEncoding().name());
		}
    	// 将资源添加到 populator 中
		for (Resource resource : resources) {
           	//  this.scripts.add(script);
			populator.addScript(resource);
		}
    	// 获取 DataSource
		DataSource dataSource = this.dataSource;
    	// 都是 null  uaername 和 password
		if (StringUtils.hasText(username) && StringUtils.hasText(password)) {
			dataSource = DataSourceBuilder.create(this.properties.getClassLoader())
					.driverClassName(this.properties.determineDriverClassName())
					.url(this.properties.determineUrl()).username(username)
					.password(password).build();
		}
    	// 进行执行语句
		DatabasePopulatorUtils.execute(populator, dataSource);
	}
```

DatabasePopulatorUtils.execute(populator, dataSource);

```java
org.springframework.jdbc.datasource.init.DatabasePopulatorUtils#execute

public static void execute(DatabasePopulator populator, DataSource dataSource) throws DataAccessException {
        Assert.notNull(populator, "DatabasePopulator must not be null");
        Assert.notNull(dataSource, "DataSource must not be null");

        try {
        	// 获取 Connection
            Connection connection = DataSourceUtils.getConnection(dataSource);

            try {
            	// 进行执行
                populator.populate(connection);
            } finally {
            	// 关闭资源  con.close();
                DataSourceUtils.releaseConnection(connection, dataSource);
            }

        } catch (Throwable var7) {
            if (var7 instanceof ScriptException) {
                throw (ScriptException)var7;
            } else {
                throw new UncategorizedScriptException("Failed to execute database script", var7);
            }
        }
    }
```

 populator.populate(connection);

```java
org.springframework.jdbc.datasource.init.ResourceDatabasePopulator#populate

public void populate(Connection connection) throws ScriptException {
    Assert.notNull(connection, "Connection must not be null");
    Iterator var2 = this.scripts.iterator();
	// 进行遍历
    while(var2.hasNext()) {
        Resource script = (Resource)var2.next();
        EncodedResource encodedScript = new EncodedResource(script, this.sqlScriptEncoding);
        // 执行
        ScriptUtils.executeSqlScript(connection, encodedScript, this.continueOnError, this.ignoreFailedDrops, this.commentPrefix, this.separator, this.blockCommentStartDelimiter, this.blockCommentEndDelimiter);
    }

}
```

ScriptUtils.executeSqlScript(...);

```java
org.springframework.jdbc.datasource.init.ScriptUtils#executeSqlScript(...)
    
public static void executeSqlScript(Connection connection, EncodedResource resource, boolean continueOnError, boolean ignoreFailedDrops, String commentPrefix, String separator, String blockCommentStartDelimiter, String blockCommentEndDelimiter) throws ScriptException {
        try {
            if (logger.isInfoEnabled()) {
                logger.info("Executing SQL script from " + resource);
            }
			// 开启计时
            long startTime = System.currentTimeMillis();

            String script;
            try {
               // 解析 .sql 文件 获取全部内容
                script = readScript(resource, commentPrefix, separator);
            } catch (IOException var27) {
                throw new CannotReadScriptException(resource, var27);
            }

            if (separator == null) {
                // 使用 ; 进行分割
                separator = ";";
            }

            if (!"^^^ END OF SCRIPT ^^^".equals(separator) && !containsSqlScriptDelimiters(script, separator)) {
                separator = "\n";
            }
			// 将切分好的 sql 语句存入 statements 中
            List<String> statements = new LinkedList();
            // 进行切分
            splitSqlScript(resource, script, separator, commentPrefix, blockCommentStartDelimiter, blockCommentEndDelimiter, statements);
            int stmtNumber = 0;
            // com.alibaba.druid.proxy.jdbc.StatementProxyImpl@17b37e9a 代理对象
            Statement stmt = connection.createStatement();

            try {
                Iterator var14 = statements.iterator();
			// 进行循环执行语句
                while(var14.hasNext()) {
                    String statement = (String)var14.next();
                    ++stmtNumber;

                    try {
                        // 执行语句
                        stmt.execute(statement);
                        int rowsAffected = stmt.getUpdateCount();
                        if (logger.isDebugEnabled()) {
                            logger.debug(rowsAffected + " returned as update count for SQL: " + statement);

                            for(SQLWarning warningToLog = stmt.getWarnings(); warningToLog != null; warningToLog = warningToLog.getNextWarning()) {
                                logger.debug("SQLWarning ignored: SQL state '" + warningToLog.getSQLState() + "', error code '" + warningToLog.getErrorCode() + "', message [" + warningToLog.getMessage() + "]");
                            }
                        }
                    } catch (SQLException var28) {
                        boolean dropStatement = StringUtils.startsWithIgnoreCase(statement.trim(), "drop");
                        if (!continueOnError && (!dropStatement || !ignoreFailedDrops)) {
                            throw new ScriptStatementFailedException(statement, stmtNumber, resource, var28);
                        }

                        if (logger.isDebugEnabled()) {
                            logger.debug(ScriptStatementFailedException.buildErrorMessage(statement, stmtNumber, resource), var28);
                        }
                    }
                }
            } finally {
                try {
                    stmt.close();
                } catch (Throwable var26) {
                    logger.debug("Could not close JDBC Statement", var26);
                }

            }
			// 计算语句执行总耗时
            long elapsedTime = System.currentTimeMillis() - startTime;
            if (logger.isInfoEnabled()) {
                logger.info("Executed SQL script from " + resource + " in " + elapsedTime + " ms.");
            }

        } catch (Exception var30) {
            if (var30 instanceof ScriptException) {
                throw (ScriptException)var30;
            } else {
                throw new UncategorizedScriptException("Failed to execute database script from resource [" + resource + "]", var30);
            }
        }
    }
```

 stmt.execute(statement);

```java
com.alibaba.druid.pool.DruidPooledStatement#execute(java.lang.String)
    
public final boolean execute(String sql) throws SQLException {
    	// check 语句
        checkOpen();
		// 不知道干嘛 应该是计算执行语句的次数 增量执行次数
        incrementExecuteCount();
        // 开启事务
        transactionRecord(sql);

        try {
            // 执行
            return stmt.execute(sql);
        } catch (Throwable t) {
            errorCheck(t);

            throw checkException(t, sql);
        }
    }

```

return stmt.execute(sql);

```java
com.alibaba.druid.proxy.jdbc.StatementProxyImpl#execute(java.lang.String)
    
@Override
    public boolean execute(String sql) throws SQLException {
        updateCount = null;
        lastExecuteSql = sql;
        lastExecuteType = StatementExecuteType.Execute;
        lastExecuteStartNano = -1L;
        lastExecuteTimeNano = -1L;
	// 创建执行链
        FilterChainImpl chain = createChain();
       // 执行
        firstResultSet = chain.statement_execute(this, sql);
        recycleFilterChain(chain);
        return firstResultSet;
    }
```

firstResultSet = chain.statement_execute(this, sql);

```java
com.alibaba.druid.filter.FilterChainImpl#statement_execute()
    
public boolean statement_execute(StatementProxy statement, String sql) throws SQLException {
    if (this.pos < filterSize) {
        // 执行
        return nextFilter().statement_execute(this, statement, sql);
    }
    return statement.getRawObject().execute(sql);
}
```

return nextFilter().statement_execute(this, statement, sql);

```java
com.alibaba.druid.filter.FilterEventAdapter#statement_execute(...)

public boolean statement_execute(FilterChain chain, StatementProxy statement, String sql) throws SQLException {
    // 前置处理
    statementExecuteBefore(statement, sql);

    try {
        // 执行
        boolean firstResult = super.statement_execute(chain, statement, sql);
		// 后置处理
        statementExecuteAfter(statement, sql, firstResult);

        return firstResult;
    } catch (SQLException error) {
        statement_executeErrorAfter(statement, sql, error);
        throw error;
    } catch (RuntimeException error) {
        statement_executeErrorAfter(statement, sql, error);
        throw error;
    } catch (Error error) {
        statement_executeErrorAfter(statement, sql, error);
        throw error;
    }
}
```

  // 执行
        boolean firstResult = super.statement_execute(chain, statement, sql);

```java
com.alibaba.druid.filter.FilterAdapter#statement_execute(...)
    
public boolean statement_execute(FilterChain chain, StatementProxy statement, String sql) throws SQLException {
    return chain.statement_execute(statement, sql);
}
```

又返回回来了  chain.statement_execute(statement, sql);

```java
com.alibaba.druid.filter.FilterAdapter#statement_execute(...)
    public boolean statement_execute(FilterChain chain, StatementProxy statement, String sql) throws SQLException {
        return chain.statement_execute(statement, sql);
    }
    
```

chain.statement_execute(statement, sql); 最终

```java
com.alibaba.druid.filter.FilterChainImpl#statement_execute(...)
    
public boolean statement_execute(StatementProxy statement, String sql) throws SQLException {
    if (this.pos < filterSize) {
        return nextFilter().statement_execute(this, statement, sql);
    }
    // 进行执行语句 Statement getRawObject(); 返回 Statement 调用其 execute
    return statement.getRawObject().execute(sql);
}
```



调用结束至此