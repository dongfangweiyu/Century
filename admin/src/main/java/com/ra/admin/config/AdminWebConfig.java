package com.ra.admin.config;


import com.ra.admin.Interceptor.AdminTokenInterceptor;
import com.ra.admin.Interceptor.AdminPermissionInterceptor;
import com.ra.common.utils.PropertyUtil;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.yeauty.standard.ServerEndpointExporter;

/**
 * Spring Boot 应用程序的配置器
 * 包括：
 * MVC拦截器注册
 * 等等
 * @author Administrator
 *
 */
@SpringBootConfiguration
public class AdminWebConfig extends WebMvcConfigurerAdapter {

	/**
	 * 检测和注册endpoints
	 * @return
	 */
	@Bean
	public ServerEndpointExporter serverEndpointExporter(){
		return new ServerEndpointExporter();
	}
	/**
	 * 配置拦截器
	 */
	@Override  
    public void addInterceptors(InterceptorRegistry registry) {  
        
		//注册自定义拦截器，添加拦截路径和排除拦截路径  

		//admin登录拦截器
		registry.addInterceptor(
        		new AdminTokenInterceptor())
        		.addPathPatterns("/**/private/**")
        		.addPathPatterns("/private/**") //拦截路径
        		.excludePathPatterns("/**/public/**") //排除路径
        		.excludePathPatterns("/public/**"); //排除路径

		//admin权限拦截器
		registry.addInterceptor(
				new AdminPermissionInterceptor())
				.addPathPatterns("/**/private/**")
				.addPathPatterns("/private/**") //拦截路径
				.excludePathPatterns("/**/public/**") //排除路径
				.excludePathPatterns("/public/**"); //排除路径
	}

	/**
     * 添加自定义的静态资源映射
      这里使用代码的方式自定义目录映射，并不影响Spring Boot的默认映射，可以同时使用。
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

		registry.addResourceHandler("/static/**").addResourceLocations("classpath:/static/").addResourceLocations("file:"+ PropertyUtil.getValue("spring.servlet.multipart.location")+"/");
//		registry.addResourceHandler("/**").addResourceLocations("/");
        super.addResourceHandlers(registry);
    }
}
