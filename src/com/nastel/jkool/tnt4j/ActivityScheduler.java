/*
 * Copyright 2014-2015 JKOOL, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.nastel.jkool.tnt4j;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import com.nastel.jkool.tnt4j.config.DefaultConfigFactory;
import com.nastel.jkool.tnt4j.config.TrackerConfig;
import com.nastel.jkool.tnt4j.core.ActivityListener;
import com.nastel.jkool.tnt4j.core.OpLevel;
import com.nastel.jkool.tnt4j.tracker.TrackingActivity;

/**
 * <p> 
 * This class allows scheduled execution of tracking activities
 * based on user defined interval.
 * </p>
 * 
 * 
 * @version $Revision: 1 $
 * 
 * @see TrackingActivity
 * @see ActivityListener
 */
public class ActivityScheduler {
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2, new TaskThreadFactory("ActivityScheduler/task"));
	
	private String name;
	private TrackingLogger logger;
	private ScheduledFuture<?> future;
	private Runnable activityTask;

	/**
	 * Creates a scheduler with specified name
	 * 
	 * @param name scheduler name
	 */
	public ActivityScheduler(String name) {
		this(name, null);
	}

	/**
	 * Creates a scheduler with specified name
	 * 
	 * @param name scheduler name
	 * @param listener activity listener invoked when scheduled activity starts and stops
	 * 
	 * @see ActivityListener
	 */
	public ActivityScheduler(String name, ActivityListener listener) {
		this.name = name;
		TrackerConfig config = DefaultConfigFactory.getInstance().getConfig(name);
		if (listener != null) config.setActivityListener(listener);
		this.logger = TrackingLogger.getInstance(config.build());
		this.logger.setKeepThreadContext(false);
	}
	
	/**
	 * Name associated with this object
	 * 
	 * @return object name
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Assign an activity listener to be used when scheduled activity starts/stops
	 * 
	 * @param listener activity listener invoked when scheduled activity starts and stops
	 * @return instance of the same scheduler
	 * @see ActivityListener
	 */
	public ActivityScheduler setListener(ActivityListener listener) {
		logger.getConfiguration().setActivityListener(listener);
		return this;
	}
	
	/**
	 * Get an activity listener used when scheduled activity starts/stops
	 * 
	 * @return return currently associated activity listener
	 * @see ActivityListener
	 */
	public ActivityListener getListener() {
		return logger.getConfiguration().getActivityListener();
	}
	
	/**
	 * Schedule activity with a specified period in milliseconds
	 * 
	 * @param period in milliseconds
	 */
	public void schedule(long period) {
		schedule("ActivityTask", period, period, TimeUnit.MILLISECONDS);
	}
	
	/**
	 * Schedule activity with a specified period in milliseconds
	 * 
	 * @param name activity name
	 * @param period in milliseconds
	 */
	public void schedule(String name, long period) {
		schedule(name, period, period, TimeUnit.MILLISECONDS);
	}
	
	/**
	 * Schedule activity with a given name and timing details
	 * 
	 * @param name activity name
	 * @param period in specified time units
	 * @param tunit time unit for period
	 */
	public void schedule(String name, long period, TimeUnit tunit) {
		schedule(name, period, period, TimeUnit.MILLISECONDS);
	}
	
	/**
	 * Schedule activity with a given name and timing details
	 * 
	 * @param name activity name
	 * @param initialDelay in specified time units
	 * @param period in specified time units
	 * @param tunit time unit for period
	 */
	public void schedule(String name, long initialDelay, long period, TimeUnit tunit) {
		schedule(name, period, period, tunit, OpLevel.SUCCESS);
	}

	/**
	 * Schedule activity with a given name and timing details
	 * 
	 * @param name activity name
	 * @param level severity level
	 * @param initialDelay in specified time units
	 * @param period in specified time units
	 * @param tunit time unit for period
	 */
	public void schedule(String name, long initialDelay, long period, TimeUnit tunit,  OpLevel level) {
		if (future == null || future.isCancelled()) {
			activityTask = newActivityTask(logger, name, level);
			future = scheduler.scheduleAtFixedRate(activityTask, initialDelay, period, tunit);
		} else {
			throw new IllegalStateException("Already scheduled");
		}
	}
		
	/**
	 * Cancel currently scheduled activity
	 * 
	 */
	public void cancel() {
		cancel(false);
	}
	
	/**
	 * Cancel currently scheduled activity
	 * 
	 * @param interrupt may interrupt currently running activity
	 */
	public void cancel (boolean interrupt) {
		future.cancel(interrupt);
	}
	
	
	/**
	 * Open current scheduled activity instance.
	 * @throws IOException 
	 * 
	 */
	public void open() throws IOException {
		logger.open();
	}

	/**
	 * Close current scheduled activity instance.
	 * 
	 */
	public void close() {
		try { 
			cancel(true);
			if (future != null) {
				future.get(); 
			}
		}
		catch (Throwable ex) {}
		finally {
			logger.close();
		}
	}
	
	/**
	 * Obtain <code>TrackingLogger</code> instance for logging
	 * 
	 * @return tracking logger instance
	 */
	public TrackingLogger getLogger() {
		return this.logger;
	}
	
	/**
	 * Override this calls to return custom instances of
	 * <code>Runnable</code> which will be invoked per specified
	 * schedule.
	 * @param lg tracking logger instance
	 * @param name of the new activity task
	 * @param level associated with the task
	 * @return <code>Runnable</code> instance
	 */
	protected Runnable newActivityTask(TrackingLogger lg, String name, OpLevel level) {
		return new ActivityTask(logger, name, level);
	}
}

class TaskThreadFactory implements ThreadFactory {
	int count = 0;
	String prefix;
	
	TaskThreadFactory(String pfix) {
		prefix = pfix;
	}
	
	@Override
    public Thread newThread(Runnable r) {
		Thread task = new Thread(r, prefix + "-" + count++);
		task.setDaemon(true);
		return task;
    }	
}