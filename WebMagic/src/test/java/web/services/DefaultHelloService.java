package web.services;

/**
 * 
 * @author 刘飞 E-mail:liufei_it@126.com
 * @version 1.0
 * @since 2014年4月25日 下午5:59:25
 */
public class DefaultHelloService implements HelloService {

	@Override
	public String getName() {
		return "hello liufei";
	}

}
