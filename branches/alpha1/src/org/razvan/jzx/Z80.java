package org.razvan.jzx;

import java.io.IOException;
import java.io.InputStream;

import android.util.Log;

/**
 * The Z80 CPU component of the Spectrum emulator.
 * 
 * @author <A HREF="mailto:razvan.surdulescu@post.harvard.edu">Razvan
 *         Surdulescu</A> (c) 2001 - 2006 <BR>
 *         You may use and distribute this software for free provided you
 *         include this copyright notice. You may not sell this software, use my
 *         name for publicity reasons or modify the code without permission from
 *         me.
 * 
 * @see BaseSpectrum
 */
public class Z80 {
	/**
	 * Indicates that the <TT>emulate()</TT> method should end.
	 */
	protected static volatile boolean m_stop;

	/**
	 * Indicates that the <TT>emulate()</TT> method should pause.
	 */
	protected static volatile boolean m_pause;

	/**
	 * The current count of T-States for the CPU.
	 * <P>
	 * This count is incremented by the appropriate amount for every instruction
	 * that is executed.
	 * <P>
	 * The T-States are used for synchonizing the rendering of screen lines that
	 * comprise the screen frame.
	 */
	private static int m_tstates;

	/** The bit mask used to extract the CARRY flag from the F register. */
	private static final int CARRY_MASK = 0x01;

	/** The bit mask used to set the 5 flag in some instructions. */
	private static final int ONE_MASK = 0x01;

	/** The bit mask used to extract the ADDSUBTRACT flag from the F register. */
	private static final int ADDSUBTRACT_MASK = 0x02;

	/** The bit mask used to extract the PARITY flag from the F register. */
	private static final int PARITY_MASK = 0x04;

	/** The bit mask used to extract the OVERFLOW flag from the F register. */
	private static final int OVERFLOW_MASK = PARITY_MASK;

	/** The bit mask used to extract the THREE flag from the F register. */
	private static final int THREE_MASK = 0x08;

	/** The bit mask used to extract the HALFCARRY flag from the F register. */
	private static final int HALFCARRY_MASK = 0x10;

	/** The bit mask used to extract the FIVE flag from the F register. */
	private static final int FIVE_MASK = 0x20;

	/** The bit mask used to extract the ZERO flag from the F register. */
	private static final int ZERO_MASK = 0x40;

	/** The bit mask used to extract the SIGN flag from the F register. */
	private static final int SIGN_MASK = 0x80;

	/**
	 * Explicit flags, which represent the bits in the F register as distinct
	 * variables.
	 * 
	 * @see #storeFlags
	 * @see #retrieveFlags
	 */
	private static boolean m_carryF, m_addsubtractF, m_parityoverflowF,
			m_halfcarryF, m_zeroF, m_signF, m_5F, m_3F;

	/** The basic registers (least significant 8 bits.) */
	private static int m_a8, m_f8, m_b8, m_c8, m_d8, m_e8, m_h8, m_l8;
	/** The alternate registers (least significant 16 bits.) */
	private static int m_af16alt, m_bc16alt, m_de16alt, m_hl16alt;
	/** The index registers (least significant 16 bits.) */
	private static int m_ix16, m_iy16, m_xx16;
	/** The core registers (least significant 16 bits.) */
	private static int m_sp16, m_pc16;
	/** The refresh and interrupt registers (least significant 8 bits.) */
	private static int m_r8, m_i8;
	/** The interrupt mode (least significant 2 bits.) */
	private static int m_im2;
	/** The flip-flops (least significant 1 bit.) */
	private static int m_iff1a, m_iff1b;

	/**
	 * Secret, internal Z80 register that is set as follows:
	 * 
	 * ADD HL,xx = high byte of HL, i.e. H, before the addition LD r,(IX/IY+d) =
	 * high byte of the resulting address IX/IY+d JR d = high byte target
	 * address of the jump
	 */
	private static int m_x8;

	/**
	 * Parity table for 256 bytes (true = even parity, false = odd parity).
	 */
	private static final boolean m_parityTable[] = { true, false, false, true,
			false, true, true, false, false, true, true, false, true, false,
			false, true, false, true, true, false, true, false, false, true,
			true, false, false, true, false, true, true, false, false, true,
			true, false, true, false, false, true, true, false, false, true,
			false, true, true, false, true, false, false, true, false, true,
			true, false, false, true, true, false, true, false, false, true,
			false, true, true, false, true, false, false, true, true, false,
			false, true, false, true, true, false, true, false, false, true,
			false, true, true, false, false, true, true, false, true, false,
			false, true, true, false, false, true, false, true, true, false,
			false, true, true, false, true, false, false, true, false, true,
			true, false, true, false, false, true, true, false, false, true,
			false, true, true, false, false, true, true, false, true, false,
			false, true, true, false, false, true, false, true, true, false,
			true, false, false, true, false, true, true, false, false, true,
			true, false, true, false, false, true, true, false, false, true,
			false, true, true, false, false, true, true, false, true, false,
			false, true, false, true, true, false, true, false, false, true,
			true, false, false, true, false, true, true, false, true, false,
			false, true, false, true, true, false, false, true, true, false,
			true, false, false, true, false, true, true, false, true, false,
			false, true, true, false, false, true, false, true, true, false,
			false, true, true, false, true, false, false, true, true, false,
			false, true, false, true, true, false, true, false, false, true,
			false, true, true, false, false, true, true, false, true, false,
			false, true, };

	/** Half carry table (addition). */
	private static boolean m_halfcarryTable[] = { false, false, true, false,
			true, false, true, true };
	/** Half carry table (subtraction). */
	private static boolean m_subhalfcarryTable[] = { false, true, true, true,
			false, false, false, true };

	/** Overflow table (addition). */
	private static boolean m_overflowTable[] = { false, true, false, false,
			false, false, true, false };
	/** Overflow table (subtraction). */
	private static boolean m_suboverflowTable[] = { false, false, false, true,
			true, false, false, false };

	private static byte[] memory;

	/**
	 * Cache references to the memory and I/O objects.
	 */
	public static void init() {
		memory = new byte[0x10000];
		try {
			readROM();
		} catch (Exception e) {
			Log.e(Z80.class.getName(), "Error reading ROM: " + e.toString());
		}
	}

	/**
	 * Set the PC, I, R, IM and IFF* registers to 0.
	 */
	public static void reset() {
		final byte[] randoms = new byte[] { -56, 56, 98, -105, 48, -46, 8, -85 };
		m_pc16 = 0;
		m_i8 = 0;
		m_r8 = 0;
		m_im2 = 0;
		m_iff1a = 0;
		m_iff1b = 0;
		int c = 0;
		for (int i = BaseScreen.SCREEN_START; i < 0xffff; i++, c++) {
			memory[i] = randoms[c];
			if (c >= (randoms.length - 1))
				c = 0;
		}
	}

	/**
	 * Does nothing.
	 */
	public static void terminate() {
		m_stop = true;
	}

	/**
	 * Interrupt the CPU.
	 * <P>
	 * If IFF1a is not zero, set both IFF flags to 0 and push the value of PC on
	 * the stack. If the IM is 2, jump to the address (I, 0xFF), otherwise jump
	 * to 0x38.
	 */
	public static void interrupt() {
		if (m_iff1a != 0) {
			// Make sure we don't return to a halt instruction.
			if (read8(m_pc16) == 0x76) {
				inc16pc();
			}

			m_iff1a = 0;
			m_iff1b = 0;
			push(m_pc16);

			if (m_im2 == 2) {
				m_tstates += 19;
				m_pc16 = read16((m_i8 << 8) | 0xff);
			} else {
				m_tstates += 13;
				m_pc16 = 0x38;
			}
		}
	}

	/**
	 * Generates a non-maskable interrupt (NMI.)
	 * <P>
	 * Set IFF1b = IFF1a, and set IFF1a to 0 (disable regular interrupts.) Push
	 * the PC on the stack, then jump to address 0x66.
	 */
	public static void nmi() {
		m_tstates += 15;

		m_iff1b = m_iff1a;
		m_iff1a = 0;

		// Make sure we don't return to a halt instruction.
		if (read8(m_pc16) == 0x76) {
			inc16pc();
		}

		push(m_pc16);
		m_pc16 = 0x66;
	}

	/**
	 * Trivial accessor for the T-States value.
	 */
	public static int getTStates() {
		return m_tstates;
	}

	/**
	 * Trivial mutator for the T-States value.
	 */
	public static void setTStates(final int tstates) {
		m_tstates = tstates;
	}

	/**
	 * Add a number to the current value of T-States.
	 */
	public static void addTStates(final int val) {
		m_tstates += val;
	}

	/**
	 * Accessor for the 16-bit AF register (it combines the A and F registers.)
	 */
	private static int af16() {
		return ((m_a8 << 8) | m_f8);
	}

	/**
	 * Accessor for the 16-bit BC register (it combines the A and F registers.)
	 */
	private static int bc16() {
		return ((m_b8 << 8) | m_c8);
	}

	/**
	 * Accessor for the 16-bit DE register (it combines the A and F registers.)
	 */
	private static int de16() {
		return ((m_d8 << 8) | m_e8);
	}

	/**
	 * Accessor for the 16-bit HL register (it combines the A and F registers.)
	 */
	private static int hl16() {
		return ((m_h8 << 8) | m_l8);
	}

	/**
	 * Mutator for the 16-bit AF register (it sets the A and F registers.)
	 */
	private static void af16(final int val16) {
		m_a8 = (val16 >> 8);
		m_f8 = (val16 & 0xff);
	}

	/**
	 * Mutator for the 16-bit BC register (it sets the A and F registers.)
	 */
	private static void bc16(final int val16) {
		m_b8 = (val16 >> 8);
		m_c8 = (val16 & 0xff);
	}

	/**
	 * Mutator for the 16-bit DE register (it sets the A and F registers.)
	 */
	private static void de16(final int val16) {
		m_d8 = (val16 >> 8);
		m_e8 = (val16 & 0xff);
	}

	/**
	 * Mutator for the 16-bit HL register (it sets the A and F registers.)
	 */
	private static void hl16(final int val16) {
		m_h8 = (val16 >> 8);
		m_l8 = (val16 & 0xff);
	}

	/**
	 * Accessor for the low 8 bits of the current index register.
	 * <P>
	 * The current index register is stored in m_xx16, and could either be
	 * m_ix16 or m_iy16.
	 */
	private static int xx16low8() {
		return (m_xx16 & 0xff);
	}

	/**
	 * Accessor for the high 8 bits of the current index register.
	 * <P>
	 * The current index register is stored in m_xx16, and could either be
	 * m_ix16 or m_iy16.
	 */
	private static int xx16high8() {
		return (m_xx16 >> 8);
	}

	/**
	 * Mutator for the low 8 bits of the current index register.
	 * <P>
	 * The current index register is stored in m_xx16, and could either be
	 * m_ix16 or m_iy16.
	 */
	private static void xx16low8(final int val8) {
		m_xx16 = ((m_xx16 & 0xff00) | val8);
	}

	/**
	 * Mutator for the high 8 bits of the current index register.
	 * <P>
	 * The current index register is stored in m_xx16, and could either be
	 * m_ix16 or m_iy16.
	 */
	private static void xx16high8(final int val8) {
		m_xx16 = ((val8 << 8) | (m_xx16 & 0xff));
	}

	/**
	 * Store the values from m_*F variables into the m_f8 register, according to
	 * the *_MASK variables.
	 */
	private static void storeFlags() {
		if (m_signF)
			m_f8 |= SIGN_MASK;
		else
			m_f8 &= ~SIGN_MASK;
		if (m_zeroF)
			m_f8 |= ZERO_MASK;
		else
			m_f8 &= ~ZERO_MASK;
		if (m_halfcarryF)
			m_f8 |= HALFCARRY_MASK;
		else
			m_f8 &= ~HALFCARRY_MASK;
		if (m_parityoverflowF)
			m_f8 |= OVERFLOW_MASK;
		else
			m_f8 &= ~OVERFLOW_MASK;
		if (m_addsubtractF)
			m_f8 |= ADDSUBTRACT_MASK;
		else
			m_f8 &= ~ADDSUBTRACT_MASK;
		if (m_carryF)
			m_f8 |= CARRY_MASK;
		else
			m_f8 &= ~CARRY_MASK;
		if (m_3F)
			m_f8 |= THREE_MASK;
		else
			m_f8 &= ~THREE_MASK;
		if (m_5F)
			m_f8 |= FIVE_MASK;
		else
			m_f8 &= ~FIVE_MASK;
	}

	/**
	 * Retrieve the flags from the m_f8 register into the the m_*F variables,
	 * according to the *_MASK variables.
	 */
	private static void retrieveFlags() {
		m_signF = ((m_f8 & SIGN_MASK) != 0);
		m_zeroF = ((m_f8 & ZERO_MASK) != 0);
		m_halfcarryF = ((m_f8 & HALFCARRY_MASK) != 0);
		m_parityoverflowF = ((m_f8 & OVERFLOW_MASK) != 0);
		m_addsubtractF = ((m_f8 & ADDSUBTRACT_MASK) != 0);
		m_carryF = ((m_f8 & CARRY_MASK) != 0);
		m_3F = ((m_f8 & THREE_MASK) != 0);
		m_5F = ((m_f8 & FIVE_MASK) != 0);
	}

	/**
	 * Increment the 16-bit PC register.
	 */
	private static int inc16pc() {
		final int work16 = m_pc16;
		m_pc16 = ((m_pc16 + 1) & 0xffff);
		return work16;
	}

	/**
	 * Increment the 16-bit SP register.
	 */
	private static int inc16sp() {
		final int work16 = m_sp16;
		m_sp16 = ((m_sp16 + 1) & 0xffff);
		return work16;
	}

	/**
	 * Increment the 16-bit BC register.
	 */
	private static int inc16bc() {
		final int work16 = bc16();
		bc16((work16 + 1) & 0xffff);
		return work16;
	}

	/**
	 * Increment the 16-bit DE register.
	 */
	private static int inc16de() {
		final int work16 = de16();
		de16((work16 + 1) & 0xffff);
		return work16;
	}

	/**
	 * Increment the 16-bit HL register.
	 */
	private static int inc16hl() {
		final int work16 = hl16();
		hl16((work16 + 1) & 0xffff);
		return work16;
	}

	/**
	 * Increment the 16-bit (current) index register (m_xx16).
	 */
	private static int inc16xx() {
		final int work16 = m_xx16;
		m_xx16 = ((m_xx16 + 1) & 0xffff);
		return work16;
	}

	/**
	 * Decrement the 16-bit PC register.
	 */
	private static int dec16pc() {
		final int work16 = m_pc16;
		m_pc16 = ((m_pc16 - 1) & 0xffff);
		return work16;
	}

	/**
	 * Decrement the 16-bit SP register.
	 */
	private static int dec16sp() {
		final int work16 = m_sp16;
		m_sp16 = ((m_sp16 - 1) & 0xffff);
		return work16;
	}

	/**
	 * Decrement the 16-bit BC register.
	 */
	private static int dec16bc() {
		final int work16 = bc16();
		bc16((work16 - 1) & 0xffff);
		return work16;
	}

	/**
	 * Decrement the 16-bit DE register.
	 */
	private static int dec16de() {
		final int work16 = de16();
		de16((work16 - 1) & 0xffff);
		return work16;
	}

	/**
	 * Decrement the 16-bit HL register.
	 */
	private static int dec16hl() {
		final int work16 = hl16();
		hl16((work16 - 1) & 0xffff);
		return work16;
	}

	/**
	 * Decrement the 16-bit (current) index register (m_xx16).
	 */
	private static int dec16xx() {
		final int work16 = m_xx16;
		m_xx16 = ((m_xx16 - 1) & 0xffff);
		return work16;
	}

	/**
	 * Add a 16-bit value to the (current) index register (m_xx16) and set the
	 * appropriate flags.
	 */
	private static void add_xx(final int val16) {
		final int work32 = m_xx16 + val16;
		final int idx = ((m_xx16 & 0x800) >> 9) | ((val16 & 0x800) >> 10)
				| ((work32 & 0x800) >> 11);
		m_xx16 = work32 & 0xffff;
		m_halfcarryF = m_halfcarryTable[idx];
		m_addsubtractF = false;
		m_carryF = ((work32 & 0x10000) != 0);

		final int work8 = m_xx16 >> 8;
		m_3F = ((work8 & THREE_MASK) != 0);
		m_5F = ((work8 & FIVE_MASK) != 0);
	}

	/**
	 * Add a 16-bit value to the HL register and set the appropriate flags.
	 */
	private static void add_hl(final int val16) {
		m_x8 = m_h8;

		final int hl16 = hl16();
		final int work32 = hl16 + val16;
		final int idx = ((hl16 & 0x800) >> 9) | ((val16 & 0x800) >> 10)
				| ((work32 & 0x800) >> 11);
		hl16(work32 & 0xffff);
		m_halfcarryF = m_halfcarryTable[idx];
		m_addsubtractF = false;
		m_carryF = ((work32 & 0x10000) != 0);
		m_3F = ((m_h8 & THREE_MASK) != 0);
		m_5F = ((m_h8 & FIVE_MASK) != 0);
	}

	/**
	 * Add a 16-bit value, with carry, to the HL register and set the
	 * appropriate flags.
	 */
	private static void adc_hl(final int val16) {
		int hl16 = hl16();
		final int work32 = hl16 + val16 + (m_carryF ? 1 : 0);
		final int idx = ((hl16 & 0x8800) >> 9) | ((val16 & 0x8800) >> 10)
				| ((work32 & 0x8800) >> 11);
		hl16 = (work32 & 0xffff);
		hl16(hl16);
		m_signF = ((hl16 & 0x8000) != 0);
		m_zeroF = (hl16 == 0);
		m_halfcarryF = m_halfcarryTable[idx & 0x7];
		m_parityoverflowF = m_overflowTable[idx >> 4];
		m_addsubtractF = false;
		m_carryF = ((work32 & 0x10000) != 0);
		m_3F = ((m_h8 & THREE_MASK) != 0);
		m_5F = ((m_h8 & FIVE_MASK) != 0);
	}

	/**
	 * Subtract a 16-bit value, with carry, from the HL register and set the
	 * appropriate flags.
	 */
	private static void sbc_hl(final int val16) {
		int hl16 = hl16();
		final int work32 = hl16 - val16 - (m_carryF ? 1 : 0);
		final int idx = ((hl16 & 0x8800) >> 9) | ((val16 & 0x8800) >> 10)
				| ((work32 & 0x8800) >> 11);
		hl16 = (work32 & 0xffff);
		hl16(hl16);
		m_signF = ((hl16 & 0x8000) != 0);
		m_zeroF = (hl16 == 0);
		m_halfcarryF = m_subhalfcarryTable[idx & 0x7];
		m_parityoverflowF = m_suboverflowTable[idx >> 4];
		m_addsubtractF = true;
		m_carryF = ((work32 & 0x10000) != 0);
		m_3F = ((m_h8 & THREE_MASK) != 0);
		m_5F = ((m_h8 & FIVE_MASK) != 0);
	}

	/**
	 * Add two 16-bit values and return the 16-bit result, without setting any
	 * flags.
	 */
	private static int add16(final int reg16, final int val16) {
		return (reg16 + val16) & 0xffff;
	}

	/**
	 * Subtract two 16-bit values and return the 16-bit result, without setting
	 * any flags.
	 */
	private static int sub16(final int reg16, final int val16) {
		return (reg16 - val16) & 0xffff;
	}

	/**
	 * Increment a 16-bit values and return the 16-bit result, without setting
	 * any flags.
	 */
	private static int inc16(final int reg16) {
		return (reg16 + 1) & 0xffff;
	}

	/**
	 * Increment twice a 16-bit values and return the 16-bit result, without
	 * setting any flags.
	 */
	private static int incinc16(final int reg16) {
		return (reg16 + 2) & 0xffff;
	}

	/**
	 * Decrement a 16-bit values and return the 16-bit result, without setting
	 * any flags.
	 */
	private static int dec16(final int reg16) {
		return (reg16 - 1) & 0xffff;
	}

	/**
	 * Decrement twice a 16-bit values and return the 16-bit result, without
	 * setting any flags.
	 */
	private static int decdec16(final int reg16) {
		return (reg16 - 2) & 0xffff;
	}

	/**
	 * Increment a 8-bit value and return the 8-bit result, setting the
	 * appropriate flags.
	 */
	private static int inc8(final int reg8) {
		final int work8 = (reg8 + 1) & 0xff;
		m_signF = ((work8 & 0x80) != 0);
		m_zeroF = (work8 == 0);
		m_halfcarryF = ((work8 & 0x0f) == 0);
		m_parityoverflowF = (work8 == 0x80);
		m_addsubtractF = false;
		m_3F = ((work8 & THREE_MASK) != 0);
		m_5F = ((work8 & FIVE_MASK) != 0);
		return work8;
	}

	/**
	 * Decrement a 8-bit value and return the 8-bit result, setting the
	 * appropriate flags.
	 */
	private static int dec8(final int reg8) {
		final int work8 = (reg8 - 1) & 0xff;
		m_signF = ((work8 & 0x80) != 0);
		m_zeroF = (work8 == 0);
		m_halfcarryF = ((work8 & 0x0f) == 0x0f);
		m_parityoverflowF = (work8 == 0x7f);
		m_addsubtractF = true;
		m_3F = ((work8 & THREE_MASK) != 0);
		m_5F = ((work8 & FIVE_MASK) != 0);
		return work8;
	}

	/**
	 * Rotate left with carry a 8-bit value and return the 8-bit result, setting
	 * the appropriate flags.
	 */
	private static int rlc8(final int reg8) {
		m_carryF = ((reg8 & 0x80) != 0);
		final int work8 = ((reg8 << 1) | (m_carryF ? 1 : 0)) & 0xff;
		shift_test(work8);
		return work8;
	}

	/**
	 * Rotate right with carry a 8-bit value and return the 8-bit result,
	 * setting the appropriate flags.
	 */
	private static int rrc8(final int reg8) {
		m_carryF = ((reg8 & 0x01) != 0);
		final int work8 = ((reg8 >> 1) | ((m_carryF ? 1 : 0) << 7));
		shift_test(work8);
		return work8;
	}

	/**
	 * Rotate left a 8-bit value and return the 8-bit result, setting the
	 * appropriate flags.
	 */
	private static int rl8(final int reg8) {
		boolean carry = ((reg8 & 0x80) != 0);
		final int work8 = ((reg8 << 1) | (m_carryF ? 1 : 0)) & 0xff;
		m_carryF = carry;
		shift_test(work8);
		return work8;
	}

	/**
	 * Rotate right a 8-bit value and return the 8-bit result, setting the
	 * appropriate flags.
	 */
	private static int rr8(final int reg8) {
		boolean carry = ((reg8 & 0x01) != 0);
		final int work8 = ((reg8 >> 1) | ((m_carryF ? 1 : 0) << 7));
		m_carryF = carry;
		shift_test(work8);
		return work8;
	}

	/**
	 * Shift left a 8-bit value and return the 8-bit result, setting the
	 * appropriate flags.
	 */
	private static int sla8(final int reg8) {
		m_carryF = ((reg8 & 0x80) != 0);
		final int work8 = (reg8 << 1) & 0xff;
		shift_test(work8);
		return work8;
	}

	/**
	 * Shift right a 8-bit value and return the 8-bit result, setting the
	 * appropriate flags.
	 */
	private static int sra8(final int reg8) {
		m_carryF = ((reg8 & 0x01) != 0);
		int work8 = (reg8 & 0x80);
		work8 = ((reg8 >> 1) | work8);
		shift_test(work8);
		return work8;
	}

	/**
	 * Shift left with 1-pad a 8-bit value and return the 8-bit result, setting
	 * the appropriate flags.
	 */
	private static int sli8(final int reg8) {
		m_carryF = ((reg8 & 0x80) != 0);
		final int work8 = ((reg8 << 1) | 0x01) & 0xff;
		shift_test(work8);
		return work8;
	}

	/**
	 * Shift right with 1-pad a 8-bit value and return the 8-bit result, setting
	 * the appropriate flags.
	 */
	private static int srl8(final int reg8) {
		m_carryF = ((reg8 & 0x01) != 0);
		final int work8 = (reg8 >> 1);
		shift_test(work8);
		return work8;
	}

	/**
	 * Set the appropriate flags as a result of a shift operation.
	 */
	private static void shift_test(final int reg8) {
		m_signF = ((reg8 & 0x80) != 0);
		m_zeroF = (reg8 == 0);
		m_halfcarryF = false;
		m_parityoverflowF = m_parityTable[reg8];
		m_addsubtractF = false;
		m_3F = ((reg8 & THREE_MASK) != 0);
		m_5F = ((reg8 & FIVE_MASK) != 0);
	}

	/**
	 * Pop the current 16-bit value at the top of the stack and return it.
	 */
	private static int pop16() {
		final int work16 = read16(m_sp16);
		m_sp16 = incinc16(m_sp16);
		return work16;
	}

	/**
	 * Push the given 16-bit value on the top of the stack.
	 */
	private static void push(final int reg16) {
		m_sp16 = decdec16(m_sp16);
		write16(m_sp16, reg16);
	}

	private static void ld_a_special(final int reg8) {
		m_a8 = reg8;
		m_signF = ((m_a8 & 0x80) != 0);
		m_zeroF = (m_a8 == 0);
		m_halfcarryF = false;
		m_parityoverflowF = (m_iff1b != 0);
		m_addsubtractF = false;
		m_3F = ((m_a8 & THREE_MASK) != 0);
		m_5F = ((m_a8 & FIVE_MASK) != 0);
	}

	/**
	 * Add a 8-bit value to the A register and set the appropriate flags.
	 */
	private static void add_a(final int val8) {
		final int work16 = m_a8 + val8;
		final int idx = ((m_a8 & 0x88) >> 1) | ((val8 & 0x88) >> 2)
				| ((work16 & 0x88) >> 3);
		m_a8 = work16 & 0xff;
		m_signF = ((m_a8 & 0x80) != 0);
		m_zeroF = (m_a8 == 0);
		m_halfcarryF = m_halfcarryTable[idx & 0x7];
		m_parityoverflowF = m_overflowTable[idx >> 4];
		m_addsubtractF = false;
		m_carryF = ((work16 & 0x100) != 0);
		m_3F = ((m_a8 & THREE_MASK) != 0);
		m_5F = ((m_a8 & FIVE_MASK) != 0);
	}

	/**
	 * Add with carry a 8-bit value to the A register and set the appropriate
	 * flags.
	 */
	private static void adc_a(final int val8) {
		final int work16 = m_a8 + val8 + (m_carryF ? 1 : 0);
		final int idx = ((m_a8 & 0x88) >> 1) | ((val8 & 0x88) >> 2)
				| ((work16 & 0x88) >> 3);
		m_a8 = work16 & 0xff;
		m_signF = ((m_a8 & 0x80) != 0);
		m_zeroF = (m_a8 == 0);
		m_halfcarryF = m_halfcarryTable[idx & 0x7];
		m_parityoverflowF = m_overflowTable[idx >> 4];
		m_addsubtractF = false;
		m_carryF = ((work16 & 0x100) != 0);
		m_3F = ((m_a8 & THREE_MASK) != 0);
		m_5F = ((m_a8 & FIVE_MASK) != 0);
	}

	/**
	 * Subtract a 8-bit value from the A register and set the appropriate flags.
	 */
	private static void sub_a(final int val8) {
		final int work16 = m_a8 - val8;
		final int idx = ((m_a8 & 0x88) >> 1) | ((val8 & 0x88) >> 2)
				| ((work16 & 0x88) >> 3);
		m_a8 = work16 & 0xff;
		m_signF = ((m_a8 & 0x80) != 0);
		m_zeroF = (m_a8 == 0);
		m_halfcarryF = m_subhalfcarryTable[idx & 0x7];
		m_parityoverflowF = m_suboverflowTable[idx >> 4];
		m_addsubtractF = true;
		m_carryF = ((work16 & 0x100) != 0);
		m_3F = ((m_a8 & THREE_MASK) != 0);
		m_5F = ((m_a8 & FIVE_MASK) != 0);
	}

	/**
	 * Subtract with carry a 8-bit value from the A register and set the
	 * appropriate flags.
	 */
	private static void sbc_a(final int val8) {
		final int work16 = m_a8 - val8 - (m_carryF ? 1 : 0);
		final int idx = ((m_a8 & 0x88) >> 1) | ((val8 & 0x88) >> 2)
				| ((work16 & 0x88) >> 3);
		m_a8 = work16 & 0xff;
		m_signF = ((m_a8 & 0x80) != 0);
		m_zeroF = (m_a8 == 0);
		m_halfcarryF = m_subhalfcarryTable[idx & 0x7];
		m_parityoverflowF = m_suboverflowTable[idx >> 4];
		m_addsubtractF = true;
		m_carryF = ((work16 & 0x100) != 0);
		m_3F = ((m_a8 & THREE_MASK) != 0);
		m_5F = ((m_a8 & FIVE_MASK) != 0);
	}

	/**
	 * And a 8-bit value to the A register and set the appropriate flags.
	 */
	private static void and_a(final int val8) {
		m_a8 &= val8;
		m_signF = ((m_a8 & 0x80) != 0);
		m_zeroF = (m_a8 == 0);
		m_halfcarryF = true;
		m_parityoverflowF = m_parityTable[m_a8];
		m_addsubtractF = false;
		m_carryF = false;
		m_3F = ((m_a8 & THREE_MASK) != 0);
		m_5F = ((m_a8 & FIVE_MASK) != 0);
	}

	/**
	 * Xor a 8-bit value to the A register and set the appropriate flags.
	 */
	private static void xor_a(final int val8) {
		m_a8 ^= val8;
		m_signF = ((m_a8 & 0x80) != 0);
		m_zeroF = (m_a8 == 0);
		m_halfcarryF = false;
		m_parityoverflowF = m_parityTable[m_a8];
		m_addsubtractF = false;
		m_carryF = false;
		m_3F = ((m_a8 & THREE_MASK) != 0);
		m_5F = ((m_a8 & FIVE_MASK) != 0);
	}

	/**
	 * Or a 8-bit value to the A register and set the appropriate flags.
	 */
	private static void or_a(final int val8) {
		m_a8 |= val8;
		m_signF = ((m_a8 & 0x80) != 0);
		m_zeroF = (m_a8 == 0);
		m_halfcarryF = false;
		m_parityoverflowF = m_parityTable[m_a8];
		m_addsubtractF = false;
		m_carryF = false;
		m_3F = ((m_a8 & THREE_MASK) != 0);
		m_5F = ((m_a8 & FIVE_MASK) != 0);
	}

	/**
	 * Compare a 8-bit value to the A register and set the appropriate flags.
	 */
	private static void cmp_a(final int val8) {
		final int work16 = m_a8 - val8;
		final int idx = ((m_a8 & 0x88) >> 1) | ((val8 & 0x88) >> 2)
				| ((work16 & 0x88) >> 3);
		m_signF = ((work16 & 0x80) != 0);
		m_zeroF = ((work16 & 0xff) == 0);
		m_halfcarryF = m_subhalfcarryTable[idx & 0x7];
		m_parityoverflowF = m_suboverflowTable[idx >> 4];
		m_addsubtractF = true;
		m_carryF = ((work16 & 0x0100) != 0);
		m_3F = ((val8 & THREE_MASK) != 0);
		m_5F = ((val8 & FIVE_MASK) != 0);
	}

	/**
	 * Compare a 8-bit value to the A register and set the appropriate flags,
	 * except PARITY and CARRY.
	 */
	private static void cmp_a_special(final int val8) {
		final int work16 = m_a8 - val8;
		final int idx = ((m_a8 & 0x88) >> 1) | ((val8 & 0x88) >> 2)
				| ((work16 & 0x88) >> 3);
		m_signF = ((work16 & 0x80) != 0);
		m_zeroF = ((work16 & 0xff) == 0);
		m_halfcarryF = m_subhalfcarryTable[idx & 0x7];
		m_addsubtractF = true;
	}

	/**
	 * Test the given bit in a byte and set the appropriate flags.
	 */
	private static void bit(final int bit3, final int reg8) {
		m_zeroF = ((reg8 & (0x01 << bit3)) == 0);
		m_halfcarryF = true;
		m_parityoverflowF = m_zeroF;
		m_addsubtractF = false;
		m_signF = ((reg8 & (0x01 << bit3)) == 0x80);
		m_3F = (bit3 == 3 && !m_zeroF);
		m_5F = (bit3 == 5 && !m_zeroF);
	}

	private static void bit_hl(final int bit3, final int val8) {
		m_zeroF = ((val8 & (0x01 << bit3)) == 0);
		m_halfcarryF = true;
		m_parityoverflowF = m_zeroF;
		m_addsubtractF = false;
		m_signF = ((val8 & (0x01 << bit3)) == 0x80);
		m_3F = ((m_x8 & THREE_MASK) != 0);
		m_5F = ((m_x8 & FIVE_MASK) != 0);
	}

	private static void bit_xx(final int bit3, final int val8) {
		m_zeroF = ((val8 & (0x01 << bit3)) == 0);
		m_halfcarryF = true;
		m_parityoverflowF = m_zeroF;
		m_addsubtractF = false;
		m_signF = ((val8 & (0x01 << bit3)) == 0x80);
		m_3F = ((xx16high8() & THREE_MASK) != 0);
		m_5F = ((xx16high8() & FIVE_MASK) != 0);
	}

	/**
	 * Read an 8-bit value from a 16-bit I/O port and return it.
	 */
	private static int in8(final int port16) {
		final int work8 = BaseIO.in8(port16);
		m_signF = ((work8 & 0x80) != 0);
		m_zeroF = (work8 == 0);
		m_halfcarryF = false;
		m_parityoverflowF = m_parityTable[work8];
		m_addsubtractF = false;
		m_3F = ((work8 & THREE_MASK) != 0);
		m_5F = ((work8 & FIVE_MASK) != 0);
		return work8;
	}

	/**
	 * M1 cycle: fetch an instruction and increment the PC register.
	 * <P>
	 * The R register is a counter that is updated every instruction, where DD,
	 * FD, ED and CB are to be regarded as separate instructions. So shifted
	 * instruction will increase R by two. There's an interesting exception:
	 * doubly-shifted opcodes, the DDCB and FDCB ones, increase R by two too.
	 * LDI increases R by two, LDIR increases it by 2 times BC, as does LDDR
	 * etcetera. The sequence LD R,A/LD A,R increases A by two, except for the
	 * highest bit: this bit of the R register is never changed.
	 */
	private static int mone8() {
		m_r8 = (m_r8 & 0x80) | ((m_r8 + 1) & 0x7f);

		return read8(inc16pc());
	}

	/**
	 * Stop the emulation (asynchronous.)
	 */
	public void stop() {
		m_stop = true;
	}

	/**
	 * Pause the emulation (asynchronous).
	 */
	public void pause() {
		synchronized (this) {
			m_pause = true;
			notifyAll();
		}
	}

	/**
	 * Unpause the emulation (asynchronous).
	 */
	public void unpause() {
		synchronized (this) {
			m_pause = false;
			notifyAll();
		}
	}

	/**
	 * Decode one instruction: call <TT>mone8()</TT> to retrieve the opcode,
	 * decode it and execute it.
	 */
	public void emulate() {
		while (true) {
			BaseSpectrum.update();

			int work16 = 0;
			int work8 = 0;
			//Log.w("emulate", toStr());
			int op8 = mone8();
			switch (op8) {

			/* nop */
			case 0x00:
				m_tstates += 4;
				break;

			/* ld bc,NN */
			case 0x01:
				m_tstates += 10;
				bc16(read16(m_pc16));
				m_pc16 = incinc16(m_pc16);
				break;

			/* ld (bc),a */
			case 0x02:
				m_tstates += 7;
				write8(bc16(), m_a8);
				break;

			/* inc bc */
			case 0x03:
				m_tstates += 6;
				inc16bc();
				break;

			/* inc b */
			case 0x04:
				m_tstates += 4;
				m_b8 = inc8(m_b8);
				break;

			/* dec b */
			case 0x05:
				m_tstates += 4;
				m_b8 = dec8(m_b8);
				break;

			/* ld b,N */
			case 0x06:
				m_tstates += 7;
				m_b8 = read8(inc16pc());
				break;

			/* rlca */
			case 0x07:
				m_tstates += 4;
				m_carryF = ((m_a8 & 0x80) != 0);
				m_a8 = (((m_a8 << 1) | (m_carryF ? 1 : 0)) & 0xff);
				m_halfcarryF = false;
				m_addsubtractF = false;
				m_3F = ((m_a8 & THREE_MASK) != 0);
				m_5F = ((m_a8 & FIVE_MASK) != 0);
				break;

			/* ex af,af' */
			case 0x08:
				m_tstates += 4;
				storeFlags();
				work16 = af16();
				af16(m_af16alt);
				m_af16alt = work16;
				retrieveFlags();
				break;

			/* add hl,bc */
			case 0x09:
				m_tstates += 11;
				add_hl(bc16());
				break;

			/* ld a,(bc) */
			case 0x0A:
				m_tstates += 7;
				m_a8 = read8(bc16());
				break;

			/* dec bc */
			case 0x0B:
				m_tstates += 6;
				dec16bc();
				break;

			/* inc c */
			case 0x0C:
				m_tstates += 4;
				m_c8 = inc8(m_c8);
				break;

			/* dec c */
			case 0x0D:
				m_tstates += 4;
				m_c8 = dec8(m_c8);
				break;

			/* ld c,N */
			case 0x0E:
				m_tstates += 7;
				m_c8 = read8(inc16pc());
				break;

			/* rrca */
			case 0x0F:
				m_tstates += 4;
				m_carryF = ((m_a8 & 0x01) != 0);
				m_a8 = (m_a8 >> 1) | ((m_carryF ? 1 : 0) << 7);
				m_halfcarryF = false;
				m_addsubtractF = false;
				m_3F = ((m_a8 & THREE_MASK) != 0);
				m_5F = ((m_a8 & FIVE_MASK) != 0);
				break;

			/* djnz D */
			case 0x10:
				m_b8 = ((m_b8 - 1) & 0xff);
				if (m_b8 != 0) {
					m_tstates += 13;
					m_pc16 = add16(m_pc16, (byte) read8(m_pc16) + 1);
				} else {
					m_tstates += 8;
					inc16pc();
				}
				break;

			/* ld de,NN */
			case 0x11:
				m_tstates += 10;
				de16(read16(m_pc16));
				m_pc16 = incinc16(m_pc16);
				break;

			/* ld (de),a */
			case 0x12:
				m_tstates += 7;
				write8(de16(), m_a8);
				break;

			/* inc de */
			case 0x13:
				m_tstates += 6;
				inc16de();
				break;

			/* inc d */
			case 0x14:
				m_tstates += 4;
				m_d8 = inc8(m_d8);
				break;

			/* dec d */
			case 0x15:
				m_tstates += 4;
				m_d8 = dec8(m_d8);
				break;

			/* ld d,N */
			case 0x16:
				m_tstates += 7;
				m_d8 = read8(inc16pc());
				break;

			/* rla */
			case 0x17:
				m_tstates += 4;
				work8 = (m_carryF ? 1 : 0);
				m_carryF = ((m_a8 & 0x80) != 0);
				m_a8 = (((m_a8 << 1) | work8) & 0xff);
				m_halfcarryF = false;
				m_addsubtractF = false;
				m_3F = ((m_a8 & THREE_MASK) != 0);
				m_5F = ((m_a8 & FIVE_MASK) != 0);
				break;

			/* jr D */
			case 0x18:
				m_tstates += 12;
				m_pc16 = add16(m_pc16, (byte) read8(m_pc16) + 1);
				m_x8 = m_pc16 >> 8;
				break;

			/* add hl,de */
			case 0x19:
				m_tstates += 11;
				add_hl(de16());
				break;

			/* ld a,(de) */
			case 0x1A:
				m_tstates += 7;
				m_a8 = read8(de16());
				break;

			/* dec de */
			case 0x1B:
				m_tstates += 6;
				dec16de();
				break;

			/* inc e */
			case 0x1C:
				m_tstates += 4;
				m_e8 = inc8(m_e8);
				break;

			/* dec e */
			case 0x1D:
				m_tstates += 4;
				m_e8 = dec8(m_e8);
				break;

			/* ld e,N */
			case 0x1E:
				m_tstates += 7;
				m_e8 = read8(inc16pc());
				break;

			/* rra */
			case 0x1F:
				m_tstates += 4;
				work8 = (m_carryF ? 1 : 0);
				m_carryF = ((m_a8 & 0x01) != 0);
				m_a8 = (m_a8 >> 1) | (work8 << 7);
				m_halfcarryF = false;
				m_addsubtractF = false;
				m_3F = ((m_a8 & THREE_MASK) != 0);
				m_5F = ((m_a8 & FIVE_MASK) != 0);
				break;

			/* jr nz,D */
			case 0x20:
				if (!m_zeroF) {
					m_tstates += 12;
					m_pc16 = add16(m_pc16, (byte) read8(m_pc16) + 1);
				} else {
					m_tstates += 7;
					inc16pc();
				}
				break;

			/* ld hl,NN */
			case 0x21:
				m_tstates += 10;
				hl16(read16(m_pc16));
				m_pc16 = incinc16(m_pc16);
				break;

			/* ld (NN),hl */
			case 0x22:
				m_tstates += 16;
				write16(read16(m_pc16), hl16());
				m_pc16 = incinc16(m_pc16);
				break;

			/* inc hl */
			case 0x23:
				m_tstates += 6;
				inc16hl();
				break;

			/* inc h */
			case 0x24:
				m_tstates += 4;
				m_h8 = inc8(m_h8);
				break;

			/* dec h */
			case 0x25:
				m_tstates += 4;
				m_h8 = dec8(m_h8);
				break;

			/* ld h,N */
			case 0x26:
				m_tstates += 7;
				m_h8 = read8(inc16pc());
				break;

			/* daa */
			case 0x27:
				m_tstates += 4;
				boolean carry = m_carryF;
				boolean addsubtract = m_addsubtractF;
				if (!addsubtract) {
					work8 = 0;
					if (m_halfcarryF || (m_a8 & 0x0f) > 9) {
						work8 = 0x06;
					}
					if (m_carryF || (m_a8 >> 4) > 9
							|| ((m_a8 >> 4) >= 9 && (m_a8 & 0x0f) > 9)) {
						work8 |= 0x60;
						carry = true;
					}
				} else {
					if (m_carryF) {
						work8 = m_halfcarryF ? 0x9a : 0xa0;
					} else {
						work8 = m_halfcarryF ? 0xfa : 0x00;
					}
				}
				add_a(work8);
				m_addsubtractF = addsubtract;
				m_parityoverflowF = m_parityTable[m_a8];
				m_carryF = carry;
				break;

			/* jr z,D */
			case 0x28:
				if (m_zeroF) {
					m_tstates += 12;
					m_pc16 = add16(m_pc16, (byte) read8(m_pc16) + 1);
				} else {
					m_tstates += 7;
					inc16pc();
				}
				break;

			/* add hl,hl */
			case 0x29:
				m_tstates += 11;
				add_hl(hl16());
				break;

			/* ld hl,(NN) */
			case 0x2A:
				m_tstates += 16;
				hl16(read16(read16(m_pc16)));
				m_pc16 = incinc16(m_pc16);
				break;

			/* dec hl */
			case 0x2B:
				m_tstates += 6;
				dec16hl();
				break;

			/* inc l */
			case 0x2C:
				m_tstates += 4;
				m_l8 = inc8(m_l8);
				break;

			/* dec l */
			case 0x2D:
				m_tstates += 4;
				m_l8 = dec8(m_l8);
				break;

			/* ld l,N */
			case 0x2E:
				m_tstates += 7;
				m_l8 = read8(inc16pc());
				break;

			/* cpl */
			case 0x2F:
				m_tstates += 4;
				m_a8 ^= 0xff;
				m_halfcarryF = true;
				m_addsubtractF = true;
				m_3F = ((m_a8 & THREE_MASK) != 0);
				m_5F = ((m_a8 & FIVE_MASK) != 0);
				break;

			/* jr nc,D */
			case 0x30:
				if (!m_carryF) {
					m_tstates += 12;
					m_pc16 = add16(m_pc16, (byte) read8(m_pc16) + 1);
				} else {
					m_tstates += 7;
					inc16pc();
				}
				break;

			/* ld sp,NN */
			case 0x31:
				m_tstates += 10;
				m_sp16 = read16(m_pc16);
				m_pc16 = incinc16(m_pc16);
				break;

			/* ld (NN),a */
			case 0x32:
				m_tstates += 13;
				write8(read16(m_pc16), m_a8);
				m_pc16 = incinc16(m_pc16);
				break;

			/* inc sp */
			case 0x33:
				m_tstates += 6;
				inc16sp();
				break;

			/* inc (hl) */
			case 0x34:
				m_tstates += 11;
				work8 = read8(hl16());
				work8 = inc8(work8);
				write8(hl16(), work8);
				break;

			/* dec (hl) */
			case 0x35:
				m_tstates += 11;
				work8 = read8(hl16());
				work8 = dec8(work8);
				write8(hl16(), work8);
				break;

			/* ld (hl),N */
			case 0x36:
				m_tstates += 10;
				write8(hl16(), read8(inc16pc()));
				break;

			/* scf */
			case 0x37:
				m_tstates += 4;
				m_halfcarryF = false;
				m_addsubtractF = false;
				m_carryF = true;
				m_3F = ((m_a8 & THREE_MASK) != 0);
				m_5F = ((m_a8 & FIVE_MASK) != 0);
				break;

			/* jr c,D */
			case 0x38:
				if (m_carryF) {
					m_tstates += 12;
					m_pc16 = add16(m_pc16, (byte) read8(m_pc16) + 1);
				} else {
					m_tstates += 7;
					inc16pc();
				}
				break;

			/* add hl,sp */
			case 0x39:
				m_tstates += 11;
				add_hl(m_sp16);
				break;

			/* ld a,(NN) */
			case 0x3A:
				m_tstates += 13;
				m_a8 = read8(read16(m_pc16));
				m_pc16 = incinc16(m_pc16);
				break;

			/* dec sp */
			case 0x3B:
				m_tstates += 6;
				dec16sp();
				break;

			/* inc a */
			case 0x3C:
				m_tstates += 4;
				m_a8 = inc8(m_a8);
				break;

			/* dec a */
			case 0x3D:
				m_tstates += 4;
				m_a8 = dec8(m_a8);
				break;

			/* ld a,N */
			case 0x3E:
				m_tstates += 7;
				m_a8 = read8(inc16pc());
				break;

			/* ccf */
			case 0x3F:
				m_tstates += 4;
				m_halfcarryF = m_carryF;
				m_addsubtractF = false;
				m_carryF = !m_carryF;
				m_3F = ((m_a8 & THREE_MASK) != 0);
				m_5F = ((m_a8 & FIVE_MASK) != 0);
				break;

			/* ld b,b */
			case 0x40:
				m_tstates += 4;
				break;

			/* ld b,c */
			case 0x41:
				m_tstates += 4;
				m_b8 = m_c8;
				break;

			/* ld b,d */
			case 0x42:
				m_tstates += 4;
				m_b8 = m_d8;
				break;

			/* ld b,e */
			case 0x43:
				m_tstates += 4;
				m_b8 = m_e8;
				break;

			/* ld b,h */
			case 0x44:
				m_tstates += 4;
				m_b8 = m_h8;
				break;

			/* ld b,l */
			case 0x45:
				m_tstates += 4;
				m_b8 = m_l8;
				break;

			/* ld b,(hl) */
			case 0x46:
				m_tstates += 7;
				m_b8 = read8(hl16());
				break;

			/* ld b,a */
			case 0x47:
				m_tstates += 4;
				m_b8 = m_a8;
				break;

			/* ld c,b */
			case 0x48:
				m_tstates += 4;
				m_c8 = m_b8;
				break;

			/* ld c,c */
			case 0x49:
				m_tstates += 4;
				break;

			/* ld c,d */
			case 0x4A:
				m_tstates += 4;
				m_c8 = m_d8;
				break;

			/* ld c,e */
			case 0x4B:
				m_tstates += 4;
				m_c8 = m_e8;
				break;

			/* ld c,h */
			case 0x4C:
				m_tstates += 4;
				m_c8 = m_h8;
				break;

			/* ld c,l */
			case 0x4D:
				m_tstates += 4;
				m_c8 = m_l8;
				break;

			/* ld c,(hl) */
			case 0x4E:
				m_tstates += 7;
				m_c8 = read8(hl16());
				break;

			/* ld c,a */
			case 0x4F:
				m_tstates += 4;
				m_c8 = m_a8;
				break;

			/* ld d,b */
			case 0x50:
				m_tstates += 4;
				m_d8 = m_b8;
				break;

			/* ld d,c */
			case 0x51:
				m_tstates += 4;
				m_d8 = m_c8;
				break;

			/* ld d,d */
			case 0x52:
				m_tstates += 4;
				break;

			/* ld d,e */
			case 0x53:
				m_tstates += 4;
				m_d8 = m_e8;
				break;

			/* ld d,h */
			case 0x54:
				m_tstates += 4;
				m_d8 = m_h8;
				break;

			/* ld d,l */
			case 0x55:
				m_tstates += 4;
				m_d8 = m_l8;
				break;

			/* ld d,(hl) */
			case 0x56:
				m_tstates += 7;
				m_d8 = read8(hl16());
				break;

			/* ld d,a */
			case 0x57:
				m_tstates += 4;
				m_d8 = m_a8;
				break;

			/* ld e,b */
			case 0x58:
				m_tstates += 4;
				m_e8 = m_b8;
				break;

			/* ld e,c */
			case 0x59:
				m_tstates += 4;
				m_e8 = m_c8;
				break;

			/* ld e,d */
			case 0x5A:
				m_tstates += 4;
				m_e8 = m_d8;
				break;

			/* ld e,e */
			case 0x5B:
				m_tstates += 4;
				break;

			/* ld e,h */
			case 0x5C:
				m_tstates += 4;
				m_e8 = m_h8;
				break;

			/* ld e,l */
			case 0x5D:
				m_tstates += 4;
				m_e8 = m_l8;
				break;

			/* ld e,(hl) */
			case 0x5E:
				m_tstates += 7;
				m_e8 = read8(hl16());
				break;

			/* ld e,a */
			case 0x5F:
				m_tstates += 4;
				m_e8 = m_a8;
				break;

			/* ld h,b */
			case 0x60:
				m_tstates += 4;
				m_h8 = m_b8;
				break;

			/* ld h,c */
			case 0x61:
				m_tstates += 4;
				m_h8 = m_c8;
				break;

			/* ld h,d */
			case 0x62:
				m_tstates += 4;
				m_h8 = m_d8;
				break;

			/* ld h,e */
			case 0x63:
				m_tstates += 4;
				m_h8 = m_e8;
				break;

			/* ld h,h */
			case 0x64:
				m_tstates += 4;
				break;

			/* ld h,l */
			case 0x65:
				m_tstates += 4;
				m_h8 = m_l8;
				break;

			/* ld h,(hl) */
			case 0x66:
				m_tstates += 7;
				m_h8 = read8(hl16());
				break;

			/* ld h,a */
			case 0x67:
				m_tstates += 4;
				m_h8 = m_a8;
				break;

			/* ld l,b */
			case 0x68:
				m_tstates += 4;
				m_l8 = m_b8;
				break;

			/* ld l,c */
			case 0x69:
				m_tstates += 4;
				m_l8 = m_c8;
				break;

			/* ld l,d */
			case 0x6A:
				m_tstates += 4;
				m_l8 = m_d8;
				break;

			/* ld l,e */
			case 0x6B:
				m_tstates += 4;
				m_l8 = m_e8;
				break;

			/* ld l,h */
			case 0x6C:
				m_tstates += 4;
				m_l8 = m_h8;
				break;

			/* ld l,l */
			case 0x6D:
				m_tstates += 4;
				break;

			/* ld l,(hl) */
			case 0x6E:
				m_tstates += 7;
				m_l8 = read8(hl16());
				break;

			/* ld l,a */
			case 0x6F:
				m_tstates += 4;
				m_l8 = m_a8;
				break;

			/* ld (hl),b */
			case 0x70:
				m_tstates += 7;
				write8(hl16(), m_b8);
				break;

			/* ld (hl),c */
			case 0x71:
				m_tstates += 7;
				write8(hl16(), m_c8);
				break;

			/* ld (hl),d */
			case 0x72:
				m_tstates += 7;
				write8(hl16(), m_d8);
				break;

			/* ld (hl),e */
			case 0x73:
				m_tstates += 7;
				write8(hl16(), m_e8);
				break;

			/* ld (hl),h */
			case 0x74:
				m_tstates += 7;
				write8(hl16(), m_h8);
				break;

			/* ld (hl),l */
			case 0x75:
				m_tstates += 7;
				write8(hl16(), m_l8);
				break;

			/* halt */
			case 0x76:
				m_tstates += 4;
				dec16pc();
				break;

			/* ld (hl),a */
			case 0x77:
				m_tstates += 7;
				write8(hl16(), m_a8);
				break;

			/* ld a,b */
			case 0x78:
				m_tstates += 4;
				m_a8 = m_b8;
				break;

			/* ld a,c */
			case 0x79:
				m_tstates += 4;
				m_a8 = m_c8;
				break;

			/* ld a,d */
			case 0x7A:
				m_tstates += 4;
				m_a8 = m_d8;
				break;

			/* ld a,e */
			case 0x7B:
				m_tstates += 4;
				m_a8 = m_e8;
				break;

			/* ld a,h */
			case 0x7C:
				m_tstates += 4;
				m_a8 = m_h8;
				break;

			/* ld a,l */
			case 0x7D:
				m_tstates += 4;
				m_a8 = m_l8;
				break;

			/* ld a,(hl) */
			case 0x7E:
				m_tstates += 7;
				m_a8 = read8(hl16());
				break;

			/* ld a,a */
			case 0x7F:
				m_tstates += 4;
				break;

			/* add a,b */
			case 0x80:
				m_tstates += 4;
				add_a(m_b8);
				break;

			/* add a,c */
			case 0x81:
				m_tstates += 4;
				add_a(m_c8);
				break;

			/* add a,d */
			case 0x82:
				m_tstates += 4;
				add_a(m_d8);
				break;

			/* add a,e */
			case 0x83:
				m_tstates += 4;
				add_a(m_e8);
				break;

			/* add a,h */
			case 0x84:
				m_tstates += 4;
				add_a(m_h8);
				break;

			/* add a,l */
			case 0x85:
				m_tstates += 4;
				add_a(m_l8);
				break;

			/* add a,(hl) */
			case 0x86:
				m_tstates += 7;
				work8 = read8(hl16());
				add_a(work8);
				break;

			/* add a,a */
			case 0x87:
				m_tstates += 4;
				add_a(m_a8);
				break;

			/* adc a,b */
			case 0x88:
				m_tstates += 4;
				adc_a(m_b8);
				break;

			/* adc a,c */
			case 0x89:
				m_tstates += 4;
				adc_a(m_c8);
				break;

			/* adc a,d */
			case 0x8A:
				m_tstates += 4;
				adc_a(m_d8);
				break;

			/* adc a,e */
			case 0x8B:
				m_tstates += 4;
				adc_a(m_e8);
				break;

			/* adc a,h */
			case 0x8C:
				m_tstates += 4;
				adc_a(m_h8);
				break;

			/* adc a,l */
			case 0x8D:
				m_tstates += 4;
				adc_a(m_l8);
				break;

			/* adc a,(hl) */
			case 0x8E:
				m_tstates += 7;
				work8 = read8(hl16());
				adc_a(work8);
				break;

			/* adc a,a */
			case 0x8F:
				m_tstates += 4;
				adc_a(m_a8);
				break;

			/* sub b */
			case 0x90:
				m_tstates += 4;
				sub_a(m_b8);
				break;

			/* sub c */
			case 0x91:
				m_tstates += 4;
				sub_a(m_c8);
				break;

			/* sub d */
			case 0x92:
				m_tstates += 4;
				sub_a(m_d8);
				break;

			/* sub e */
			case 0x93:
				m_tstates += 4;
				sub_a(m_e8);
				break;

			/* sub h */
			case 0x94:
				m_tstates += 4;
				sub_a(m_h8);
				break;

			/* sub l */
			case 0x95:
				m_tstates += 4;
				sub_a(m_l8);
				break;

			/* sub (hl) */
			case 0x96:
				m_tstates += 7;
				work8 = read8(hl16());
				sub_a(work8);
				break;

			/* sub a */
			case 0x97:
				m_tstates += 4;
				sub_a(m_a8);
				break;

			/* sbc a,b */
			case 0x98:
				m_tstates += 4;
				sbc_a(m_b8);
				break;

			/* sbc a,c */
			case 0x99:
				m_tstates += 4;
				sbc_a(m_c8);
				break;

			/* sbc a,d */
			case 0x9A:
				m_tstates += 4;
				sbc_a(m_d8);
				break;

			/* sbc a,e */
			case 0x9B:
				m_tstates += 4;
				sbc_a(m_e8);
				break;

			/* sbc a,h */
			case 0x9C:
				m_tstates += 4;
				sbc_a(m_h8);
				break;

			/* sbc a,l */
			case 0x9D:
				m_tstates += 4;
				sbc_a(m_l8);
				break;

			/* sbc a,(hl) */
			case 0x9E:
				m_tstates += 7;
				work8 = read8(hl16());
				sbc_a(work8);
				break;

			/* sbc a,a */
			case 0x9F:
				m_tstates += 4;
				sbc_a(m_a8);
				break;

			/* and b */
			case 0xA0:
				m_tstates += 4;
				and_a(m_b8);
				break;

			/* and c */
			case 0xA1:
				m_tstates += 4;
				and_a(m_c8);
				break;

			/* and d */
			case 0xA2:
				m_tstates += 4;
				and_a(m_d8);
				break;

			/* and e */
			case 0xA3:
				m_tstates += 4;
				and_a(m_e8);
				break;

			/* and h */
			case 0xA4:
				m_tstates += 4;
				and_a(m_h8);
				break;

			/* and l */
			case 0xA5:
				m_tstates += 4;
				and_a(m_l8);
				break;

			/* and (hl) */
			case 0xA6:
				m_tstates += 7;
				work8 = read8(hl16());
				and_a(work8);
				break;

			/* and a */
			case 0xA7:
				m_tstates += 4;
				and_a(m_a8);
				break;

			/* xor b */
			case 0xA8:
				m_tstates += 4;
				xor_a(m_b8);
				break;

			/* xor c */
			case 0xA9:
				m_tstates += 4;
				xor_a(m_c8);
				break;

			/* xor d */
			case 0xAA:
				m_tstates += 4;
				xor_a(m_d8);
				break;

			/* xor e */
			case 0xAB:
				m_tstates += 4;
				xor_a(m_e8);
				break;

			/* xor h */
			case 0xAC:
				m_tstates += 4;
				xor_a(m_h8);
				break;

			/* xor l */
			case 0xAD:
				m_tstates += 4;
				xor_a(m_l8);
				break;

			/* xor (hl) */
			case 0xAE:
				m_tstates += 7;
				work8 = read8(hl16());
				xor_a(work8);
				break;

			/* xor a */
			case 0xAF:
				m_tstates += 4;
				xor_a(m_a8);
				break;

			/* or b */
			case 0xB0:
				m_tstates += 4;
				or_a(m_b8);
				break;

			/* or c */
			case 0xB1:
				m_tstates += 4;
				or_a(m_c8);
				break;

			/* or d */
			case 0xB2:
				m_tstates += 4;
				or_a(m_d8);
				break;

			/* or e */
			case 0xB3:
				m_tstates += 4;
				or_a(m_e8);
				break;

			/* or h */
			case 0xB4:
				m_tstates += 4;
				or_a(m_h8);
				break;

			/* or l */
			case 0xB5:
				m_tstates += 4;
				or_a(m_l8);
				break;

			/* or (hl) */
			case 0xB6:
				m_tstates += 7;
				work8 = read8(hl16());
				or_a(work8);
				break;

			/* or a */
			case 0xB7:
				m_tstates += 4;
				or_a(m_a8);
				break;

			/* cp b */
			case 0xB8:
				m_tstates += 4;
				cmp_a(m_b8);
				break;

			/* cp c */
			case 0xB9:
				m_tstates += 4;
				cmp_a(m_c8);
				break;

			/* cp d */
			case 0xBA:
				m_tstates += 4;
				cmp_a(m_d8);
				break;

			/* cp e */
			case 0xBB:
				m_tstates += 4;
				cmp_a(m_e8);
				break;

			/* cp h */
			case 0xBC:
				m_tstates += 4;
				cmp_a(m_h8);
				break;

			/* cp l */
			case 0xBD:
				m_tstates += 4;
				cmp_a(m_l8);
				break;

			/* cp (hl) */
			case 0xBE:
				m_tstates += 7;
				work8 = read8(hl16());
				cmp_a(work8);
				break;

			/* cp a */
			case 0xBF:
				m_tstates += 4;
				cmp_a(m_a8);
				break;

			/* ret nz */
			case 0xC0:
				m_tstates += 5;
				if (!m_zeroF) {
					m_tstates += 6;
					m_pc16 = pop16();
				}
				break;

			/* pop bc */
			case 0xC1:
				m_tstates += 10;
				bc16(pop16());
				break;

			/* jp nz,NN */
			case 0xC2:
				m_tstates += 10;
				if (!m_zeroF) {
					m_pc16 = read16(m_pc16);
				} else {
					m_pc16 = incinc16(m_pc16);
				}
				break;

			/* jp NN */
			case 0xC3:
				m_tstates += 10;
				m_pc16 = read16(m_pc16);
				break;

			/* call nz,NN */
			case 0xC4:
				if (!m_zeroF) {
					m_tstates += 17;
					push(incinc16(m_pc16));
					m_pc16 = read16(m_pc16);
				} else {
					m_tstates += 10;
					m_pc16 = incinc16(m_pc16);
				}
				break;

			/* push bc */
			case 0xC5:
				m_tstates += 11;
				push(bc16());
				break;

			/* add a,N */
			case 0xC6:
				m_tstates += 7;
				work8 = read8(inc16pc());
				add_a(work8);
				break;

			/* rst 0x00 */
			case 0xC7:
				m_tstates += 11;
				push(m_pc16);
				m_pc16 = 0x0;
				break;

			/* ret z */
			case 0xC8:
				m_tstates += 5;
				if (m_zeroF) {
					m_tstates += 6;
					m_pc16 = pop16();
				}
				break;

			/* ret */
			case 0xC9:
				m_tstates += 10;
				m_pc16 = pop16();
				break;

			/* jp z,NN */
			case 0xCA:
				m_tstates += 10;
				if (m_zeroF) {
					m_pc16 = read16(m_pc16);
				} else {
					m_pc16 = incinc16(m_pc16);
				}
				break;

			/**
			 * 0xCB instructions subset
			 */
			case 0xCB:
				op8 = mone8();

				decodeCB(op8);
				break;

			/* call z,NN */
			case 0xCC:
				if (m_zeroF) {
					m_tstates += 17;
					push(incinc16(m_pc16));
					m_pc16 = read16(m_pc16);
				} else {
					m_tstates += 10;
					m_pc16 = incinc16(m_pc16);
				}
				break;

			/* call NN */
			case 0xCD:
				m_tstates += 17;
				push(incinc16(m_pc16));
				m_pc16 = read16(m_pc16);
				break;

			/* adc a,N */
			case 0xCE:
				m_tstates += 7;
				work8 = read8(inc16pc());
				adc_a(work8);
				break;

			/* rst 0x08 */
			case 0xCF:
				m_tstates += 11;
				push(m_pc16);
				m_pc16 = 0x8;
				break;

			/* ret nc */
			case 0xD0:
				m_tstates += 5;
				if (!m_carryF) {
					m_tstates += 6;
					m_pc16 = pop16();
				}
				break;

			/* pop de */
			case 0xD1:
				m_tstates += 10;
				de16(pop16());
				break;

			/* jp nc,NN */
			case 0xD2:
				m_tstates += 10;
				if (!m_carryF) {
					m_pc16 = read16(m_pc16);
				} else {
					m_pc16 = incinc16(m_pc16);
				}
				break;

			/* out (N),a */
			case 0xD3:
				m_tstates += 11;
				BaseIO.out((m_a8 << 8) | read8(inc16pc()), m_a8);
				break;

			/* call nc,NN */
			case 0xD4:
				if (!m_carryF) {
					m_tstates += 17;
					push(incinc16(m_pc16));
					m_pc16 = read16(m_pc16);
				} else {
					m_tstates += 10;
					m_pc16 = incinc16(m_pc16);
				}
				break;

			/* push de */
			case 0xD5:
				m_tstates += 11;
				push(de16());
				break;

			/* sub N */
			case 0xD6:
				m_tstates += 7;
				work8 = read8(inc16pc());
				sub_a(work8);
				break;

			/* rst 0x10 */
			case 0xD7:
				m_tstates += 11;
				push(m_pc16);
				m_pc16 = 0x10;
				break;

			/* ret c */
			case 0xD8:
				m_tstates += 5;
				if (m_carryF) {
					m_tstates += 6;
					m_pc16 = pop16();
				}
				break;

			/* exx */
			case 0xD9:
				m_tstates += 4;
				work16 = bc16();
				bc16(m_bc16alt);
				m_bc16alt = work16;
				work16 = de16();
				de16(m_de16alt);
				m_de16alt = work16;
				work16 = hl16();
				hl16(m_hl16alt);
				m_hl16alt = work16;
				break;

			/* jp c,NN */
			case 0xDA:
				m_tstates += 10;
				if (m_carryF) {
					m_pc16 = read16(m_pc16);
				} else {
					m_pc16 = incinc16(m_pc16);
				}
				break;

			/* in a,N */
			case 0xDB:
				m_tstates += 11;
				m_a8 = BaseIO.in8((m_a8 << 8) | read8(inc16pc()));
				break;

			/* call c,NN */
			case 0xDC:
				if (m_carryF) {
					m_tstates += 17;
					push(incinc16(m_pc16));
					m_pc16 = read16(m_pc16);
				} else {
					m_tstates += 10;
					m_pc16 = incinc16(m_pc16);
				}
				break;

			/**
			 * IX register operations
			 */
			case 0xDD:
				op8 = mone8();

				m_xx16 = m_ix16;
				decodeXX(op8);
				m_ix16 = m_xx16;
				break;

			/* sbc a,N */
			case 0xDE:
				m_tstates += 7;
				work8 = read8(inc16pc());
				sbc_a(work8);
				break;

			/* rst 0x18 */
			case 0xDF:
				m_tstates += 11;
				push(m_pc16);
				m_pc16 = 0x18;
				break;

			/* ret po */
			case 0xE0:
				m_tstates += 5;
				if (!m_parityoverflowF) {
					m_tstates += 6;
					m_pc16 = pop16();
				}
				break;

			/* pop hl */
			case 0xE1:
				m_tstates += 10;
				hl16(pop16());
				break;

			/* jp po,NN */
			case 0xE2:
				m_tstates += 10;
				if (!m_parityoverflowF) {
					m_pc16 = read16(m_pc16);
				} else {
					m_pc16 = incinc16(m_pc16);
				}
				break;

			/* ex (sp),hl */
			case 0xE3:
				m_tstates += 19;
				work16 = read16(m_sp16);
				write16(m_sp16, hl16());
				hl16(work16);
				break;

			/* call po,NN */
			case 0xE4:
				if (!m_parityoverflowF) {
					m_tstates += 17;
					push(incinc16(m_pc16));
					m_pc16 = read16(m_pc16);
				} else {
					m_tstates += 10;
					m_pc16 = incinc16(m_pc16);
				}
				break;

			/* push hl */
			case 0xE5:
				m_tstates += 11;
				push(hl16());
				break;

			/* and N */
			case 0xE6:
				m_tstates += 7;
				work8 = read8(inc16pc());
				and_a(work8);
				break;

			/* rst 0x20 */
			case 0xE7:
				m_tstates += 11;
				push(m_pc16);
				m_pc16 = 0x20;
				break;

			/* ret pe */
			case 0xE8:
				m_tstates += 5;
				if (m_parityoverflowF) {
					m_tstates += 6;
					m_pc16 = pop16();
				}
				break;

			/* jp (hl) */
			case 0xE9:
				m_tstates += 4;
				m_pc16 = hl16();
				break;

			/* jp pe,NN */
			case 0xEA:
				m_tstates += 10;
				if (m_parityoverflowF) {
					m_pc16 = read16(m_pc16);
				} else {
					m_pc16 = incinc16(m_pc16);
				}
				break;

			/* ex de,hl */
			case 0xEB:
				m_tstates += 4;
				work16 = de16();
				de16(hl16());
				hl16(work16);
				break;

			/* call pe,NN */
			case 0xEC:
				if (m_parityoverflowF) {
					m_tstates += 17;
					push(incinc16(m_pc16));
					m_pc16 = read16(m_pc16);
				} else {
					m_tstates += 10;
					m_pc16 = incinc16(m_pc16);
				}
				break;

			/**
			 * 0xED instructions subset
			 */
			case 0xED:
				op8 = mone8();

				decodeED(op8);
				break;

			/* xor N */
			case 0xEE:
				m_tstates += 7;
				work8 = read8(inc16pc());
				xor_a(work8);
				break;

			/* rst 0x28 */
			case 0xEF:
				m_tstates += 11;
				push(m_pc16);
				m_pc16 = 0x28;
				break;

			/* ret p */
			case 0xF0:
				m_tstates += 5;
				if (!m_signF) {
					m_tstates += 6;
					m_pc16 = pop16();
				}
				break;

			/* pop af */
			case 0xF1:
				m_tstates += 10;
				af16(pop16());
				retrieveFlags();
				break;

			/* jp p,NN */
			case 0xF2:
				m_tstates += 10;
				if (!m_signF) {
					m_pc16 = read16(m_pc16);
				} else {
					m_pc16 = incinc16(m_pc16);
				}
				break;

			/* di */
			case 0xF3:
				m_tstates += 4;
				m_iff1a = 0;
				m_iff1b = 0;
				break;

			/* call p,NN */
			case 0xF4:
				if (!m_signF) {
					m_tstates += 17;
					push(incinc16(m_pc16));
					m_pc16 = read16(m_pc16);
				} else {
					m_tstates += 10;
					m_pc16 = incinc16(m_pc16);
				}
				break;

			/* push af */
			case 0xF5:
				m_tstates += 11;
				storeFlags();
				push(af16());
				break;

			/* or N */
			case 0xF6:
				m_tstates += 7;
				work8 = read8(inc16pc());
				or_a(work8);
				break;

			/* rst 0x30 */
			case 0xF7:
				m_tstates += 11;
				push(m_pc16);
				m_pc16 = 0x30;
				break;

			/* ret m */
			case 0xF8:
				m_tstates += 5;
				if (m_signF) {
					m_tstates += 6;
					m_pc16 = pop16();
				}
				break;

			/* ld sp,hl */
			case 0xF9:
				m_tstates += 6;
				m_sp16 = hl16();
				break;

			/* jp m,NN */
			case 0xFA:
				m_tstates += 10;
				if (m_signF) {
					m_pc16 = read16(m_pc16);
				} else {
					m_pc16 = incinc16(m_pc16);
				}
				break;

			/* ei */
			case 0xFB:
				m_tstates += 4;
				m_iff1a = 1;
				m_iff1b = 1;
				break;

			/* call m,NN */
			case 0xFC:
				if (m_signF) {
					m_tstates += 17;
					push(incinc16(m_pc16));
					m_pc16 = read16(m_pc16);
				} else {
					m_tstates += 10;
					m_pc16 = incinc16(m_pc16);
				}
				break;

			/**
			 * IY register operations
			 */
			case 0xFD:
				op8 = mone8();

				m_xx16 = m_iy16;
				decodeXX(op8);
				m_iy16 = m_xx16;
				break;

			/* cp N */
			case 0xFE:
				m_tstates += 7;
				work8 = read8(inc16pc());
				cmp_a(work8);
				break;

			/* rst 0x38 */
			case 0xFF:
				m_tstates += 11;
				push(m_pc16);
				m_pc16 = 0x38;
				break;
			}

			if (m_stop) {
				break;
			}

			synchronized (this) {
				while (m_pause) {
					try {
						wait();
					} catch (InterruptedException ie) {
						Log.e(this.getClass().getName(), ie.toString());
					}
				}
			}
		}
	}

	/**
	 * Decode the instructions whose first opcode is 0xCB.
	 */
	private static void decodeCB(final int op8) {
		int work8 = 0;

		switch (op8) {
		/* rlc b */
		case 0x00:
			m_tstates += 8;
			m_b8 = rlc8(m_b8);
			break;

		/* rlc c */
		case 0x01:
			m_tstates += 8;
			m_c8 = rlc8(m_c8);
			break;

		/* rlc d */
		case 0x02:
			m_tstates += 8;
			m_d8 = rlc8(m_d8);
			break;

		/* rlc e */
		case 0x03:
			m_tstates += 8;
			m_e8 = rlc8(m_e8);
			break;

		/* rlc h */
		case 0x04:
			m_tstates += 8;
			m_h8 = rlc8(m_h8);
			break;

		/* rlc l */
		case 0x05:
			m_tstates += 8;
			m_l8 = rlc8(m_l8);
			break;

		/* rlc (hl) */
		case 0x06:
			m_tstates += 15;
			work8 = read8(hl16());
			work8 = rlc8(work8);
			write8(hl16(), work8);
			break;

		/* rlc a */
		case 0x07:
			m_tstates += 8;
			m_a8 = rlc8(m_a8);
			break;

		/* rrc b */
		case 0x08:
			m_tstates += 8;
			m_b8 = rrc8(m_b8);
			break;

		/* rrc c */
		case 0x09:
			m_tstates += 8;
			m_c8 = rrc8(m_c8);
			break;

		/* rrc d */
		case 0x0A:
			m_tstates += 8;
			m_d8 = rrc8(m_d8);
			break;

		/* rrc e */
		case 0x0B:
			m_tstates += 8;
			m_e8 = rrc8(m_e8);
			break;

		/* rrc h */
		case 0x0C:
			m_tstates += 8;
			m_h8 = rrc8(m_h8);
			break;

		/* rrc l */
		case 0x0D:
			m_tstates += 8;
			m_l8 = rrc8(m_l8);
			break;

		/* rrc (hl) */
		case 0x0E:
			m_tstates += 15;
			work8 = read8(hl16());
			work8 = rrc8(work8);
			write8(hl16(), work8);
			break;

		/* rrc a */
		case 0x0F:
			m_tstates += 8;
			m_a8 = rrc8(m_a8);
			break;

		/* rl b */
		case 0x10:
			m_tstates += 8;
			m_b8 = rl8(m_b8);
			break;

		/* rl c */
		case 0x11:
			m_tstates += 8;
			m_c8 = rl8(m_c8);
			break;

		/* rl d */
		case 0x12:
			m_tstates += 8;
			m_d8 = rl8(m_d8);
			break;

		/* rl e */
		case 0x13:
			m_tstates += 8;
			m_e8 = rl8(m_e8);
			break;

		/* rl h */
		case 0x14:
			m_tstates += 8;
			m_h8 = rl8(m_h8);
			break;

		/* rl l */
		case 0x15:
			m_tstates += 8;
			m_l8 = rl8(m_l8);
			break;

		/* rl (hl) */
		case 0x16:
			m_tstates += 15;
			work8 = read8(hl16());
			work8 = rl8(work8);
			write8(hl16(), work8);
			break;

		/* rl a */
		case 0x17:
			m_tstates += 8;
			m_a8 = rl8(m_a8);
			break;

		/* rr b */
		case 0x18:
			m_tstates += 8;
			m_b8 = rr8(m_b8);
			break;

		/* rr c */
		case 0x19:
			m_tstates += 8;
			m_c8 = rr8(m_c8);
			break;

		/* rr d */
		case 0x1A:
			m_tstates += 8;
			m_d8 = rr8(m_d8);
			break;

		/* rr e */
		case 0x1B:
			m_tstates += 8;
			m_e8 = rr8(m_e8);
			break;

		/* rr h */
		case 0x1C:
			m_tstates += 8;
			m_h8 = rr8(m_h8);
			break;

		/* rr l */
		case 0x1D:
			m_tstates += 8;
			m_l8 = rr8(m_l8);
			break;

		/* rr (hl) */
		case 0x1E:
			m_tstates += 15;
			work8 = read8(hl16());
			work8 = rr8(work8);
			write8(hl16(), work8);
			break;

		/* rr a */
		case 0x1F:
			m_tstates += 8;
			m_a8 = rr8(m_a8);
			break;

		/* sla b */
		case 0x20:
			m_tstates += 8;
			m_b8 = sla8(m_b8);
			break;

		/* sla c */
		case 0x21:
			m_tstates += 8;
			m_c8 = sla8(m_c8);
			break;

		/* sla d */
		case 0x22:
			m_tstates += 8;
			m_d8 = sla8(m_d8);
			break;

		/* sla e */
		case 0x23:
			m_tstates += 8;
			m_e8 = sla8(m_e8);
			break;

		/* sla h */
		case 0x24:
			m_tstates += 8;
			m_h8 = sla8(m_h8);
			break;

		/* sla l */
		case 0x25:
			m_tstates += 8;
			m_l8 = sla8(m_l8);
			break;

		/* sla (hl) */
		case 0x26:
			m_tstates += 15;
			work8 = read8(hl16());
			work8 = sla8(work8);
			write8(hl16(), work8);
			break;

		/* sla a */
		case 0x27:
			m_tstates += 8;
			m_a8 = sla8(m_a8);
			break;

		/* sra b */
		case 0x28:
			m_tstates += 8;
			m_b8 = sra8(m_b8);
			break;

		/* sra c */
		case 0x29:
			m_tstates += 8;
			m_c8 = sra8(m_c8);
			break;

		/* sra d */
		case 0x2A:
			m_tstates += 8;
			m_d8 = sra8(m_d8);
			break;

		/* sra e */
		case 0x2B:
			m_tstates += 8;
			m_e8 = sra8(m_e8);
			break;

		/* sra h */
		case 0x2C:
			m_tstates += 8;
			m_h8 = sra8(m_h8);
			break;

		/* sra l */
		case 0x2D:
			m_tstates += 8;
			m_l8 = sra8(m_l8);
			break;

		/* sra (hl) */
		case 0x2E:
			m_tstates += 15;
			work8 = read8(hl16());
			work8 = sra8(work8);
			write8(hl16(), work8);
			break;

		/* sra a */
		case 0x2F:
			m_tstates += 8;
			m_a8 = sra8(m_a8);
			break;

		/**
		 * Undocumented instructions.
		 * 
		 * These instructions shift left the operand and make bit 0 always one.
		 */

		/* sli b */
		case 0x30:
			m_tstates += 8;
			m_b8 = sli8(m_b8);
			break;

		/* sli c */
		case 0x31:
			m_tstates += 8;
			m_c8 = sli8(m_c8);
			break;

		/* sli d */
		case 0x32:
			m_tstates += 8;
			m_d8 = sli8(m_d8);
			break;

		/* sli e */
		case 0x33:
			m_tstates += 8;
			m_e8 = sli8(m_e8);
			break;

		/* sli h */
		case 0x34:
			m_tstates += 8;
			m_h8 = sli8(m_h8);
			break;

		/* sli l */
		case 0x35:
			m_tstates += 8;
			m_l8 = sli8(m_l8);
			break;

		/* sli (hl) */
		case 0x36:
			m_tstates += 15;
			work8 = read8(hl16());
			work8 = sli8(work8);
			write8(hl16(), work8);
			break;

		/* sli a */
		case 0x37:
			m_tstates += 8;
			m_a8 = sli8(m_a8);
			break;

		/* srl b */
		case 0x38:
			m_tstates += 8;
			m_b8 = srl8(m_b8);
			break;

		/* srl c */
		case 0x39:
			m_tstates += 8;
			m_c8 = srl8(m_c8);
			break;

		/* srl d */
		case 0x3A:
			m_tstates += 8;
			m_d8 = srl8(m_d8);
			break;

		/* srl e */
		case 0x3B:
			m_tstates += 8;
			m_e8 = srl8(m_e8);
			break;

		/* srl h */
		case 0x3C:
			m_tstates += 8;
			m_h8 = srl8(m_h8);
			break;

		/* srl l */
		case 0x3D:
			m_tstates += 8;
			m_l8 = srl8(m_l8);
			break;

		/* srl (hl) */
		case 0x3E:
			m_tstates += 15;
			work8 = read8(hl16());
			work8 = srl8(work8);
			write8(hl16(), work8);
			break;

		/* srl a */
		case 0x3F:
			m_tstates += 8;
			m_a8 = srl8(m_a8);
			break;

		/* bit 0,b */
		case 0x40:
			m_tstates += 8;
			bit(0, m_b8);
			break;

		/* bit 0,c */
		case 0x41:
			m_tstates += 8;
			bit(0, m_c8);
			break;

		/* bit 0,d */
		case 0x42:
			m_tstates += 8;
			bit(0, m_d8);
			break;

		/* bit 0,e */
		case 0x43:
			m_tstates += 8;
			bit(0, m_e8);
			break;

		/* bit 0,h */
		case 0x44:
			m_tstates += 8;
			bit(0, m_h8);
			break;

		/* bit 0,l */
		case 0x45:
			m_tstates += 8;
			bit(0, m_l8);
			break;

		/* bit 0,(hl) */
		case 0x46:
			m_tstates += 12;
			bit_hl(0, read8(hl16()));
			break;

		/* bit 0,a */
		case 0x47:
			m_tstates += 8;
			bit(0, m_a8);
			break;

		/* bit 1,b */
		case 0x48:
			m_tstates += 8;
			bit(1, m_b8);
			break;

		/* bit 1,c */
		case 0x49:
			m_tstates += 8;
			bit(1, m_c8);
			break;

		/* bit 1,d */
		case 0x4A:
			m_tstates += 8;
			bit(1, m_d8);
			break;

		/* bit 1,e */
		case 0x4B:
			m_tstates += 8;
			bit(1, m_e8);
			break;

		/* bit 1,h */
		case 0x4C:
			m_tstates += 8;
			bit(1, m_h8);
			break;

		/* bit 1,l */
		case 0x4D:
			m_tstates += 8;
			bit(1, m_l8);
			break;

		/* bit 1,(hl) */
		case 0x4E:
			m_tstates += 12;
			bit_hl(1, read8(hl16()));
			break;

		/* bit 1,a */
		case 0x4F:
			m_tstates += 8;
			bit(1, m_a8);
			break;

		/* bit 2,b */
		case 0x50:
			m_tstates += 8;
			bit(2, m_b8);
			break;

		/* bit 2,c */
		case 0x51:
			m_tstates += 8;
			bit(2, m_c8);
			break;

		/* bit 2,d */
		case 0x52:
			m_tstates += 8;
			bit(2, m_d8);
			break;

		/* bit 2,e */
		case 0x53:
			m_tstates += 8;
			bit(2, m_e8);
			break;

		/* bit 2,h */
		case 0x54:
			m_tstates += 8;
			bit(2, m_h8);
			break;

		/* bit 2,l */
		case 0x55:
			m_tstates += 8;
			bit(2, m_l8);
			break;

		/* bit 2,(hl) */
		case 0x56:
			m_tstates += 12;
			bit_hl(2, read8(hl16()));
			break;

		/* bit 2,a */
		case 0x57:
			m_tstates += 8;
			bit(2, m_a8);
			break;

		/* bit 3,b */
		case 0x58:
			m_tstates += 8;
			bit(3, m_b8);
			break;

		/* bit 3,c */
		case 0x59:
			m_tstates += 8;
			bit(3, m_c8);
			break;

		/* bit 3,d */
		case 0x5A:
			m_tstates += 8;
			bit(3, m_d8);
			break;

		/* bit 3,e */
		case 0x5B:
			m_tstates += 8;
			bit(3, m_e8);
			break;

		/* bit 3,h */
		case 0x5C:
			m_tstates += 8;
			bit(3, m_h8);
			break;

		/* bit 3,l */
		case 0x5D:
			m_tstates += 8;
			bit(3, m_l8);
			break;

		/* bit 3,(hl) */
		case 0x5E:
			m_tstates += 12;
			bit_hl(3, read8(hl16()));
			break;

		/* bit 3,a */
		case 0x5F:
			m_tstates += 8;
			bit(3, m_a8);
			break;

		/* bit 4,b */
		case 0x60:
			m_tstates += 8;
			bit(4, m_b8);
			break;

		/* bit 4,c */
		case 0x61:
			m_tstates += 8;
			bit(4, m_c8);
			break;

		/* bit 4,d */
		case 0x62:
			m_tstates += 8;
			bit(4, m_d8);
			break;

		/* bit 4,e */
		case 0x63:
			m_tstates += 8;
			bit(4, m_e8);
			break;

		/* bit 4,h */
		case 0x64:
			m_tstates += 8;
			bit(4, m_h8);
			break;

		/* bit 4,l */
		case 0x65:
			m_tstates += 8;
			bit(4, m_l8);
			break;

		/* bit 4,(hl) */
		case 0x66:
			m_tstates += 12;
			bit_hl(4, read8(hl16()));
			break;

		/* bit 4,a */
		case 0x67:
			m_tstates += 8;
			bit(4, m_a8);
			break;

		/* bit 5,b */
		case 0x68:
			m_tstates += 8;
			bit(5, m_b8);
			break;

		/* bit 5,c */
		case 0x69:
			m_tstates += 8;
			bit(5, m_c8);
			break;

		/* bit 5,d */
		case 0x6A:
			m_tstates += 8;
			bit(5, m_d8);
			break;

		/* bit 5,e */
		case 0x6B:
			m_tstates += 8;
			bit(5, m_e8);
			break;

		/* bit 5,h */
		case 0x6C:
			m_tstates += 8;
			bit(5, m_h8);
			break;

		/* bit 5,l */
		case 0x6D:
			m_tstates += 8;
			bit(5, m_l8);
			break;

		/* bit 5,(hl) */
		case 0x6E:
			m_tstates += 12;
			bit_hl(5, read8(hl16()));
			break;

		/* bit 5,a */
		case 0x6F:
			m_tstates += 8;
			bit(5, m_a8);
			break;

		/* bit 6,b */
		case 0x70:
			m_tstates += 8;
			bit(6, m_b8);
			break;

		/* bit 6,c */
		case 0x71:
			m_tstates += 8;
			bit(6, m_c8);
			break;

		/* bit 6,d */
		case 0x72:
			m_tstates += 8;
			bit(6, m_d8);
			break;

		/* bit 6,e */
		case 0x73:
			m_tstates += 8;
			bit(6, m_e8);
			break;

		/* bit 6,h */
		case 0x74:
			m_tstates += 8;
			bit(6, m_h8);
			break;

		/* bit 6,l */
		case 0x75:
			m_tstates += 8;
			bit(6, m_l8);
			break;

		/* bit 6,(hl) */
		case 0x76:
			m_tstates += 12;
			bit_hl(6, read8(hl16()));
			break;

		/* bit 6,a */
		case 0x77:
			m_tstates += 8;
			bit(6, m_a8);
			break;

		/* bit 7,b */
		case 0x78:
			m_tstates += 8;
			bit(7, m_b8);
			break;

		/* bit 7,c */
		case 0x79:
			m_tstates += 8;
			bit(7, m_c8);
			break;

		/* bit 7,d */
		case 0x7A:
			m_tstates += 8;
			bit(7, m_d8);
			break;

		/* bit 7,e */
		case 0x7B:
			m_tstates += 8;
			bit(7, m_e8);
			break;

		/* bit 7,h */
		case 0x7C:
			m_tstates += 8;
			bit(7, m_h8);
			break;

		/* bit 7,l */
		case 0x7D:
			m_tstates += 8;
			bit(7, m_l8);
			break;

		/* bit 7,(hl) */
		case 0x7E:
			m_tstates += 12;
			bit_hl(7, read8(hl16()));
			break;

		/* bit 7,a */
		case 0x7F:
			m_tstates += 8;
			bit(7, m_a8);
			break;

		/* res 0,b */
		case 0x80:
			m_tstates += 8;
			m_b8 &= 0xfe;
			break;

		/* res 0,c */
		case 0x81:
			m_tstates += 8;
			m_c8 &= 0xfe;
			break;

		/* res 0,d */
		case 0x82:
			m_tstates += 8;
			m_d8 &= 0xfe;
			break;

		/* res 0,e */
		case 0x83:
			m_tstates += 8;
			m_e8 &= 0xfe;
			break;

		/* res 0,h */
		case 0x84:
			m_tstates += 8;
			m_h8 &= 0xfe;
			break;

		/* res 0,l */
		case 0x85:
			m_tstates += 8;
			m_l8 &= 0xfe;
			break;

		/* res 0,(hl) */
		case 0x86:
			m_tstates += 15;
			write8(hl16(), (read8(hl16()) & 0xfe));
			break;

		/* res 0,a */
		case 0x87:
			m_tstates += 8;
			m_a8 &= 0xfe;
			break;

		/* res 1,b */
		case 0x88:
			m_tstates += 8;
			m_b8 &= 0xfd;
			break;

		/* res 1,c */
		case 0x89:
			m_tstates += 8;
			m_c8 &= 0xfd;
			break;

		/* res 1,d */
		case 0x8A:
			m_tstates += 8;
			m_d8 &= 0xfd;
			break;

		/* res 1,e */
		case 0x8B:
			m_tstates += 8;
			m_e8 &= 0xfd;
			break;

		/* res 1,h */
		case 0x8C:
			m_tstates += 8;
			m_h8 &= 0xfd;
			break;

		/* res 1,l */
		case 0x8D:
			m_tstates += 8;
			m_l8 &= 0xfd;
			break;

		/* res 1,(hl) */
		case 0x8E:
			m_tstates += 15;
			write8(hl16(), (read8(hl16()) & 0xfd));
			break;

		/* res 1,a */
		case 0x8F:
			m_tstates += 8;
			m_a8 &= 0xfd;
			break;

		/* res 2,b */
		case 0x90:
			m_tstates += 8;
			m_b8 &= 0xfb;
			break;

		/* res 2,c */
		case 0x91:
			m_tstates += 8;
			m_c8 &= 0xfb;
			break;

		/* res 2,d */
		case 0x92:
			m_tstates += 8;
			m_d8 &= 0xfb;
			break;

		/* res 2,e */
		case 0x93:
			m_tstates += 8;
			m_e8 &= 0xfb;
			break;

		/* res 2,h */
		case 0x94:
			m_tstates += 8;
			m_h8 &= 0xfb;
			break;

		/* res 2,l */
		case 0x95:
			m_tstates += 8;
			m_l8 &= 0xfb;
			break;

		/* res 2,(hl) */
		case 0x96:
			m_tstates += 15;
			write8(hl16(), (read8(hl16()) & 0xfb));
			break;

		/* res 2,a */
		case 0x97:
			m_tstates += 8;
			m_a8 &= 0xfb;
			break;

		/* res 3,b */
		case 0x98:
			m_tstates += 8;
			m_b8 &= 0xf7;
			break;

		/* res 3,c */
		case 0x99:
			m_tstates += 8;
			m_c8 &= 0xf7;
			break;

		/* res 3,d */
		case 0x9A:
			m_tstates += 8;
			m_d8 &= 0xf7;
			break;

		/* res 3,e */
		case 0x9B:
			m_tstates += 8;
			m_e8 &= 0xf7;
			break;

		/* res 3,h */
		case 0x9C:
			m_tstates += 8;
			m_h8 &= 0xf7;
			break;

		/* res 3,l */
		case 0x9D:
			m_tstates += 8;
			m_l8 &= 0xf7;
			break;

		/* res 3,(hl) */
		case 0x9E:
			m_tstates += 15;
			write8(hl16(), (read8(hl16()) & 0xf7));
			break;

		/* res 3,a */
		case 0x9F:
			m_tstates += 8;
			m_a8 &= 0xf7;
			break;

		/* res 4,b */
		case 0xA0:
			m_tstates += 8;
			m_b8 &= 0xef;
			break;

		/* res 4,c */
		case 0xA1:
			m_tstates += 8;
			m_c8 &= 0xef;
			break;

		/* res 4,d */
		case 0xA2:
			m_tstates += 8;
			m_d8 &= 0xef;
			break;

		/* res 4,e */
		case 0xA3:
			m_tstates += 8;
			m_e8 &= 0xef;
			break;

		/* res 4,h */
		case 0xA4:
			m_tstates += 8;
			m_h8 &= 0xef;
			break;

		/* res 4,l */
		case 0xA5:
			m_tstates += 8;
			m_l8 &= 0xef;
			break;

		/* res 4,(hl) */
		case 0xA6:
			m_tstates += 15;
			write8(hl16(), (read8(hl16()) & 0xef));
			break;

		/* res 4,a */
		case 0xA7:
			m_tstates += 8;
			m_a8 &= 0xef;
			break;

		/* res 5,b */
		case 0xA8:
			m_tstates += 8;
			m_b8 &= 0xdf;
			break;

		/* res 5,c */
		case 0xA9:
			m_tstates += 8;
			m_c8 &= 0xdf;
			break;

		/* res 5,d */
		case 0xAA:
			m_tstates += 8;
			m_d8 &= 0xdf;
			break;

		/* res 5,e */
		case 0xAB:
			m_tstates += 8;
			m_e8 &= 0xdf;
			break;

		/* res 5,h */
		case 0xAC:
			m_tstates += 8;
			m_h8 &= 0xdf;
			break;

		/* res 5,l */
		case 0xAD:
			m_tstates += 8;
			m_l8 &= 0xdf;
			break;

		/* res 5,(hl) */
		case 0xAE:
			m_tstates += 15;
			write8(hl16(), (read8(hl16()) & 0xdf));
			break;

		/* res 5,a */
		case 0xAF:
			m_tstates += 8;
			m_a8 &= 0xdf;
			break;

		/* res 6,b */
		case 0xB0:
			m_tstates += 8;
			m_b8 &= 0xbf;
			break;

		/* res 6,c */
		case 0xB1:
			m_tstates += 8;
			m_c8 &= 0xbf;
			break;

		/* res 6,d */
		case 0xB2:
			m_tstates += 8;
			m_d8 &= 0xbf;
			break;

		/* res 6,e */
		case 0xB3:
			m_tstates += 8;
			m_e8 &= 0xbf;
			break;

		/* res 6,h */
		case 0xB4:
			m_tstates += 8;
			m_h8 &= 0xbf;
			break;

		/* res 6,l */
		case 0xB5:
			m_tstates += 8;
			m_l8 &= 0xbf;
			break;

		/* res 6,(hl) */
		case 0xB6:
			m_tstates += 15;
			write8(hl16(), (read8(hl16()) & 0xbf));
			break;

		/* res 6,a */
		case 0xB7:
			m_tstates += 8;
			m_a8 &= 0xbf;
			break;

		/* res 7,b */
		case 0xB8:
			m_tstates += 8;
			m_b8 &= 0x7f;
			break;

		/* res 7,c */
		case 0xB9:
			m_tstates += 8;
			m_c8 &= 0x7f;
			break;

		/* res 7,d */
		case 0xBA:
			m_tstates += 8;
			m_d8 &= 0x7f;
			break;

		/* res 7,e */
		case 0xBB:
			m_tstates += 8;
			m_e8 &= 0x7f;
			break;

		/* res 7,h */
		case 0xBC:
			m_tstates += 8;
			m_h8 &= 0x7f;
			break;

		/* res 7,l */
		case 0xBD:
			m_tstates += 8;
			m_l8 &= 0x7f;
			break;

		/* res 7,(hl) */
		case 0xBE:
			m_tstates += 15;
			write8(hl16(), (read8(hl16()) & 0x7f));
			break;

		/* res 7,a */
		case 0xBF:
			m_tstates += 8;
			m_a8 &= 0x7f;
			break;

		/* set 0,b */
		case 0xC0:
			m_tstates += 8;
			m_b8 |= 0x1;
			break;

		/* set 0,c */
		case 0xC1:
			m_tstates += 8;
			m_c8 |= 0x1;
			break;

		/* set 0,d */
		case 0xC2:
			m_tstates += 8;
			m_d8 |= 0x1;
			break;

		/* set 0,e */
		case 0xC3:
			m_tstates += 8;
			m_e8 |= 0x1;
			break;

		/* set 0,h */
		case 0xC4:
			m_tstates += 8;
			m_h8 |= 0x1;
			break;

		/* set 0,l */
		case 0xC5:
			m_tstates += 8;
			m_l8 |= 0x1;
			break;

		/* set 0,(hl) */
		case 0xC6:
			m_tstates += 15;
			write8(hl16(), (read8(hl16()) | 0x1));
			break;

		/* set 0,a */
		case 0xC7:
			m_tstates += 8;
			m_a8 |= 0x1;
			break;

		/* set 1,b */
		case 0xC8:
			m_tstates += 8;
			m_b8 |= 0x2;
			break;

		/* set 1,c */
		case 0xC9:
			m_tstates += 8;
			m_c8 |= 0x2;
			break;

		/* set 1,d */
		case 0xCA:
			m_tstates += 8;
			m_d8 |= 0x2;
			break;

		/* set 1,e */
		case 0xCB:
			m_tstates += 8;
			m_e8 |= 0x2;
			break;

		/* set 1,h */
		case 0xCC:
			m_tstates += 8;
			m_h8 |= 0x2;
			break;

		/* set 1,l */
		case 0xCD:
			m_tstates += 8;
			m_l8 |= 0x2;
			break;

		/* set 1,(hl) */
		case 0xCE:
			m_tstates += 15;
			write8(hl16(), (read8(hl16()) | 0x2));
			break;

		/* set 1,a */
		case 0xCF:
			m_tstates += 8;
			m_a8 |= 0x2;
			break;

		/* set 2,b */
		case 0xD0:
			m_tstates += 8;
			m_b8 |= 0x4;
			break;

		/* set 2,c */
		case 0xD1:
			m_tstates += 8;
			m_c8 |= 0x4;
			break;

		/* set 2,d */
		case 0xD2:
			m_tstates += 8;
			m_d8 |= 0x4;
			break;

		/* set 2,e */
		case 0xD3:
			m_tstates += 8;
			m_e8 |= 0x4;
			break;

		/* set 2,h */
		case 0xD4:
			m_tstates += 8;
			m_h8 |= 0x4;
			break;

		/* set 2,l */
		case 0xD5:
			m_tstates += 8;
			m_l8 |= 0x4;
			break;

		/* set 2,(hl) */
		case 0xD6:
			m_tstates += 15;
			write8(hl16(), (read8(hl16()) | 0x4));
			break;

		/* set 2,a */
		case 0xD7:
			m_tstates += 8;
			m_a8 |= 0x4;
			break;

		/* set 3,b */
		case 0xD8:
			m_tstates += 8;
			m_b8 |= 0x8;
			break;

		/* set 3,c */
		case 0xD9:
			m_tstates += 8;
			m_c8 |= 0x8;
			break;

		/* set 3,d */
		case 0xDA:
			m_tstates += 8;
			m_d8 |= 0x8;
			break;

		/* set 3,e */
		case 0xDB:
			m_tstates += 8;
			m_e8 |= 0x8;
			break;

		/* set 3,h */
		case 0xDC:
			m_tstates += 8;
			m_h8 |= 0x8;
			break;

		/* set 3,l */
		case 0xDD:
			m_tstates += 8;
			m_l8 |= 0x8;
			break;

		/* set 3,(hl) */
		case 0xDE:
			m_tstates += 15;
			write8(hl16(), (read8(hl16()) | 0x8));
			break;

		/* set 3,a */
		case 0xDF:
			m_tstates += 8;
			m_a8 |= 0x8;
			break;

		/* set 4,b */
		case 0xE0:
			m_tstates += 8;
			m_b8 |= 0x10;
			break;

		/* set 4,c */
		case 0xE1:
			m_tstates += 8;
			m_c8 |= 0x10;
			break;

		/* set 4,d */
		case 0xE2:
			m_tstates += 8;
			m_d8 |= 0x10;
			break;

		/* set 4,e */
		case 0xE3:
			m_tstates += 8;
			m_e8 |= 0x10;
			break;

		/* set 4,h */
		case 0xE4:
			m_tstates += 8;
			m_h8 |= 0x10;
			break;

		/* set 4,l */
		case 0xE5:
			m_tstates += 8;
			m_l8 |= 0x10;
			break;

		/* set 4,(hl) */
		case 0xE6:
			m_tstates += 15;
			write8(hl16(), (read8(hl16()) | 0x10));
			break;

		/* set 4,a */
		case 0xE7:
			m_tstates += 8;
			m_a8 |= 0x10;
			break;

		/* set 5,b */
		case 0xE8:
			m_tstates += 8;
			m_b8 |= 0x20;
			break;

		/* set 5,c */
		case 0xE9:
			m_tstates += 8;
			m_c8 |= 0x20;
			break;

		/* set 5,d */
		case 0xEA:
			m_tstates += 8;
			m_d8 |= 0x20;
			break;

		/* set 5,e */
		case 0xEB:
			m_tstates += 8;
			m_e8 |= 0x20;
			break;

		/* set 5,h */
		case 0xEC:
			m_tstates += 8;
			m_h8 |= 0x20;
			break;

		/* set 5,l */
		case 0xED:
			m_tstates += 8;
			m_l8 |= 0x20;
			break;

		/* set 5,(hl) */
		case 0xEE:
			m_tstates += 15;
			write8(hl16(), (read8(hl16()) | 0x20));
			break;

		/* set 5,a */
		case 0xEF:
			m_tstates += 8;
			m_a8 |= 0x20;
			break;

		/* set 6,b */
		case 0xF0:
			m_tstates += 8;
			m_b8 |= 0x40;
			break;

		/* set 6,c */
		case 0xF1:
			m_tstates += 8;
			m_c8 |= 0x40;
			break;

		/* set 6,d */
		case 0xF2:
			m_tstates += 8;
			m_d8 |= 0x40;
			break;

		/* set 6,e */
		case 0xF3:
			m_tstates += 8;
			m_e8 |= 0x40;
			break;

		/* set 6,h */
		case 0xF4:
			m_tstates += 8;
			m_h8 |= 0x40;
			break;

		/* set 6,l */
		case 0xF5:
			m_tstates += 8;
			m_l8 |= 0x40;
			break;

		/* set 6,(hl) */
		case 0xF6:
			m_tstates += 15;
			write8(hl16(), (read8(hl16()) | 0x40));
			break;

		/* set 6,a */
		case 0xF7:
			m_tstates += 8;
			m_a8 |= 0x40;
			break;

		/* set 7,b */
		case 0xF8:
			m_tstates += 8;
			m_b8 |= 0x80;
			break;

		/* set 7,c */
		case 0xF9:
			m_tstates += 8;
			m_c8 |= 0x80;
			break;

		/* set 7,d */
		case 0xFA:
			m_tstates += 8;
			m_d8 |= 0x80;
			break;

		/* set 7,e */
		case 0xFB:
			m_tstates += 8;
			m_e8 |= 0x80;
			break;

		/* set 7,h */
		case 0xFC:
			m_tstates += 8;
			m_h8 |= 0x80;
			break;

		/* set 7,l */
		case 0xFD:
			m_tstates += 8;
			m_l8 |= 0x80;
			break;

		/* set 7,(hl) */
		case 0xFE:
			m_tstates += 15;
			write8(hl16(), (read8(hl16()) | 0x80));
			break;

		/* set 7,a */
		case 0xFF:
			m_tstates += 8;
			m_a8 |= 0x80;
			break;
		}
	}

	/**
	 * Decode the index register operations.
	 * <P>
	 * This method only operates on the m_xx16 variable, following that the
	 * caller of this method will assign m_xx16 to the appropriate index
	 * register (m_ix16 or m_iy16).
	 */
	public static void decodeXX(final int op8) {
		int work16 = 0;
		int work8 = 0;

		switch (op8) {
		/* add xx,bc */
		case 0x09:
			m_tstates += 15;
			add_xx(bc16());
			break;

		/* add xx,de */
		case 0x19:
			m_tstates += 15;
			add_xx(de16());
			break;

		/* ld xx,NN */
		case 0x21:
			m_tstates += 14;
			m_xx16 = read16(m_pc16);
			m_pc16 = incinc16(m_pc16);
			break;

		/* ld (NN),xx */
		case 0x22:
			m_tstates += 20;
			write16(read16(m_pc16), m_xx16);
			m_pc16 = incinc16(m_pc16);
			break;

		/* inc xx */
		case 0x23:
			m_tstates += 10;
			inc16xx();
			break;

		/* inc hx */
		case 0x24:
			m_tstates += 8;
			xx16high8(inc8(xx16high8()));
			break;

		/* dec hx */
		case 0x25:
			m_tstates += 8;
			xx16high8(dec8(xx16high8()));
			break;

		/* ld hx,N */
		case 0x26:
			m_tstates += 11;
			xx16high8(read8(inc16pc()));
			break;

		/* add xx,xx */
		case 0x29:
			m_tstates += 15;
			add_xx(m_xx16);
			break;

		/* ld xx,(NN) */
		case 0x2A:
			m_tstates += 20;
			m_xx16 = read16(read16(m_pc16));
			m_pc16 = incinc16(m_pc16);
			break;

		/* dec xx */
		case 0x2B:
			m_tstates += 10;
			dec16xx();
			break;

		/* inc lx */
		case 0x2C:
			m_tstates += 8;
			xx16low8(inc8(xx16low8()));
			break;

		/* dec lx */
		case 0x2D:
			m_tstates += 8;
			xx16low8(dec8(xx16low8()));
			break;

		/* ld lx,N */
		case 0x2E:
			m_tstates += 11;
			xx16low8(read8(inc16pc()));
			break;

		/* inc (xx+d) */
		case 0x34:
			m_tstates += 23;
			work16 = add16(m_xx16, (byte) read8(inc16pc()));
			work8 = read8(work16);
			work8 = inc8(work8);
			write8(work16, work8);
			break;

		/* dec (xx+d) */
		case 0x35:
			m_tstates += 23;
			work16 = add16(m_xx16, (byte) read8(inc16pc()));
			work8 = read8(work16);
			work8 = dec8(work8);
			write8(work16, work8);
			break;

		/* ld (xx+d),N */
		case 0x36:
			m_tstates += 19;
			write8(add16(m_xx16, (byte) read8(m_pc16)),
					read8(inc16(m_pc16)));
			m_pc16 = incinc16(m_pc16);
			break;

		/* add xx,sp */
		case 0x39:
			m_tstates += 15;
			add_xx(m_sp16);
			break;

		/* ld b,hx */
		case 0x44:
			m_tstates += 8;
			m_b8 = xx16high8();
			break;

		/* ld b,lx */
		case 0x45:
			m_tstates += 8;
			m_b8 = xx16low8();
			break;

		/* ld b,(xx+d) */
		case 0x46:
			m_tstates += 15;
			work16 = add16(m_xx16, (byte) read8(inc16pc()));
			m_x8 = work16 >> 8;
			m_b8 = read8(work16);
			break;

		/* ld c,hx */
		case 0x4C:
			m_tstates += 8;
			m_c8 = xx16high8();
			break;

		/* ld c,lx */
		case 0x4D:
			m_tstates += 8;
			m_c8 = xx16low8();
			break;

		/* ld c,(xx+d) */
		case 0x4E:
			m_tstates += 15;
			work16 = add16(m_xx16, (byte) read8(inc16pc()));
			m_x8 = work16 >> 8;
			m_c8 = read8(work16);
			break;

		/* ld d,hx */
		case 0x54:
			m_tstates += 8;
			m_d8 = xx16high8();
			break;

		/* ld d,lx */
		case 0x55:
			m_tstates += 8;
			m_d8 = xx16low8();
			break;

		/* ld d,(xx+d) */
		case 0x56:
			m_tstates += 19;
			work16 = add16(m_xx16, (byte) read8(inc16pc()));
			m_x8 = work16 >> 8;
			m_d8 = read8(work16);
			break;

		/* ld e,hx */
		case 0x5C:
			m_tstates += 8;
			m_e8 = xx16high8();
			break;

		/* ld e,lx */
		case 0x5D:
			m_tstates += 8;
			m_e8 = xx16low8();
			break;

		/* ld e,(xx+d) */
		case 0x5E:
			m_tstates += 19;
			work16 = add16(m_xx16, (byte) read8(inc16pc()));
			m_x8 = work16 >> 8;
			m_e8 = read8(work16);
			break;

		/* ld hx,b */
		case 0x60:
			m_tstates += 8;
			xx16high8(m_b8);
			break;

		/* ld hx,c */
		case 0x61:
			m_tstates += 8;
			xx16high8(m_c8);
			break;

		/* ld hx,d */
		case 0x62:
			m_tstates += 8;
			xx16high8(m_d8);
			break;

		/* ld hx,e */
		case 0x63:
			m_tstates += 8;
			xx16high8(m_e8);
			break;

		/* ld hx,hx */
		case 0x64:
			m_tstates += 8;
			break;

		/* ld hx,lx */
		case 0x65:
			m_tstates += 8;
			xx16high8(xx16low8());
			break;

		/* ld h,(xx+d) */
		case 0x66:
			m_tstates += 19;
			work16 = add16(m_xx16, (byte) read8(inc16pc()));
			m_x8 = work16 >> 8;
			m_h8 = read8(work16);
			break;

		/* ld hx,a */
		case 0x67:
			m_tstates += 8;
			xx16high8(m_a8);
			break;

		/* ld lx,b */
		case 0x68:
			m_tstates += 8;
			xx16low8(m_b8);
			break;

		/* ld lx,c */
		case 0x69:
			m_tstates += 8;
			xx16low8(m_c8);
			break;

		/* ld lx,d */
		case 0x6A:
			m_tstates += 8;
			xx16low8(m_d8);
			break;

		/* ld lx,e */
		case 0x6B:
			m_tstates += 8;
			xx16low8(m_e8);
			break;

		/* ld lx,hx */
		case 0x6C:
			m_tstates += 8;
			xx16low8(xx16high8());
			break;

		/* ld lx,lx */
		case 0x6D:
			m_tstates += 8;
			break;

		/* ld l,(xx+d) */
		case 0x6E:
			m_tstates += 19;
			work16 = add16(m_xx16, (byte) read8(inc16pc()));
			m_x8 = work16 >> 8;
			m_l8 = read8(work16);
			break;

		/* ld lx,a */
		case 0x6F:
			m_tstates += 8;
			xx16low8(m_a8);
			break;

		/* ld (xx+d),b */
		case 0x70:
			m_tstates += 19;
			write8(add16(m_xx16, (byte) read8(inc16pc())),
					m_b8);
			break;

		/* ld (xx+d),c */
		case 0x71:
			m_tstates += 19;
			write8(add16(m_xx16, (byte) read8(inc16pc())),
					m_c8);
			break;

		/* ld (xx+d),d */
		case 0x72:
			m_tstates += 19;
			write8(add16(m_xx16, (byte) read8(inc16pc())),
					m_d8);
			break;

		/* ld (xx+d),e */
		case 0x73:
			m_tstates += 19;
			write8(add16(m_xx16, (byte) read8(inc16pc())),
					m_e8);
			break;

		/* ld (xx+d),h */
		case 0x74:
			m_tstates += 19;
			write8(add16(m_xx16, (byte) read8(inc16pc())),
					m_h8);
			break;

		/* ld (xx+d),l */
		case 0x75:
			m_tstates += 19;
			write8(add16(m_xx16, (byte) read8(inc16pc())),
					m_l8);
			break;

		/* ld (xx+d),a */
		case 0x77:
			m_tstates += 19;
			write8(add16(m_xx16, (byte) read8(inc16pc())),
					m_a8);
			break;

		/* ld a,hx */
		case 0x7C:
			m_tstates += 8;
			m_a8 = xx16high8();
			break;

		/* ld a,lx */
		case 0x7D:
			m_tstates += 8;
			m_a8 = xx16low8();
			break;

		/* ld a,(xx+d) */
		case 0x7E:
			m_tstates += 19;
			work16 = add16(m_xx16, (byte) read8(inc16pc()));
			m_x8 = work16 >> 8;
			m_a8 = read8(work16);
			break;

		/* add a,hx */
		case 0x84:
			m_tstates += 8;
			add_a(xx16high8());
			break;

		/* add a,lx */
		case 0x85:
			m_tstates += 8;
			add_a(xx16low8());
			break;

		/* add a,(xx+d) */
		case 0x86:
			m_tstates += 19;
			work8 = read8(add16(m_xx16, (byte) read8(inc16pc())));
			add_a(work8);
			break;

		/* adc a,hx */
		case 0x8C:
			m_tstates += 8;
			adc_a(xx16high8());
			break;

		/* adc a,lx */
		case 0x8D:
			m_tstates += 8;
			adc_a(xx16low8());
			break;

		/* adc a,(xx+d) */
		case 0x8E:
			m_tstates += 19;
			work8 = read8(add16(m_xx16, (byte) read8(inc16pc())));
			adc_a(work8);
			break;

		/* sub hx */
		case 0x94:
			m_tstates += 8;
			sub_a(xx16high8());
			break;

		/* sub lx */
		case 0x95:
			m_tstates += 8;
			sub_a(xx16low8());
			break;

		/* sub (xx+d) */
		case 0x96:
			m_tstates += 19;
			work8 = read8(add16(m_xx16, (byte) read8(inc16pc())));
			sub_a(work8);
			break;

		/* sbc a,hx */
		case 0x9C:
			m_tstates += 8;
			sbc_a(xx16high8());
			break;

		/* sbc a,l */
		case 0x9D:
			m_tstates += 8;
			sbc_a(xx16low8());
			break;

		/* sbc a,(xx+d) */
		case 0x9E:
			m_tstates += 19;
			work8 = read8(add16(m_xx16, (byte) read8(inc16pc())));
			sbc_a(work8);
			break;

		/* and hx */
		case 0xA4:
			m_tstates += 8;
			and_a(xx16high8());
			break;

		/* and lx */
		case 0xA5:
			m_tstates += 8;
			and_a(xx16low8());
			break;

		/* and (xx+d) */
		case 0xA6:
			m_tstates += 19;
			work8 = read8(add16(m_xx16, (byte) read8(inc16pc())));
			and_a(work8);
			break;

		/* xor hx */
		case 0xAC:
			m_tstates += 8;
			xor_a(xx16high8());
			break;

		/* xor lx */
		case 0xAD:
			m_tstates += 8;
			xor_a(xx16low8());
			break;

		/* xor (xx+d) */
		case 0xAE:
			m_tstates += 19;
			work8 = read8(add16(m_xx16, (byte) read8(inc16pc())));
			xor_a(work8);
			break;

		/* or hx */
		case 0xB4:
			m_tstates += 8;
			or_a(xx16high8());
			break;

		/* or lx */
		case 0xB5:
			m_tstates += 8;
			or_a(xx16low8());
			break;

		/* or (xx+d) */
		case 0xB6:
			m_tstates += 19;
			work8 = read8(add16(m_xx16, (byte) read8(inc16pc())));
			or_a(work8);
			break;

		/* cp hx */
		case 0xBC:
			m_tstates += 8;
			cmp_a(xx16high8());
			break;

		/* cp lx */
		case 0xBD:
			m_tstates += 8;
			cmp_a(xx16low8());
			break;

		/* cp (xx+d) */
		case 0xBE:
			m_tstates += 19;
			work8 = read8(add16(m_xx16, (byte) read8(inc16pc())));
			cmp_a(work8);
			break;

		/* Index register with 0xCB handling */
		case 0xCB: {
			byte disp8 = (byte) read8(inc16pc());
			m_xx16 = add16(m_xx16, disp8);

			switch (read8(inc16pc())) {
			// xxcbops

			/* rlc (xx+d),b */
			case 0x00:
				m_tstates += 23;
				m_b8 = read8(m_xx16);
				m_b8 = rlc8(m_b8);
				write8(m_xx16, m_b8);
				break;

			/* rlc (xx+d),c */
			case 0x01:
				m_tstates += 23;
				m_c8 = read8(m_xx16);
				m_c8 = rlc8(m_c8);
				write8(m_xx16, m_c8);
				break;

			/* rlc (xx+d),d */
			case 0x02:
				m_tstates += 23;
				m_d8 = read8(m_xx16);
				m_d8 = rlc8(m_d8);
				write8(m_xx16, m_d8);
				break;

			/* rlc (xx+d),e */
			case 0x03:
				m_tstates += 23;
				m_e8 = read8(m_xx16);
				m_e8 = rlc8(m_e8);
				write8(m_xx16, m_e8);
				break;

			/* rlc (xx+d),h */
			case 0x04:
				m_tstates += 23;
				m_h8 = read8(m_xx16);
				m_h8 = rlc8(m_h8);
				write8(m_xx16, m_h8);
				break;

			/* rlc (xx+d),l */
			case 0x05:
				m_tstates += 23;
				m_l8 = read8(m_xx16);
				m_l8 = rlc8(m_l8);
				write8(m_xx16, m_l8);
				break;

			/* rlc (xx+d) */
			case 0x06:
				m_tstates += 23;
				work8 = read8(m_xx16);
				work8 = rlc8(work8);
				write8(m_xx16, work8);
				break;

			/* rlc (xx+d),a */
			case 0x07:
				m_tstates += 23;
				m_a8 = read8(m_xx16);
				m_a8 = rlc8(m_a8);
				write8(m_xx16, m_a8);
				break;

			/* rrc (xx+d),b */
			case 0x08:
				m_tstates += 23;
				m_b8 = read8(m_xx16);
				m_b8 = rrc8(m_b8);
				write8(m_xx16, m_b8);
				break;

			/* rrc (xx+d),c */
			case 0x09:
				m_tstates += 23;
				m_c8 = read8(m_xx16);
				m_c8 = rrc8(m_c8);
				write8(m_xx16, m_c8);
				break;

			/* rrc (xx+d),d */
			case 0x0A:
				m_tstates += 23;
				m_d8 = read8(m_xx16);
				m_d8 = rrc8(m_d8);
				write8(m_xx16, m_d8);
				break;

			/* rrc (xx+d),e */
			case 0x0B:
				m_tstates += 23;
				m_e8 = read8(m_xx16);
				m_e8 = rrc8(m_e8);
				write8(m_xx16, m_e8);
				break;

			/* rrc (xx+d),h */
			case 0x0C:
				m_tstates += 23;
				m_h8 = read8(m_xx16);
				m_h8 = rrc8(m_h8);
				write8(m_xx16, m_h8);
				break;

			/* rrc (xx+d),l */
			case 0x0D:
				m_tstates += 23;
				m_l8 = read8(m_xx16);
				m_l8 = rrc8(m_l8);
				write8(m_xx16, m_l8);
				break;

			/* rrc (xx+d) */
			case 0x0E:
				m_tstates += 23;
				work8 = read8(m_xx16);
				work8 = rrc8(work8);
				write8(m_xx16, work8);
				break;

			/* rrc a */
			case 0x0F:
				m_tstates += 23;
				m_a8 = read8(m_xx16);
				m_a8 = rrc8(m_a8);
				write8(m_xx16, m_a8);
				break;

			/* rl (xx+d),b */
			case 0x10:
				m_tstates += 23;
				m_b8 = read8(m_xx16);
				m_b8 = rl8(m_b8);
				write8(m_xx16, m_b8);
				break;

			/* rl (xx+d),c */
			case 0x11:
				m_tstates += 23;
				m_c8 = read8(m_xx16);
				m_c8 = rl8(m_c8);
				write8(m_xx16, m_c8);
				break;

			/* rl (xx+d),d */
			case 0x12:
				m_tstates += 23;
				m_d8 = read8(m_xx16);
				m_d8 = rl8(m_d8);
				write8(m_xx16, m_d8);
				break;

			/* rl (xx+d),e */
			case 0x13:
				m_tstates += 23;
				m_e8 = read8(m_xx16);
				m_e8 = rl8(m_e8);
				write8(m_xx16, m_e8);
				break;

			/* rl (xx+d),h */
			case 0x14:
				m_tstates += 23;
				m_h8 = read8(m_xx16);
				m_h8 = rl8(m_h8);
				write8(m_xx16, m_h8);
				break;

			/* rl (xx+d),l */
			case 0x15:
				m_tstates += 23;
				m_l8 = read8(m_xx16);
				m_l8 = rl8(m_l8);
				write8(m_xx16, m_l8);
				break;

			/* rl (xx+d) */
			case 0x16:
				m_tstates += 23;
				work8 = read8(m_xx16);
				work8 = rl8(work8);
				write8(m_xx16, work8);
				break;

			/* rl (xx+d),a */
			case 0x17:
				m_tstates += 23;
				m_a8 = read8(m_xx16);
				m_a8 = rl8(m_a8);
				write8(m_xx16, m_a8);
				break;

			/* rr (xx+d),b */
			case 0x18:
				m_tstates += 23;
				m_b8 = read8(m_xx16);
				m_b8 = rr8(m_b8);
				write8(m_xx16, m_b8);
				break;

			/* rr (xx+d),c */
			case 0x19:
				m_tstates += 23;
				m_c8 = read8(m_xx16);
				m_c8 = rr8(m_c8);
				write8(m_xx16, m_c8);
				break;

			/* rr (xx+d),d */
			case 0x1A:
				m_tstates += 23;
				m_d8 = read8(m_xx16);
				m_d8 = rr8(m_d8);
				write8(m_xx16, m_d8);
				break;

			/* rr (xx+d),e */
			case 0x1B:
				m_tstates += 23;
				m_e8 = read8(m_xx16);
				m_e8 = rr8(m_e8);
				write8(m_xx16, m_e8);
				break;

			/* rr (xx+d),h */
			case 0x1C:
				m_tstates += 23;
				m_h8 = read8(m_xx16);
				m_h8 = rr8(m_h8);
				write8(m_xx16, m_h8);
				break;

			/* rr (xx+d),l */
			case 0x1D:
				m_tstates += 23;
				m_l8 = read8(m_xx16);
				m_l8 = rr8(m_l8);
				write8(m_xx16, m_l8);
				break;

			/* rr (xx+d) */
			case 0x1E:
				m_tstates += 23;
				work8 = read8(m_xx16);
				work8 = rr8(work8);
				write8(m_xx16, work8);
				break;

			/* rr (xx+d),a */
			case 0x1F:
				m_tstates += 23;
				m_a8 = read8(m_xx16);
				m_a8 = rr8(m_a8);
				write8(m_xx16, m_a8);
				break;

			/* sla (xx+d),b */
			case 0x20:
				m_tstates += 23;
				m_b8 = read8(m_xx16);
				m_b8 = sla8(m_b8);
				write8(m_xx16, m_b8);
				break;

			/* sla (xx+d),c */
			case 0x21:
				m_tstates += 23;
				m_c8 = read8(m_xx16);
				m_c8 = sla8(m_c8);
				write8(m_xx16, m_c8);
				break;

			/* sla (xx+d),d */
			case 0x22:
				m_tstates += 23;
				m_d8 = read8(m_xx16);
				m_d8 = sla8(m_d8);
				write8(m_xx16, m_d8);
				break;

			/* sla (xx+d),e */
			case 0x23:
				m_tstates += 23;
				m_e8 = read8(m_xx16);
				m_e8 = sla8(m_e8);
				write8(m_xx16, m_e8);
				break;

			/* sla (xx+d),h */
			case 0x24:
				m_tstates += 23;
				m_h8 = read8(m_xx16);
				m_h8 = sla8(m_h8);
				write8(m_xx16, m_h8);
				break;

			/* sla (xx+d),l */
			case 0x25:
				m_tstates += 23;
				m_l8 = read8(m_xx16);
				m_l8 = sla8(m_l8);
				write8(m_xx16, m_l8);
				break;

			/* sla (xx+d) */
			case 0x26:
				m_tstates += 23;
				work8 = read8(m_xx16);
				work8 = sla8(work8);
				write8(m_xx16, work8);
				break;

			/* sla (xx+d),a */
			case 0x27:
				m_tstates += 23;
				m_a8 = read8(m_xx16);
				m_a8 = sla8(m_a8);
				write8(m_xx16, m_a8);
				break;

			/* sra (xx+d),b */
			case 0x28:
				m_tstates += 23;
				m_b8 = read8(m_xx16);
				m_b8 = sra8(m_b8);
				write8(m_xx16, m_b8);
				break;

			/* sra (xx+d),c */
			case 0x29:
				m_tstates += 23;
				m_c8 = read8(m_xx16);
				m_c8 = sra8(m_c8);
				write8(m_xx16, m_c8);
				break;

			/* sra (xx+d),d */
			case 0x2A:
				m_tstates += 23;
				m_d8 = read8(m_xx16);
				m_d8 = sra8(m_d8);
				write8(m_xx16, m_d8);
				break;

			/* sra (xx+d),e */
			case 0x2B:
				m_tstates += 23;
				m_e8 = read8(m_xx16);
				m_e8 = sra8(m_e8);
				write8(m_xx16, m_e8);
				break;

			/* sra (xx+d),h */
			case 0x2C:
				m_tstates += 23;
				m_h8 = read8(m_xx16);
				m_h8 = sra8(m_h8);
				write8(m_xx16, m_h8);
				break;

			/* sra (xx+d),l */
			case 0x2D:
				m_tstates += 23;
				m_l8 = read8(m_xx16);
				m_l8 = sra8(m_l8);
				write8(m_xx16, m_l8);
				break;

			/* sra (xx+d) */
			case 0x2E:
				m_tstates += 23;
				work8 = read8(m_xx16);
				work8 = sra8(work8);
				write8(m_xx16, work8);
				break;

			/* sra (xx+d),a */
			case 0x2F:
				m_tstates += 23;
				m_a8 = read8(m_xx16);
				m_a8 = sra8(m_a8);
				write8(m_xx16, m_a8);
				break;

			/**
			 * The next 8 opcodes are undocumented.
			 */

			/* sli (xx+d),b */
			case 0x30:
				m_tstates += 23;
				m_b8 = read8(m_xx16);
				m_b8 = sli8(m_b8);
				write8(m_xx16, m_b8);
				break;

			/* sli (xx+d),c */
			case 0x31:
				m_tstates += 23;
				m_c8 = read8(m_xx16);
				m_c8 = sli8(m_c8);
				write8(m_xx16, m_c8);
				break;

			/* sli (xx+d),d */
			case 0x32:
				m_tstates += 23;
				m_d8 = read8(m_xx16);
				m_d8 = sli8(m_d8);
				write8(m_xx16, m_d8);
				break;

			/* sli (xx+d),e */
			case 0x33:
				m_tstates += 23;
				m_e8 = read8(m_xx16);
				m_e8 = sli8(m_e8);
				write8(m_xx16, m_e8);
				break;

			/* sli (xx+d),h */
			case 0x34:
				m_tstates += 23;
				m_h8 = read8(m_xx16);
				m_h8 = sli8(m_h8);
				write8(m_xx16, m_h8);
				break;

			/* sli (xx+d),l */
			case 0x35:
				m_tstates += 23;
				m_l8 = read8(m_xx16);
				m_l8 = sli8(m_l8);
				write8(m_xx16, m_l8);
				break;

			/* sli (xx+d) */
			case 0x36:
				m_tstates += 23;
				work8 = read8(m_xx16);
				work8 = sli8(work8);
				write8(m_xx16, work8);
				break;

			/* sli (xx+d),a */
			case 0x37:
				m_tstates += 23;
				m_a8 = read8(m_xx16);
				m_a8 = sli8(m_a8);
				write8(m_xx16, m_a8);
				break;

			/* srl (xx+d),b */
			case 0x38:
				m_tstates += 23;
				m_b8 = read8(m_xx16);
				m_b8 = srl8(m_b8);
				write8(m_xx16, m_b8);
				break;

			/* srl (xx+d),c */
			case 0x39:
				m_tstates += 23;
				m_c8 = read8(m_xx16);
				m_c8 = srl8(m_c8);
				write8(m_xx16, m_c8);
				break;

			/* srl (xx+d),d */
			case 0x3A:
				m_tstates += 23;
				m_d8 = read8(m_xx16);
				m_d8 = srl8(m_d8);
				write8(m_xx16, m_d8);
				break;

			/* srl (xx+d),e */
			case 0x3B:
				m_tstates += 23;
				m_e8 = read8(m_xx16);
				m_e8 = srl8(m_e8);
				write8(m_xx16, m_e8);
				break;

			/* srl (xx+d),h */
			case 0x3C:
				m_tstates += 23;
				m_h8 = read8(m_xx16);
				m_h8 = srl8(m_h8);
				write8(m_xx16, m_h8);
				break;

			/* srl (xx+d),l */
			case 0x3D:
				m_tstates += 23;
				m_l8 = read8(m_xx16);
				m_l8 = srl8(m_l8);
				write8(m_xx16, m_l8);
				break;

			/* srl (xx+d) */
			case 0x3E:
				m_tstates += 23;
				work8 = read8(m_xx16);
				work8 = srl8(work8);
				write8(m_xx16, work8);
				break;

			/* srl (xx+d),a */
			case 0x3F:
				m_tstates += 23;
				m_a8 = read8(m_xx16);
				m_a8 = srl8(m_a8);
				write8(m_xx16, m_a8);
				break;

			/* bit 0,(ix+d) */
			case 0x40: /* unofficial */
			case 0x41: /* unofficial */
			case 0x42: /* unofficial */
			case 0x43: /* unofficial */
			case 0x44: /* unofficial */
			case 0x45: /* unofficial */
			case 0x46:
			case 0x47: /* unofficial */
				m_tstates += 20;
				bit_xx(0, read8(m_xx16));
				break;

			/* bit 1,(xx+d) */
			case 0x48: /* unofficial */
			case 0x49: /* unofficial */
			case 0x4A: /* unofficial */
			case 0x4B: /* unofficial */
			case 0x4C: /* unofficial */
			case 0x4D: /* unofficial */
			case 0x4E:
			case 0x4F: /* unofficial */
				m_tstates += 20;
				bit_xx(1, read8(m_xx16));
				break;

			/* bit 2,(xx+d) */
			case 0x50: /* unofficial */
			case 0x51: /* unofficial */
			case 0x52: /* unofficial */
			case 0x53: /* unofficial */
			case 0x54: /* unofficial */
			case 0x55: /* unofficial */
			case 0x56:
			case 0x57: /* unofficial */
				m_tstates += 20;
				bit_xx(2, read8(m_xx16));
				break;

			/* bit 3,(xx+d) */
			case 0x58: /* unofficial */
			case 0x59: /* unofficial */
			case 0x5A: /* unofficial */
			case 0x5B: /* unofficial */
			case 0x5C: /* unofficial */
			case 0x5D: /* unofficial */
			case 0x5E:
			case 0x5F: /* unofficial */
				m_tstates += 20;
				bit_xx(3, read8(m_xx16));
				break;

			/* bit 4,(xx+d) */
			case 0x60: /* unofficial */
			case 0x61: /* unofficial */
			case 0x62: /* unofficial */
			case 0x63: /* unofficial */
			case 0x64: /* unofficial */
			case 0x65: /* unofficial */
			case 0x66:
			case 0x67: /* unofficial */
				m_tstates += 20;
				bit_xx(4, read8(m_xx16));
				break;

			/* bit 5,(xx+d) */
			case 0x68: /* unofficial */
			case 0x69: /* unofficial */
			case 0x6A: /* unofficial */
			case 0x6B: /* unofficial */
			case 0x6C: /* unofficial */
			case 0x6D: /* unofficial */
			case 0x6E:
			case 0x6F: /* unofficial */
				m_tstates += 20;
				bit_xx(5, read8(m_xx16));
				break;

			/* bit 6,(xx+d) */
			case 0x70: /* unofficial */
			case 0x71: /* unofficial */
			case 0x72: /* unofficial */
			case 0x73: /* unofficial */
			case 0x74: /* unofficial */
			case 0x75: /* unofficial */
			case 0x76:
			case 0x77: /* unofficial */
				m_tstates += 20;
				bit_xx(6, read8(m_xx16));
				break;

			/* bit 7,(xx+d) */
			case 0x78: /* unofficial */
			case 0x79: /* unofficial */
			case 0x7A: /* unofficial */
			case 0x7B: /* unofficial */
			case 0x7C: /* unofficial */
			case 0x7D: /* unofficial */
			case 0x7E:
			case 0x7F: /* unofficial */
				m_tstates += 20;
				bit_xx(7, read8(m_xx16));
				break;

			/* res 0,(xx+d),b */
			case 0x80:
				m_tstates += 23;
				m_b8 = read8(m_xx16) & 0xfe;
				write8(m_xx16, m_b8);
				break;

			/* res 0,(xx+d),c */
			case 0x81:
				m_tstates += 23;
				m_c8 = read8(m_xx16) & 0xfe;
				write8(m_xx16, m_c8);
				break;

			/* res 0,(xx+d),d */
			case 0x82:
				m_tstates += 23;
				m_d8 = read8(m_xx16) & 0xfe;
				write8(m_xx16, m_d8);
				break;

			/* res 0,(xx+d),e */
			case 0x83:
				m_tstates += 23;
				m_e8 = read8(m_xx16) & 0xfe;
				write8(m_xx16, m_e8);
				break;

			/* res 0,(xx+d),h */
			case 0x84:
				m_tstates += 23;
				m_h8 = read8(m_xx16) & 0xfe;
				write8(m_xx16, m_h8);
				break;

			/* res 0,(xx+d),l */
			case 0x85:
				m_tstates += 23;
				m_l8 = read8(m_xx16) & 0xfe;
				write8(m_xx16, m_l8);
				break;

			/* res 0,(xx+d) */
			case 0x86:
				m_tstates += 23;
				write8(m_xx16, (read8(m_xx16) & 0xfe));
				break;

			/* res 0,(xx+d),a */
			case 0x87:
				m_tstates += 23;
				m_a8 = read8(m_xx16) & 0xfe;
				write8(m_xx16, m_a8);
				break;

			/* res 1,(xx+d),b */
			case 0x88:
				m_tstates += 23;
				m_b8 = read8(m_xx16) & 0xfd;
				write8(m_xx16, m_b8);
				break;

			/* res 1,(xx+d),c */
			case 0x89:
				m_tstates += 23;
				m_c8 = read8(m_xx16) & 0xfd;
				write8(m_xx16, m_c8);
				break;

			/* res 1,(xx+d),d */
			case 0x8A:
				m_tstates += 23;
				m_d8 = read8(m_xx16) & 0xfd;
				write8(m_xx16, m_d8);
				break;

			/* res 1,(xx+d),e */
			case 0x8B:
				m_tstates += 23;
				m_e8 = read8(m_xx16) & 0xfd;
				write8(m_xx16, m_e8);
				break;

			/* res 1,(xx+d),h */
			case 0x8C:
				m_tstates += 23;
				m_h8 = read8(m_xx16) & 0xfd;
				write8(m_xx16, m_h8);
				break;

			/* res 1,(xx+d),l */
			case 0x8D:
				m_tstates += 23;
				m_l8 = read8(m_xx16) & 0xfd;
				write8(m_xx16, m_l8);
				break;

			/* res 1,(xx+d) */
			case 0x8E:
				m_tstates += 23;
				write8(m_xx16, (read8(m_xx16) & 0xfd));
				break;

			/* res 1,(xx+d),a */
			case 0x8F:
				m_tstates += 23;
				m_a8 = read8(m_xx16) & 0xfd;
				write8(m_xx16, m_a8);
				break;

			/* res 2,(xx+d),b */
			case 0x90:
				m_tstates += 23;
				m_b8 = read8(m_xx16) & 0xfb;
				write8(m_xx16, m_b8);
				break;

			/* res 2,(xx+d),c */
			case 0x91:
				m_tstates += 23;
				m_c8 = read8(m_xx16) & 0xfb;
				write8(m_xx16, m_c8);
				break;

			/* res 2,(xx+d),d */
			case 0x92:
				m_tstates += 23;
				m_d8 = read8(m_xx16) & 0xfb;
				write8(m_xx16, m_d8);
				break;

			/* res 2,(xx+d),e */
			case 0x93:
				m_tstates += 23;
				m_e8 = read8(m_xx16) & 0xfb;
				write8(m_xx16, m_e8);
				break;

			/* res 2,(xx+d),h */
			case 0x94:
				m_tstates += 23;
				m_h8 = read8(m_xx16) & 0xfb;
				write8(m_xx16, m_h8);
				break;

			/* res 2,(xx+d),l */
			case 0x95:
				m_tstates += 23;
				m_l8 = read8(m_xx16) & 0xfb;
				write8(m_xx16, m_l8);
				break;

			/* res 2,(xx+d) */
			case 0x96:
				m_tstates += 23;
				write8(m_xx16, (read8(m_xx16) & 0xfb));
				break;

			/* res 2,(xx+d),a */
			case 0x97:
				m_tstates += 23;
				m_a8 = read8(m_xx16) & 0xfb;
				write8(m_xx16, m_a8);
				break;

			/* res 3,(xx+d),b */
			case 0x98:
				m_tstates += 23;
				m_b8 = read8(m_xx16) & 0xf7;
				write8(m_xx16, m_b8);
				break;

			/* res 3,(xx+d),c */
			case 0x99:
				m_tstates += 23;
				m_c8 = read8(m_xx16) & 0xf7;
				write8(m_xx16, m_c8);
				break;

			/* res 3,(xx+d),d */
			case 0x9A:
				m_tstates += 23;
				m_d8 = read8(m_xx16) & 0xf7;
				write8(m_xx16, m_d8);
				break;

			/* res 3,(xx+d),e */
			case 0x9B:
				m_tstates += 23;
				m_e8 = read8(m_xx16) & 0xf7;
				write8(m_xx16, m_e8);
				break;

			/* res 3,(xx+d),h */
			case 0x9C:
				m_tstates += 23;
				m_h8 = read8(m_xx16) & 0xf7;
				write8(m_xx16, m_h8);
				break;

			/* res 3,(xx+d),l */
			case 0x9D:
				m_tstates += 23;
				m_l8 = read8(m_xx16) & 0xf7;
				write8(m_xx16, m_l8);
				break;

			/* res 3,(xx+d) */
			case 0x9E:
				m_tstates += 23;
				write8(m_xx16, (read8(m_xx16) & 0xf7));
				break;

			/* res 3,(xx+d),a */
			case 0x9F:
				m_tstates += 23;
				m_a8 = read8(m_xx16) & 0xf7;
				write8(m_xx16, m_a8);
				break;

			/* res 4,(xx+d),b */
			case 0xA0:
				m_tstates += 23;
				m_b8 = read8(m_xx16) & 0xef;
				write8(m_xx16, m_b8);
				break;

			/* res 4,(xx+d),c */
			case 0xA1:
				m_tstates += 23;
				m_c8 = read8(m_xx16) & 0xef;
				write8(m_xx16, m_c8);
				break;

			/* res 4,(xx+d),d */
			case 0xA2:
				m_tstates += 23;
				m_d8 = read8(m_xx16) & 0xef;
				write8(m_xx16, m_d8);
				break;

			/* res 4,(xx+d),e */
			case 0xA3:
				m_tstates += 23;
				m_e8 = read8(m_xx16) & 0xef;
				write8(m_xx16, m_e8);
				break;

			/* res 4,(xx+d),h */
			case 0xA4:
				m_tstates += 23;
				m_h8 = read8(m_xx16) & 0xef;
				write8(m_xx16, m_h8);
				break;

			/* res 4,(xx+d),l */
			case 0xA5:
				m_tstates += 23;
				m_l8 = read8(m_xx16) & 0xef;
				write8(m_xx16, m_l8);
				break;

			/* res 4,(xx+d) */
			case 0xA6:
				m_tstates += 23;
				write8(m_xx16, (read8(m_xx16) & 0xef));
				break;

			/* res 4,(xx+d),a */
			case 0xA7:
				m_tstates += 23;
				m_a8 = read8(m_xx16) & 0xef;
				write8(m_xx16, m_a8);
				break;

			/* res 5,(xx+d),b */
			case 0xA8:
				m_tstates += 23;
				m_b8 = read8(m_xx16) & 0xdf;
				write8(m_xx16, m_b8);
				break;

			/* res 5,(xx+d),c */
			case 0xA9:
				m_tstates += 23;
				m_c8 = read8(m_xx16) & 0xdf;
				write8(m_xx16, m_c8);
				break;

			/* res 5,(xx+d),d */
			case 0xAA:
				m_tstates += 23;
				m_d8 = read8(m_xx16) & 0xdf;
				write8(m_xx16, m_d8);
				break;

			/* res 5,(xx+d),e */
			case 0xAB:
				m_tstates += 23;
				m_e8 = read8(m_xx16) & 0xdf;
				write8(m_xx16, m_e8);
				break;

			/* res 5,(xx+d),h */
			case 0xAC:
				m_tstates += 23;
				m_h8 = read8(m_xx16) & 0xdf;
				write8(m_xx16, m_h8);
				break;

			/* res 5,(xx+d),l */
			case 0xAD:
				m_tstates += 23;
				m_l8 = read8(m_xx16) & 0xdf;
				write8(m_xx16, m_l8);
				break;

			/* res 5,(xx+d) */
			case 0xAE:
				m_tstates += 23;
				write8(m_xx16, (read8(m_xx16) & 0xdf));
				break;

			/* res 5,(xx+d),a */
			case 0xAF:
				m_tstates += 23;
				m_a8 = read8(m_xx16) & 0xdf;
				write8(m_xx16, m_a8);
				break;

			/* res 6,(xx+d),b */
			case 0xB0:
				m_tstates += 23;
				m_b8 = read8(m_xx16) & 0xbf;
				write8(m_xx16, m_b8);
				break;

			/* res 6,(xx+d),c */
			case 0xB1:
				m_tstates += 23;
				m_c8 = read8(m_xx16) & 0xbf;
				write8(m_xx16, m_c8);
				break;

			/* res 6,(xx+d),d */
			case 0xB2:
				m_tstates += 23;
				m_d8 = read8(m_xx16) & 0xbf;
				write8(m_xx16, m_d8);
				break;

			/* res 6,(xx+d),e */
			case 0xB3:
				m_tstates += 23;
				m_e8 = read8(m_xx16) & 0xbf;
				write8(m_xx16, m_e8);
				break;

			/* res 6,(xx+d),h */
			case 0xB4:
				m_tstates += 23;
				m_h8 = read8(m_xx16) & 0xbf;
				write8(m_xx16, m_h8);
				break;

			/* res 6,(xx+d),l */
			case 0xB5:
				m_tstates += 23;
				m_l8 = read8(m_xx16) & 0xbf;
				write8(m_xx16, m_l8);
				break;

			/* res 6,(xx+d) */
			case 0xB6:
				m_tstates += 23;
				write8(m_xx16, (read8(m_xx16) & 0xbf));
				break;

			/* res 6,(xx+d),a */
			case 0xB7:
				m_tstates += 23;
				m_a8 = read8(m_xx16) & 0xbf;
				write8(m_xx16, m_a8);
				break;

			/* res 7,(xx+d),b */
			case 0xB8:
				m_tstates += 23;
				m_b8 = read8(m_xx16) & 0x7f;
				write8(m_xx16, m_b8);
				break;

			/* res 7,(xx+d),c */
			case 0xB9:
				m_tstates += 23;
				m_c8 = read8(m_xx16) & 0x7f;
				write8(m_xx16, m_c8);
				break;

			/* res 7,(xx+d),d */
			case 0xBA:
				m_tstates += 23;
				m_d8 = read8(m_xx16) & 0x7f;
				write8(m_xx16, m_d8);
				break;

			/* res 7,(xx+d),e */
			case 0xBB:
				m_tstates += 23;
				m_e8 = read8(m_xx16) & 0x7f;
				write8(m_xx16, m_e8);
				break;

			/* res 7,(xx+d),h */
			case 0xBC:
				m_tstates += 23;
				m_h8 = read8(m_xx16) & 0x7f;
				write8(m_xx16, m_h8);
				break;

			/* res 7,(xx+d),l */
			case 0xBD:
				m_tstates += 23;
				m_l8 = read8(m_xx16) & 0x7f;
				write8(m_xx16, m_l8);
				break;

			/* res 7,(xx+d) */
			case 0xBE:
				m_tstates += 23;
				write8(m_xx16, (read8(m_xx16) & 0x7f));
				break;

			/* res 7,(xx+d),a */
			case 0xBF:
				m_tstates += 23;
				m_a8 = read8(m_xx16) & 0x7f;
				write8(m_xx16, m_a8);
				break;

			/* set 0,(xx+d),b */
			case 0xC0:
				m_tstates += 23;
				m_b8 = read8(m_xx16) | 0x1;
				write8(m_xx16, m_b8);
				break;

			/* set 0,(xx+d),c */
			case 0xC1:
				m_tstates += 23;
				m_c8 = read8(m_xx16) | 0x1;
				write8(m_xx16, m_c8);
				break;

			/* set 0,(xx+d),d */
			case 0xC2:
				m_tstates += 23;
				m_d8 = read8(m_xx16) | 0x1;
				write8(m_xx16, m_d8);
				break;

			/* set 0,(xx+d),e */
			case 0xC3:
				m_tstates += 23;
				m_e8 = read8(m_xx16) | 0x1;
				write8(m_xx16, m_e8);
				break;

			/* set 0,(xx+d),h */
			case 0xC4:
				m_tstates += 23;
				m_h8 = read8(m_xx16) | 0x1;
				write8(m_xx16, m_h8);
				break;

			/* set 0,(xx+d),l */
			case 0xC5:
				m_tstates += 23;
				m_l8 = read8(m_xx16) | 0x1;
				write8(m_xx16, m_l8);
				break;

			/* set 0,(xx+d) */
			case 0xC6:
				m_tstates += 23;
				write8(m_xx16, (read8(m_xx16) | 0x1));
				break;

			/* set 0,(xx+d),a */
			case 0xC7:
				m_tstates += 23;
				m_a8 = read8(m_xx16) | 0x1;
				write8(m_xx16, m_a8);
				break;

			/* set 1,(xx+d),b */
			case 0xC8:
				m_tstates += 23;
				m_b8 = read8(m_xx16) | 0x2;
				write8(m_xx16, m_b8);
				break;

			/* set 1,(xx+d),c */
			case 0xC9:
				m_tstates += 23;
				m_c8 = read8(m_xx16) | 0x2;
				write8(m_xx16, m_c8);
				break;

			/* set 1,(xx+d),d */
			case 0xCA:
				m_tstates += 23;
				m_d8 = read8(m_xx16) | 0x2;
				write8(m_xx16, m_d8);
				break;

			/* set 1,(xx+d),e */
			case 0xCB:
				m_tstates += 23;
				m_e8 = read8(m_xx16) | 0x2;
				write8(m_xx16, m_e8);
				break;

			/* set 1,(xx+d),h */
			case 0xCC:
				m_tstates += 23;
				m_h8 = read8(m_xx16) | 0x2;
				write8(m_xx16, m_h8);
				break;

			/* set 1,(xx+d),l */
			case 0xCD:
				m_tstates += 23;
				m_l8 = read8(m_xx16) | 0x2;
				write8(m_xx16, m_l8);
				break;

			/* set 1,(xx+d) */
			case 0xCE:
				m_tstates += 23;
				write8(m_xx16, (read8(m_xx16) | 0x2));
				break;

			/* set 1,(xx+d),a */
			case 0xCF:
				m_tstates += 23;
				m_a8 = read8(m_xx16) | 0x2;
				write8(m_xx16, m_a8);
				break;

			/* set 2,(xx+d),b */
			case 0xD0:
				m_tstates += 23;
				m_b8 = read8(m_xx16) | 0x4;
				write8(m_xx16, m_b8);
				break;

			/* set 2,(xx+d),c */
			case 0xD1:
				m_tstates += 23;
				m_c8 = read8(m_xx16) | 0x4;
				write8(m_xx16, m_c8);
				break;

			/* set 2,(xx+d),d */
			case 0xD2:
				m_tstates += 23;
				m_d8 = read8(m_xx16) | 0x4;
				write8(m_xx16, m_d8);
				break;

			/* set 2,(xx+d),e */
			case 0xD3:
				m_tstates += 23;
				m_e8 = read8(m_xx16) | 0x4;
				write8(m_xx16, m_e8);
				break;

			/* set 2,(xx+d),h */
			case 0xD4:
				m_tstates += 23;
				m_h8 = read8(m_xx16) | 0x4;
				write8(m_xx16, m_h8);
				break;

			/* set 2,(xx+d),l */
			case 0xD5:
				m_tstates += 23;
				m_l8 = read8(m_xx16) | 0x4;
				write8(m_xx16, m_l8);
				break;

			/* set 2,(xx+d) */
			case 0xD6:
				m_tstates += 23;
				write8(m_xx16, (read8(m_xx16) | 0x4));
				break;

			/* set 2,(xx+d),a */
			case 0xD7:
				m_tstates += 23;
				m_a8 = read8(m_xx16) | 0x4;
				write8(m_xx16, m_a8);
				break;

			/* set 3,(xx+d),b */
			case 0xD8:
				m_tstates += 23;
				m_b8 = read8(m_xx16) | 0x8;
				write8(m_xx16, m_b8);
				break;

			/* set 3,(xx+d),c */
			case 0xD9:
				m_tstates += 23;
				m_c8 = read8(m_xx16) | 0x8;
				write8(m_xx16, m_c8);
				break;

			/* set 3,(xx+d),d */
			case 0xDA:
				m_tstates += 23;
				m_d8 = read8(m_xx16) | 0x8;
				write8(m_xx16, m_d8);
				break;

			/* set 3,(xx+d),e */
			case 0xDB:
				m_tstates += 23;
				m_e8 = read8(m_xx16) | 0x8;
				write8(m_xx16, m_e8);
				break;

			/* set 3,(xx+d),h */
			case 0xDC:
				m_tstates += 23;
				m_h8 = read8(m_xx16) | 0x8;
				write8(m_xx16, m_h8);
				break;

			/* set 3,(xx+d),l */
			case 0xDD:
				m_tstates += 23;
				m_l8 = read8(m_xx16) | 0x8;
				write8(m_xx16, m_l8);
				break;

			/* set 3,(xx+d) */
			case 0xDE:
				m_tstates += 23;
				write8(m_xx16, (read8(m_xx16) | 0x8));
				break;

			/* set 3,(xx+d),a */
			case 0xDF:
				m_tstates += 23;
				m_a8 = read8(m_xx16) | 0x8;
				write8(m_xx16, m_a8);
				break;

			/* set 4,(xx+d),b */
			case 0xE0:
				m_tstates += 23;
				m_b8 = read8(m_xx16) | 0x10;
				write8(m_xx16, m_b8);
				break;

			/* set 4,(xx+d),c */
			case 0xE1:
				m_tstates += 23;
				m_c8 = read8(m_xx16) | 0x10;
				write8(m_xx16, m_c8);
				break;

			/* set 4,(xx+d),d */
			case 0xE2:
				m_tstates += 23;
				m_d8 = read8(m_xx16) | 0x10;
				write8(m_xx16, m_d8);
				break;

			/* set 4,(xx+d),e */
			case 0xE3:
				m_tstates += 23;
				m_e8 = read8(m_xx16) | 0x10;
				write8(m_xx16, m_e8);
				break;

			/* set 4,(xx+d),h */
			case 0xE4:
				m_tstates += 23;
				m_h8 = read8(m_xx16) | 0x10;
				write8(m_xx16, m_h8);
				break;

			/* set 4,(xx+d),l */
			case 0xE5:
				m_tstates += 23;
				m_l8 = read8(m_xx16) | 0x10;
				write8(m_xx16, m_l8);
				break;

			/* set 4,(xx+d) */
			case 0xE6:
				m_tstates += 23;
				write8(m_xx16, (read8(m_xx16) | 0x10));
				break;

			/* set 4,(xx+d),a */
			case 0xE7:
				m_tstates += 23;
				m_a8 = read8(m_xx16) | 0x10;
				write8(m_xx16, m_a8);
				break;

			/* set 5,(xx+d),b */
			case 0xE8:
				m_tstates += 23;
				m_b8 = read8(m_xx16) | 0x20;
				write8(m_xx16, m_b8);
				break;

			/* set 5,(xx+d),c */
			case 0xE9:
				m_tstates += 23;
				m_c8 = read8(m_xx16) | 0x20;
				write8(m_xx16, m_c8);
				break;

			/* set 5,(xx+d),d */
			case 0xEA:
				m_tstates += 23;
				m_d8 = read8(m_xx16) | 0x20;
				write8(m_xx16, m_d8);
				break;

			/* set 5,(xx+d),e */
			case 0xEB:
				m_tstates += 23;
				m_e8 = read8(m_xx16) | 0x20;
				write8(m_xx16, m_e8);
				break;

			/* set 5,(xx+d),h */
			case 0xEC:
				m_tstates += 23;
				m_h8 = read8(m_xx16) | 0x20;
				write8(m_xx16, m_h8);
				break;

			/* set 5,(xx+d),l */
			case 0xED:
				m_tstates += 23;
				m_l8 = read8(m_xx16) | 0x20;
				write8(m_xx16, m_l8);
				break;

			/* set 5,(xx+d) */
			case 0xEE:
				m_tstates += 23;
				write8(m_xx16, (read8(m_xx16) | 0x20));
				break;

			/* set 5(xx+d),,a */
			case 0xEF:
				m_tstates += 23;
				m_a8 = read8(m_xx16) | 0x20;
				write8(m_xx16, m_a8);
				break;

			/* set 6,(xx+d),b */
			case 0xF0:
				m_tstates += 23;
				m_b8 = read8(m_xx16) | 0x40;
				write8(m_xx16, m_b8);
				break;

			/* set 6,(xx+d),c */
			case 0xF1:
				m_tstates += 23;
				m_c8 = read8(m_xx16) | 0x40;
				write8(m_xx16, m_c8);
				break;

			/* set 6,(xx+d),d */
			case 0xF2:
				m_tstates += 23;
				m_d8 = read8(m_xx16) | 0x40;
				write8(m_xx16, m_d8);
				break;

			/* set 6,(xx+d),e */
			case 0xF3:
				m_tstates += 23;
				m_e8 = read8(m_xx16) | 0x40;
				write8(m_xx16, m_e8);
				break;

			/* set 6,(xx+d),h */
			case 0xF4:
				m_tstates += 23;
				m_h8 = read8(m_xx16) | 0x40;
				write8(m_xx16, m_h8);
				break;

			/* set 6,(xx+d),l */
			case 0xF5:
				m_tstates += 23;
				m_l8 = read8(m_xx16) | 0x40;
				write8(m_xx16, m_l8);
				break;

			/* set 6,(xx+d) */
			case 0xF6:
				m_tstates += 23;
				write8(m_xx16, (read8(m_xx16) | 0x40));
				break;

			/* set 6,(xx+d),a */
			case 0xF7:
				m_tstates += 23;
				m_a8 = read8(m_xx16) | 0x40;
				write8(m_xx16, m_a8);
				break;

			/* set 7,(xx+d),b */
			case 0xF8:
				m_tstates += 23;
				m_b8 = read8(m_xx16) | 0x80;
				write8(m_xx16, m_b8);
				break;

			/* set 7,(xx+d),c */
			case 0xF9:
				m_tstates += 23;
				m_c8 = read8(m_xx16) | 0x80;
				write8(m_xx16, m_c8);
				break;

			/* set 7,(xx+d),d */
			case 0xFA:
				m_tstates += 23;
				m_d8 = read8(m_xx16) | 0x80;
				write8(m_xx16, m_d8);
				break;

			/* set 7,(xx+d),e */
			case 0xFB:
				m_tstates += 23;
				m_e8 = read8(m_xx16) | 0x80;
				write8(m_xx16, m_e8);
				break;

			/* set 7,(xx+d),h */
			case 0xFC:
				m_tstates += 23;
				m_h8 = read8(m_xx16) | 0x80;
				write8(m_xx16, m_h8);
				break;

			/* set 7,(xx+d),l */
			case 0xFD:
				m_tstates += 23;
				m_l8 = read8(m_xx16) | 0x80;
				write8(m_xx16, m_l8);
				break;

			/* set 7,(xx+d) */
			case 0xFE:
				m_tstates += 23;
				write8(m_xx16, (read8(m_xx16) | 0x80));
				break;

			/* set 7,(xx+d),a */
			case 0xFF:
				m_tstates += 23;
				m_a8 = read8(m_xx16) | 0x80;
				write8(m_xx16, m_a8);
				break;
			}

			m_xx16 = sub16(m_xx16, disp8);
		}
			break;

		/* pop xx */
		case 0xE1:
			m_tstates += 14;
			m_xx16 = pop16();
			break;

		/* ex (sp),xx */
		case 0xE3:
			m_tstates += 23;
			work16 = read16(m_sp16);
			write16(m_sp16, m_xx16);
			m_xx16 = work16;
			break;

		/* push XX */
		case 0xE5:
			m_tstates += 15;
			push(m_xx16);
			break;

		/* jp (xx) */
		case 0xE9:
			m_tstates += 8;
			m_pc16 = m_xx16;
			break;

		/* ld sp,xx */
		case 0xF9:
			m_tstates += 10;
			m_sp16 = m_xx16;
			break;

		/**
		 * Many DD or FD opcodes after each other will effectively be NOPs,
		 * doing nothing except repeatedly setting the flag "treat HL as IX" (or
		 * IY) and taking up 4 T states
		 */

		case 0xDD:
		case 0xFD:
			m_tstates += 4;
			dec16pc(); /* Back to the second DD/FD */
			break;

		/**
		 * DD and FD have no effect on ED instructions, or on EX DE,HL.
		 */

		case 0xEB:
		case 0xED:
			m_tstates += 8;
			break;

		/**
		 * Each unimplemented opcode does the same as the nonprefixed opcode.
		 */

		default:
			m_tstates += 4;
			dec16pc(); /* Back to the nonprefixed opcode */

			Log.e(Z80.class.getName(), "Unimplemented instruction: "
					+ read8(dec16(m_pc16)) + " "
					+ read8(m_pc16) + " at " + dec16(m_pc16));
			break;
		}
	}

	/**
	 * Decode the instructions whose first opcode is 0xED.
	 */
	private static void decodeED(final int op8) {
		int work8 = 0;

		switch (op8) {
		/* in b,(c) */
		case 0x40:
			m_tstates += 12;
			m_b8 = in8(bc16());
			break;

		/* out (c),b */
		case 0x41:
			m_tstates += 12;
			BaseIO.out(bc16(), m_b8);
			break;

		/* sbc hl,bc */
		case 0x42:
			m_tstates += 15;
			sbc_hl(bc16());
			break;

		/* ld (NN),bc */
		case 0x43:
			m_tstates += 20;
			write16(read16(m_pc16), bc16());
			m_pc16 = incinc16(m_pc16);
			break;

		/* neg */
		case 0x44:
		case 0x4c: /* unofficial */
		case 0x54: /* unofficial */
		case 0x5c: /* unofficial */
		case 0x64: /* unofficial */
		case 0x6c: /* unofficial */
		case 0x74: /* unofficial */
		case 0x7c: /* unofficial */
			m_tstates += 8;
			work8 = m_a8;
			m_a8 = 0;
			sub_a(work8);
			break;

		/* retn */
		case 0x45:
		case 0x55: /* unofficial */
		case 0x5d: /* unofficial */
		case 0x65: /* unofficial */
		case 0x6d: /* unofficial */
		case 0x75: /* unofficial */
		case 0x7d: /* unofficial */
			m_tstates += 14;
			m_pc16 = pop16();
			m_iff1a = m_iff1b;
			break;

		/**
		 * The IM 0/1 instruction puts the processor in either IM 0 or 1
		 * (undetermined at this time). The SGS booklet says that there are two
		 * flip-flops which determine the interrupt mode and are set by the IM
		 * instructions. The possible values are:
		 * 
		 * 00 IM 0 01 not used 10 IM 1 11 IM 2
		 * 
		 * so it is entirely possible that the IM 0/1 instruction sets these
		 * registers to 01. What the Z80 does when this happens is anyone's
		 * guess.
		 */

		/* im 0 */
		case 0x46:
		case 0x4e: /* unofficial */
		case 0x66: /* unofficial */
		case 0x6e: /* unofficial */
			m_tstates += 8;
			m_im2 = 0;
			break;

		/* ld i,a */
		case 0x47:
			m_tstates += 9;
			m_i8 = m_a8;
			break;

		/* in c,(c) */
		case 0x48:
			m_tstates += 12;
			m_c8 = in8(bc16());
			break;

		/* out (c),c */
		case 0x49:
			m_tstates += 12;
			BaseIO.out(bc16(), m_c8);
			break;

		/* adc hl,bc */
		case 0x4A:
			m_tstates += 15;
			adc_hl(bc16());
			break;

		/* ld bc,(NN) */
		case 0x4B:
			m_tstates += 20;
			bc16(read16(read16(m_pc16)));
			m_pc16 = incinc16(m_pc16);
			break;

		/* reti */
		case 0x4D:
			m_tstates += 14;
			m_pc16 = pop16();
			// m_iff1a = m_iff1b;
			break;

		/* ld r,a */
		case 0x4F:
			m_tstates += 9;
			m_r8 = m_a8;
			break;

		/* in d,(c) */
		case 0x50:
			m_tstates += 12;
			m_d8 = in8(bc16());
			break;

		/* out (c),d */
		case 0x51:
			m_tstates += 12;
			BaseIO.out(bc16(), m_d8);
			break;

		/* sbc hl,de */
		case 0x52:
			m_tstates += 15;
			sbc_hl(de16());
			break;

		/* ld (NN),de */
		case 0x53:
			m_tstates += 20;
			write16(read16(m_pc16), de16());
			m_pc16 = incinc16(m_pc16);
			break;

		/* im 1 */
		case 0x56:
		case 0x76: /* unofficial */
			m_tstates += 8;
			m_im2 = 1;
			break;

		/* ld a,i */
		case 0x57:
			m_tstates += 9;
			ld_a_special(m_i8);
			break;

		/* in e,(c) */
		case 0x58:
			m_tstates += 12;
			m_e8 = in8(bc16());
			break;

		/* out (c),e */
		case 0x59:
			m_tstates += 12;
			BaseIO.out(bc16(), m_e8);
			break;

		/* adc hl,de */
		case 0x5A:
			m_tstates += 15;
			adc_hl(de16());
			break;

		/* ld de,(NN) */
		case 0x5B:
			m_tstates += 20;
			de16(read16(read16(m_pc16)));
			m_pc16 = incinc16(m_pc16);
			break;

		/* im 2 */
		case 0x5E:
		case 0x7e: /* unofficial */
			m_tstates += 8;
			m_im2 = 2;
			break;

		/* ld a,r */
		case 0x5F:
			m_tstates += 9;
			ld_a_special(m_r8);
			break;

		/* in h,(c) */
		case 0x60:
			m_tstates += 12;
			m_h8 = in8(bc16());
			break;

		/* out (c),h */
		case 0x61:
			m_tstates += 12;
			BaseIO.out(bc16(), m_h8);
			break;

		/* sbc hl,hl */
		case 0x62:
			m_tstates += 15;
			sbc_hl(hl16());
			break;

		/* ld (nn),hl */
		case 0x63: /* unofficial */
			m_tstates += 20;
			write16(read16(m_pc16), hl16());
			m_pc16 = incinc16(m_pc16);
			break;

		/* rrd */
		case 0x67:
			m_tstates += 18;
			work8 = read8(hl16());
			write8(hl16(), ((work8 >> 4) | (m_a8 << 4)));
			m_a8 = (m_a8 & 0xf0) | (work8 & 0x0f);
			m_signF = ((m_a8 & 0x80) != 0);
			m_zeroF = (m_a8 == 0);
			m_halfcarryF = false;
			m_parityoverflowF = m_parityTable[m_a8];
			m_addsubtractF = false;
			m_3F = ((m_a8 & THREE_MASK) != 0);
			m_5F = ((m_a8 & FIVE_MASK) != 0);
			break;

		/* in l,(c) */
		case 0x68:
			m_tstates += 12;
			m_l8 = in8(bc16());
			break;

		/* out (c),l */
		case 0x69:
			m_tstates += 12;
			BaseIO.out(bc16(), m_l8);
			break;

		/* adc hl,hl */
		case 0x6A:
			m_tstates += 15;
			adc_hl(hl16());
			break;

		/* ld hl,(NN) */
		case 0x6B: /* unofficial */
			m_tstates += 20;
			hl16(read16(read16(m_pc16)));
			m_pc16 = incinc16(m_pc16);
			break;

		/* rld */
		case 0x6F:
			m_tstates += 18;
			work8 = read8(hl16());
			write8(hl16(), (((work8 << 4) | (m_a8 & 0x0f))) & 0xff);
			m_a8 = (m_a8 & 0xf0) | (work8 >> 4);
			m_signF = ((m_a8 & 0x80) != 0);
			m_zeroF = (m_a8 == 0);
			m_halfcarryF = false;
			m_parityoverflowF = m_parityTable[m_a8];
			m_addsubtractF = false;
			m_3F = ((m_a8 & THREE_MASK) != 0);
			m_5F = ((m_a8 & FIVE_MASK) != 0);
			break;

		/**
		 * The ED70 instruction reads from port (C), just like the other
		 * instructions, but throws away the result. It does change the flags in
		 * the same way as the other IN instructions, however.
		 */

		/* in f,(c) */
		case 0x70:
			m_tstates += 12;
			in8(bc16());
			break;

		/* out (c),0 */
		case 0x71: /* unofficial */
			m_tstates += 12;
			BaseIO.out(bc16(), 0);
			break;

		/* sbc hl,sp */
		case 0x72:
			m_tstates += 15;
			sbc_hl(m_sp16);
			break;

		/* ld (NN),sp */
		case 0x73:
			m_tstates += 20;
			write16(read16(m_pc16), m_sp16);
			m_pc16 = incinc16(m_pc16);
			break;

		/* nop */
		case 0x77: /* unofficial */
		case 0x7f: /* unofficial */
			m_tstates += 8;
			break;

		/* in a,(c) */
		case 0x78:
			m_tstates += 12;
			m_a8 = in8(bc16());
			break;

		/* out (c),a */
		case 0x79:
			m_tstates += 12;
			BaseIO.out(bc16(), m_a8);
			break;

		/* adc hl,sp */
		case 0x7A:
			m_tstates += 15;
			adc_hl(m_sp16);
			break;

		/* ld sp,(NN) */
		case 0x7B:
			m_tstates += 20;
			m_sp16 = read16(read16(m_pc16));
			m_pc16 = incinc16(m_pc16);
			break;

		/* ldi */
		case 0xA0:
			m_tstates += 16;
			work8 = read8(hl16());
			write8(de16(), work8);
			inc16de();
			inc16hl();
			dec16bc();
			m_parityoverflowF = (bc16() != 0);
			m_halfcarryF = false;
			m_addsubtractF = false;
			work8 += m_a8;
			m_3F = ((work8 & THREE_MASK) != 0);
			m_5F = ((work8 & ONE_MASK) != 0);
			break;

		/* cpi */
		case 0xA1:
			m_tstates += 16;
			work8 = read8(hl16());
			cmp_a_special(work8);
			inc16hl();
			dec16bc();
			m_parityoverflowF = (bc16() != 0);
			work8 = m_a8 - work8 - (m_halfcarryF ? 1 : 0);
			m_3F = ((work8 & THREE_MASK) != 0);
			m_5F = ((work8 & ONE_MASK) != 0);
			break;

		/* ini */
		case 0xA2:
			m_tstates += 16;
			write8(hl16(), BaseIO.in8(bc16()));
			m_b8 = ((m_b8 - 1) & 0xff);
			inc16hl();
			m_zeroF = (m_b8 == 0);
			m_addsubtractF = true;
			break;

		/* outi */
		case 0xA3:
			m_tstates += 16;
			m_b8 = ((m_b8 - 1) & 0xff);
			BaseIO.out(bc16(), read8(hl16()));
			inc16hl();
			m_zeroF = (m_b8 == 0);
			m_addsubtractF = true;
			break;

		/* ldd */
		case 0xA8:
			m_tstates += 16;
			work8 = read8(hl16());
			write8(de16(), work8);
			dec16de();
			dec16hl();
			dec16bc();
			m_parityoverflowF = (bc16() != 0);
			m_halfcarryF = false;
			m_addsubtractF = false;
			work8 += m_a8;
			m_3F = ((work8 & THREE_MASK) != 0);
			m_5F = ((work8 & ONE_MASK) != 0);
			break;

		/* cpd */
		case 0xA9:
			m_tstates += 16;
			work8 = read8(hl16());
			cmp_a_special(work8);
			dec16hl();
			dec16bc();
			m_parityoverflowF = (bc16() != 0);
			work8 = m_a8 - work8 - (m_halfcarryF ? 1 : 0);
			m_3F = ((work8 & THREE_MASK) != 0);
			m_5F = ((work8 & ONE_MASK) != 0);
			break;

		/* ind */
		case 0xAA:
			m_tstates += 16;
			write8(hl16(), BaseIO.in8(bc16()));
			m_b8 = ((m_b8 - 1) & 0xff);
			dec16hl();
			m_zeroF = (m_b8 == 0);
			m_addsubtractF = true;
			break;

		/* outd */
		case 0xAB:
			m_tstates += 16;
			m_b8 = ((m_b8 - 1) & 0xff);
			BaseIO.out(bc16(), read8(hl16()));
			dec16hl();
			m_zeroF = (m_b8 == 0);
			m_addsubtractF = true;
			break;

		/* ldir */
		case 0xB0:
			m_tstates += 16;
			work8 = read8(hl16());
			write8(de16(), work8);
			inc16hl();
			inc16de();
			dec16bc();
			m_parityoverflowF = (bc16() != 0);
			m_halfcarryF = false;
			m_addsubtractF = false;
			if (m_parityoverflowF) {
				m_tstates += 5;
				m_pc16 = decdec16(m_pc16);
			}
			work8 += m_a8;
			m_3F = ((work8 & THREE_MASK) != 0);
			m_5F = ((work8 & ONE_MASK) != 0);
			break;

		/* cpir */
		case 0xB1:
			m_tstates += 16;
			work8 = read8(hl16());
			cmp_a_special(work8);
			inc16hl();
			dec16bc();
			m_parityoverflowF = (bc16() != 0);
			if (m_parityoverflowF && !m_zeroF) {
				m_tstates += 5;
				m_pc16 = decdec16(m_pc16);
			}
			work8 = m_a8 - work8 - (m_halfcarryF ? 1 : 0);
			m_3F = ((work8 & THREE_MASK) != 0);
			m_5F = ((work8 & ONE_MASK) != 0);
			break;

		/* inir */
		case 0xB2:
			m_tstates += 16;
			write8(hl16(), BaseIO.in8(bc16()));
			m_b8 = ((m_b8 - 1) & 0xff);
			inc16hl();
			m_zeroF = (m_b8 == 0);
			m_addsubtractF = true;
			if (!m_zeroF) {
				m_tstates += 5;
				m_pc16 = decdec16(m_pc16);
			}
			break;

		/* otir */
		case 0xB3:
			m_tstates += 16;
			m_b8 = ((m_b8 - 1) & 0xff);
			BaseIO.out(bc16(), read8(hl16()));
			inc16hl();
			m_zeroF = (m_b8 == 0);
			m_addsubtractF = true;
			if (!m_zeroF) {
				m_tstates += 5;
				m_pc16 = decdec16(m_pc16);
			}
			break;

		/* lddr */
		case 0xB8:
			m_tstates += 16;
			work8 = read8(hl16());
			write8(de16(), work8);
			dec16hl();
			dec16de();
			dec16bc();
			m_parityoverflowF = (bc16() != 0);
			m_halfcarryF = false;
			m_addsubtractF = false;
			if (m_parityoverflowF) {
				m_tstates += 5;
				m_pc16 = decdec16(m_pc16);
			}
			work8 += m_a8;
			m_3F = ((work8 & THREE_MASK) != 0);
			m_5F = ((work8 & ONE_MASK) != 0);
			break;

		/* cpdr */
		case 0xB9:
			m_tstates += 16;
			work8 = read8(hl16());
			cmp_a_special(work8);
			dec16hl();
			dec16bc();
			m_parityoverflowF = (bc16() != 0);
			if (m_parityoverflowF && !m_zeroF) {
				m_tstates += 5;
				m_pc16 = decdec16(m_pc16);
			}
			work8 = m_a8 - work8 - (m_halfcarryF ? 1 : 0);
			m_3F = ((work8 & THREE_MASK) != 0);
			m_5F = ((work8 & ONE_MASK) != 0);
			break;

		/* indr */
		case 0xBA:
			m_tstates += 16;
			write8(hl16(), BaseIO.in8(bc16()));
			m_b8 = ((m_b8 - 1) & 0xff);
			dec16hl();
			m_zeroF = (m_b8 == 0);
			m_addsubtractF = true;
			if (!m_zeroF) {
				m_tstates += 5;
				m_pc16 = decdec16(m_pc16);
			}
			break;

		/* otdr */
		case 0xBB:
			m_tstates += 16;
			m_b8 = ((m_b8 - 1) & 0xff);
			BaseIO.out(bc16(), read8(hl16()));
			dec16hl();
			m_zeroF = (m_b8 == 0);
			m_addsubtractF = true;
			if (!m_zeroF) {
				m_tstates += 5;
				m_pc16 = decdec16(m_pc16);
			}
			break;

		case 0xFB:
			m_tstates += 8;
			break;

		case 0xFC:
			m_tstates += 8;
			break;

		case 0xFD:
			m_tstates += 8;
			break;

		/**
		 * The ED opcodes in the range 00-3F and 80-FF (except for the block
		 * instructions of course) do nothing at all but taking up 8 T states
		 * and incrementing the R register by 2.
		 */

		default:
			m_tstates += 8;

			Log
					.e(Z80.class.getClass().getName(), "Unimplemented instruction: "
							+ read8(decdec16(m_pc16)) + " "
							+ read8(dec16(m_pc16)) + " at "
							+ decdec16(m_pc16));

			break;
		}
	}

	/**
	 * Return a string representation of the CPU state, which is useful for
	 * debugging purposes.
	 */
	public static String toStr() {
		storeFlags();

		return "A=" + m_a8 + ",F=" + m_f8 + ",B=" + m_b8 + ",C=" + m_c8 + ",D="
				+ m_d8 + ",E=" + m_e8 + ",H=" + m_h8 + ",L=" + m_l8 + ",AF1="
				+ m_af16alt + ",BC1=" + m_bc16alt + ",DE1=" + m_de16alt
				+ ",HL1=" + m_hl16alt + ",IX=" + m_ix16 + ",IY=" + m_iy16
				+ ",SP=" + m_sp16 + ",PC=" + m_pc16 + ",R=" + m_r8 + ",I="
				+ m_i8 + ",IM=" + m_im2 + ",IFF1=" + m_iff1a + ",IFF2="
				+ m_iff1b + ",OP=" + (memory[Math.abs(m_pc16)] & 0xff);
	}

	/**
	 * Load the CPU contents from the loader object.
	 */
	public static void load(BaseLoader loader) {
		af16(loader.getAF16());
		bc16(loader.getBC16());
		de16(loader.getDE16());
		hl16(loader.getHL16());
		m_af16alt = loader.getAF16ALT();
		m_bc16alt = loader.getBC16ALT();
		m_de16alt = loader.getDE16ALT();
		m_hl16alt = loader.getHL16ALT();
		m_ix16 = loader.getIX16();
		m_iy16 = loader.getIY16();
		m_sp16 = loader.getSP16();
		m_pc16 = loader.getPC16();
		m_r8 = loader.getR8();
		m_i8 = loader.getI8();
		m_im2 = loader.getIM2();
		m_iff1a = loader.getIFF1a();
		m_iff1b = loader.getIFF1b();

		retrieveFlags();
		byte[] loadedMemory = loader.getMemory();

		System.arraycopy(loadedMemory, BaseScreen.SCREEN_START, memory, BaseScreen.SCREEN_START,
				memory.length - BaseScreen.SCREEN_START);

	}

	public static int read16(final int addr16) {
		// Log.w("Z80.read16", Integer.toString(addr16));
		// Log.w("Z80.read16", Integer.toString(memory.length));
		return ((memory[addr16 + 1] & 0xff) << 8) | (memory[addr16] & 0xff);
	}

	public static void write16(final int addr16, final int val16) {
		// Log.w("Z80.write16", Integer.toString(addr16));
		memory[addr16] = (byte) (val16 & 0xff);
		memory[(addr16 + 1) & 0xffff] = (byte) (val16 >> 8);
		if (addr16 >= BaseScreen.SCREEN_START
				&& addr16 <= BaseScreen.SCREEN_END) {
			if (addr16 < (BaseScreen.SCREEN_START + BaseScreen.PIXEL_LENGTH)) {
				BaseScreen.screenTouch(addr16);
				BaseScreen.screenTouch(addr16 + 1);
			} else if (addr16 < (BaseScreen.SCREEN_START
					+ BaseScreen.PIXEL_LENGTH + BaseScreen.ATTR_LENGTH)) {
				BaseScreen.attrTouch(addr16);
				BaseScreen.attrTouch(addr16 + 1);
			}
		}
	}

	public static void write8(final int addr16, final int val8) {
		// RAM0 is read-only
		if (addr16 < BaseScreen.SCREEN_START) {
			return;
		}
		memory[addr16] = (byte) val8;
		if (addr16 >= BaseScreen.SCREEN_START
				&& addr16 <= BaseScreen.SCREEN_END) {
			if (addr16 < (BaseScreen.SCREEN_START + BaseScreen.PIXEL_LENGTH))
				BaseScreen.screenTouch(addr16);
			else if (addr16 < (BaseScreen.SCREEN_START
					+ BaseScreen.PIXEL_LENGTH + BaseScreen.ATTR_LENGTH))
				BaseScreen.attrTouch(addr16);
		}
	}

	public static int read8(final int addr16) {
		return memory[addr16] & 0xff;
	}

	public static byte[] getMemory() {
		return memory;
	}

	public static void readROM() throws IOException {
		InputStream is = BaseSpectrum.getRomIS();
		try {
			int offset = 0;
			while (true) {
				int length = is.read(memory, offset, memory.length - offset);
				if (length <= 0) {
					break;
				} else {
					offset += length;
				}
			}
		} finally {
			is.close();
		}
	}
}
