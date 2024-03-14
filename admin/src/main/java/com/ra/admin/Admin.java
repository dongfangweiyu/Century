package com.ra.admin;

import com.ra.dao.factory.BaseRepositoryFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Spring Boot 应用程序的唯一入口
 * @author Administrator
 *
 */
@SpringBootApplication
@ComponentScan(basePackages={"com.ra.*"})//扫描service、component、bean组件
@EnableJpaRepositories(repositoryFactoryBeanClass = BaseRepositoryFactory.class,basePackages = "com.ra.dao.*")//扫描JpaRepository组件
@EntityScan("com.ra.dao.*") // 扫描实体类
@EnableTransactionManagement//启动事务管理器
public class Admin extends SpringBootServletInitializer {

	public static void main(String[] args) {
		SpringApplication.run(Admin.class, args);
	}
}
