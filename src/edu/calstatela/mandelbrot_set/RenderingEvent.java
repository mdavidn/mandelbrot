/*
 * RenderingEvent.java
 * cs450
 *
 * Created Jun 6, 2004
 *
 * Copyright (c) 2004 Matthew Nelson. All Rights Reserved.
 * See LICENSE for the full notice.
 */

package edu.calstatela.mandelbrot_set;

import java.util.EventObject;

/**
 * @author Matthew Nelson
 */
public class RenderingEvent extends EventObject {

	private final Scene task;
	private final float completed;
	private final boolean isComplete; 

	public RenderingEvent(Object source, Scene task,
			float completedRatio, boolean isComplete) {
		super (source);
		
		if (task == null) {
			throw new NullPointerException();
		}
		
		this.task = task;
		this.completed = completedRatio;
		this.isComplete = isComplete;
		
	}
	
	public Scene getTask() {
		return task;
	}
	
	public float getCompletedRatio() {
		return completed;
	}
	
	public boolean isComplete() {
		return isComplete;
	}

}
