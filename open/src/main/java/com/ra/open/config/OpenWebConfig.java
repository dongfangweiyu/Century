package com.ra.open.config;


import com.ra.open.Interceptor.OpenRequestLogInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * Spring Boot 应用程序的配置器
 * 包括：
 * MVC拦截器注册
 * 等等
 * @author ouyang
 *
 */
@SpringBootConfiguration
public class OpenWebConfig extends WebMvcConfigurerAdapter {

	/**
	 * 配置请求拦截器
	 */
	@Override  
    public void addInterceptors(InterceptorRegistry registry) {  
        
		//注册自定义拦截器，添加拦截路径和排除拦截路径  

		//API请求日志拦截器
		registry.addInterceptor(new OpenRequestLogInterceptor()).addPathPatterns("/**")
				.excludePathPatterns("/static/**")
//				.excludePathPatterns("/public/otc/**")
		.excludePathPatterns("/static/")
		.excludePathPatterns("/error");
	}

	/**
	 * 添加自定义的静态资源映射
	 这里使用代码的方式自定义目录映射，并不影响Spring Boot的默认映射，可以同时使用。
	 */
	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/static/**").addResourceLocations("classpath:/static/");

		//swagger不拦截为404
		registry.addResourceHandler("swagger-ui.html")
				.addResourceLocations("classpath:/META-INF/resources/");
		super.addResourceHandlers(registry);
	}


//	@InitBinder
//	protected void init(HttpServletRequest request, ServletRequestDataBinder binder) {
//		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//		dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));/*TimeZone时区，解决差8小时的问题*/
//		binder.registerCustomEditor(Date.class, new CustomDateEditor(dateFormat, false));
//	}
//    /**
//     * HTTP请求数据格式转换
//     * 把from params格式转换成body json
//     * Content-type:application/json
//     */
//    @Override
//    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
//        super.configureMessageConverters(converters);
//        //1.需要定义一个convert转换消息的对象;
//        FastJsonHttpMessageConverter fastJsonHttpMessageConverter = new FastJsonHttpMessageConverter();
//        //2.添加fastJson的配置信息，比如：是否要格式化返回的json数据;
//        FastJsonConfig fastJsonConfig = new FastJsonConfig();
//        fastJsonConfig.setSerializerFeatures(SerializerFeature.PrettyFormat);
//        //3处理中文乱码问题
//        List<MediaType> fastMediaTypes = new ArrayList<>();
//        fastMediaTypes.add(MediaType.APPLICATION_JSON_UTF8);
//        //4.在convert中添加配置信息.
//        fastJsonHttpMessageConverter.setSupportedMediaTypes(fastMediaTypes);
//        fastJsonHttpMessageConverter.setFastJsonConfig(fastJsonConfig);
//        //5.将convert添加到converters当中.
//        converters.add(fastJsonHttpMessageConverter);
//    }
}
