/*
 * MandelbrotPanel.java
 * cs450
 *
 * Created Jun 5, 2004
 *
 * Copyright (c) 2004 Matthew Nelson. All Rights Reserved.
 * See LICENSE for the full notice.
 */

package edu.calstatela.mandelbrot_set;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.Stack;

/**
 * <p><b>All methods of this class must execute on the AWT Event Dispatch
 * thread, or serious collisions may occur.</b></p>
 * 
 * @author Matthew Nelson
 */
public class MandelbrotPanel extends JPanel {

	/**
	 * A convenience methods for running a MandelbrotPanel in a JFrame as
	 * a demo.
	 */
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				initFrame();
			}
		});
	}

	/**
	 * Initializes a <code>JFrame</code> containing a
	 * <code>MandelbrotPanel</code>.
	 */
	private static void initFrame() {
		JFrame frame = new JFrame(Messages.getString(
				"MandelbrotPanel.frame_title")); //$NON-NLS-1$
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().add(
				new MandelbrotPanel(true, 5000), BorderLayout.CENTER);
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}
	
	/*
	 * The following variables store references to GUI components.
	 */
	
	private final ImageSelectionComponent imageComponent;
	private final JProgressBar progressBar;
	private final JLabel progressLabel;
	private final JPanel blackPanel;
	private final JButton cancelButton;
		
	/*
	 * The following variables store the viewport's state, including a cache
	 * of images.
	 */
	
	private Stack frameStack = new Stack();
	private Scene discardedFrame = null;
	private boolean cancelable = false;
	
	private Rectangle lastKnownBounds = getBounds();

	/**
	 * The background rendering daemon thread.
	 */
	private final RenderingDaemon renderer;
	
	/**
	 * Creates a new <code>MandelbrotPanel</code>.
	 * 
	 * @param scaling true if the previously rendered image should scale
	 *          while a new image is rendered
	 * @param limit the maximum number of iterations before concluding that
	 *          a given point is in the Mandelbrot set
	 */
	public MandelbrotPanel(boolean scaling, int limit) {
		
		setLayout(new BorderLayout());
		setOpaque(true);
		setPreferredSize(new Dimension(300, 300));

		// Progress bar
		progressBar = new JProgressBar(0, 100);
		progressBar.setValue(0);
		progressBar.setMaximumSize(new Dimension(100, 10));
		progressBar.setAlignmentX(Component.CENTER_ALIGNMENT);

		// Progress message
		progressLabel = new JLabel(Messages.getString(
				"MandelbrotPanel.rendering_message"), JLabel.CENTER); //$NON-NLS-1$
		progressLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		progressLabel.setForeground(Color.white);
		
		// Cancel button
		cancelButton = new JButton(Messages.getString(
				"MandelbrotPanel.cancel_button")); //$NON-NLS-1$
		cancelButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		cancelButton.addActionListener(cancelListener);
		
		// Black panel containing progress bar, lable, and button
		blackPanel = new JPanel();
		// VERY cool alpha effect, but sometimes has glitches
		// blackPanel.setBackground(new Color(0, 0, 0, 128));
		blackPanel.setBackground(Color.black);
		blackPanel.setLayout(new BoxLayout(blackPanel, BoxLayout.Y_AXIS));
		blackPanel.add(progressLabel);
		blackPanel.add(Box.createVerticalStrut(5));
		blackPanel.add(progressBar);
		blackPanel.add(Box.createVerticalStrut(5));
		blackPanel.add(cancelButton);
		blackPanel.setMaximumSize(blackPanel.getPreferredSize());
		blackPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));

		// Image panel with the progress bar
		imageComponent = new ImageSelectionComponent(scaling);
		imageComponent.setLayout(
				new BoxLayout(imageComponent, BoxLayout.Y_AXIS));
		imageComponent.add(Box.createVerticalGlue());
		imageComponent.add(blackPanel);
		imageComponent.add(Box.createVerticalGlue());
		imageComponent.addMouseListener(zoomOutListener);
		imageComponent.addSelectionListener(zoomInListener);
		imageComponent.addKeyListener(keyListener);

		// This JPanel		
		add(imageComponent, BorderLayout.CENTER);
		
		// Start up renderer
		renderer = new RenderingDaemon(renderingListener, true);
		// Slightly incrase GUI responsiveness by decreasing daemon priority
		renderer.setPriority(Thread.MIN_PRIORITY);
		renderer.start();

		// Push initial view onto stack
		frameStack.push(new Scene(1, 1, limit, 2, -2, -2, 2));
		doRenderCheck();
		
	}
	
	/**
	 * Listens for signals from rendering thread.
	 */
	private final RenderingListener renderingListener
			= new RenderingListener() {

		public void renderingProgress(RenderingEvent e) {
			if (e.getTask() == frameStack.peek()) {
				if (e.isComplete()){
					// Display completed image
					doRenderCheck();			
				} else {
					//  Update progress bar
					progressBar.setValue(
							Math.round(e.getCompletedRatio() * 100));
				}
			}
		}
		
	};

	/**
	 * Listens for signals from selection panel.
	 */
	private final SelectionEventListener zoomInListener
			= new SelectionEventListener() {

		public void selectionMade(SelectionEvent e) {
			Rectangle s = e.getSelection();
			doZoomIn(s);
		}
		
	};
	
	/**
	 * Listens for right-clicks in image, causing view to zoom out.
	 */
	private final MouseListener zoomOutListener = new MouseListener() {
		public void mouseEntered(MouseEvent e) {}
		public void mouseExited(MouseEvent e) {}
		public void mousePressed(MouseEvent e) {}			
		public void mouseReleased(MouseEvent e) {}
				
		public void mouseClicked(MouseEvent e) {
			Scene frame = (Scene) frameStack.peek();

			if (e.getButton() != MouseEvent.BUTTON3
					|| frame.getImage() == null) {
				return;
			}

			doZoomOut();			

		}
	};
	
	/**
	 * Listens for key events, causing view to zoom out or reset.
	 */
	private final KeyListener keyListener = new KeyListener() {
		public void keyPressed(KeyEvent e) {}
		public void keyTyped(KeyEvent e) {}
		
		public void keyReleased(KeyEvent e) {
			Scene frame = (Scene) frameStack.peek();

			if (frame.getImage() == null) {
				return;
			}
			
			switch (e.getKeyCode()) {
				
				case KeyEvent.VK_BACK_SPACE:
				case KeyEvent.VK_O:
					doZoomOut();
					break;
				
				case KeyEvent.VK_R:    // Reset
				case KeyEvent.VK_HOME: // Home
				case KeyEvent.VK_H:
					doReset();
					break;
				
			}

			
		}
		
	};
	
	/**
	 * Listens for user actions on the cancel button. When an event is
	 * detected, the current rendering job is cancelled if possible.
	 */
	private final ActionListener cancelListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			doCancel();			
		}
	};

	/**
	 * Creates a new stack frame and causes the view to zoom in and enlarge
	 * the selection.
	 * 
	 * @param s a rectangular subset of screen pixels
	 */
	private void doZoomIn(Rectangle s) {
		Scene task = (Scene) frameStack.peek();
			
		// Compute new view
		double wt = task.getSeeTop();
		double wb = task.getSeeBottom();
		double wl = task.getSeeLeft();
		double wr = task.getSeeRight();
		double wdx = wr - wl;
		double wdy = wt - wb;
		int sdy = task.getVerticalResolution();
		int sdx = task.getHorizontalResolution();
		int limit = task.getLimit();
		discardedFrame = null;
		frameStack.push(new Scene(sdx, sdy, limit,
				wt - s.y * wdy / sdy,
				wt - (s.y + s.height) * wdy / sdy,
				wl + s.x * wdx / sdx,
				wl + (s.x + s.width) * wdx / sdx));
			
		// Zoom in
		cancelable = true;
		imageComponent.clearSelection();
		imageComponent.setVisible(s);
		progressLabel.setText(Messages.getString(
				"MandelbrotPanel.zooming_in_message")); //$NON-NLS-1$
		doRenderCheck();
			
		// Allow GC on unused image if the VM runs out of memory
		task.allowImageGC();

	}

	/**
	 * Discards a frame and causes the view to zoom out. This action is
	 * cancelable by the uesr.
	 */
	private void doZoomOut() {

		if (frameStack.size() < 2) {
			return;
		}

		// Zoom out
		discardedFrame = (Scene) frameStack.pop();
		cancelable = true;
		progressLabel.setText(Messages.getString("MandelbrotPanel.zooming_out_message")); //$NON-NLS-1$
		doRenderCheck();

		// Allow GC on unused image if the VM runs out of memory
		discardedFrame.allowImageGC();

	}
	
	/**
	 * Discards all frames but the top frame. This action is not cancelable.
	 */
	private void doReset() {
		
		frameStack.setSize(1);
		cancelable = false;
		progressLabel.setText(Messages.getString("MandelbrotPanel.reseting_message")); //$NON-NLS-1$
		doRenderCheck();
		
	}
	
	/**
	 * Discards the rendering frame and restores the last one visible.
	 */
	private void doCancel() {
		
		if (!cancelable) {
			// Should be redundant
			return;
		}

		if (discardedFrame == null) {
			frameStack.pop();
			((Scene) frameStack.peek()).disallowImageGC();
		} else {
			discardedFrame.disallowImageGC();
			frameStack.push(discardedFrame);
			discardedFrame = null;
		}
			
		renderer.abortRendering();
		imageComponent.clearSelection();
		cancelable = false;
		progressLabel.setText(Messages.getString("MandelbrotPanel.canceling_message")); //$NON-NLS-1$
		doRenderCheck();	
		
	}
	
	/**
	 * Updates GUI components and begins rendering if needed. Generally,
	 * rendering restarts whenever the rendering thread has not been notified
	 * of the current task, or when the window size changes. Aborts rendering
	 * if the panel's size is (0, 0). Finally, displays the available image.
	 */
	private void doRenderCheck() {
		
		// Read currently displayed frame from stack
		Scene task = (Scene) frameStack.peek();

		// Update rendering thread
		if (    // If no image is available ...
				   task.getImage() == null
				// ... and the rendering thread is not up-to-date ...
				&& renderer.getTask() != task
				// .. and the rendering would be worthwhile ...
				&& getWidth() != 0 && getHeight() != 0) {
			
			// We seem to be rendering a new image, so set up GUI components
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			imageComponent.setLocked(true);
			progressBar.setValue(0);
			blackPanel.setVisible(true);
			cancelButton.setVisible(cancelable);
			
			// Notify background thread
			renderer.beginRendering(task);

		// Otherwise, we may have a new image availabe
		} else if (task.getImage() != null) {

			// Clear cancelable flag
			cancelable = false;

			// We don't seem to be rendering, so put away GUI components
			setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
			imageComponent.clearSelection();
			imageComponent.setLocked(false);
			blackPanel.setVisible(false);
			progressLabel.setText(Messages.getString("MandelbrotPanel.scaling_message")); //$NON-NLS-1$
		
			// Flush image to panel, computing the visible rectangle
			Rectangle visible = getVisible(task);
			if (imageComponent.getImage() != task.getImage()) {
				imageComponent.setImage(task.getImage(), visible);
			} else if (!imageComponent.getVisible().equals(visible)) {
				imageComponent.setVisible(visible);
			}

		}
	}
	
	/**
	 * Forces re-rendering when panel size changes.  
	 */
	public void invalidate() {
		super.invalidate();

		// Replace topmost frame if resolution has changed	
		if (!lastKnownBounds.equals(getBounds())) {

			// Clear all cached images			
			for (int i = 0; i < frameStack.size(); ++i) {
				Scene task = (Scene) frameStack.get(i);
				frameStack.set(i, new Scene(
						Math.max(1, getWidth()),
						Math.max(1, getHeight()),
						task.getLimit(),
						task.getLookTop(), task.getLookBottom(),
						task.getLookLeft(), task.getLookRight()));
			}
			if (discardedFrame != null) {
				discardedFrame = new Scene(
						Math.max(1, getWidth()),
						Math.max(1, getHeight()),
						discardedFrame.getLimit(),
						discardedFrame.getLookTop(),
						discardedFrame.getLookBottom(),
						discardedFrame.getLookLeft(),
						discardedFrame.getLookRight());
			}
			
			lastKnownBounds = getBounds();
			
			System.gc();

			// Rendering may be required
			doRenderCheck();
			
		}

	}
	
	/**
	 * Computes the visible rectangle in a scene. Useful for passing
	 * {@link ImageSelectionComponent#setImage(Image,Rectangle)} the correct
	 * arguments.
	 * 
	 * @param task the task from which to read data
	 * @return a rectangle indicating which portion of the image the uesr
	 *         is interested in viewing
	 */
	private static Rectangle getVisible(Scene task) {
		Image image = task.getImage();
		int iw = image.getWidth(null);
		int ih = image.getHeight(null);
		double st = task.getSeeTop();
		double sb = task.getSeeBottom();
		double sl = task.getSeeLeft();
		double sr = task.getSeeRight();
		double lt = task.getLookTop();
		double lb = task.getLookBottom();
		double ll = task.getLookLeft();
		double lr = task.getLookRight();
		return new Rectangle(
				(int) Math.round((ll * iw - sl * iw) / (sr - sl)),
				(int) Math.round((st * ih - lt * ih) / (st - sb)),
				(int) Math.round((lr * iw - ll * iw) / (sr - sl)),
				(int) Math.round((lt * ih - lb * ih) / (st - sb)));
	}
	
}
