package org.razvan.jzx;

/**
 * Log messages to the console (System.out),
 * when JZX is running as a regular Frame.
 *
 * @author <A HREF="mailto:razvan.surdulescu@post.harvard.edu">Razvan Surdulescu</A> (c) 2001 - 2006
 * <BR>
 * You may use and distribute this software for free provided you include
 * this copyright notice. You may not sell this software, use my name for
 * publicity reasons or modify the code without permission from me.
 *
 * @see ILogger
 */
public class ConsoleLogger implements ILogger {
	/**
	 * Display the message to System.out.
	 */
	public void log(int channel, String message) {
		System.err.println("Channel: " + channel + ", " + message);
	}
	
	/**
	 * Display the exception stack trace to System.out.
	 */
	public void log(int channel, Throwable t) {
		System.out.println("Channel: " + channel);
		t.printStackTrace(System.out);
	}
}
