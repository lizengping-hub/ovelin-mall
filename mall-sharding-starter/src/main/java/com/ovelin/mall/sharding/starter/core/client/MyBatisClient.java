package com.ovelin.mall.sharding.starter.core.client;

import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import javax.sql.DataSource;
import java.util.Objects;

public class MyBatisClient {

    private final SqlSessionTemplate sqlSessionTemplate;

    public MyBatisClient(
            DataSource dataSource,
            String typeAliasesPackage){
        SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
        bean.setDataSource(dataSource);
        bean.setTypeAliasesPackage(typeAliasesPackage);

        Configuration cfg = new Configuration();
        cfg.setMapUnderscoreToCamelCase(true);
        cfg.setCacheEnabled(false);
        registerMappers(cfg, typeAliasesPackage);
        bean.setConfiguration(cfg);
        SqlSessionFactory sqlSessionFactory = null;
        try {
            sqlSessionFactory = bean.getObject();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (sqlSessionFactory == null) {
            throw new IllegalStateException(
                    "Failed to create SqlSessionFactory for DataSource: " + dataSource);
        }
        this.sqlSessionTemplate = new SqlSessionTemplate(sqlSessionFactory);
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
    public <M> M mapper(Class<M> type) {
        return sqlSessionTemplate.getMapper(type);
    }
}