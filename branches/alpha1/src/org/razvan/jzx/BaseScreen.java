package org.razvan.jzx;

import org.razvan.jzx.v48.Spectrum;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Bitmap.Config;
import android.util.Log;
import android.view.SurfaceHolder;

/**
 * Base class extended by all screen components that comprise the emulator.
 * <P>
 * Note: BaseScreen must extend Canvas, so it does not extend BaseComponent,
 * although it (correctly) should. Instead, it implements all the BaseComponent
 * methods explicitly.
 * <P>
 * This class provides basic, common screen functionality for the screen
 * subsystem of the emulator. Specific methods are overriden or implemented so
 * as to comply with the idiosyncracies of various JVM versions. There are no
 * differences between the Spectrum 48k and the Spectrum 128k models.
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
public class BaseScreen implements Runnable {
	/** Memory address where the screen area starts. */
	public static final int PIXEL_START = 16384;
	/** Number of character rows on the screen. */
	public static final int ROWS = 24;
	/** Number of character columns on the screen. */
	public static final int COLS = 32;
	/** Number of X pixels on the screen. */
	public static final int X_PIXELS = (COLS * 8);
	/** Number of Y pixels on the screen. */
	public static final int Y_PIXELS = (ROWS * 8);
	/** Length of pixel memory area. */
	public static final int PIXEL_LENGTH = ((X_PIXELS / 8) * Y_PIXELS);
	/** Memory address where the attribute area starts. */
	public static final int ATTR_START = PIXEL_START + PIXEL_LENGTH;
	/** Length of attribute memory area. */
	public static final int ATTR_LENGTH = (ROWS * COLS);
	/** Length of screen memory area. */
	public static final int SCREEN_LENGTH = (PIXEL_LENGTH + ATTR_LENGTH);
	public static final int SCREEN_START = 0x4000;
	public static final int SCREEN_END = 0x4000 + ((256 / 8) * 192) + (32 * 24);;

	/** Width of border area, in pixels. */
	public static final int BORDER_PIXELS = 30;
	/** The Spectrum index of the color "black" */
	public static final int BLACK = 0;
	/** The Spectrum index of the color "blue" */
	public static final int BLUE = 1;
	/** The Spectrum index of the color "red" */
	public static final int RED = 2;
	/** The Spectrum index of the color "magenta" */
	public static final int MAGENTA = 3;
	/** The Spectrum index of the color "green" */
	public static final int GREEN = 4;
	/** The Spectrum index of the color "cyan" */
	public static final int CYAN = 5;
	/** The Spectrum index of the color "yellow" */
	public static final int YELLOW = 6;
	/** The Spectrum index of the color "white" */
	public static final int WHITE = 7;
	/** The Spectrum index of the color "bright black" */
	public static final int BRIGHT_BLACK = 8;
	/** The Spectrum index of the color "bright blue" */
	public static final int BRIGHT_BLUE = 9;
	/** The Spectrum index of the color "bright red" */
	public static final int BRIGHT_RED = 10;
	/** The Spectrum index of the color "bright magenta" */
	public static final int BRIGHT_MAGENTA = 11;
	/** The Spectrum index of the color "bright green" */
	public static final int BRIGHT_GREEN = 12;
	/** The Spectrum index of the color "bright cyan" */
	public static final int BRIGHT_CYAN = 13;
	/** The Spectrum index of the color "bright yellow" */
	public static final int BRIGHT_YELLOW = 14;
	/** The Spectrum index of the color "bright white" */
	public static final int BRIGHT_WHITE = 15;
	/** Flash attribute mask. */
	public static final int FLASH = 0x80;
	/** Bright attribute mask. */
	public static final int BRIGHT = 0x40;
	/** Paper attribute mask. */
	public static final int PAPER = 0x38;
	/** Ink attribute mask. */
	public static final int INK = 0x07;
	private static int CHARSET_ADDR = 15616;
	/** The screen scaling factor. */
	private static int m_scale = 1;
	/** The screen width, in pixels (uses m_scale.) */
	private static int m_screenWidth = X_PIXELS * m_scale;
	/**
	 * An array of Color objects corresponding to the color indices above.
	 */
	private static final int[] s_colorPalette = { 0x000000, /* black */
	0x0000bf, /* blue */
	0xbf0000, /* red */
	0xbf00bf, /* magenta */
	0x00bf00, /* green */
	0x00bfbf, /* cyan */
	0xbfbf00, /* yellow */
	0xbfbfbf, /* white */
	0x000000, /* black */
	0x0000ff, /* bright blue */
	0xff0000, /* bright red */
	0xff00ff, /* bright magenta */
	0x00ff00, /* bright green */
	0x00ffff, /* bright cyan */
	0xffff00, /* bright yellow */
	0xffffff /* bright white */
	};
	/**
	 * An array of RGB integral values corresponding to the color indices above.
	 */
	private static final int[] s_rgbPalette = { 0x000000, /* black */
	0x0000bf, /* blue */
	0xbf0000, /* red */
	0xbf00bf, /* magenta */
	0x00bf00, /* green */
	0x00bfbf, /* cyan */
	0xbfbf00, /* yellow */
	0xbfbfbf, /* white */
	0x000000, /* black */
	0x0000ff, /* bright blue */
	0xff0000, /* bright red */
	0xff00ff, /* bright magenta */
	0x00ff00, /* bright green */
	0x00ffff, /* bright cyan */
	0xffff00, /* bright yellow */
	0xffffff /* bright white */
	};
	/** Is the flash phase normal or inverted? */
	private static boolean m_flashPhase;
	/**
	 * Maps a given attribute value to its corresponding ink value, to avoid
	 * having to decode it explicitly.
	 */
	private static int[] m_inkTable;
	/**
	 * Maps a given attribute value to its corresponding paper value, to avoid
	 * having to decode it explicitly.
	 */
	private static int[] m_paperTable;
	/**
	 * Bit vector that indicates whether a particular memory byte has been
	 * changed.
	 */
	private static boolean[] m_screenChanged;
	/** If true, the screen needs repainting. */
	protected static boolean m_screenDirty;
	/** If true, the border needs repainting. */
	private static boolean m_borderDirty;
	/** Surface holder of the main surface */
	private static SurfaceHolder surfaceHolder;
	/**
	 * The physical memory page from which data starts.
	 */
	public static int M_PAGE = 0x4000;
	/** The current cursor position used by {@link #print(char, int)}. */
	private static int m_cursorX = 0, m_cursorY = 0;
	/** screen buffer - screen is held here and rendered on demand */
	private static int[] screenBuffer = new int[X_PIXELS * Y_PIXELS];
	/** Bitmap used for rendering */
	private static Bitmap screenBitmap;
	private static int m_pix8Width;

	static {
		screenBitmap = createCompatibleImage(X_PIXELS, Y_PIXELS);
		m_screenChanged = new boolean[SCREEN_LENGTH];
	}

	/**
	 * <UL>
	 * <LI>Set cached pointers to the BaseSpectrum and BaseMemory objects.</LI>
	 * <LI>Allocate pixel modification bit vector.</LI>
	 * <LI>Allocate ink and paper tables.</LI>
	 * <LI>Set the size of the Canvas object, and add it to the Container.</LI>
	 * <LI>Add the keyboard object as the KeyListener.</LI>
	 * <LI>Set the scale.</LI>
	 * </UL>
	 */
	public static void init(final SurfaceHolder hodler) {
		m_inkTable = new int[128];
		m_paperTable = new int[128];
		surfaceHolder = hodler;
		// Build hash tables to speed up paint()
		for (int i = 0; i < 2; i++) {
			for (int j = 0; j < 8; j++) {
				for (int k = 0; k < 8; k++) {
					m_inkTable[(i << 6) + (j << 3) + k] = k + (i << 3);
					m_paperTable[(i << 6) + (j << 3) + k] = j + (i << 3);
				}
			}
		}
		M_PAGE = 0x4000;
		m_pix8Width = 8 * m_scale;
	}

	/**
	 * Touch all the screen bits in order to force the initial refresh.
	 */
	public static void reset() {
		for (int i = 0; i < PIXEL_LENGTH; i++) {
			m_screenChanged[i] = true;
		}
		m_screenDirty = true;
		m_borderDirty = true;
	}

	/**
	 * Release all cached pointers, set all the tables to null.
	 */
	public static void terminate() {
		m_screenChanged = null;
		m_inkTable = null;
		m_paperTable = null;
		screenBitmap = null;
		screenBuffer = null;
	}

	/**
	 * Trivial accessor to the physical memory page for the screen data.
	 */
	public static int getPage() {
		return M_PAGE;
	}

	/**
	 * Trivial mutator for the physical memory page for the screen data.
	 */
	public static void setPage(int page) {
		M_PAGE = page;
	}

	/**
	 * Redraw the screen, but do <i>not</i> erase it first in order to avoid
	 * flicker.
	 * 
	 * This method just calls {@link #paint} with the specified Canvas context.
	 */
	public static void update() {
		if (BaseSpectrum.isNoCache())
			paintAll(surfaceHolder);
		else
			paint(surfaceHolder);
	}

	public void run() {
		if (BaseSpectrum.isNoCache())
			paintAll(surfaceHolder);
		else
			paint(surfaceHolder);
	}

	/**
	 * If the screen is dirty, update it by walking the memory page and
	 * re-drawing the data that has changed.
	 * <P>
	 * Call the abstract <TT>draw8()</TT> method below to do the actual work for
	 * every pixel.
	 * <P>
	 * Note that there exists a benign race condition in this code: a PAINT
	 * event may be issued as the CPU is decoding instructions and updating the
	 * screen from the main thread. The reason this race condition is benign is
	 * because in the worst case it can cause aliasing: the screen contents are
	 * changed half way through a frame. Fixing this race condition would
	 * involve making a copy of the screen memory page, which hurts performance.
	 * 
	 * @see #draw8
	 */
	protected static void paint(final SurfaceHolder h) {
		if (m_screenDirty) {
			startRender();
			final byte[] memory = Z80.getMemory();
			for (int addr16 = 0; addr16 < PIXEL_LENGTH; addr16++) {
				if (!m_screenChanged[addr16]) {
					continue;
				}
				m_screenChanged[addr16] = false;
				int x = ((addr16 & 0x1f) << 3);
				int y = ((addr16 & 0x00e0) >> 2) + ((addr16 & 0x0700) >> 8)
						+ ((addr16 & 0x1800) >> 5);
				int pix8 = memory[addr16 + M_PAGE];
				int attr8 = ((int) memory[M_PAGE + PIXEL_LENGTH
						+ ((x >> 3) + ((y & 0xf8) << 2))]) & 0xff;
				draw8(x, y, pix8, attr8);
			}
			endRender();
			Canvas c = null;
			try {
				c = h.lockCanvas();
				screenBitmap.setPixels(screenBuffer, 0, X_PIXELS, 0, 0,
						X_PIXELS, Y_PIXELS);
				c.drawBitmap(screenBitmap, 0, 0, null);
			} finally {
				if (c != null)
					h.unlockCanvasAndPost(c);
			}
			m_screenDirty = false;
		}
	}

	private static void paintAll(final SurfaceHolder h) {
		startRender();
		final byte[] memory = Z80.getMemory();
		for (int addr16 = 0; addr16 < PIXEL_LENGTH; addr16++) {
			int x = ((addr16 & 0x1f) << 3);
			int y = ((addr16 & 0x00e0) >> 2) + ((addr16 & 0x0700) >> 8)
					+ ((addr16 & 0x1800) >> 5);
			int pix8 = memory[M_PAGE + addr16];
			int attr8 = ((int) memory[M_PAGE + PIXEL_LENGTH
					+ ((x >> 3) + ((y & 0xf8) << 2))]) & 0xff;
			draw8(x, y, pix8, attr8);
		}
		endRender();
		Canvas c = null;
		try {
			c = h.lockCanvas();
			screenBitmap.setPixels(screenBuffer, 0, X_PIXELS, 0, 0, X_PIXELS,
					Y_PIXELS);
			c.drawBitmap(screenBitmap, 0, 0, null);
		} finally {
			if (c != null)
				h.unlockCanvasAndPost(c);
		}
		m_screenDirty = false;
	}

	/**
	 * Draws the given 8 bits (pix8), with the given attribute values (attr8) at
	 * coordinates x, y on the Canvas.
	 * <P>
	 * This method is implemented as appropriate for performance reasons in the
	 * various child classes.
	 * 
	 * @param x
	 *            The X coordinate where the pixels should be drawn onto the
	 *            Canvas object.
	 * @param y
	 *            The Y coordinat where the pixels should be drawn onto the
	 *            Canvas object.
	 * @param pix8
	 *            The 8 bits to draw starting at (X,Y) ending at (X+8,Y).
	 * @param attr8
	 *            The attributes used to draw the 8 bits.
	 */
	private static void draw8(final int x, final int y, int pix8, int attr8) {
		if (m_flashPhase && ((attr8 & FLASH) != 0)) {
			attr8 = (attr8 & 0xc0) | (~attr8 & 0x3f);
		}
		final int ipix = m_inkTable[attr8 & ~FLASH];
		final int ppix = m_paperTable[attr8 & ~FLASH];
		final int offset, width;
		width = m_screenWidth;
		offset = y * width + x;
		for (int i = 0; i < m_pix8Width;) {
			int rgb = s_rgbPalette[((pix8 & 0x80) != 0 ? ipix : ppix)];
			for (int j = 0; j < m_scale; j++) {
				screenBuffer[offset + i++] = rgb;
			}
			pix8 <<= 1;
		}
		/*
		 * for (int i = 1; i < m_pix8Height; i++) {
		 * System.arraycopy(screenBuffer, offset, screenBuffer, offset + i
		 * width, m_pix8Width); }
		 */
	}

	/**
	 * Create and return an Image object that isa appropriate for this Screen
	 * object.
	 * <P>
	 * Override this method to return image types that are optimized for the
	 * particular JVM and/or platform.
	 */
	private static Bitmap createCompatibleImage(final int width,
			final int height) {
		final Bitmap b = Bitmap.createBitmap(width, height, Config.RGB_565);
		return b;
	}

	/**
	 * Trigger method that is called right before the screen is rendered.
	 * <P>
	 * Override this method to receive a notification.
	 */
	public static void startRender() {
		// Empty
	}

	/**
	 * Trigger method that is called right afterthe screen is rendered.
	 * <P>
	 * Override this method to receive a notification.
	 */
	public static void endRender() {
		// Empty
	}

	/**
	 * When the flash phase changes, update all attribute values that have the
	 * flash bit set.
	 * <P>
	 * The next time the screen is rendered, all flash attributes will appear
	 * correctly.
	 */
	public static void flash() {
		if (BaseSpectrum.isNoCache())
			return;
		m_flashPhase = !m_flashPhase;
		final byte[] memory = Z80.getMemory();
		for (int i = 0, addr16 = PIXEL_LENGTH; i < ATTR_LENGTH; i++, addr16++) {
			int val8 = ((int) memory[M_PAGE + addr16]) & 0xff;
			if ((val8 & FLASH) != 0) {
				attrTouch(addr16);
			}
		}
	}

	/**
	 * Touch a particular attribute value, marking it for update next time the
	 * screen is rendered.
	 * <P>
	 * The touching of an attribute entails touching the 8 byte values whose
	 * screen representation is altered by this attribute value.
	 * 
	 * @param addr16
	 *            The 16-bit absolute memory address (in the attribute memory
	 *            area) which should be touched.
	 */
	public static void attrTouch(int addr16) {
		addr16 = ((addr16 & 0x300) << 3) + (addr16 & 0xff);
		for (int i = 0; i < 8; i++, addr16 += X_PIXELS) {
			screenTouch(addr16);
		}
	}

	/**
	 * Touch a particular byte value in screen memory, marking it for update
	 * next time the screen is rendered.
	 * 
	 * @param addr16
	 *            The 16-bit absolute memory address (in the screen memory area)
	 *            which should be touched.
	 */
	public static void screenTouch(final int addr16) {
		try {
			m_screenChanged[addr16 & 0x3fff] = true;
			m_screenDirty = true;
		} catch (Exception e) {
			Log.e("screenTouch", "Error:" + addr16 + "; " + (addr16 & 0x3fff)
					+ "; " + m_screenChanged.length);
		}
	}

	/**
	 * Change the current border color, and mark the border as dirty for update
	 * at the next screen refresh.
	 */
	public static void setBorderColor(int val8) {
		m_borderDirty = true;
	}

	/**
	 * Convert a given (x, y) character coordinate to a memory pixel address.
	 * 
	 * @param x
	 *            X coordinate (0-31) of character on screen
	 * @param y
	 *            Y coordinate (0-23) of character on screen
	 * @return Memory pixel address corresponding to given character coordinate
	 */
	private static int cursorToPixelAddr16(int x, int y) {
		if (x < 0 || x >= COLS || y < 0 || y >= ROWS) {
			throw new IllegalArgumentException("Invalid coordinates: " + x
					+ "," + y);
		}
		// Convert y from letter coordinates to a memory address
		// Note that x (in letter coordinates) is already a
		// memory address
		y = y * 8;
		return (PIXEL_START + (y >> 6) * (PIXEL_LENGTH / 3) + ((y & 0x3f) >> 3)
				* COLS + (y & 0x07) * X_PIXELS + x);
	}

	/**
	 * Convert a given (x, y) character coordinate to a memory attribute
	 * address.
	 * 
	 * @param x
	 *            X coordinate (0-31) of character on screen
	 * @param y
	 *            Y coordinate (0-23) of character on screen
	 * @return Memory attribute address corresponding to given character
	 *         coordinate
	 */
	private static int cursorToAttrAddr16(int x, int y) {
		if (x < 0 || x >= COLS || y < 0 || y >= ROWS) {
			throw new IllegalArgumentException("Invalid coordinates: " + x
					+ "," + y);
		}
		return (ATTR_START + y * COLS + x);
	}

	/**
	 * Set the current cursor position used by print methods below.
	 * 
	 * @param x
	 *            New cursor X position
	 * @param y
	 *            New cursor Y position
	 * 
	 * @see #print(char, int)
	 * @see #println(char, int)
	 * @see #print(String, int)
	 * @see #println(String, int)
	 */
	public static void setCursor(int x, int y) {
		if (x < 0 || x >= COLS || y < 0 || y >= ROWS) {
			throw new IllegalArgumentException("Invalid coordinates: " + x
					+ "," + y);
		}
		m_cursorX = x;
		m_cursorY = y;
	}

	/**
	 * Retrieve current cursor X position.
	 * 
	 * @return Current cursor X position
	 */
	public static int getCursorX() {
		return m_cursorX;
	}

	/**
	 * Retrieve current cursor Y position.
	 * 
	 * @return Current cursor Y position
	 */
	public static int getCursorY() {
		return m_cursorY;
	}

	/**
	 * Clear the screen with the given attribute color.
	 * 
	 * @param attr
	 *            Attribute color used to clear the screen
	 */
	public static void clear(int attr) {
		setCursor(0, 0);
		clear(COLS, ROWS, attr);
	}

	/**
	 * Clear a box on the screen, with upper-left corner at the current cursor
	 * position, with given height and width using the given attribute color.
	 * 
	 * @param width
	 *            Width of box to clear
	 * @param height
	 *            Height of box to clear
	 * @param attr
	 *            Attribute color used to clear
	 */
	public static void clear(int width, int height, int attr) {
		int saveX = m_cursorX;
		int saveY = m_cursorY;
		for (int row = saveY; row < height; row++) {
			for (int col = saveX; col < width; col++) {
				setCursor(col, row);
				print(' ', attr);
			}
		}
		setCursor(saveX, saveY);
	}

	/**
	 * Print a given character at the current cursor position and advance the
	 * cursor X position by 1.
	 * 
	 * If the cursor falls outside the bounds of the screen, do not display
	 * anything.
	 * 
	 * If the letter is invalid (not in the interval [32, 127]) display '?'.
	 * 
	 * You must manually call {@link #repaint}
	 * 
	 * @param letter
	 *            The letter to print on the screen
	 * @param attr
	 *            The attribute used to print the letter on the screen
	 */
	public static void print(char letter, int attr) {
		if (m_cursorX < COLS && m_cursorY < ROWS) {
			int screenAddr16 = cursorToPixelAddr16(m_cursorX, m_cursorY);
			// The Spectrum character set is a subset of ASCII,
			// going from space (32) to (C) (127)
			if (letter < 32 || letter > 127) {
				letter = '?';
			}
			int letterAddr16 = CHARSET_ADDR + (letter - ' ') * 8;
			Z80.write8(cursorToAttrAddr16(m_cursorX, m_cursorY), attr);
			for (int i = 0; i < 8; i++) {
				Z80.write8(screenAddr16, Z80.getMemory()[letterAddr16]);
				screenAddr16 += X_PIXELS;
				letterAddr16++;
			}
			m_cursorX++;
		}
	}

	/**
	 * Call {@link #print(char, int)} and advance the cursor Y position by 1.
	 * 
	 * @param letter
	 *            The letter to print on the screen
	 * @param attr
	 *            The attribute used to print the letter on the screen
	 * 
	 * @see #print(char, int)
	 */
	public static void println(char letter, int attr) {
		int saveX = m_cursorX;
		int saveY = m_cursorY;
		print(letter, attr);
		setCursor(saveX, saveY + 1);
	}

	/**
	 * Call {@link #print(char, int)} for every character in the given string.
	 * 
	 * @param string
	 *            The string to print on the screen
	 * @param attr
	 *            The attribute used to print the letter on the screen
	 * 
	 * @see #print(char, int)
	 */
	public static void print(String string, int attr) {
		for (int i = 0; i < string.length(); i++) {
			print(string.charAt(i), attr);
		}
	}

	/**
	 * Call {@link #print(String, int)} with the given string, then set the
	 * cursor to be on the line immediately underneath the original cursor
	 * position.
	 * 
	 * @param string
	 *            The string to print on the screen
	 * @param attr
	 *            The attribute used to print the letter on the screen
	 * 
	 * @see #print(String, int)
	 */
	public static void println(String string, int attr) {
		int saveX = m_cursorX;
		int saveY = m_cursorY;
		print(string, attr);
		setCursor(saveX, saveY + 1);
	}

	/**
	 * Load the screen contents from the given BaseLoader object.
	 * <P>
	 * Since the screen data is not saved, this method does nothing.
	 */
	public static void load(BaseLoader loader) {
		// Empty
	}

	public void dumpScreenshot() {
		// Empty
	}
}
