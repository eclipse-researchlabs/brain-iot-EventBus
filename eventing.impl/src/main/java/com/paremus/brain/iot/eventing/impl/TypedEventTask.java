package com.paremus.brain.iot.eventing.impl;

import static org.osgi.util.converter.Converters.standardConverter;

import java.util.Map;

import eu.brain.iot.eventing.api.BrainIoTEvent;
import eu.brain.iot.eventing.api.SmartBehaviour;

class TypedEventTask extends EventTask {
	private final String eventType;
	private final Class<?> eventClassOfLastResort;
	private final Map<String, Object> eventProps;
	private final SmartBehaviour<BrainIoTEvent> eventProcessor;
	
	public TypedEventTask(String eventType, Class<?> eventClassOfLastResort, Map<String, Object> eventProps,
			SmartBehaviour<BrainIoTEvent> eventProcessor) {
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
			targetEventClass = eventClassOfLastResort;
		}
		
		if(BrainIoTEvent.class.isAssignableFrom(targetEventClass)) {
			// All good
			
			BrainIoTEvent event = (BrainIoTEvent) standardConverter()
					.convert(eventProps).targetAsDTO().to(targetEventClass);
			
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