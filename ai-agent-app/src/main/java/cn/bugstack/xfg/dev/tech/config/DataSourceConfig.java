package cn.bugstack.xfg.dev.tech.config;

import com.zaxxer.hikari.HikariDataSource;
// 👇 1. 新增了这个导包
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * 数据库多数据源配置类
 * 核心职责：将 MySQL(业务数据) 和 PGVector(向量数据) 隔离管理，并为它们分别装配好操作工具（MyBatis / JdbcTemplate）
 */
@Configuration
public class DataSourceConfig {

    // ==================== 第一部分：MySQL 主数据源配置 (用于存取业务配置，如 ai_client 等表) ====================

    /**
     * 配置 MySQL 数据源 (使用高性能的 HikariCP 连接池)
     * @Primary 注解非常关键：当系统中有多个 DataSource 时，告诉 Spring 默认使用这一个。
     * 这样如果你在其他地方直接 @Resource DataSource，拿到的一定是 MySQL 这个主库。
     */
    @Bean("mysqlDataSource")
    @Primary
    public DataSource mysqlDataSource(@Value("${spring.datasource.mysql.driver-class-name}") String driverClassName,
                                      @Value("${spring.datasource.mysql.url}") String url,
                                      @Value("${spring.datasource.mysql.username}") String username,
                                      @Value("${spring.datasource.mysql.password}") String password,
                                      @Value("${spring.datasource.mysql.hikari.maximum-pool-size:10}") int maximumPoolSize,
                                      @Value("${spring.datasource.mysql.hikari.minimum-idle:5}") int minimumIdle,
                                      @Value("${spring.datasource.mysql.hikari.idle-timeout:30000}") long idleTimeout,
                                      @Value("${spring.datasource.mysql.hikari.connection-timeout:30000}") long connectionTimeout,
                                      @Value("${spring.datasource.mysql.hikari.max-lifetime:1800000}") long maxLifetime) {
        // 实例化并配置 Hikari 连接池
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName(driverClassName);
        dataSource.setJdbcUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);

        // 连接池调优参数
        dataSource.setMaximumPoolSize(maximumPoolSize); // 最大连接数
        dataSource.setMinimumIdle(minimumIdle);         // 最小空闲连接数
        dataSource.setIdleTimeout(idleTimeout);         // 空闲连接存活最大时间
        dataSource.setConnectionTimeout(connectionTimeout); // 获取连接的超时时间
        dataSource.setMaxLifetime(maxLifetime);         // 连接在池中的最大生命周期
        dataSource.setPoolName("MainHikariPool");       // 给线程池起个名字，方便排查日志

        return dataSource;
    }

    /**
     * 配置 MyBatis 的核心工厂，并【强制绑定】到 MySQL 数据源上
     * @Qualifier("mysqlDataSource") 确保 MyBatis 绝不会去连那个向量数据库
     */
    @Bean("sqlSessionFactory")
    public SqlSessionFactoryBean sqlSessionFactory(@Qualifier("mysqlDataSource") DataSource mysqlDataSource) throws Exception {
        SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
        sqlSessionFactoryBean.setDataSource(mysqlDataSource);

        // 告诉 MyBatis 去哪里找它的全局配置文件 (通常用来配一些插件、别名、驼峰转换等)
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        sqlSessionFactoryBean.setConfigLocation(resolver.getResource("classpath:/mybatis/config/mybatis-config.xml"));

        // 告诉 MyBatis 去哪里找 XML 映射文件 (你写的那些 SQL 语句都在这里)
        sqlSessionFactoryBean.setMapperLocations(resolver.getResources("classpath:/mybatis/mapper/*.xml"));

        return sqlSessionFactoryBean;
    }

    /**
     * 配置 SqlSessionTemplate，它是 MyBatis 供 Spring 使用的线程安全、支持事务的 SqlSession 操作模板
     */
    @Bean("sqlSessionTemplate")
    // 👇 2. 这里的参数从 SqlSessionFactoryBean 改成了 SqlSessionFactory
    public SqlSessionTemplate sqlSessionTemplate(@Qualifier("sqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        // 👇 3. 直接传入 sqlSessionFactory 产品，不需要再调 getObject() 了
        return new SqlSessionTemplate(sqlSessionFactory);
    }


    // ==================== 第二部分：PGVector 副数据源配置 (专用于 RAG 知识库向量存储) ====================

    /**
     * 配置 PostgreSQL(PgVector) 数据源
     * 注意：这里绝对不能加 @Primary，否则会和 MySQL 打架。
     */
    @Bean("pgVectorDataSource")
    public DataSource pgVectorDataSource(@Value("${spring.datasource.pgvector.driver-class-name}") String driverClassName,
                                         @Value("${spring.datasource.pgvector.url}") String url,
                                         @Value("${spring.datasource.pgvector.username}") String username,
                                         @Value("${spring.datasource.pgvector.password}") String password,
                                         @Value("${spring.datasource.pgvector.hikari.maximum-pool-size:5}") int maximumPoolSize,
                                         @Value("${spring.datasource.pgvector.hikari.minimum-idle:2}") int minimumIdle,
                                         @Value("${spring.datasource.pgvector.hikari.idle-timeout:30000}") long idleTimeout,
                                         @Value("${spring.datasource.pgvector.hikari.connection-timeout:30000}") long connectionTimeout) {

        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName(driverClassName);
        dataSource.setJdbcUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);

        // 向量库并发通常不如主库高，所以最大连接数(5)和最小空闲数(2)设得比较小，节省资源
        dataSource.setMaximumPoolSize(maximumPoolSize);
        dataSource.setMinimumIdle(minimumIdle);
        dataSource.setIdleTimeout(idleTimeout);
        dataSource.setConnectionTimeout(connectionTimeout);

        // 高可用防护机制：确保 Spring Boot 启动时，如果连不上 PG 数据库，能快速失败报错，而不是一直卡着
        dataSource.setInitializationFailTimeout(1);  // 1ms 快速失败检测
        dataSource.setConnectionTestQuery("SELECT 1"); // 拿一个最简单的 SQL 测试连接是否通畅
        dataSource.setAutoCommit(true);              // 自动提交事务
        dataSource.setPoolName("PgVectorHikariPool"); // 专属连接池名称
        return dataSource;
    }

    /**
     * 为 PGVector 专属配置一个 JdbcTemplate
     * 作用：Spring AI 框架底层的 PgVectorStore 组件，默认就是通过 JdbcTemplate 去执行向量比对 SQL（比如 <=> 符号）的。
     * @Qualifier("pgVectorDataSource") 确保这个 JdbcTemplate 拿着的是 PG 的连接钥匙。
     */
    @Bean("pgVectorJdbcTemplate")
    public JdbcTemplate pgVectorJdbcTemplate(@Qualifier("pgVectorDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

}