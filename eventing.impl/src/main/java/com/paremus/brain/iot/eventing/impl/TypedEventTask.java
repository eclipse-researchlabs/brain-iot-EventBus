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

import java.util.Map;

import eu.brain.iot.eventing.api.BrainIoTEvent;
import eu.brain.iot.eventing.api.SmartBehaviour;

class TypedEventTask extends EventTask {
	private final String eventType;
	private final Class<? extends BrainIoTEvent> eventClassOfLastResort;
	private final Map<String, Object> eventProps;
	private final SmartBehaviour<BrainIoTEvent> eventProcessor;
	
	public TypedEventTask(String eventType, Class<? extends BrainIoTEvent> eventClassOfLastResort, 
			Map<String, Object> eventProps, SmartBehaviour<BrainIoTEvent> eventProcessor) {
		super();
		this.eventType = eventType;
		this.eventClassOfLastResort = eventClassOfLastResort;
		this.eventProps = eventProps;
		this.eventProcessor = eventProcessor;
	}

	@Override
	public void notifyListener() {
		Class<?> targetEventClass;
		try {
			targetEventClass = eventProcessor.getClass().getClassLoader().loadClass(eventType);
		} catch (ClassNotFoundException e) {
			// TODO LOG a warning that the listener doesn't have access to the type
			e.printStackTrace();
			if(eventClassOfLastResort != null) {
				targetEventClass = eventClassOfLastResort;
			} else {
				// TODO log an error that we can't deliver the event
				return;
			}
		}
		
		if(BrainIoTEvent.class.isAssignableFrom(targetEventClass)) {
			// All good
			
			@SuppressWarnings("unchecked")
			BrainIoTEvent event = EventConverter.convert(eventProps, 
					(Class<? extends BrainIoTEvent>)targetEventClass);
			
			try {
				eventProcessor.notify(event);
			} catch (Exception e) {
				// TODO log this, also blacklist?
			}
			
		} else {
			// The target class is not a BrainIoTEvent - log and fail, possibly blacklist
		}
	}
}
