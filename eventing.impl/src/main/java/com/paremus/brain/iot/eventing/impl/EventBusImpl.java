/* Copyright 2019 Paremus, Ltd - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.paremus.brain.iot.eventing.impl;

import static org.osgi.util.converter.Converters.standardConverter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.util.converter.TypeReference;

import eu.brain.iot.eventing.annotation.SmartBehaviourDefinition;
import eu.brain.iot.eventing.api.BrainIoTEvent;
import eu.brain.iot.eventing.api.EventBus;
import eu.brain.iot.eventing.api.SmartBehaviour;

@Component
public class EventBusImpl implements EventBus {
	
	private static final TypeReference<List<String>> LIST_OF_STRINGS = 
			new TypeReference<List<String>>() {};
	private static final TypeReference<Map<String, Object>> MAP_WITH_STRING_KEYS = 
			new TypeReference<Map<String, Object>>() {};
	
			
	private final Object lock = new Object();
	
	/**
	 * Map access and mutation must be synchronized on {@link #lock}.
	 * Values from the map should be copied as the contents are not thread safe.
	 */
	private final Map<String, Map<SmartBehaviour<BrainIoTEvent>, Filter>> consumedToMaps = 
			new HashMap<>();

	/**
	 * Map access and mutation must be synchronized on {@link #lock}.
	 * Values from the map should be copied as the contents are not thread safe.
	 */
	private final Map<Long, List<String>> knownBehaviours = new HashMap<>();
	
	private final BlockingQueue<Event> queue = new LinkedBlockingQueue<>();
	
	/**
	 * 
	 * Field access must be synchronized on {@link #threadLock}
	 */
	private EventThread thread;

	private final Object threadLock = new Object();

	@Reference(cardinality=ReferenceCardinality.MULTIPLE, policy=ReferencePolicy.DYNAMIC)
	void addSmartBehaviour(SmartBehaviour<BrainIoTEvent> behaviour, Map<String, Object> properties) {
		Object consumed = properties.get(SmartBehaviourDefinition.PREFIX_ + "consumed");
		
		if(consumed == null) {
			//TODO log a broken behaviour
			return;
		}
		
		List<String> list = standardConverter().convert(consumed).to(LIST_OF_STRINGS);
		
		Long serviceId = getServiceId(properties);
		
		Filter f;
		try {
			f = getFilter(properties);
		} catch (InvalidSyntaxException e) {
			// TODO Log a broken behaviour
			e.printStackTrace();
			return;
		}
		
		doAddSmartBehaviour(behaviour, f, list, serviceId);
	}

	private void doAddSmartBehaviour(SmartBehaviour<BrainIoTEvent> behaviour, Filter filter, 
			List<String> list, Long serviceId) {
		synchronized (lock) {
			knownBehaviours.put(serviceId, list);
			
			list.forEach(s -> {
				Map<SmartBehaviour<BrainIoTEvent>, Filter> behaviours = 
						consumedToMaps.computeIfAbsent(s, x -> new HashMap<>());
				behaviours.put(behaviour, filter);
			});
		}
	}
	
	void removeSmartBehaviour(SmartBehaviour<BrainIoTEvent> behaviour, Map<String, Object> properties) {
        
		Long serviceId = getServiceId(properties);
		
		doRemoveSmartBehaviour(behaviour, serviceId);
	}

	private Long getServiceId(Map<String, Object> properties) {
		return standardConverter().convert(properties.get(Constants.SERVICE_ID)).to(Long.class);
	}

	private Filter getFilter(Map<String, Object> properties) throws InvalidSyntaxException {
		Object o = properties.get(SmartBehaviourDefinition.PREFIX_ + "filter");
		
		if(o == null) {
			return null;
		} else {
			return FrameworkUtil.createFilter(String.valueOf(o));
		}
	}

	private void doRemoveSmartBehaviour(SmartBehaviour<BrainIoTEvent> behaviour, Long serviceId) {
		synchronized (lock) {
			List<String> consumed = knownBehaviours.remove(serviceId);
			if(consumed != null) {
				consumed.forEach(s -> {
					Map<SmartBehaviour<BrainIoTEvent>, Filter> behaviours = consumedToMaps.get(s);
					if(behaviours != null) {
						behaviours.remove(behaviour);
						if(behaviours.isEmpty()) {
							consumedToMaps.remove(s);
						}
					}
				});
			}
		}
	}
	
	void updatedSmartBehaviour(SmartBehaviour<BrainIoTEvent> behaviour, Map<String, Object> properties) {
		Object consumed = properties.get(SmartBehaviourDefinition.PREFIX_ + "consumed");
		
		if(consumed == null) {
			consumed = Collections.emptyList();
		}
		
		List<String> list = standardConverter().convert(consumed).to(LIST_OF_STRINGS);
		
		Long serviceId = getServiceId(properties);
		
		Filter f;
		try {
			f = getFilter(properties);
		} catch (InvalidSyntaxException e) {
			// TODO Log a broken behaviour
			e.printStackTrace();
			return;
		}
		
		synchronized (lock) {
			doRemoveSmartBehaviour(behaviour, serviceId);
			doAddSmartBehaviour(behaviour, f, list, serviceId);
		}
	}
	
	@Activate
	void start() {
		EventThread thread = new EventThread();
		
		synchronized (threadLock) {
			this.thread = thread;
		}
		
		thread.start();
	}
	
	@Deactivate
	void stop() {
		EventThread thread;
		
		synchronized (threadLock) {
			thread = this.thread;
			this.thread = null;
		}
		
		thread.shutdown();
		
		try {
			thread.join(2000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	public void deliver(BrainIoTEvent event) {
		
		autoPopulateEventData(event);
		
		String eventName = event.getClass().getName();
		
		Map<String, Object> map = standardConverter().convert(event).sourceAsDTO().to(MAP_WITH_STRING_KEYS);
		
		List<SmartBehaviour<BrainIoTEvent>> behaviours;
		
		synchronized (lock) {
			Map<SmartBehaviour<BrainIoTEvent>, Filter> tmp = consumedToMaps.get(eventName);
			if(tmp == null) {
				behaviours = new ArrayList<>();
			} else {
				behaviours = tmp.entrySet().stream()
						.filter(e -> e.getValue() == null || e.getValue().matches(map))
						.map(Entry::getKey)
						.collect(Collectors.toList());
			}
		}
		
		if(behaviours.isEmpty()) {
			// TODO log that nobody was listening and call the listener of last resort
			System.out.println("Listener of last resort is not yet implemented");
		} else {
			behaviours.forEach(sb -> queue.add(new Event(eventName, event.getClass(), map, sb)));
		}
		
		
	}

	private void autoPopulateEventData(BrainIoTEvent event) {
		if(event.timestamp == null) {
			event.timestamp = Instant.now();
			event.securityToken = null;
		}
		
		if(event.sourceNode == null) {
			// TODO add the source node
			event.securityToken = null;
		}
		
		if(event.securityToken == null) {
			// TODO apply proper security
			event.securityToken = new byte[] {1,2,3};
		}
	}
	
	private class EventThread extends Thread {

		private final AtomicBoolean running = new AtomicBoolean(true);
		
		public EventThread() {
			super("BRAIN-IoT EventBus Delivery Thread");
		}

		public void shutdown() {
			running.set(false);
			interrupt();
		}
		
		public void run() {
			
			
			while(running.get()) {
				
				Event take;
				try {
					take = queue.take();
				} catch (InterruptedException e) {
					// TODO log the interrupt and continue
					e.printStackTrace();
					continue;
				}
				
				Class<?> targetEventClass;
				try {
					targetEventClass = take.eventProcessor.getClass().getClassLoader().loadClass(take.eventType);
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					targetEventClass = take.eventClassOfLastResort;
				}
				
				if(BrainIoTEvent.class.isAssignableFrom(targetEventClass)) {
					// All good
					
					BrainIoTEvent event = (BrainIoTEvent) standardConverter()
							.convert(take.eventProps).targetAsDTO().to(targetEventClass);
					
					try {
						take.eventProcessor.notify(event);
					} catch (Exception e) {
						// TODO log this, also blacklist?
					}
					
				} else {
					// The target class is not a BrainIoTEvent - log and fail, possibly blacklist
				}
			}
			
		}
		
	}
	
	private static class Event {
		private final String eventType;
		private final Class<?> eventClassOfLastResort;
		private final Map<String, Object> eventProps;
		private final SmartBehaviour<BrainIoTEvent> eventProcessor;
		
		public Event(String eventType, Class<?> eventClassOfLastResort, Map<String, Object> eventProps,
				SmartBehaviour<BrainIoTEvent> eventProcessor) {
			super();
			this.eventType = eventType;
			this.eventClassOfLastResort = eventClassOfLastResort;
			this.eventProps = eventProps;
			this.eventProcessor = eventProcessor;
		}
	}
}
