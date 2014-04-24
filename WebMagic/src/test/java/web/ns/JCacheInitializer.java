package web.ns;

/**
 * 
 * @author 刘飞 E-mail:liufei_it@126.com
 * @version 1.0
 * @since 2014年4月24日 下午1:32:19
 */
public class JCacheInitializer {
	private String name;

    public JCacheInitializer(String name) {
        this.name = name;
    }

    public void initialize() {
        // lots of JCache API calls to initialize the named cache...
    	System.err.println("JCacheInitializer.initialize()");
    }
}
