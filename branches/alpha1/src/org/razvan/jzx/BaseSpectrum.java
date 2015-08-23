package org.razvan.jzx;

import java.io.InputStream;

import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;

/**
 * Base class extended by all Spectrum models (48k, 128k).
 * <P>
 * This class provides basic, common spectrum functionality for the various
 * spectrum models.
 * 
 * @author <A HREF="mailto:razvan.surdulescu@post.harvard.edu">Razvan
 *         Surdulescu</A> (c) 2001 - 2006
 * @author <A HREF="mailto:webmaster@zx-spectrum.net">Erik Kunze</A> (c) 1995,
 *         1996, 1997
 * @author <A HREF="mailto:des@ops.netcom.net.uk">Des Herriott</A> (c) 1993,
 *         1994 <BR>
 *         You may use and distribute this software for free provided you
 *         include this copyright notice. You may not sell this software, use my
 *         name for publicity reasons or modify the code without permission from
 *         me.
 */
public class BaseSpectrum implements Runnable {
	/**
	 * The 20ms frequency of the interrupt clock.
	 * 
	 * NOTE: This is set to 19 (instead of 20) because many games are
	 * frame-synchronized, and if they miss a screen refresh even by a little
	 * they wait until the next one. This means that roughly one out of two
	 * times, they will wait a frame too long. This effectively reduces the
	 * framerate by 50% to around 30FPS. By setting the frequency to 19, we
	 * guarantee that an interrupt will have arrived by the next frame, so the
	 * effective framerate is around 50FPS.
	 */
	public static final long FREQUENCY_MS = 19L;
	// public static final long FREQUENCY_MS = 1L;
	/** 48k Spectrum model 2 (issue 2) */
	public static final int ISSUE_2 = 2;
	/** 48k Spectrum model 3 (issue 3) */
	public static final int ISSUE_3 = 3;
	private static long m_frequency = FREQUENCY_MS;
	/** The model (issue) of this Spectrum. */
	private static int m_issue = ISSUE_3;
	/** The current vertical line being drawn. */
	private static int m_vline;
	/**
	 * The current scale value for this Spectrum.
	 * <P>
	 * This value is retrieved by the screen object during its <TT>init()</TT>
	 * phase.
	 */
	private static int m_scale = 1;
	/**
	 * The number of lines per TV frame.
	 * <P>
	 * This value is different for the different Spectrum models.
	 */
	private static int m_tvLines;
	/**
	 * The number of CPU T-States per TV line.
	 * <P>
	 * This value is different for the different Spectrum models.
	 */
	private static int m_cyclesLine;
	private static InputStream romIS;
	private static boolean noCache;
	private static Clock clock;
	private static Z80 cpu;

	/**
	 * Allocate the clock object, and initialize all contained references (CPU,
	 * memory, I/O, screen and keyboard.)
	 */
	public static void init(SurfaceHolder holder, InputStream is) {
		clock = new Clock(m_frequency);
		cpu = new Z80();
		Z80.init();
		BaseIO.init();
		BaseScreen.init(holder);
		BaseKeyboard.init();
		m_tvLines = 312;
		m_cyclesLine = 224;
		noCache = false;
		romIS = is;
	}

	/**
	 * Reset all contained references (CPU, memory, I/O, screen and keyboard.)
	 */
	public static void reset() {
		Z80.reset();
		BaseIO.reset();
		BaseScreen.reset();
		BaseKeyboard.reset();
	}

	/**
	 * Terminate all contained references (CPU, memory, I/O, screen and
	 * keyboard) and subsequently set them to null.
	 */
	public static void terminate() {
		BaseKeyboard.terminate();
		BaseIO.terminate();
		BaseScreen.terminate();
		Z80.terminate();
		clock = null;
	}

	public void run() {
		Thread.currentThread().setName("SpectrumEmulator");
		BaseSpectrum.emulate();
	}

	/**
	 * Trivial accessor for the issue (model) of this Spectrum.
	 */
	public static int getIssue() {
		return m_issue;
	}

	/**
	 * Trivial accessor for the scale parameter of this Spectrum.
	 * <P>
	 * This method is called by the screen object in its <TT>init()</TT> method.
	 */
	public static int getScale() {
		return m_scale;
	}

	/**
	 * Trivial mutator for the scale parameter of this Spectrum.
	 */
	public static void setScale(int scale) {
		m_scale = scale;
	}

	/**
	 * Trivial accessor for the current TV line being drawn.
	 */
	public static int getVline() {
		return m_vline;
	}

	/**
	 * Get the frequency used by the {@link #m_clock}.
	 * 
	 * @return The clock frequency, in milliseconds
	 */
	public static long getFrequency() {
		return m_frequency;
	}

	/**
	 * Set the frequency used by the {@link #m_clock}.
	 * 
	 * @param frequency
	 *            The clock frequency, in milliseconds
	 */
	public void setFrequency(long frequency) {
		if (frequency <= 0) {
			throw new IllegalArgumentException("Invalid frequency: "
					+ frequency);
		}
		m_frequency = frequency;
		clock.setFrequency(frequency);
	}

	/** String that describes the type of Spectrum ("48", "128"). */
	public static String getMode() {
		return "48";
	}

	/**
	 * The main emulator loop.
	 * <P>
	 * The steps performed are as follows:
	 * <UL>
	 * <LI>Start the clock.</LI>
	 * <LI>Repeat until stopped:</LI>
	 * <UL>
	 * <LI>If the current CPU T-States is more than the number of cycles per
	 * line, increment the number of lines.</LI>
	 * <LI>If the number of lines is greater than the number of lines per frame:
	 * </LI>
	 * <UL>
	 * <LI>If this is the 25th frame (twice per second) toggle the flash.</LI>
	 * </UL>
	 * <LI>Refresh the current screen frame.</LI>
	 * <LI>Wait for the next CPU interrupt.</LI>
	 * <LI>Decode and execute the next CPU instruction.</LI> </UL> </UL>
	 */
	public static void emulate() {
		clock.start();
		cpu.emulate();
		clock.end();
	}

	private static int m_frames;
	private static int m_interrupts;
	private static long m_fpsTimer = System.currentTimeMillis();

	public static void update() {
		int tStates = Z80.getTStates();
		if (tStates >= m_cyclesLine) {
			tStates -= m_cyclesLine;
			Z80.setTStates(tStates);
			// Tell the IO that this many cycles have elapsed
			// in order to facilitate processing data in the
			// streamers
			// BaseIO.advance(m_cyclesLine);
			if (++m_vline == m_tvLines) {
				m_vline = 0;
				m_frames++;
				long elapsed = System.currentTimeMillis() - m_fpsTimer;
				if (elapsed > 1000L) {
					m_fpsTimer = System.currentTimeMillis();
					m_frames = 0;
				}
				// Fire an interrupt per frame
				m_interrupts++;
				// Every 25 interrupts (twice/sec)
				// toggle the flash.
				if (m_interrupts == 25) {
					m_interrupts = 0;
					BaseScreen.flash();
				}
				// TODO:measure performance and lock the video memory
				if ((m_interrupts % 2) == 0)
					BaseScreen.update();
				// new Thread(m_screen).start();
				// m_screen.update();
				synchronized (clock) {
					while (!clock.interrupted) {
						try {
							clock.wait();
						} catch (InterruptedException ie) {
							Log.e(BaseSpectrum.class.getName(), ie.toString());
						}
					}
					clock.interrupted = false;
				}
				Z80.interrupt();
			}
		}
	}

	/**
	 * Pause the emulation (asynchronous).
	 */
	public static void pause() {
		cpu.pause();
	}

	/**
	 * Unpause the emulation (asynchronous).
	 */
	public static void unpause() {
		cpu.unpause();
	}

	/**
	 * Stop the emulation (asynchronous.)
	 */
	public static void stop() {
		cpu.stop();
	}

	/**
	 * Load the Spectrum contents from the given loader, by calling the
	 * <TT>load()</TT> method of all contained components.
	 */
	public static void load(BaseLoader loader) {
		Z80.load(loader);
		BaseIO.load(loader);
		BaseScreen.load(loader);
		BaseKeyboard.load(loader);
	}

	public static InputStream getRomIS() {
		return romIS;
	}

	public static void setRomIS(InputStream romIS) {
		BaseSpectrum.romIS = romIS;
	}

	public static boolean doKeyUp(int keyCode, KeyEvent msg) {
		BaseKeyboard.keyReleased(msg);
		return true;
	}

	public static boolean doKeyDown(int keyCode, KeyEvent msg) {
		BaseKeyboard.keyPressed(msg);
		return true;
	}

	public static boolean isNoCache() {
		return noCache;
	}

	public static void setNoCache(boolean noCache) {
		BaseSpectrum.noCache = noCache;
	}

}
