package web.magic;

import java.lang.management.OperatingSystemMXBean;

import sun.management.ManagementFactory;

/**
 * 
 * @author 刘飞 E-mail:liufei_it@126.com
 * @version 1.0
 * @since 2014年4月12日 下午6:08:37
 */
public class Sys {

	public static void main(String[] args) {
		OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
		System.out.println("sun : " + operatingSystemMXBean.getSystemLoadAverage());
	}
}
