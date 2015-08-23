package org.razvan.jzx;

import java.util.Enumeration;
import java.util.Hashtable;

/**
 * Track various time periods in the emulator and report them.
 *
 * <P>
 * This class is used to determine possible performance bottlenecks.
 *
 * @author <A HREF="mailto:razvan.surdulescu@post.harvard.edu">Razvan Surdulescu</A> (c) 2001 - 2006
 * <BR>
 * You may use and distribute this software for free provided you include
 * this copyright notice. You may not sell this software, use my name for
 * publicity reasons or modify the code without permission from me.
 */
public class PerformanceCounter {
	/**
	 * Store the total amount of time performed by a particular action
	 * and the number of times that action was performed.
	 */
	private static class Pair {
		double time;
		int count;
	}
	
	/** Enable or disable the performance counter functionality. */
	private static boolean s_enable = false;
	
	/**
	 * Skip this many events of any given time before recording
	 * times for that event type.
	 */
	private static int s_warmup = 50;
	
	/** The list of start times for an event of a given type. */
	private static Hashtable s_startTimes = new Hashtable();
	
	/** The list of {@link Pair} objects for an event of a given type. */
	private static Hashtable s_timePairs = new Hashtable();
	
	/** Enable or disable the counter functionality. */
	public static void setEnable(boolean flag) {
		s_enable = flag;
	}
	
	/** Set the warmup count. */
	public static void setWarmup(int value) {
		if (value < 0) {
			throw new IllegalArgumentException("Invalid warmup count: " + value);
		}
		
		s_warmup = value;
	}
	
	/**
	 * Start tracking a new event of specified type.
	 *
	 * <P>
	 * You may not start the same event type twice in a row;
	 * you must {@link #end} it first!
	 *
	 * @param name The type of performance event to start
	 */
	public static synchronized void start(String name) {
		if (!s_enable) {
			return;
		}
		
		if (s_startTimes.containsKey(name)) {
			throw new IllegalArgumentException("Timer '" + name + "' already started");
		} else {
			long start = NativeTimer.currentTimeNanos();
			
			s_startTimes.put(name, new Double(start / 1000.0));
		}
	}
	
	/**
	 * End tracking an event of specified time and record
	 * the amount of time it took to complete.
	 *
	 * <P>
	 * You may not end the same event type twice in a row;
	 * you must {@link #start} it first!
	 *
	 * @param name The type of performance event to end
	 */
	public static synchronized void end(String name) {
		if (!s_enable) {
			return;
		}
		
		long end = NativeTimer.currentTimeNanos();
		
		if (!s_startTimes.containsKey(name)) {
			throw new IllegalArgumentException("Timer '" + name + "' not already started");
		} else {
			double start = ((Double) s_startTimes.remove(name)).doubleValue();
			
			Pair pair;
			if (s_timePairs.containsKey(name)) {
				pair = (Pair) s_timePairs.get(name);
			} else {
				pair = new Pair();
				s_timePairs.put(name, pair);
			}
			
			pair.count++;
			if (pair.count > s_warmup) {
				pair.time += (end / 1000.0 - start);
			}
		}
	}
	
	/**
	 * Report all the performance events gathered thus far.
	 */
	public static synchronized void report() {
		if (!s_enable) {
			return;
		}
		
		for (Enumeration keys = s_timePairs.keys(); keys.hasMoreElements(); ) {
			String name = (String) keys.nextElement();
			Pair pair = (Pair) s_timePairs.get(name);
			
			int count = pair.count - s_warmup;
			System.out.println(name + "=" + (pair.time / count) + " (" + count + ")");
		}
	}
}
