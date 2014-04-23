package web;

import java.net.URI;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 
 * @author 刘飞 E-mail:liufei_it@126.com
 * @version 1.0
 * @since 2014年4月23日 下午9:25:08
 */
public class URIBuilder {

	public static void main(String[] args) throws Exception {
		UriComponents uriComponents1 = UriComponentsBuilder.fromUriString(
				"http://example.com/hotels/{hotel}/bookings/{booking}").build();
		URI uri = uriComponents1.expand("42", "21").encode().toUri();

		System.out.println("uri : " + uri);

		UriComponents uriComponents2 = UriComponentsBuilder.newInstance().scheme("http").host("example.com")
				.path("/hotels/{hotel}/bookings/{booking}").build().expand("42", "21").encode();
		System.out.println("u : " + uriComponents2.toUriString());

		HttpServletRequest request = null;

		ServletUriComponentsBuilder ucb1 = ServletUriComponentsBuilder.fromRequest(request);
		ucb1.replaceQueryParam("accountId", "{id}");
		ucb1.build().expand("123").encode();

		ServletUriComponentsBuilder ucb2 = ServletUriComponentsBuilder.fromContextPath(request);
		ucb2.path("/accounts");
		ucb2.build();
		
//		UriComponents uriComponents3 = MvcUriComponentsBuilder.fromMethodName(BookingController.class, "getBooking",21);
//		uriComponents3.buildAndExpand(42);
//
//			URI uri123 = uriComponents3.encode().toUri();
		
//		UriComponents uriComponents = MvcUriComponentsBuilder
//			    .fromMethodCall(on(BookingController.class).getBooking(21)).buildAndExpand(42);
//
//			URI uri = uriComponents.encode().toUri();
		
		
		/*<bean id="localeResolver" class="org.springframework.web.servlet.i18n.CookieLocaleResolver">

		    <property name="cookieName" value="clientlanguage"/>
	
		    <!-- in seconds. If set to -1, the cookie is not persisted (deleted when browser shuts down) -->
		    <property name="cookieMaxAge" value="100000">
	
		</bean>
		*/
		
//		<bean id="localeChangeInterceptor"
//		        class="org.springframework.web.servlet.i18n.LocaleChangeInterceptor">
//		    <property name="paramName" value="siteLanguage"/>
//		</bean>
//
//		<bean id="localeResolver"
//		        class="org.springframework.web.servlet.i18n.CookieLocaleResolver"/>
//
//		<bean id="urlMapping"
//		        class="org.springframework.web.servlet.handler.SimpleUrlHandlerMapping">
//		    <property name="interceptors">
//		        <list>
//		            <ref bean="localeChangeInterceptor"/>
//		        </list>
//		    </property>
//		    <property name="mappings">
//		        <value>/*/.view=someController</value>
//		    </property>
//		</bean>
	}
}
