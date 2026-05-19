package com.collab.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MybatisPlusConfig {
    // MyBatis-Plus 会自动配置 SqlSessionFactory
    // 如果需要自定义配置，可以在这里添加

    /**
     * 实现MyBatis-Plus分页插件配置
     * 用于实现数据库分页查询功能
     * @return
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(){
        //MyBatis-Plus 的拦截器核心类
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        //分页拦截器
        interceptor.addInnerInterceptor(
                new PaginationInnerInterceptor(DbType.MYSQL)
        );

        return interceptor;
    }
}
