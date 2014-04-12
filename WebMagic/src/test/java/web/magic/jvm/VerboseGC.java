package web.magic.jvm;

import static java.lang.management.ManagementFactory.GARBAGE_COLLECTOR_MXBEAN_DOMAIN_TYPE;
import static java.lang.management.ManagementFactory.MEMORY_MXBEAN_NAME;
import static java.lang.management.ManagementFactory.MEMORY_POOL_MXBEAN_DOMAIN_TYPE;
import static java.lang.management.ManagementFactory.RUNTIME_MXBEAN_NAME;
import static java.lang.management.ManagementFactory.getGarbageCollectorMXBeans;
import static java.lang.management.ManagementFactory.getMemoryMXBean;
import static java.lang.management.ManagementFactory.getMemoryPoolMXBeans;
import static java.lang.management.ManagementFactory.getRuntimeMXBean;
import static java.lang.management.ManagementFactory.newPlatformMXBeanProxy;

import java.io.IOException;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.RuntimeMXBean;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

public class VerboseGC {
	private MBeanServerConnection server;

	private JMXConnector jmxc;

	public VerboseGC(String hostname, int port) {
		System.out.println("Connecting to " + hostname + ":" + port);

		// Create an RMI connector client and connect it to
		// the RMI connector server
		String urlPath = "/jndi/rmi://" + hostname + ":" + port + "/jmxrmi";
		connect(urlPath);
	}

	public void dump(long interval, long samples) {
		try {
			PrintGCStat pstat = new PrintGCStat(server);
			for (int i = 0; i < samples; i++) {
				pstat.printVerboseGc();
				try {
					Thread.sleep(interval);
				} catch (InterruptedException e) {
					System.exit(1);
				}
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
		if (args.length < 1) {
			usage();
		}

		String hostname = "";
		int port = -1;
		long interval = 5000; // default is 5 second interval
		long mins = 5;
		for (int argIndex = 0; argIndex < args.length; argIndex++) {
			String arg = args[argIndex];
			if (args[argIndex].startsWith("-")) {
				if (arg.equals("-h") || arg.equals("-help") || arg.equals("-?")) {
					usage();
				} else if (arg.startsWith("-interval=")) {
					try {
						interval = Integer.parseInt(arg.substring(10)) * 1000;
					} catch (NumberFormatException ex) {
						usage();
					}
				} else if (arg.startsWith("-duration=")) {
					try {
						mins = Integer.parseInt(arg.substring(10));
					} catch (NumberFormatException ex) {
						usage();
					}
				} else {
					// Unknown switch
					System.err.println("Unrecognized option: " + arg);
					usage();
				}
			} else {
				String[] arg2 = arg.split(":");
				if (arg2.length != 2) {
					usage();
				}
				hostname = arg2[0];
				try {
					port = Integer.parseInt(arg2[1]);
				} catch (NumberFormatException x) {
					usage();
				}
				if (port < 0) {
					usage();
				}
			}
		}

		// get full thread dump and perform deadlock detection
		VerboseGC vgc = new VerboseGC(hostname, port);
		long samples = (mins * 60 * 1000) / interval;
		vgc.dump(interval, samples);

	}

	private static void usage() {
		System.out.print("Usage: java VerboseGC <hostname>:<port> ");
		System.out.println(" [-interval=seconds] [-duration=minutes]");
	}
}

class PrintGCStat {
	private RuntimeMXBean rmbean;

	@SuppressWarnings("unused")
	private MemoryMXBean mmbean;

	private List<MemoryPoolMXBean> pools;

	private List<GarbageCollectorMXBean> gcmbeans;

	/**
	 * Constructs a PrintGCStat object to monitor a remote JVM.
	 */
	public PrintGCStat(MBeanServerConnection server) throws IOException {
		// Create the platform mxbean proxies
		this.rmbean = newPlatformMXBeanProxy(server, RUNTIME_MXBEAN_NAME, RuntimeMXBean.class);
		this.mmbean = newPlatformMXBeanProxy(server, MEMORY_MXBEAN_NAME, MemoryMXBean.class);
		ObjectName poolName = null;
		ObjectName gcName = null;
		try {
			poolName = new ObjectName(MEMORY_POOL_MXBEAN_DOMAIN_TYPE + ",*");
			gcName = new ObjectName(GARBAGE_COLLECTOR_MXBEAN_DOMAIN_TYPE + ",*");
		} catch (MalformedObjectNameException e) {
			// should not reach here
			assert (false);
		}

		Set<?> mbeans = server.queryNames(poolName, null);
		if (mbeans != null) {
			pools = new ArrayList<MemoryPoolMXBean>();
			Iterator<?> iterator = mbeans.iterator();
			while (iterator.hasNext()) {
				ObjectName objName = (ObjectName) iterator.next();
				MemoryPoolMXBean p = newPlatformMXBeanProxy(server, objName.getCanonicalName(), MemoryPoolMXBean.class);
				pools.add(p);
			}
		}

		mbeans = server.queryNames(gcName, null);
		if (mbeans != null) {
			gcmbeans = new ArrayList<GarbageCollectorMXBean>();
			Iterator<?> iterator = mbeans.iterator();
			while (iterator.hasNext()) {
				ObjectName objName = (ObjectName) iterator.next();
				GarbageCollectorMXBean gc = newPlatformMXBeanProxy(server, objName.getCanonicalName(),
						GarbageCollectorMXBean.class);
				gcmbeans.add(gc);
			}
		}
	}

	/**
	 * Constructs a PrintGCStat object to monitor the local JVM.
	 */
	public PrintGCStat() {
		// Obtain the platform mxbean instances for the running JVM.
		this.rmbean = getRuntimeMXBean();
		this.mmbean = getMemoryMXBean();
		this.pools = getMemoryPoolMXBeans();
		this.gcmbeans = getGarbageCollectorMXBeans();
	}

	/**
	 * Prints the verbose GC log to System.out to list the memory usage of all
	 * memory pools as well as the GC statistics.
	 */
	public void printVerboseGc() {
		System.out.print("Uptime: " + formatMillis(rmbean.getUptime()));
		for (GarbageCollectorMXBean gc : gcmbeans) {
			System.out.print(" [" + gc.getName() + ": ");
			System.out.print("Count=" + gc.getCollectionCount());
			System.out.print(" GCTime=" + formatMillis(gc.getCollectionTime()));
			System.out.print("]");
		}
		System.out.println();
		for (MemoryPoolMXBean p : pools) {
			System.out.print("  [" + p.getName() + ":");
			MemoryUsage u = p.getUsage();
			System.out.print(" Used=" + formatBytes(u.getUsed()));
			System.out.print(" Committed=" + formatBytes(u.getCommitted()));
			System.out.println("]");
		}
	}

	private String formatMillis(long ms) {
		return String.format("%.4fsec", ms / (double) 1000);
	}

	private String formatBytes(long bytes) {
		long kb = bytes;
		if (bytes > 0) {
			kb = bytes / 1024;
		}
		return kb + "K";
	}
}