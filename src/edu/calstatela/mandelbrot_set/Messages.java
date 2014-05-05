/*
 * Messages.java
 * cs450
 *
 * Created Jun 7, 2004
 *
 * Copyright (c) 2004 Matthew Nelson. All Rights Reserved.
 * See LICENSE for the full notice.
 */

package edu.calstatela.mandelbrot_set;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Convenience class for importing the string resource bundle.
 * 
 * @author Matthew Nelson
 */
public class Messages {

	private static final String BUNDLE_NAME
			= "edu.calstatela.mandelbrot_set.text"; //$NON-NLS-1$

	private static final ResourceBundle RESOURCE_BUNDLE =
		ResourceBundle.getBundle(BUNDLE_NAME);

	public static String getString(String key) {
		try {
			return RESOURCE_BUNDLE.getString(key);
		} catch (MissingResourceException e) {
			return '!' + key + '!';
		}
	}
}
