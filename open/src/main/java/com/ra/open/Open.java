package com.ra.open;

import com.ra.dao.factory.BaseRepositoryFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@ComponentScan(basePackages={"com.ra.*"})//扫描service、component、bean组件
@EnableJpaRepositories(repositoryFactoryBeanClass = BaseRepositoryFactory.class,basePackages = "com.ra.dao.*")//扫描JpaRepository组件
@EntityScan("com.ra.dao.*") // 扫描实体类
@EnableTransactionManagement // 启用事务注解
@EnableScheduling
@EnableAsync
public class Open {

	public static void main(String[] args) {
		SpringApplication.run(Open.class, args);
	}
}
