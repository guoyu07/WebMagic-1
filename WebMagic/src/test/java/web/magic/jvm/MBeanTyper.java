package web.magic.jvm;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

class MBeanTyper {
	static final boolean DEBUG = Boolean.getBoolean("jboss.jmx.debug");

	/**
	 * create a typed object from an mbean
	 */
	public static final Object typeMBean(MBeanServer server, ObjectName mbean, Class<?> mainInterface) throws Exception {
		List<Class<?>> interfaces = new ArrayList<Class<?>>();
		if (mainInterface.isInterface()) {
			interfaces.add(mainInterface);
		}
		addInterfaces(mainInterface.getInterfaces(), interfaces);
		Class<?> cl[] = (Class[]) interfaces.toArray(new Class[interfaces.size()]);
		if (DEBUG) {
			System.err.println("typeMean->server=" + server + ",mbean=" + mbean + ",mainInterface=" + mainInterface);
			for (int c = 0; c < cl.length; c++) {
				System.err.println("     :" + cl[c]);
			}
		}

		return Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), cl, new MBeanTyperInvoker(server,
				mbean));
	}

	private static final void addInterfaces(Class<?> cl[], List<Class<?>> list) {
		if (cl == null)
			return;
		for (int c = 0; c < cl.length; c++) {
			list.add(cl[c]);
			addInterfaces(cl[c].getInterfaces(), list);
		}
	}
}

/**
 * MBeanTyperInvoker handles method invocations against the MBeanTyper target
 * object and forwards them to the MBeanServer and ObjectName for invocation.
 * 
 * @author <a href="mailto:jhaynie@vocalocity.net">Jeff Haynie</a>
 */
final class MBeanTyperInvoker implements java.lang.reflect.InvocationHandler {
	private final MBeanServer server;

	private final ObjectName mbean;

	private final Map<Method, String[]> signatureCache = Collections.synchronizedMap(new HashMap<Method, String[]>());

	MBeanTyperInvoker(MBeanServer server, ObjectName mbean) {
		this.server = server;
		this.mbean = mbean;
	}

	private boolean isJMXAttribute(Method m) {
		String name = m.getName();
		return (name.startsWith("get"));

	}

	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		if (MBeanTyper.DEBUG) {
			System.err.println("  ++ method=" + method.getName() + ",args=" + args);
		}
		try {
			if (method.getDeclaringClass() == Object.class) {
				String name = method.getName();
				if (name.equals("hashCode")) {
					return new Integer(this.hashCode());
				} else if (name.equals("toString")) {
					return this.toString();
				} else if (name.equals("equals")) {
					// FIXME: this needs to be reviewed - we should be
					// smarter about this ...
					return new Boolean(equals(args[0]));
				}
			} else if (isJMXAttribute(method) && (args == null || args.length <= 0)) {
				String name = method.getName().substring(3);
				return server.getAttribute(mbean, name);
			}

			String sig[] = (String[]) signatureCache.get(method);
			if (sig == null) {
				// get the method signature from the method argument directly
				// vs. the arguments passed, since there may be primitives that
				// are wrapped as objects in the arguments
				Class<?> _args[] = method.getParameterTypes();
				if (_args != null && _args.length > 0) {
					sig = new String[_args.length];
					for (int c = 0; c < sig.length; c++) {
						if (_args[c] != null) {
							sig[c] = _args[c].getName();
						}
					}
				} else {
					sig = new String[0];
				}
				signatureCache.put(method, sig);
			}
			return server.invoke(mbean, method.getName(), args, sig);
		} catch (Throwable t) {
			if (MBeanTyper.DEBUG) {
				t.printStackTrace();
			}
			if (t instanceof UndeclaredThrowableException) {
				UndeclaredThrowableException ut = (UndeclaredThrowableException) t;
				throw ut.getUndeclaredThrowable();
			} else if (t instanceof InvocationTargetException) {
				InvocationTargetException it = (InvocationTargetException) t;
				throw it.getTargetException();
			} else if (t instanceof MBeanException) {
				MBeanException me = (MBeanException) t;
				throw me.getTargetException();
			} else {
				throw t;
			}
		}
	}
}