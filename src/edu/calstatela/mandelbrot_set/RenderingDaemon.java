/*
 * RenderingDaemon.java
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

import java.awt.image.BufferedImage;
import javax.swing.SwingUtilities;

/**
 * A daemon thread that renders scenes of Mandelbrot images in the background. 
 * 
 * @author Matthew Nelson
 */
class RenderingDaemon extends Thread {
	
	private Scene synchronizedTask = null;
	private final RenderingListener listener;
	private final boolean fireEventsOnAWT;
	
	public RenderingDaemon(RenderingListener l, boolean eventsOnAWT) {
		setDaemon(true);
		listener = l;
		fireEventsOnAWT = eventsOnAWT;
	}
	
	public synchronized void beginRendering(Scene task) {
		synchronizedTask = task;
		notifyAll();
	}
	
	public synchronized Scene getTask() {
		return synchronizedTask;
	}
	
	public synchronized void abortRendering() {
		synchronizedTask = null;
	}

	public void run() {
		
		// Main loop
		while (true) {
			
			// Wait for task in synchronized reference
			Scene task = waitForTask();

			// Perform rendering
			doRender(task);			
			
			// Clear task, loop back, and wait for another
			synchronized (this) {
				if (synchronizedTask == task) {
					synchronizedTask = null;
				}
			}
			
			System.gc();
			
		}
		
	}
	
	private synchronized Scene waitForTask() {
		Scene task = synchronizedTask;
		while (task == null) {
			try {
				wait();
			} catch (InterruptedException unused) {}
			task = synchronizedTask;
		}	
		return task;	
	}

	private void doRender(Scene task) {
		
		// Shorter variable names that don't call accessor methods :)
		final double wt = task.getSeeTop();
		final double wb = task.getSeeBottom();
		final double wl = task.getSeeLeft();
		final double wr = task.getSeeRight();
		final int sdy = task.getVerticalResolution();
		final int sdx = task.getHorizontalResolution();
		final int limit = task.getLimit();
		
		// Create image buffer
		BufferedImage output = new BufferedImage(
				sdx, sdy,	BufferedImage.TYPE_INT_RGB);
			
		// Calculate some reused values
		//   Center offsets, for translating to each pixel's center
		final double xco = (wr - wl) / (2 * sdx);
		final double yco = (wt - wb) / (2 * sdy);
			
		for (int sx = 0; sx < sdx; ++sx) {

			// Calculate position of this column in world
			final double wx = wl + xco * (2 * sx + 1);

			for (int sy = 0; sy < sdy; ++sy) {
					
				// Calculate position of this row in world
				final double wy = wt - yco * (2 * sy + 1);
					
				// Initialize values of c, represented here as zx + zy*i
				double zx = wx;
				double zy = wy;
					
				// Squares of zx and zy, so they aren't computed twice
				double zx2 = zx * zx;
				double zy2 = zy * zy;
					
				int n = 0;
				while (n < limit && zx2 + zy2 < 4) {
						
					// z <- z^2 + c, where z is the complex number x+y*i
					zy = 2 * zx * zy + wy;
					zx = zx2 - zy2 + wx;
						
					// Update squares
					zx2 = zx * zx;
					zy2 = zy * zy;
						
					// Update counter
					++n;
						
				}
					
				// Save results for pixel to buffer
				if (n == limit || n == 0) {
					output.setRGB(sx, sy, 0);
				} else {
					output.setRGB(sx, sy, 0xffffff / limit * n);
				}
										
			}

			// Check our status so the thread can switch tasks quickly
			synchronized (this) {
				if (synchronizedTask != task) {
					// A new task; break out
					return; 
				}
			}
				
			// Notify client
			fireRenderingEvent(new RenderingEvent(this, task, 
					(sx + 1) / (float) sdx, false));
				
		}
				
		task.setImage(output);
			
		// Notify client
		fireRenderingEvent(new RenderingEvent(this, task, 1.0f, true));
		
	}
		
	private void fireRenderingEvent(RenderingEvent e) {
		if (fireEventsOnAWT) {
			final RenderingEvent fe = e;
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					listener.renderingProgress(fe);
				}
			});
		} else {
			listener.renderingProgress(e);
		}
	}
	
}
