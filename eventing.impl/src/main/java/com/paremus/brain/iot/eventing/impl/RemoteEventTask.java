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
