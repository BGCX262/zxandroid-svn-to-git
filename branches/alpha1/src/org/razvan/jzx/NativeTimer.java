package org.razvan.jzx;

/**
 * Obtain high-resolution timing values using a native hardware timer.
 *
 * <P>
 * Makes use of a JNI library, if possible; if the library does not exist
 * or is not accessible in the current security context (e.g. Applet), default
 * to returning milliseconds from <TT>System.currentTimeMillis()</TT>.
 *
 * @author <A HREF="mailto:razvan.surdulescu@post.harvard.edu">Razvan Surdulescu</A> (c) 2003 - 2006
 * <BR>
 * You may use and distribute this software for free provided you include
 * this copyright notice. You may not sell this software, use my name for
 * publicity reasons or modify the code without permission from me.
 */
public class NativeTimer {
	private static boolean s_native = true;
	
	static {
		try {
			System.loadLibrary("NativeTimer");
		} catch (Throwable t) {
			System.err.println("Failed to load NativeTimer: " + t.getMessage());
			s_native = false;
		}
	}
	
	/**
	 * Retrieve the current time, in nanoseconds if possible; if not,
	 * return the time in milliseconds, by calling
	 * <TT>System.currentTimeMillis()</TT>.
	 */
	public static final long currentTimeNanos() {
		if (s_native) {
			return nativeCurrentTimeNanos();
		} else {
			return (long) (System.currentTimeMillis() * 1E6);
		}
	}
	
	private static final native long nativeCurrentTimeNanos();
}
