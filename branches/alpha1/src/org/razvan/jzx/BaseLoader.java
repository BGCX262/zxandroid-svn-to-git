package org.razvan.jzx;

import java.io.InputStream;

/**
 * Base class extended by all classes that can load information from a saved
 * file format into the Spectrum.
 * <P>
 * This class provides basic, common loader functionality for the various loader
 * types.
 * <P>
 * The BaseLoader class provides methods for retrieving the data that was
 * loaded. The actual loading code, parsing of the file format etc. is performed
 * in child classes. The BaseLoader class is implemented (almost) as Visitor
 * pattern, whereby it is passed into all the components of the Spectrum tree,
 * and each component retrieves from it the data that is relevant to that
 * component.
 * 
 * @author <A HREF="mailto:razvan.surdulescu@post.harvard.edu">Razvan
 *         Surdulescu</A> (c) 2001 - 2006 <BR>
 *         You may use and distribute this software for free provided you
 *         include this copyright notice. You may not sell this software, use my
 *         name for publicity reasons or modify the code without permission from
 *         me.
 */
public class BaseLoader {
	/** 48k Spectrum model data */
	public static final int MODE_48 = 0;

	/** 128k Spectrum model data */
	public static final int MODE_128 = 1;

	/** Z80 basic registers. */
	protected int m_af16, m_bc16, m_de16, m_hl16;
	/** Z80 alternate registers. */
	protected int m_af16alt, m_bc16alt, m_de16alt, m_hl16alt;
	/** Z80 index registers. */
	protected int m_ix16, m_iy16;
	/** Z80 core registers. */
	protected int m_sp16, m_pc16;
	/** Z80 interrupt and refresh registers. */
	protected int m_r8, m_i8;
	/** Z80 interrupt mode. */
	protected int m_im2;
	/** Z80 flip-flops. */
	protected int m_iff1a, m_iff1b;

	/** Spectrum model (MODE_48 or MODE_128). */
	protected int m_mode;
	/** Spectrum model (issue). */
	protected int m_issue;
	/** Border color. */
	protected int m_border;
	/** 128k specific saved I/O registers. */
	protected int m_last0x7ffd;
	protected int m_last0xfffd;

	protected InputStream m_is;
	/**
	 * "headless" memory object that stores the memory image.
	 * 
	 * @see BaseMemory
	 */
	protected byte [] m_memory;

	public int getAF16() {
		return m_af16;
	}

	public int getBC16() {
		return m_bc16;
	}

	public int getDE16() {
		return m_de16;
	}

	public int getHL16() {
		return m_hl16;
	}

	public int getAF16ALT() {
		return m_af16alt;
	}

	public int getBC16ALT() {
		return m_bc16alt;
	}

	public int getDE16ALT() {
		return m_de16alt;
	}

	public int getHL16ALT() {
		return m_hl16alt;
	}

	public int getIX16() {
		return m_ix16;
	}

	public int getIY16() {
		return m_iy16;
	}

	public int getSP16() {
		return m_sp16;
	}

	public int getPC16() {
		return m_pc16;
	}

	public int getR8() {
		return m_r8;
	}

	public int getI8() {
		return m_i8;
	}

	public int getIM2() {
		return m_im2;
	}

	public int getIFF1a() {
		return m_iff1a;
	}

	public int getIFF1b() {
		return m_iff1b;
	}

	public int getMode() {
		return m_mode;
	}

	public int getIssue() {
		return m_issue;
	}

	public int getBorder() {
		return m_border;
	}

	public int getLast0x7ffd() {
		return m_last0x7ffd;
	}

	public int getLast0xfffd() {
		return m_last0xfffd;
	}

	/** Trivial accessor for the (headless) memory object. */
	public byte [] getMemory() {
		return m_memory;
	}

	/**
	 * Setup the logger and url, allocate and initialize the "headless" memory
	 * object.
	 */
	public BaseLoader() {
		m_memory = new byte [0x10000];
	}
}
