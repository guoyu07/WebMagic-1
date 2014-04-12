package web.magic.jvm;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.logging.Logger;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.ReflectionException;

/**
 * Non-instantiable class that provides jmx utility methods.
 */
public class Utilities {

	/**
	 * Prints info about a bean to a {@link VarOutputSink}.
	 * 
	 * @param sink
	 *            The {@link VarOutputSink} to which info will be sent
	 * @param mbs
	 *            The {@link MBeanServer} with respect to which the
	 *            {@code objectName} is accessed
	 * @param objectName
	 *            The {@link ObjectName} that identifies this bean
	 */
	public static void printMBeanInfo(VarOutputSink sink, MBeanServer mbs, ObjectName objectName) {
		MBeanInfo info = getMBeanInfoSafely(sink, mbs, objectName);
		if (info == null) {
			return;
		}

		sink.echo("\nCLASSNAME: \t" + info.getClassName());
		sink.echo("\nDESCRIPTION: \t" + info.getDescription());
		sink.echo("\nATTRIBUTES");
		MBeanAttributeInfo[] attrInfo = info.getAttributes();
		sink.printVariable("attrcount", Integer.toString(attrInfo.length));
		if (attrInfo.length > 0) {
			for (int i = 0; i < attrInfo.length; i++) {
				sink.echo(" ** NAME: \t" + attrInfo[i].getName());
				sink.echo("    DESCR: \t" + attrInfo[i].getDescription());
				sink.echo("    TYPE: \t" + attrInfo[i].getType() + "\tREAD: " + attrInfo[i].isReadable() + "\tWRITE: "
						+ attrInfo[i].isWritable());
			}
		} else
			sink.echo(" ** No attributes **");
		sink.echo("\nCONSTRUCTORS");
		MBeanConstructorInfo[] constrInfo = info.getConstructors();
		for (int i = 0; i < constrInfo.length; i++) {
			sink.echo(" ** NAME: \t" + constrInfo[i].getName());
			sink.echo("    DESCR: \t" + constrInfo[i].getDescription());
			sink.echo("    PARAM: \t" + constrInfo[i].getSignature().length + " parameter(s)");
		}
		sink.echo("\nOPERATIONS");
		MBeanOperationInfo[] opInfo = info.getOperations();
		if (opInfo.length > 0) {
			for (int i = 0; i < opInfo.length; i++) {
				sink.echo(" ** NAME: \t" + opInfo[i].getName());
				sink.echo("    DESCR: \t" + opInfo[i].getDescription());
				sink.echo("    PARAM: \t" + opInfo[i].getSignature().length + " parameter(s)");
			}
		} else
			sink.echo(" ** No operations ** ");
		sink.echo("\nNOTIFICATIONS");
		MBeanNotificationInfo[] notifInfo = info.getNotifications();
		if (notifInfo.length > 0) {
			for (int i = 0; i < notifInfo.length; i++) {
				sink.echo(" ** NAME: \t" + notifInfo[i].getName());
				sink.echo("    DESCR: \t" + notifInfo[i].getDescription());
				String notifTypes[] = notifInfo[i].getNotifTypes();
				for (int j = 0; j < notifTypes.length; j++) {
					sink.echo("    TYPE: \t" + notifTypes[j]);
				}
			}
		} else
			sink.echo(" ** No notifications **");
	}

	private static MBeanInfo getMBeanInfoSafely(VarOutputSink sink, MBeanServer mbs, ObjectName objectName) {
		MBeanInfo info = null;
		try {
			info = mbs.getMBeanInfo(objectName);
		} catch (InstanceNotFoundException e) {
			sink.printVariable("ObjectName", "Not found");
		} catch (IntrospectionException e) {
			sink.printVariable("ObjectName", "IntrospectionException");
		} catch (ReflectionException e) {
			sink.printVariable("ObjectName", "ReflectionException");
		}
		return info;
	}

	/**
	 * Prints bean attributes to a {@link VarOutputSink}.
	 * 
	 * @param sink
	 *            The {@link VarOutputSink} to which attributes will be sent
	 * @param mbs
	 *            The {@link MBeanServer} with respect to which the
	 *            {@code objectName} is accessed
	 * @param objectName
	 *            The {@link ObjectName} that identifies this bean
	 */
	public static void printMBeanAttributes(VarOutputSink sink, MBeanServer mbs, ObjectName objectName) {
		MBeanInfo info = getMBeanInfoSafely(sink, mbs, objectName);
		if (info == null) {
			sink.printVariable(objectName.getCanonicalName(), "can't fetch info");
			return;
		}
		MBeanAttributeInfo[] attrInfo = info.getAttributes();
		if (attrInfo.length > 0) {
			for (int i = 0; i < attrInfo.length; i++) {
				String attrName = attrInfo[i].getName();
				Object attrValue = null;
				String attrValueString = null;
				try {
					attrValue = mbs.getAttribute(objectName, attrName);
				} catch (AttributeNotFoundException e) {
					attrValueString = "AttributeNotFoundException";
				} catch (InstanceNotFoundException e) {
					attrValueString = "InstanceNotFoundException";
				} catch (MBeanException e) {
					attrValueString = "MBeanException";
				} catch (ReflectionException e) {
					attrValueString = "ReflectionException";
				}
				if (attrValueString == null) {
					attrValueString = attrValue.toString();
				}
				sink.printVariable(attrName, attrValueString);
			}
		}
	}

	/**
	 * Helper interface defining output sinks used with
	 * {@link Utilities#printMBeanInfo(com.google.enterprise.util.jmx.Utils.VarOutputSink, MBeanServer, ObjectName)}
	 * and
	 * {@link Utilities#printMBeanAttributes(com.google.enterprise.util.jmx.Utils.VarOutputSink, MBeanServer, ObjectName)}
	 */
	public interface VarOutputSink {
		public void printVariable(String name, String value);

		public void echo(String string);
	}

	/**
	 * Static {@link VarOutputSink} that uses {@link System#out}
	 */
	public static final VarOutputSink SYSTEM_OUT_SINK = new PrintStreamVarOutputSink(System.out);

	public static class LoggerVarOutputSink implements VarOutputSink {
		private final Logger logger;

		public LoggerVarOutputSink(Logger logger) {
			this.logger = logger;
		}

		public void printVariable(String name, String value) {
			logger.info(name + " " + value);
		}

		public void echo(String string) {
			logger.info(string);
		}
	}

	public static class PrintWriterVarOutputSink implements VarOutputSink {
		private final PrintWriter printWriter;

		public PrintWriterVarOutputSink(PrintWriter printWriter) {
			this.printWriter = printWriter;
		}

		public void printVariable(String name, String value) {
			printWriter.print("  <b>");
			printWriter.print(name);
			printWriter.print("</b> ");
			printWriter.print(value);
			printWriter.println("<br>");
		}

		public void echo(String string) {
			printWriter.print(string);
		}
	}

	public static class PrintStreamVarOutputSink implements VarOutputSink {
		private final PrintStream printStream;

		public PrintStreamVarOutputSink(PrintStream printStream) {
			this.printStream = printStream;
		}

		public void printVariable(String name, String value) {
			printStream.print("  <b>");
			printStream.print(name);
			printStream.print("</b> ");
			printStream.print(value);
			printStream.println("<br>");
		}

		public void echo(String string) {
			printStream.print(string);
		}
	}
}