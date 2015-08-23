package org.razvan.jzx;

/**
 * Base class extended by all components that comprise the emulator.
 * <P>
 * This class is part of a Composite pattern, whereby it contains a BaseSpectrum
 * instance which, in turn, it exposes all the other relevant components that
 * make up the Spectrum. Any component can get access to any other component by
 * using the BaseSpectrum instance.
 * <P>
 * The class mandates four major lifetime events for any component:
 * <UL>
 * <LI><TT>init()</TT>: Called when the component is initialized. This
 * method should only be called once at the beginning, or after
 * <TT>terminate()</TT> is called. After this method is called, it does <I>not</I>
 * immediately follow that the component's state is reset. You need to
 * <I>explicitly</I> call <TT>reset()</TT> below in order to start with a
 * clean slate.</LI>
 * <LI><TT>terminate()</TT>: Called when the component is terminated. This
 * method should only be called after <TT>init()</TT> has been called.</LI>
 * <LI><TT>reset()</TT>: Called to cleanup the component's state. This
 * method can only be called after <TT>init()</TT> and before
 * <TT>terminate()</TT>. This method can be called as many times as you wish.</LI>
 * <LI><TT>load()</TT>: Called to load up state that was previously saved.
 * This method can only be called after <TT>init()</TT> and before
 * <TT>terminate()</TT> A call to <TT>reset()</TT> undoes the work done by
 * this method. This method can be called as many times as you wish.</LI>
 * </UL>
 * 
 * @author <A HREF="mailto:razvan.surdulescu@post.harvard.edu">Razvan Surdulescu</A>
 *         (c) 2001 - 2006 <BR>
 *         You may use and distribute this software for free provided you
 *         include this copyright notice. You may not sell this software, use my
 *         name for publicity reasons or modify the code without permission from
 *         me.
 * 
 * @see BaseSpectrum
 */
public abstract class BaseComponent {
	/**
	 * The BaseSpectrum instance, of which this BaseComponent is a part.
	 */
	protected BaseSpectrum m_spectrum;

	/**
	 * Trivial accessor for the BaseSpectrum instance.
	 */
	public BaseSpectrum getSpectrum() {
		return m_spectrum;
	}

	/**
	 * Initialize the component.
	 * <P>
	 * This method can only be called once at the beginning, or after
	 * <TT>terminate()</TT> is called.
	 */
	public void init(BaseSpectrum spectrum) {
		m_spectrum = spectrum;
	}

	/**
	 * Terminate the component.
	 * <P>
	 * This method can only be called once after <TT>init()</TT> is called.
	 */
	public void terminate() {
		m_spectrum = null;
	}

	/**
	 * Reset the state of the component.
	 * <P>
	 * This method can be called as many times as you wish in between a call to
	 * <TT>init()</TT> and a call to <TT>terminate()</TT>.
	 */
	public abstract void reset();

	/**
	 * Load previously saved state into the component.
	 * <P>
	 * This method can be called as many times as you wish in between a call to
	 * <TT>init()</TT> and a call to <TT>terminate()</TT>. This method is
	 * undone by a call to <TT>reset()</TT>.
	 */
	public abstract void load(BaseLoader loader);
}
