/*
 * SelectionEvent.java
 * cs450
 *
 * Created Jun 6, 2004
 *
 * Send any comments or patches to the author at matthew.nelson@acm.org.
 *
 * Copyright (c) 2004 Matthew Nelson. All Rights Reserved.
 * See COPYRIGHT for the full notice.
 */

package edu.calstatela.mandelbrot_set;

import java.awt.Rectangle;
import java.util.EventObject;

/**
 * @author Matthew Nelson
 */
public class SelectionEvent extends EventObject {
	
	private final Rectangle selection;

	public SelectionEvent(Object source, Rectangle selection) {
		super(source);

		if (selection == null) {
			throw new NullPointerException();	
		}
		
		this.selection = selection;
	}
	
	public Rectangle getSelection() {
		return (Rectangle) selection.clone();
	}

}
