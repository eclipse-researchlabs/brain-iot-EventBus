package com.paremus.brain.iot.eventing.impl;

import java.util.HashMap;
import java.util.Map;

import eu.brain.iot.eventing.api.UntypedSmartBehaviour;

class UntypedEventTask extends EventTask {
	private final String eventType;
	private final Map<String, Object> eventProps;
	private final UntypedSmartBehaviour eventProcessor;
	
	public UntypedEventTask(String eventType, Map<String, Object> eventProps,
			UntypedSmartBehaviour eventProcessor) {
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