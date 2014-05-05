/*
 * Scene.java
 * cs450
 *
 * Created Jun 5, 2004
 *
 * Copyright (c) 2004 Matthew Nelson. All Rights Reserved.
 * See LICENSE for the full notice.
 */

package edu.calstatela.mandelbrot_set;

import java.awt.Image;
import java.lang.ref.SoftReference;

/**
 * @author Matthew Nelson
 */
class Scene {

	/*
	 * The following variables are used by the RenderingDaemon. They describe
	 * the actual world window to be rendered.
	 */

	private final double top;
	private final double bottom;
	private final double left;
	private final double right;

	/*
	 * The following variables serve two purposes. The RenderingDaemon uses
	 * them to scale the output image, and the MandelbrotPanel useses them
	 * to determine if window resolution has changed.
	 */

	private final int hRes;
	private final int vRes;
	private final int limit;
	
	/*
	 * The following variables are used by MandelbrotPanel for computing
	 * frames derived from this one.
	 */
	
	private final double lookTop;
	private final double lookBottom;
	private final double lookLeft;
	private final double lookRight;
	
	/*
	 * The following variable stores the finalized image when available.
	 */

	private Image output = null;
	private SoftReference outputReference = null;
	
	/**
	 * Constructs a new rendering job.
	 * 
	 * @param top the upper world coordinate
	 * @param bottom the lower world coordinate
	 * @param left the left world coordinate
	 * @param right the right world coordinate
	 * @param hRes the number of pixels spanning horizontally
	 * @param vRes the number of pixels spanning vertically
	 * @throws IllegalArgumentException if any resolution is nonpositive
	 */
	public Scene(int hRes, int vRes, int limit, double lookTop,
			double lookBottom, double lookLeft, double lookRight) {
		
		if (hRes <= 0 || vRes <= 0 || limit <= 0) {
			throw new IllegalArgumentException("nonpositive resolution");
		}
		
		this.lookTop = lookTop;
		this.lookBottom = lookBottom;
		this.lookLeft = lookLeft;
		this.lookRight = lookRight;
		this.hRes = hRes;
		this.vRes = vRes;
		this.limit = limit;

		// Preserve aspect ratio on screen by expanding world window as needed,
		// assuming an individual screen pixel has an aspect ratio of 1
		double wdx = lookRight - lookLeft;
		double wdy = lookTop - lookBottom;
		double wa = wdx / wdy;
		double sa = hRes / (double) vRes;
		if (wa < sa) {
			double d = (wdy * sa - wdx) / 2;
			this.top = lookTop;
			this.bottom = lookBottom;
			this.left = lookLeft - d;
			this.right = lookRight + d;
		} else {
			double d = (wdx / sa - wdy) / 2;
			this.top = lookTop + d;
			this.bottom = lookBottom - d;
			this.left = lookLeft;
			this.right = lookRight;
		}
				
	}
	
	public synchronized void setImage(Image output) {
		if (output == null) {
			throw new NullPointerException();
		}
		this.output = output;
		this.outputReference = null;
	}
	
	public synchronized Image getImage() {
		if (outputReference != null) {
			Object reference = outputReference.get();
			return (reference == null) ? null : (Image) reference;
		} else {
			return output;
		}
	}
	
	public synchronized void allowImageGC() {
		if (output != null && outputReference == null) {
			outputReference = new SoftReference(output);
			output = null; 
		}
	}
	
	public synchronized void disallowImageGC() {
		if (outputReference != null) {
			Object reference = outputReference.get();
			output = (reference == null) ? null : (Image) reference;
			outputReference = null;
		}
	}

	public double getSeeTop() {
		return top;
	}
	
	public double getSeeBottom() {
		return bottom;
	}
	
	public int getHorizontalResolution() {
		return hRes;
	}
	
	public double getSeeLeft() {
		return left;
	}
	
	public double getSeeRight() {
		return right;
	}
	
	public int getVerticalResolution() {
		return vRes;
	}
	
	public int getLimit() {
		return limit;
	}
	
	public double getLookTop() {
		return lookTop;
	}
	
	public double getLookBottom() {
		return lookBottom;
	}
	
	public double getLookLeft() {
		return lookLeft;
	}
	
	public double getLookRight() {
		return lookRight;
	}

}
