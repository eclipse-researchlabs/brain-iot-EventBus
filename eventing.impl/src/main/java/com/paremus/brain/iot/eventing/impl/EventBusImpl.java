/* Copyright 2019 Paremus, Ltd - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.paremus.brain.iot.eventing.impl;

import static java.util.Collections.emptyMap;
import static org.osgi.util.converter.Converters.standardConverter;

import java.time.Instant;
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
import eu.brain.iot.eventing.api.UntypedSmartBehaviour;

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
	private final Map<String, Map<SmartBehaviour<BrainIoTEvent>, Filter>> eventTypeToSBs = 
			new HashMap<>();
	
	/**
	 * Map access and mutation must be synchronized on {@link #lock}.
	 * Values from the map should be copied as the contents are not thread safe.
	 */
	private final Map<String, Map<UntypedSmartBehaviour, Filter>> eventTypeToUntypedSBs = 
			new HashMap<>();
	
	/**
	 * Map access and mutation must be synchronized on {@link #lock}.
	 * Values from the map should be copied as the contents are not thread safe.
	 */
	private final Map<Long, List<String>> knownBehaviours = new HashMap<>();
	
	private final BlockingQueue<EventTask> queue = new LinkedBlockingQueue<>();
	
	/**
	 * 
	 * Field access must be synchronized on {@link #threadLock}
	 */
	private EventThread thread;

	private final Object threadLock = new Object();

	@Reference(cardinality=ReferenceCardinality.MULTIPLE, policy=ReferencePolicy.DYNAMIC)
	void addSmartBehaviour(SmartBehaviour<BrainIoTEvent> behaviour, Map<String, Object> properties) {
		doAddSmartBehaviour(eventTypeToSBs, behaviour, properties);
	}

	@Reference(cardinality=ReferenceCardinality.MULTIPLE, policy=ReferencePolicy.DYNAMIC)
	void addUntypedSmartBehaviour(UntypedSmartBehaviour behaviour, Map<String, Object> properties) {
		doAddSmartBehaviour(eventTypeToUntypedSBs, behaviour, properties);
	}
	
	private <T> void doAddSmartBehaviour(Map<String, Map<T, Filter>> map, T behaviour, 
			Map<String, Object> properties) {
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
		
		doAddToMap(map, behaviour, f, list, serviceId);
	}

	private <T> void doAddToMap(Map<String, Map<T, Filter>> map, T behaviour, Filter filter, 
			List<String> list, Long serviceId) {
		synchronized (lock) {
			knownBehaviours.put(serviceId, list);
			
			list.forEach(s -> {
				Map<T, Filter> behaviours = 
						map.computeIfAbsent(s, x -> new HashMap<>());
				behaviours.put(behaviour, filter);
			});
		}
	}
	
	void removeSmartBehaviour(SmartBehaviour<BrainIoTEvent> behaviour, Map<String, Object> properties) {
        
		Long serviceId = getServiceId(properties);
		
		doRemoveSmartBehaviour(eventTypeToSBs, behaviour, serviceId);
	}

	void removeUntypedSmartBehaviour(UntypedSmartBehaviour behaviour, Map<String, Object> properties) {
		
		Long serviceId = getServiceId(properties);
		
		doRemoveSmartBehaviour(eventTypeToUntypedSBs, behaviour, serviceId);
	}

	private Long getServiceId(Map<String, Object> properties) {
		return standardConverter().convert(properties.get(Constants.SERVICE_ID)).to(Long.class);
	}

	private Filter getFilter(Map<String, Object> properties) throws InvalidSyntaxException {
		Object o = properties.get(SmartBehaviourDefinition.PREFIX_ + "filter");
		
		if(o == null || "".equals(o)) {
			return null;
		} else {
			return FrameworkUtil.createFilter(String.valueOf(o));
		}
	}

	private <T> void doRemoveSmartBehaviour(Map<String, Map<T, Filter>> map, T behaviour, 
			Long serviceId) {
		synchronized (lock) {
			List<String> consumed = knownBehaviours.remove(serviceId);
			if(consumed != null) {
				consumed.forEach(s -> {
					Map<T, Filter> behaviours = map.get(s);
					if(behaviours != null) {
						behaviours.remove(behaviour);
						if(behaviours.isEmpty()) {
							map.remove(s);
						}
					}
				});
			}
		}
	}
	
	void updatedSmartBehaviour(SmartBehaviour<BrainIoTEvent> behaviour, Map<String, Object> properties) {
		doUpdatedSmartBehaviour(eventTypeToSBs, behaviour, properties);
	}
	
	void updatedUntypedSmartBehaviour(UntypedSmartBehaviour behaviour, Map<String, Object> properties) {
		doUpdatedSmartBehaviour(eventTypeToUntypedSBs, behaviour, properties);
	}
	
	private <T> void doUpdatedSmartBehaviour(Map<String, Map<T, Filter>> map, T behaviour, 
			Map<String, Object> properties) {
		Long serviceId = getServiceId(properties);
		
		synchronized (lock) {
			doRemoveSmartBehaviour(map, behaviour, serviceId);
			doAddSmartBehaviour(map, behaviour, properties);
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
			// This is not an error, it just means that we should stop
			// waiting and let the interrupt propagate
			Thread.currentThread().interrupt();
		}
	}
	
	@Override
	public void deliver(BrainIoTEvent event) {
		
		autoPopulateEventData(event);
		
		String eventName = event.getClass().getName();
		
		Map<String, Object> map = standardConverter().convert(event).sourceAsDTO().to(MAP_WITH_STRING_KEYS);
		
		List<SmartBehaviour<BrainIoTEvent>> behaviours;

		List<UntypedSmartBehaviour> untypedBehaviours;
		
		synchronized (lock) {
			behaviours = eventTypeToSBs.getOrDefault(eventName, emptyMap())
					.entrySet().stream()
						.filter(e -> e.getValue() == null || e.getValue().matches(map))
						.map(Entry::getKey)
						.collect(Collectors.toList());

			untypedBehaviours = eventTypeToUntypedSBs.getOrDefault(eventName, emptyMap())
					.entrySet().stream()
					.filter(e -> e.getValue() == null || e.getValue().matches(map))
					.map(Entry::getKey)
					.collect(Collectors.toList());
		}
		
		if(behaviours.isEmpty()) {
			// TODO log that nobody was listening and call the listener of last resort
			System.out.println("Listener of last resort is not yet implemented");
		} else {
			behaviours.forEach(sb -> queue.add(new TypedEventTask(eventName, 
					event.getClass(), map, sb)));
			untypedBehaviours.forEach(sb -> queue.add(new UntypedEventTask(eventName, 
					 map, sb)));
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
				
				EventTask take;
				try {
					take = queue.take();
				} catch (InterruptedException e) {
					// TODO log the interrupt and continue
					e.printStackTrace();
					continue;
				}
				
				take.notifyListener();
			}
			
		}
		
	}
}
