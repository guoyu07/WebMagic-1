package web.services;

import java.util.ServiceLoader;

/**
 * 
 * @author 刘飞 E-mail:liufei_it@126.com
 * @version 1.0
 * @since 2014年4月25日 下午6:01:11
 */
public class Hello {

	public static void main(String[] args) {
		ServiceLoader<HelloService> loader = ServiceLoader.load(HelloService.class);
		for (HelloService helloService : loader) {
			System.out.println(helloService.getName());
		}
	}
}
