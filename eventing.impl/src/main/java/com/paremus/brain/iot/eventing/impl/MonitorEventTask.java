/* Copyright 2019 Paremus, Ltd - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.paremus.brain.iot.eventing.impl;

import java.util.HashMap;
import java.util.Map;

class MonitorEventTask extends EventTask {
	
	private final String eventType;
	private final Map<String, Object> eventProps;
	private final boolean remote;
	private final EventMonitorImpl monitorImpl;
	
	public MonitorEventTask(String eventType, Map<String, Object> eventProps,
			boolean remote, EventMonitorImpl monitorImpl) {
		super();
		this.eventType = eventType;
		this.eventProps = eventProps;
		this.remote = remote;
		this.monitorImpl = monitorImpl;
	}

	@Override
	public void notifyListener() {
		try {
			monitorImpl.event(eventType, new HashMap<>(eventProps), remote);
		} catch (Exception e) {
			// TODO log this, also blacklist?
		}
	}
}