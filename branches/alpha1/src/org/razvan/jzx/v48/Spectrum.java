package org.razvan.jzx.v48;

import java.io.InputStream;

import org.razvan.jzx.BaseSpectrum;

/**
 * The 48k model specialization of the BaseSpectrum class.
 * 
 * @author <A HREF="mailto:razvan.surdulescu@post.harvard.edu">Razvan
 *         Surdulescu</A> (c) 2001 - 2006 <BR>
 *         You may use and distribute this software for free provided you
 *         include this copyright notice. You may not sell this software, use my
 *         name for publicity reasons or modify the code without permission from
 *         me.
 */
public class Spectrum extends BaseSpectrum {

	private static boolean noCache;

	public Spectrum(InputStream is) {
	}

	public static void setNoCache(boolean noCache) {
		Spectrum.noCache = noCache;
	}

}