/*
 * ImageSelectionComponent.java
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

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * A panel for displaying an image and allowing a user to make rectangular
 * selections. This class has largely been customized for use by
 * {@link MandelbrotPanel}. In particular, selections do not behave correctly
 * when resizing, so {@link MandelbrotPanel} guarantees that such a situation
 * will not arise.
 * 
 * @author Matthew Nelson
 */
public class ImageSelectionComponent extends JPanel {
	
	/*
	 * The following code handles the marching ants effect. In addition, the
	 * constructor creates a Timer that periodically invokes marchAnts.run(),
	 * and the paintComponent(Graphics) method picks the correct stroke.
	 */

	private static final Stroke[] ANT_STROKES = new Stroke[6];
	private int currentStroke = 0;
	private static final int ANT_DELAY = 100;

	static {
		for (int i = 0; i < ANT_STROKES.length; ++i) {
			ANT_STROKES[i] = new BasicStroke(1.0f, BasicStroke.CAP_BUTT,
					BasicStroke.JOIN_MITER, 10.0f, new float[] {3.0f}, i);
		}
	}

	private final Runnable marchAnts = new Runnable() {
		public void run() {
			if (box != null) {
				currentStroke = (currentStroke + 1) % ANT_STROKES.length;

				// Mark dirty region for repainting
				repaint(box);
			}
		}
	};
	
	/*
	 * The following member variables store the selection status and image.
	 */
	
	private Point origin = null;
	private Rectangle box = null;
	private boolean locked = false; 
	private boolean leftButtonDown = false;
	
	private boolean scaleImage;
	private Image image = null;
	private Rectangle visible;	
	
	/**
	 * Constructs a new <code>ImageSelectionComponent</code>.
	 * 
	 * @param scaling whether the panel should automatically scale the
	 *          image to maximize the <code>visible</code> rectangle
	 * @param image the image to display, or null for none
	 * @param visible the portion of the image to be maximized when scaling 
	 */
	public ImageSelectionComponent(boolean scaling,
			Image image, Rectangle visible) {

		this.scaleImage = scaling;
		setImage(image, visible);

		setOpaque(true);
		setDoubleBuffered(true);
		setBackground(Color.black);
		setForeground(Color.white);
		setFocusable(true);
		
		addMouseListener(mouseListener);
		addMouseMotionListener(mouseMotionListener);
		addKeyListener(keyListener);

		Timer marchingAntTimer = new Timer(true);
		marchingAntTimer.scheduleAtFixedRate(new TimerTask() {
			public void run() {
				SwingUtilities.invokeLater(marchAnts);
			}
		}, ANT_DELAY, ANT_DELAY);

	}

	/**
	 * Constructs a new <code>ImageSelectionComponent</code>.
	 * 
	 * @param scaling whether the panel should automatically scale the
	 *          image to maximize the <code>visible</code> rectangle 
	 */
	public ImageSelectionComponent(boolean scaling) {
		this(scaling, null, null);
	}

	/**
	 * An adapter that listens for mouse presses and releases. Is
	 * responsible for creating selections and firing events when the
	 * user has finalized a selection.
	 */
	private final MouseListener mouseListener = new MouseListener() {
		public void mouseClicked(MouseEvent e) {}
		public void mouseEntered(MouseEvent e) {}
		public void mouseExited(MouseEvent e) {}
			
		public void mousePressed(MouseEvent e) {
				
			if (locked || e.getButton() != MouseEvent.BUTTON1) {
				return;
			}
				
			// Request keyboard focus (for escape presses)
			requestFocusInWindow();
				
			// Clear any existing selection (for redrawing)
			clearSelection();
				
			// Create new selection
			origin = new Point(e.getX(), e.getY());
			box = new Rectangle(e.getX(), e.getY(), 0, 0);
			
			leftButtonDown = true;
		}
			
		public void mouseReleased(MouseEvent e) {

			if (locked || box == null || e.getButton() != MouseEvent.BUTTON1) {
				return;
			}

			if (box.width != 0 || box.height != 0) {
				fireSelectionEvent(box);
			}

			leftButtonDown = false;

		}
			
	};
		
	/**
	 * An adapter that listens for mouse motion. Is responsible
	 * for resizing the selection within the boundaries of the image.
	 */
	private final MouseMotionListener mouseMotionListener
			= new MouseMotionListener() {
		public void mouseMoved(MouseEvent e) {}
			
		public void mouseDragged(MouseEvent e) {
				
			if (locked || box == null || !leftButtonDown) {
				return;
			}
				 
			Rectangle dirtyRegion = (Rectangle) box.clone();
				
			// Compute new selection
			box.width = Math.abs(e.getX() - origin.x);
			box.height = Math.abs(e.getY() - origin.y);
			box.x = Math.min(e.getX(), origin.x);
			box.y = Math.min(e.getY(), origin.y);
			if (box.x < 0) {
				box.width = box.width + box.x;
				box.x = 0;
			}
			if (box.y < 0) {
				box.height = box.height + box.y;
				box.y = 0;
			}
			if (box.x + box.width > getWidth()) {
				box.width = getWidth() - box.x;
			}
			if (box.y + box.height > getHeight()) {
				box.height = getHeight() - box.y;
			}

			// Mark dirty region for repainting
			dirtyRegion = dirtyRegion.union(box);
			repaint(dirtyRegion);
		}
			
	};
		
	/**
	 * An adapter that listens for escape key presses and cancels
	 * any selection when one is detected.
	 */
	private final KeyListener keyListener = new KeyListener() {
		public void keyPressed(KeyEvent e) {}
		public void keyReleased(KeyEvent e) {}
			
		public void keyTyped(KeyEvent e) {
			if (!locked && e.getKeyChar() == KeyEvent.VK_ESCAPE) {
				clearSelection();
			}
		}
			
	};
	
	/**
	 * Paints the image, scaled appropriately, and any selection rectangle.
	 * It's important to note that the current implementation of this class
	 * <b>does not</b> scale the selection rectangle when the panel is resized.
	 */
	protected void paintComponent(Graphics g) {
		Graphics2D g2 = (Graphics2D) g;
		
		g2.setColor(getBackground());
		if (image == null) {
			
			// No image, so draw empty background
			g2.fillRect(0, 0, getWidth(), getHeight());
			
		} else {
			
			if (scaleImage) {
			
				// Scale and adjust image so
				// the visible portion is maximized
				int sw = getWidth();
				int sh = getHeight();
				int iw = image.getWidth(null);
				int ih = image.getHeight(null);
				float sa = sw / (float) sh;
				float va = visible.width / (float) visible.height;
				int rx, ry, rw, rh;
				if (va < sa) {
					rx = Math.round((sw - sh * va) / 2);
					ry = 0;
					rw = Math.round(sh * va);
					rh = sh;
				} else {
					rx = 0;
					ry = Math.round((sh - sw / va) / 2);
					rw = sw;
					rh = Math.round(sw / va);
				}
				int dx = rx - rh * visible.x / visible.height;
				int dy = ry - rw * visible.y / visible.width;
				int dw = rw * iw / visible.width;
				int dh = rh * ih / visible.height;
				g2.drawImage(image, dx, dy, dw, dh, getBackground(), null);
				
				// Fill any spaces with the background color 
				if (dx > 0) {
					g2.fillRect(0, 0, dx, sh);				
				}
				if (dx + dw < sw) {
					g2.fillRect(dx + dw, 0, sw - dw - dx, sh);
				}
				if (dy > 0) {
					int x = Math.max(0, dx);
					g2.fillRect(x, 0, Math.min(dw, sw - x), dy);
				}
				if (dy + dh < sh) {
					int x = Math.max(0, dx);
					g2.fillRect(x, dy + dh, Math.min(dw, sw - x), sh - dy - dh);				
				}

			} else {

				// Scaling is off, draw simple image				
				g2.fillRect(0, 0, getWidth(), getHeight());
				g2.drawImage(image, 0, 0, null);
				
			}
			
		}
		
		// Draw selection box
		if (box != null && box.width != 0 && box.height != 0) {
			Stroke originalStroke = g2.getStroke();
			g2.setStroke(ANT_STROKES[currentStroke]);
			g2.setColor(getForeground());
			g2.drawRect(box.x, box.y, box.width - 1, box.height - 1);
			g2.setStroke(originalStroke);
		}

	}
	
	/**
	 * Clears any current selection and removes the selection highlight.
	 */
	public void clearSelection() {
		if (box != null) {
			// Mark dirty region for repainting
			repaint(box);
		}
		origin = null;
		box = null;
	}
	
	/**
	 * Sets the status of the selection lock. When locked, the user is unable
	 * to make any selection changes.
	 * 
	 * @param b the new locked status
	 */
	public void setLocked(boolean b) {
		locked = b;
	}
	
	/**
	 * Returns the new current locked status.
	 * 
	 * @return true if the user is disallowed from making selection changes
	 */
	public boolean isLocked() {
		return locked;
	}
	
	/**
	 * Returns the current user selection.
	 * 
	 * @return a <code>Rectangle</code> selected by the user
	 */
	public Rectangle getSelection() {
		return box;
	}

	/**
	 * Sets the image to display.
	 * 
	 * @param i the new image to display, or null for none
	 * @paream visible if scaling is enabled, this portion of the image will
	 *           be scaled to fit the panel
	 * @throw NullPointerException if <code>visible</code> is null, but
	 *           <code>i</code> is not 
	 */
	public void setImage(Image i, Rectangle visible) {
		if (i != null && visible == null) {
			throw new NullPointerException();
		}
		image = i;
		this.visible = visible;
		repaint();
	}
	
	/**
	 * Sets the visible portion of the image to display.
	 * 
	 * @paream visible if scaling is enabled, this portion of the image will
	 *           be scaled to fit the panel
	 * @throw NullPointerException if <code>visible</code> is null, but
	 *           <code>getImage()</code> is not 
	 */
	public void setVisible(Rectangle visible) {
		if (image != null && visible == null) {
			throw new NullPointerException();
		}
		this.visible = visible;
		repaint();
	}
	
	/**
	 * Returns the current image.
	 * 
	 * @return the image last set with {@link setImage(Image, Rectangle)}
	 */
	public Image getImage() {
		return image;
	}

	/**
	 * Returns the visibile portion of the current image.
	 * 
	 * @return the rectangle last set with {@link setImage(Image, Rectangle)}
	 */
	public Rectangle getVisible() {
		return visible;
	}
	
	/**
	 * Returns the status of the image scaling feature.
	 * 
	 * @return true if images are scaled
	 */
	public boolean isScaling() {
		return scaleImage;
	}

	/**
	 * Turns the image scaling feature on or off.
	 * 
	 * @param scaling whether the panel should automatically scale the
	 *          image to maximize the <code>visible</code> rectangle
	 */	
	public void setScaling(boolean scaling) {
		this.scaleImage = scaling;
	}
	
	/*
	 * The following code manages this object's selection listeners.
	 */
	
	private LinkedList selectionEventListeners = new LinkedList();
	
	public synchronized void addSelectionListener(SelectionEventListener l) {
		selectionEventListeners.add(l);
	}
	
	public synchronized void removeSelectionListener(SelectionEventListener l) {
		selectionEventListeners.remove(l);
	}
	
	private void fireSelectionEvent(Rectangle selection) {
		SelectionEvent e = new SelectionEvent(this, selection);
		Iterator i;
		synchronized(this) {
			i = ((LinkedList) selectionEventListeners.clone()).iterator();
		}
		while (i.hasNext()) {
			((SelectionEventListener) i.next()).selectionMade(e);
		}		
	}
	
}
