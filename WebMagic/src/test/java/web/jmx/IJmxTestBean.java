package web.jmx;

/**
 * 
 * @author 刘飞 E-mail:liufei_it@126.com
 * @version 1.0
 * @since 2014年4月16日 下午7:36:09
 */
public interface IJmxTestBean {

	public int add(int x, int y);

	public long myOperation();

	public int getAge();

	public void setAge(int age);

	public void setName(String name);

	public String getName();
}
