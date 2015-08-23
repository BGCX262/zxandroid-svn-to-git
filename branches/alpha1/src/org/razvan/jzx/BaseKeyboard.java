package org.razvan.jzx;

import android.util.Log;
import android.view.KeyEvent;

;

/**
 * Base class extended by all Keyboard components that comprise the emulator.
 * <P>
 * There are currently no children of this class, so all keyboard functionality
 * is included in this class.
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
public class BaseKeyboard {

	/** Joystick UP event = Keypad 8 */
	protected static final int JOY_UP = KeyEvent.KEYCODE_DPAD_RIGHT;

	/** Joystick DOWN event = Keypad 2 */
	protected static final int JOY_DOWN = KeyEvent.KEYCODE_DPAD_LEFT;

	/** Joystick LEFT event = Keypad 4 */
	protected static final int JOY_LEFT = KeyEvent.KEYCODE_DPAD_UP;

	/** Joystick RIGHT event = Keypad 5 */
	protected static final int JOY_RIGHT = KeyEvent.KEYCODE_DPAD_DOWN;

	/** Joystick FIRE event = Keypad 0 */
	protected static final int JOY_FIRE = KeyEvent.KEYCODE_DPAD_CENTER;

	/**
	 * Maps ASCII keys to a keyboard row and bit to set/reset.
	 * <P>
	 * e.g. m_keyTable['a'] means set/reset bit 0 (0xfe) of keyboard row 1.
	 * 
	 * Keyboard row 8 (in the table below) means "unused".
	 * 
	 */
	
//	protected static final int[][] m_keyTable = {
//		{8, 0x00}, {8, 0x00}, {8, 0x00}, {8, 0x00}, {8, 0x00}, {8, 0x00}, {8, 0x00}, {8, 0x00},
//		{8, 0x00}, {8, 0x00}, {8, 0x00}, {8, 0x00}, {8, 0x00}, {8, 0x00}, {8, 0x00}, {8, 0x00},
//		{8, 0x00}, {8, 0x00}, {8, 0x00}, {8, 0x00}, {8, 0x00}, {8, 0x00}, {8, 0x00}, {8, 0x00},
//		{8, 0x00}, {8, 0x00}, {8, 0x00}, {8, 0x00}, {8, 0x00}, {8, 0x00}, {8, 0x00}, {8, 0x00},
//		/* SP       PGUP       PGDN       END        HOME       LEFT       UP         RIGHT  */
//		{7, 0xfe}, {8, 0x00}, {8, 0x00}, {8, 0x00}, {8, 0x00}, {8, 0x00}, {8, 0x00}, {8, 0x00},
//		/* DOWN                                      COMMA      MINUS      PERIOD     SLASH  */
//		{8, 0x00}, {8, 0x00}, {8, 0x00}, {8, 0x00}, {8, 0x00}, {8, 0x00}, {8, 0x00}, {8, 0x00},
//		/* 0        1          2          3          4          5          6          7      */
//		{4, 0xfe}, {3, 0xfe}, {3, 0xfd}, {3, 0xfb}, {3, 0xf7}, {3, 0xef}, {4, 0xef}, {4, 0xf7},
//		/* 8        9	                  SEMICOL               EQUALS                       */
//		{4, 0xfb}, {4, 0xfd}, {8, 0x00}, {5, 0xfd}, {8, 0x00}, {6, 0xfd}, {8, 0x00}, {8, 0x00},
//		/*          A          B          C          D          E          F          G      */
//		{8, 0x00}, {1, 0xfe}, {7, 0xef}, {0, 0xf7}, {1, 0xfb}, {2, 0xfb}, {1, 0xf7}, {1, 0xef},
//		/* H        I          J          K          L          M          N          O      */
//		{6, 0xef}, {5, 0xfb}, {6, 0xf7}, {6, 0xfb}, {6, 0xfd}, {7, 0xfb}, {7, 0xf7}, {5, 0xfd},
//		/* P        Q          R          S          T          U          V          W      */
//		{5, 0xfe}, {2, 0xfe}, {2, 0xf7}, {1, 0xfd}, {2, 0xef}, {5, 0xf7}, {0, 0xef}, {2, 0xfd},
//		/* X        Y          Z          OPENBR     BACKSLA    CLOSEBR                      */
//		{0, 0xfb}, {5, 0xef}, {0, 0xfd}, {8, 0x00}, {8, 0x00}, {8, 0x00}, {8, 0x00}, {8, 0x00},
//		/*          NUMPAD0    NUMPAD1    NUMPAD2    NUMPAD3    NUMPAD4    NUMPAD5    NUMPAD6*/
//		{8, 0x00}, {8, 0x00}, {8, 0x00}, {8, 0x00}, {8, 0x00}, {8, 0x00}, {8, 0x00}, {8, 0x00},
//		/*NUMPAD7   NUMPAD8    NUMPAD9    NUMPAD*    NUMPAD+               NUMPAD-    NUMPAD.*/
//		{8, 0x00}, {8, 0x00}, {8, 0x00}, {8, 0x00}, {8, 0x00}, {8, 0x00}, {8, 0x00}, {8, 0x00},
//		/*NUMPAD/                                                                            */
//		{8, 0x00}, {8, 0x00}, {8, 0x00}, {8, 0x00}, {8, 0x00}, {8, 0x00}, {8, 0x00}, {8, 0x00},
//		/*                                                                            NUMPADD*/
//		{8, 0x00}, {8, 0x00}, {8, 0x00}, {8, 0x00}, {8, 0x00}, {8, 0x00}, {8, 0x00}, {8, 0x00}
//	};
	
/*	
	Address       	Keys        		Half-row
	FEFE-65278     V  C  X  Z  CS     000 1
	FDFE-65022     G  F  D  S  A      001 2
	FBFE-64510     T  R  E  W  Q      010 3
	F7FE-63486     5  4  3  2  1      011 4
	EFFE-61438     6  7  8  9  0      100 5
	DFFE-57342     Y  U  I  O  P      101 6
	BFFE-49150     H  J  K  L Enter   110 7
	7FFE-32766     B  N  M SS Space   111 8
	     D4 D3 D2 D1  D0	
*/	     
	protected static final int[][] keyTable = new int [256][2];
	static {
		keyTable[KeyEvent.KEYCODE_1] = new int [] {3, 0xfe};
		keyTable[KeyEvent.KEYCODE_2] = new int [] {3, 0xfd};
		keyTable[KeyEvent.KEYCODE_3] = new int [] {3, 0xfb};
		keyTable[KeyEvent.KEYCODE_4] = new int [] {3, 0xf7};
		keyTable[KeyEvent.KEYCODE_5] = new int [] {3, 0xef};
		keyTable[KeyEvent.KEYCODE_6] = new int [] {4, 0xef};
		keyTable[KeyEvent.KEYCODE_7] = new int [] {4, 0xf7};
		keyTable[KeyEvent.KEYCODE_8] = new int [] {4, 0xfb};
		keyTable[KeyEvent.KEYCODE_9] = new int [] {4, 0xfd};
		keyTable[KeyEvent.KEYCODE_0] = new int [] {4, 0xfe};
		keyTable[KeyEvent.KEYCODE_A] = new int [] {1, 0xfe};
		keyTable[KeyEvent.KEYCODE_B] = new int [] {7, 0xef};
		keyTable[KeyEvent.KEYCODE_C] = new int [] {0, 0xf7};
		keyTable[KeyEvent.KEYCODE_D] = new int [] {1, 0xfb};
		keyTable[KeyEvent.KEYCODE_E] = new int [] {2, 0xfb};
		keyTable[KeyEvent.KEYCODE_F] = new int [] {1, 0xf7};
		keyTable[KeyEvent.KEYCODE_G] = new int [] {1, 0xef};
		keyTable[KeyEvent.KEYCODE_H] = new int [] {6, 0xef};
		keyTable[KeyEvent.KEYCODE_I] = new int [] {5, 0xfb};
		keyTable[KeyEvent.KEYCODE_J] = new int [] {6, 0xf7};
		keyTable[KeyEvent.KEYCODE_K] = new int [] {6, 0xfb};
		keyTable[KeyEvent.KEYCODE_L] = new int [] {6, 0xfd};
		keyTable[KeyEvent.KEYCODE_M] = new int [] {7, 0xfb};
		keyTable[KeyEvent.KEYCODE_N] = new int [] {7, 0xf7};
		keyTable[KeyEvent.KEYCODE_O] = new int [] {5, 0xfd};
		keyTable[KeyEvent.KEYCODE_P] = new int [] {5, 0xfe};
		keyTable[KeyEvent.KEYCODE_Q] = new int [] {2, 0xfe};
		keyTable[KeyEvent.KEYCODE_R] = new int [] {2, 0xf7};
		keyTable[KeyEvent.KEYCODE_S] = new int [] {1, 0xfd};
		keyTable[KeyEvent.KEYCODE_T] = new int [] {2, 0xef};
		keyTable[KeyEvent.KEYCODE_U] = new int [] {5, 0xf7};
		keyTable[KeyEvent.KEYCODE_V] = new int [] {0, 0xef};
		keyTable[KeyEvent.KEYCODE_W] = new int [] {2, 0xfd};
		keyTable[KeyEvent.KEYCODE_X] = new int [] {0, 0xfb};
		keyTable[KeyEvent.KEYCODE_Y] = new int [] {5, 0xef};
		keyTable[KeyEvent.KEYCODE_Z] = new int [] {0, 0xfd};
		keyTable[KeyEvent.KEYCODE_SPACE] = new int [] {7, 0xfe};
	
	}

	/**
	 * Is the cursor currently in SHIFT mode?
	 */
	private static  boolean capsShiftON;
	private static  boolean symbShiftON;

	/**
	 * Cache the reference to the I/O component.
	 */
	public static void init() {
	}

	/**
	 * Does nothing.
	 */
	public static void reset() {
		// Empty
	}

	/**
	 * Releases the cached I/O component reference.
	 */
	public static void terminate() {

	}

	/**
	 * Modify the I/O ports in accordance with the key that was pressed.
	 */
	public static void keyPressed(KeyEvent kevent) {
		int key = kevent.getKeyCode();
		Log.e("keyPressed", "Received key code " + key);
		// Joystick takes precedence.
		if (key == JOY_UP) {
			BaseIO.orIn(BaseIO.P_KEMPSTON, BaseIO.B_UP);
		} else if (key == JOY_DOWN) {
			BaseIO.orIn(BaseIO.P_KEMPSTON, BaseIO.B_DOWN);
		} else if (key == JOY_LEFT) {
			BaseIO.orIn(BaseIO.P_KEMPSTON, BaseIO.B_LEFT);
		} else if (key == JOY_RIGHT) {
			BaseIO.orIn(BaseIO.P_KEMPSTON, BaseIO.B_RIGHT);
		} else if (key == JOY_FIRE) {
			BaseIO.orIn(BaseIO.P_KEMPSTON, BaseIO.B_FIRE);
		}

		switch (key) {
		case KeyEvent.KEYCODE_DPAD_CENTER:
		case KeyEvent.KEYCODE_ENTER:
			BaseIO.andKey(6, 0xfe);
			break;
		case KeyEvent.KEYCODE_DEL:
			BaseIO.andKey(0, 0xfe);
			BaseIO.andKey(4, 0xfe);
			break;
		case KeyEvent.KEYCODE_DPAD_RIGHT:
			if (!capsShiftON) {
				BaseIO.andKey(0, 0xfe);
			}
			BaseIO.andKey(4, 0xf7);
			break;
		case KeyEvent.KEYCODE_DPAD_LEFT:
			if (!capsShiftON) {
				BaseIO.andKey(0, 0xfe);
			}
			BaseIO.andKey(4, 0xef);
			break;
		case KeyEvent.KEYCODE_DPAD_UP:
			if (!capsShiftON) {
				BaseIO.andKey(0, 0xfe);
			}
			BaseIO.andKey(3, 0xef);
			break;
		case KeyEvent.KEYCODE_DPAD_DOWN:
			if (!capsShiftON) {
				BaseIO.andKey(0, 0xfe);
			}
			BaseIO.andKey(4, 0xfb);
			break;
		case KeyEvent.KEYCODE_COMMA:
			BaseIO.andKey(7, 0xfd);
			BaseIO.andKey(7, 0xf7);
			break;
		case KeyEvent.KEYCODE_MINUS:
			BaseIO.andKey(7, 0xfd);
			BaseIO.andKey(6, 0xf7);
			break;
		case KeyEvent.KEYCODE_PERIOD:
			BaseIO.andKey(7, 0xfd);
			BaseIO.andKey(7, 0xfb);
			break;
		case KeyEvent.KEYCODE_SLASH:
			BaseIO.andKey(7, 0xfd);
			BaseIO.andKey(0, 0xef);
			break;
		case KeyEvent.KEYCODE_SEMICOLON:
			/*
			 * BaseIO.andKey(7, 0xfd); BaseIO.andKey(5, 0xfd);
			 */
			break;
		case KeyEvent.KEYCODE_EQUALS:
			/*
			 * BaseIO.andKey(7, 0xfd); BaseIO.andKey(6, 0xfd);
			 */
			break;
		case KeyEvent.KEYCODE_SHIFT_LEFT:
		case KeyEvent.KEYCODE_SHIFT_RIGHT:
			BaseIO.andKey(0, 0xfe);
			break;
		case KeyEvent.KEYCODE_ALT_LEFT:
		case KeyEvent.KEYCODE_ALT_RIGHT:
			BaseIO.andKey(7, 0xfd);
			break;

		default:
			if (key < 128) {
				BaseIO.andKey(keyTable[key][0], keyTable[key][1]);
			}
		break;
		}
	}

	/**
	 * Modify the I/O ports in accordance with the key that was released.
	 */
	public static void keyReleased(KeyEvent kevent) {
		int key = kevent.getKeyCode();
		Log.e("keyReleased", "Received key code " + key);
		// Joystick takes precedence.
		if (key == JOY_UP) {
			BaseIO.andIn(BaseIO.P_KEMPSTON, ~BaseIO.B_UP);
		} else if (key == JOY_DOWN) {
			BaseIO.andIn(BaseIO.P_KEMPSTON, ~BaseIO.B_DOWN);
		} else if (key == JOY_LEFT) {
			BaseIO.andIn(BaseIO.P_KEMPSTON, ~BaseIO.B_LEFT);
		} else if (key == JOY_RIGHT) {
			BaseIO.andIn(BaseIO.P_KEMPSTON, ~BaseIO.B_RIGHT);
		} else if (key == JOY_FIRE) {
			BaseIO.andIn(BaseIO.P_KEMPSTON, ~BaseIO.B_FIRE);
		}
		switch (key) {
		case KeyEvent.KEYCODE_SHIFT_LEFT:
		case KeyEvent.KEYCODE_SHIFT_RIGHT:
			if (capsShiftON)
				BaseIO.orKey(0, 0x01);
			if (symbShiftON) {
				BaseIO.orKey(7, 0x02);
				symbShiftON = !symbShiftON;
			}
			capsShiftON = !capsShiftON;
			break;
		case KeyEvent.KEYCODE_ALT_LEFT:
		case KeyEvent.KEYCODE_ALT_RIGHT:
			if (symbShiftON)
				BaseIO.orKey(7, 0x02);
			if (capsShiftON) { // turn off caps shift (we're in the E mode) 
				BaseIO.orKey(0, 0x01);
				capsShiftON = !capsShiftON;
			}
			symbShiftON = !symbShiftON;
			break;			
			
		case KeyEvent.KEYCODE_ENTER:
		case KeyEvent.KEYCODE_DPAD_CENTER:			
			BaseIO.orKey(6, 0x01);
			break;
		case KeyEvent.KEYCODE_DEL:
			BaseIO.orKey(0, 0x01);
			BaseIO.orKey(4, 0x01);
			break;
		case KeyEvent.KEYCODE_DPAD_LEFT:
			if (!capsShiftON) {
				BaseIO.orKey(0, 0x01);
			}
			BaseIO.orKey(4, 0x08);
			break;
		case KeyEvent.KEYCODE_DPAD_RIGHT:
			if (!capsShiftON) {
				BaseIO.orKey(0, 0x01);
			}
			BaseIO.orKey(4, 0x10);
			break;
		case KeyEvent.KEYCODE_DPAD_UP:
			if (!capsShiftON) {
				BaseIO.orKey(0, 0x01);
			}
			BaseIO.orKey(3, 0x10);
			break;
		case KeyEvent.KEYCODE_DPAD_DOWN:
			if (!capsShiftON) {
				BaseIO.orKey(0, 0x01);
			}
			BaseIO.orKey(4, 0x04);
			break;
		case KeyEvent.KEYCODE_COMMA:
			BaseIO.orKey(7, ~0xf7);
			BaseIO.orKey(7, 0x02);
			break;
		case KeyEvent.KEYCODE_MINUS:
			BaseIO.orKey(6, ~0xf7);
			BaseIO.orKey(7, 0x02);
			break;
		case KeyEvent.KEYCODE_PERIOD:
			BaseIO.orKey(7, ~0xfb);
			BaseIO.orKey(7, 0x02);
			break;
		case KeyEvent.KEYCODE_SLASH:
			BaseIO.orKey(0, ~0xef);
			BaseIO.orKey(7, 0x02);
			break;
		case KeyEvent.KEYCODE_SEMICOLON:
			/*
			 * BaseIO.orKey(5, ~0xfd); BaseIO.orKey(7, 0xfd);
			 */
			break;
		case KeyEvent.KEYCODE_EQUALS:
			/*
			 * BaseIO.orKey(6, ~0xfd); BaseIO.orKey(7, 0xfd);
			 */
			break;
		default:
			if (key < 128) {
				BaseIO.orKey(keyTable[key][0], ~keyTable[key][1]);
			}

		break;
		}
	}

	/**
	 * Does nothing.
	 * <P>
	 * No keyboard state is saved, so there is nothing to load.
	 */
	public static void load(BaseLoader loader) {
	}

	public static boolean isCapsShiftON() {
		return capsShiftON;
	}

	public static void setCapsShiftON(boolean capsShiftON) {
		BaseKeyboard.capsShiftON = capsShiftON;
	}

	public static boolean isSymbShiftON() {
		return symbShiftON;
	}

	public static void setSymbShiftON(boolean symbShiftON) {
		BaseKeyboard.symbShiftON = symbShiftON;
	}

	
}
