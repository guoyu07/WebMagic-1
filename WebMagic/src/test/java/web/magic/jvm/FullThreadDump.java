package web.magic.jvm;

import static java.lang.management.ManagementFactory.THREAD_MXBEAN_NAME;
import static java.lang.management.ManagementFactory.getThreadMXBean;
import static java.lang.management.ManagementFactory.newPlatformMXBeanProxy;

import java.io.IOException;
import java.lang.management.LockInfo;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.net.MalformedURLException;

import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

public class FullThreadDump {
	private MBeanServerConnection server;

	private JMXConnector jmxc;

	public FullThreadDump(String hostname, int port) {
		System.out.println("Connecting to " + hostname + ":" + port);

		// Create an RMI connector client and connect it to
		// the RMI connector server
		String urlPath = "/jndi/rmi://" + hostname + ":" + port + "/jmxrmi";
		connect(urlPath);
	}

	public void dump() {
		try {
			ThreadMonitor monitor = new ThreadMonitor(server);
			monitor.threadDump();
			if (!monitor.findDeadlock()) {
				System.out.println("No deadlock found.");
			}
		} catch (IOException e) {
			System.err.println("\nCommunication error: " + e.getMessage());
			System.exit(1);
		}
	}

	/**
	 * Connect to a JMX agent of a given URL.
	 */
	private void connect(String urlPath) {
		try {
			JMXServiceURL url = new JMXServiceURL("rmi", "", 0, urlPath);
			this.jmxc = JMXConnectorFactory.connect(url);
			this.server = jmxc.getMBeanServerConnection();
		} catch (MalformedURLException e) {
			// should not reach here
		} catch (IOException e) {
			System.err.println("\nCommunication error: " + e.getMessage());
			System.exit(1);
		}
	}

	public static void main(String[] args) {
		if (args.length != 1) {
			usage();
		}

		String[] arg2 = args[0].split(":");
		if (arg2.length != 2) {
			usage();
		}
		String hostname = arg2[0];
		int port = -1;
		try {
			port = Integer.parseInt(arg2[1]);
		} catch (NumberFormatException x) {
			usage();
		}
		if (port < 0) {
			usage();
		}

		// get full thread dump and perform deadlock detection
		FullThreadDump ftd = new FullThreadDump(hostname, port);
		ftd.dump();
	}

	private static void usage() {
		System.out.println("Usage: java FullThreadDump <hostname>:<port>");
	}
}
class ThreadMonitor {
	private MBeanServerConnection server;

	private ThreadMXBean tmbean;

	private ObjectName objname;

	// default - JDK 6+ VM
	private String findDeadlocksMethodName = "findDeadlockedThreads";

	private boolean canDumpLocks = true;

	/**
	 * Constructs a ThreadMonitor object to get thread information in a remote
	 * JVM.
	 */
	public ThreadMonitor(MBeanServerConnection server) throws IOException {
		this.server = server;
		this.tmbean = newPlatformMXBeanProxy(server, THREAD_MXBEAN_NAME, ThreadMXBean.class);
		try {
			objname = new ObjectName(THREAD_MXBEAN_NAME);
		} catch (MalformedObjectNameException e) {
			// should not reach here
			InternalError ie = new InternalError(e.getMessage());
			ie.initCause(e);
			throw ie;
		}
		parseMBeanInfo();
	}

	/**
	 * Constructs a ThreadMonitor object to get thread information in the local
	 * JVM.
	 */
	public ThreadMonitor() {
		this.tmbean = getThreadMXBean();
	}

	/**
	 * Prints the thread dump information to System.out.
	 */
	public void threadDump() {
		if (canDumpLocks) {
			if (tmbean.isObjectMonitorUsageSupported() && tmbean.isSynchronizerUsageSupported()) {
				// Print lock info if both object monitor usage
				// and synchronizer usage are supported.
				// This sample code can be modified to handle if
				// either monitor usage or synchronizer usage is supported.
				dumpThreadInfoWithLocks();
			}
		} else {
			dumpThreadInfo();
		}
	}

	private void dumpThreadInfo() {
		System.out.println("Full Java thread dump");
		long[] tids = tmbean.getAllThreadIds();
		ThreadInfo[] tinfos = tmbean.getThreadInfo(tids, Integer.MAX_VALUE);
		for (ThreadInfo ti : tinfos) {
			printThreadInfo(ti);
		}
	}

	/**
	 * Prints the thread dump information with locks info to System.out.
	 */
	private void dumpThreadInfoWithLocks() {
		System.out.println("Full Java thread dump with locks info");

		ThreadInfo[] tinfos = tmbean.dumpAllThreads(true, true);
		for (ThreadInfo ti : tinfos) {
			printThreadInfo(ti);
			LockInfo[] syncs = ti.getLockedSynchronizers();
			printLockInfo(syncs);
		}
		System.out.println();
	}

	private static String INDENT = "    ";

	private void printThreadInfo(ThreadInfo ti) {
		// print thread information
		printThread(ti);

		// print stack trace with locks
		StackTraceElement[] stacktrace = ti.getStackTrace();
		MonitorInfo[] monitors = ti.getLockedMonitors();
		for (int i = 0; i < stacktrace.length; i++) {
			StackTraceElement ste = stacktrace[i];
			System.out.println(INDENT + "at " + ste.toString());
			for (MonitorInfo mi : monitors) {
				if (mi.getLockedStackDepth() == i) {
					System.out.println(INDENT + "  - locked " + mi);
				}
			}
		}
		System.out.println();
	}

	private void printThread(ThreadInfo ti) {
		StringBuilder sb = new StringBuilder("\"" + ti.getThreadName() + "\"" + " Id=" + ti.getThreadId() + " in "
				+ ti.getThreadState());
		if (ti.getLockName() != null) {
			sb.append(" on lock=" + ti.getLockName());
		}
		if (ti.isSuspended()) {
			sb.append(" (suspended)");
		}
		if (ti.isInNative()) {
			sb.append(" (running in native)");
		}
		System.out.println(sb.toString());
		if (ti.getLockOwnerName() != null) {
			System.out.println(INDENT + " owned by " + ti.getLockOwnerName() + " Id=" + ti.getLockOwnerId());
		}
	}

	void printMonitorInfo(ThreadInfo ti, MonitorInfo[] monitors) {
		System.out.println(INDENT + "Locked monitors: count = " + monitors.length);
		for (MonitorInfo mi : monitors) {
			System.out.println(INDENT + "  - " + mi + " locked at ");
			System.out.println(INDENT + "      " + mi.getLockedStackDepth() + " " + mi.getLockedStackFrame());
		}
	}

	private void printLockInfo(LockInfo[] locks) {
		System.out.println(INDENT + "Locked synchronizers: count = " + locks.length);
		for (LockInfo li : locks) {
			System.out.println(INDENT + "  - " + li);
		}
		System.out.println();
	}

	/**
	 * Checks if any threads are deadlocked. If any, print the thread dump
	 * information.
	 */
	public boolean findDeadlock() {
		long[] tids;
		if (findDeadlocksMethodName.equals("findDeadlockedThreads") && tmbean.isSynchronizerUsageSupported()) {
			tids = tmbean.findDeadlockedThreads();
			if (tids == null) {
				return false;
			}

			System.out.println("Deadlock found :-");
			ThreadInfo[] infos = tmbean.getThreadInfo(tids, true, true);
			for (ThreadInfo ti : infos) {
				printThreadInfo(ti);
				printLockInfo(ti.getLockedSynchronizers());
				System.out.println();
			}
		} else {
			tids = tmbean.findMonitorDeadlockedThreads();
			if (tids == null) {
				return false;
			}
			ThreadInfo[] infos = tmbean.getThreadInfo(tids, Integer.MAX_VALUE);
			for (ThreadInfo ti : infos) {
				// print thread information
				printThreadInfo(ti);
			}
		}

		return true;
	}

	private void parseMBeanInfo() throws IOException {
		try {
			MBeanOperationInfo[] mopis = server.getMBeanInfo(objname).getOperations();

			// look for findDeadlockedThreads operations;
			boolean found = false;
			for (MBeanOperationInfo op : mopis) {
				if (op.getName().equals(findDeadlocksMethodName)) {
					found = true;
					break;
				}
			}
			if (!found) {
				// if findDeadlockedThreads operation doesn't exist,
				// the target VM is running on JDK 5 and details about
				// synchronizers and locks cannot be dumped.
				findDeadlocksMethodName = "findMonitorDeadlockedThreads";
				canDumpLocks = false;
			}
		} catch (IntrospectionException e) {
			InternalError ie = new InternalError(e.getMessage());
			ie.initCause(e);
			throw ie;
		} catch (InstanceNotFoundException e) {
			InternalError ie = new InternalError(e.getMessage());
			ie.initCause(e);
			throw ie;
		} catch (ReflectionException e) {
			InternalError ie = new InternalError(e.getMessage());
			ie.initCause(e);
			throw ie;
		}
	}
}
