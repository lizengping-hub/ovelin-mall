package com.ovelin.mall.sharding.starter.core.client;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.DynamicTableNameInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.ovelin.mall.sharding.starter.api.router.ResolvedRoute;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import javax.sql.DataSource;
import java.util.Objects;

public class MyBatisPlusClient {
    private static final Logger logger = LoggerFactory.getLogger(MyBatisPlusClient.class);
    // 拦截器无状态，可以所有实例共用一份
    private static final MybatisPlusInterceptor PLUGIN_INTERCEPTOR = buildInterceptor();

    private final SqlSessionTemplate sqlSessionTemplate;

    public MyBatisPlusClient(DataSource dataSource, String typeAliasesPackage) {
        MybatisSqlSessionFactoryBean bean = new MybatisSqlSessionFactoryBean();
        bean.setDataSource(dataSource);
        bean.setTypeAliasesPackage(typeAliasesPackage);

        MybatisConfiguration cfg = new MybatisConfiguration();
        cfg.setMapUnderscoreToCamelCase(true);
        cfg.setCacheEnabled(false);
        bean.setConfiguration(cfg);

        // MP 全局配置：逻辑删除、自动填充、ID 生成策略等
        GlobalConfig globalConfig = GlobalConfigUtils.defaults();
        globalConfig.setBanner(false);
        bean.setGlobalConfig(globalConfig);

        bean.setPlugins(PLUGIN_INTERCEPTOR);

        registerMappers(cfg, typeAliasesPackage); // 你原来的扫描逻辑保持不变，
        // @Mapper 接口不管是不是继承 BaseMapper 都能扫到

        SqlSessionFactory sqlSessionFactory;
        try {
            sqlSessionFactory = bean.getObject();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        this.sqlSessionTemplate = new SqlSessionTemplate(sqlSessionFactory);
    }


    private static MybatisPlusInterceptor buildInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        DynamicTableNameInnerInterceptor dynamicTableName = new DynamicTableNameInnerInterceptor();
        dynamicTableName.setTableNameHandler((sql, tableName) -> {
            ResolvedRoute route = TableRouteContext.get();
            if (route == null) {
                throw new IllegalStateException(
                        "No shard route in context for table '" + tableName +
                                "'. Must be called through ShardedMyBatisPlusExecutor.");
            }
            String resolvedTableName = route.resolveTableName(tableName);
            logger.debug("Resolved table name '{}' for original table '{}'", resolvedTableName, tableName);
            return resolvedTableName;
        });
        interceptor.addInnerInterceptor(dynamicTableName);
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    public <M> M mapper(Class<M> type) {
        return sqlSessionTemplate.getMapper(type);
    }
    private void registerMappers(Configuration cfg, String basePackage) {
        // 默认的 ClassPathScanningCandidateComponentProvider 只接受具体类（isConcrete），
        // 会把 @Mapper 接口过滤掉，所以这里必须覆写 isCandidateComponent 以放行接口，
        // 否则扫描结果永远为空，Mapper 不会被注册到 MyBatis 的 Configuration 中。
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false) {
                    @Override
                    protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
                        return beanDefinition.getMetadata().isInterface()
                                && beanDefinition.getMetadata().isIndependent();
                    }
                };
        scanner.addIncludeFilter(new AnnotationTypeFilter(
                org.apache.ibatis.annotations.Mapper.class));

        for (BeanDefinition bd : scanner.findCandidateComponents(basePackage)) {
            try {
                cfg.addMapper(Class.forName(Objects.requireNonNull(bd.getBeanClassName())));
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("Failed to register mapper: " + bd.getBeanClassName(), e);
            }
        }
    }
}