/*
 * RenderingProgressListener.java
 * cs450
 *
 * Created Jun 5, 2004
 *
 * Send any comments or patches to the author at matthew.nelson@acm.org.
 *
 * Copyright (c) 2004 Matthew Nelson. All Rights Reserved.
 * See COPYRIGHT for the full notice.
 */

package edu.calstatela.mandelbrot_set;

/**
 * @author Matthew Nelson
 */
interface RenderingListener {
	public void renderingProgress(RenderingEvent job);
}
