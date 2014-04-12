package web.magic.jvm;

import static java.lang.management.ManagementFactory.THREAD_MXBEAN_NAME;
import static java.lang.management.ManagementFactory.getThreadMXBean;
import static java.lang.management.ManagementFactory.newPlatformMXBeanProxy;

import java.io.IOException;
import java.lang.management.LockInfo;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

public class Deadlock {
	@SuppressWarnings("unused")
	public static void main(String[] argv) {
		Deadlock dl = new Deadlock();

		// Now find deadlock
		ThreadMonitor0 monitor = new ThreadMonitor0();
		boolean found = false;
		while (!found) {
			found = monitor.findDeadlock();
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				System.exit(1);
			}
		}

		System.out.println("\nPress <Enter> to exit this Deadlock program.\n");
		waitForEnterPressed();
	}

	private CyclicBarrier barrier = new CyclicBarrier(6);

	public Deadlock() {
		DeadlockThread[] dThreads = new DeadlockThread[6];

		Monitor a = new Monitor("a");
		Monitor b = new Monitor("b");
		Monitor c = new Monitor("c");
		dThreads[0] = new DeadlockThread("MThread-1", a, b);
		dThreads[1] = new DeadlockThread("MThread-2", b, c);
		dThreads[2] = new DeadlockThread("MThread-3", c, a);

		Lock d = new ReentrantLock();
		Lock e = new ReentrantLock();
		Lock f = new ReentrantLock();

		dThreads[3] = new DeadlockThread("SThread-4", d, e);
		dThreads[4] = new DeadlockThread("SThread-5", e, f);
		dThreads[5] = new DeadlockThread("SThread-6", f, d);

		// make them daemon threads so that the test will exit
		for (int i = 0; i < 6; i++) {
			dThreads[i].setDaemon(true);
			dThreads[i].start();
		}
	}

	class DeadlockThread extends Thread {
		private Lock lock1 = null;

		private Lock lock2 = null;

		private Monitor mon1 = null;

		private Monitor mon2 = null;

		private boolean useSync;

		DeadlockThread(String name, Lock lock1, Lock lock2) {
			super(name);
			this.lock1 = lock1;
			this.lock2 = lock2;
			this.useSync = true;
		}

		DeadlockThread(String name, Monitor mon1, Monitor mon2) {
			super(name);
			this.mon1 = mon1;
			this.mon2 = mon2;
			this.useSync = false;
		}

		public void run() {
			if (useSync) {
				syncLock();
			} else {
				monitorLock();
			}
		}

		private void syncLock() {
			lock1.lock();
			try {
				try {
					barrier.await();
				} catch (InterruptedException e) {
					e.printStackTrace();
					System.exit(1);
				} catch (BrokenBarrierException e) {
					e.printStackTrace();
					System.exit(1);
				}
				goSyncDeadlock();
			} finally {
				lock1.unlock();
			}
		}

		private void goSyncDeadlock() {
			try {
				barrier.await();
			} catch (InterruptedException e) {
				e.printStackTrace();
				System.exit(1);
			} catch (BrokenBarrierException e) {
				e.printStackTrace();
				System.exit(1);
			}
			lock2.lock();
			throw new RuntimeException("should not reach here.");
		}

		private void monitorLock() {
			synchronized (mon1) {
				try {
					barrier.await();
				} catch (InterruptedException e) {
					e.printStackTrace();
					System.exit(1);
				} catch (BrokenBarrierException e) {
					e.printStackTrace();
					System.exit(1);
				}
				goMonitorDeadlock();
			}
		}

		private void goMonitorDeadlock() {
			try {
				barrier.await();
			} catch (InterruptedException e) {
				e.printStackTrace();
				System.exit(1);
			} catch (BrokenBarrierException e) {
				e.printStackTrace();
				System.exit(1);
			}
			synchronized (mon2) {
				throw new RuntimeException(getName() + " should not reach here.");
			}
		}
	}

	class Monitor {
		String name;

		Monitor(String name) {
			this.name = name;
		}
	}

	private static void waitForEnterPressed() {
		try {
			boolean done = false;
			while (!done) {
				char ch = (char) System.in.read();
				if (ch < 0 || ch == '\n') {
					done = true;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}
}

class ThreadMonitor0 {
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
	public ThreadMonitor0(MBeanServerConnection server) throws IOException {
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
	public ThreadMonitor0() {
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