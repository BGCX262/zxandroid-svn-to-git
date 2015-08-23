package org.razvan.jzx;

import android.util.Log;

/**
 * Spectrum interrupt clock which is used for generating the 20ms interrupt on
 * which the screen frames are synchronized.
 * <P>
 * The Spectrum clock is integral to emulating the Spectrum at a speed that
 * resembles that of the original machine. Specifically, the main loop decodes
 * and executes instructions at the full speed of the host machine. As each
 * instruction is decoded, a virtual T-State counter is incremented, and
 * whenever the virtual T-States add up to what <I>should</I> be one screen
 * frame, the emulation loop simply blocks on the Clock thread until an
 * interrupt is generated. The Clock thereby ensures that although the
 * instructions are decoded much faster than in the original machine, the screen
 * frames, and thus the CPU interrupts are produced at the same rate as the
 * original hardware.
 * 
 * @author <A HREF="mailto:razvan.surdulescu@post.harvard.edu">Razvan
 *         Surdulescu</A> (c) 2001 - 2006 <BR>
 *         You may use and distribute this software for free provided you
 *         include this copyright notice. You may not sell this software, use my
 *         name for publicity reasons or modify the code without permission from
 *         me.
 * 
 * @see BaseSpectrum#emulate
 */
public class Clock extends Thread {
	/**
	 * This is set to true, and <TT>notifyAll()</TT> is called whenever an
	 * interrupt has been generated.
	 * <P>
	 * Whomever is listening for the <TT>notifyAll()</TT> call is responsible
	 * for setting the interrupted flag back to false.
	 */
	public volatile boolean interrupted;

	/**
	 * The frequency of the clock.
	 * <P>
	 * 
	 * @see BaseSpectrum#FREQUENCY_MS
	 */
	private volatile long m_frequency;

	/**
	 * If true, stops the clock and exits the thread.
	 */
	private volatile boolean m_stop;

	/**
	 * Create the Clock thread and mark it as daemon.
	 */
	public Clock(long frequency) {
		super("ClockThread");
		setDaemon(true);
		setPriority(Thread.NORM_PRIORITY + 3);

		m_frequency = frequency;
	}

	/**
	 * Get the clock frequency.
	 * 
	 * @return The clock frequency (in milliseconds).
	 */
	public long getFrequency() {
		return m_frequency;
	}

	/**
	 * Set the clock frequency.
	 * 
	 * @param frequency
	 *            The clock frequency (in milliseconds).
	 */
	public void setFrequency(long frequency) {
		if (frequency <= 0) {
			throw new IllegalArgumentException("Invalid frequency: "
					+ frequency);
		}

		m_frequency = frequency;
	}

	/**
	 * Generate interrupts every specified number of milliseconds.
	 * 
	 * <UL>
	 * <LI>while(true)</LI>
	 * <UL>
	 * <LI>sleep for "m_frequency" milliseconds.</LI>
	 * <LI>set "interrupted" to true.</LI>
	 * <LI>notifyAll().</LI>
	 * </UL>
	 * </UL>
	 */
	public void run() {
		while (true) {
			try {
				sleep(m_frequency);
			} catch (InterruptedException ex) {
				Log.e(this.getClass().getName(), ex.toString());
			}

			if (m_stop) {
				return;
			}

			synchronized (this) {
				interrupted = true;
				notifyAll();
			}

		}
	}

	/**
	 * Stops the Clock thread and causes it to exit (asynchronous.)
	 */
	public void end() {
		m_stop = true;
		this.interrupt();
	}
}
