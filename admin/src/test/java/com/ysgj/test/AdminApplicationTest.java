package com.ysgj.test;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * 该类为单元测试类
 * @author Administrator
 *
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AdminApplicationTest {

	
//	@Before：初始化方法
//	@After：释放资源
//	@Test：测试方法，在这里可以测试期望异常和超时时间
//	@Ignore：忽略的测试方法
	
	@Before
	public void Before(){
		System.out.println("单元测试开始之前执行的方法....");
	}
	
	
	@Test
	public void test() throws Exception {
		System.out.println("单元测试方法被执行...");
	}
	
	@After
	public void After(){
		System.out.println("单元测试结束之后执行的方法。");
	}
	
	@Ignore
	public void Ignore(){
		System.out.println("单元测试忽略的方法...");
	}
}
