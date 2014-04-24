package web.ns;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * 
 * @author 刘飞 E-mail:liufei_it@126.com
 * @version 1.0
 * @since 2014年4月24日 上午11:43:02
 */
public class DateFormatNamespaceHandler extends NamespaceHandlerSupport {

	public static void main(String[] args) {
		ApplicationContext context = new ClassPathXmlApplicationContext("/dateFormat.xml", "/component.xml");
		
		SimpleDateFormat dateFormat = context.getBean("defaultDateFormat", SimpleDateFormat.class);
		
		System.out.println(dateFormat.format(new Date()));
		
		Component component = context.getBean("bionic-family", Component.class);
		
		System.out.println(component);
	}
	
	@Override
	public void init() {
		registerBeanDefinitionParser("dateformat", new DateFormatBeanDefinitionParser());
	}
}
