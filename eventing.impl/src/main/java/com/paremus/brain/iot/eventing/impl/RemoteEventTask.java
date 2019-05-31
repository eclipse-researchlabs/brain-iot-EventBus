/* Copyright 2019 Paremus, Ltd - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.paremus.brain.iot.eventing.impl;

import java.util.HashMap;
import java.util.Map;

import com.paremus.brain.iot.eventing.spi.remote.RemoteEventBus;

class RemoteEventTask extends EventTask {
	private final String eventType;
	private final Map<String, Object> eventProps;
	private final RemoteEventBus eventProcessor;
	
	public RemoteEventTask(String eventType, Map<String, Object> eventProps,
			RemoteEventBus eventProcessor) {
		super();
		this.eventType = eventType;
		this.eventProps = eventProps;
		this.eventProcessor = eventProcessor;
	}

	@Override
	public void notifyListener() {
		try {
			eventProcessor.notify(eventType, new HashMap<>(eventProps));
		} catch (Exception e) {
			// TODO log this, also blacklist?
		}
	}
}