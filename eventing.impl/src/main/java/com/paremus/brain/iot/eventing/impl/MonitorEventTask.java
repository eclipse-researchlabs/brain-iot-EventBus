/*******************************************************************************
 * Copyright (C) 2021 Paremus Ltd
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/

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
