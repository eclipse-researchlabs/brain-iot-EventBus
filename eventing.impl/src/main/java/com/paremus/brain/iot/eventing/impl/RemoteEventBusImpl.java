package com.paremus.brain.iot.eventing.impl;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceRegistration;

import com.paremus.brain.iot.eventing.spi.remote.RemoteEventBus;

import eu.brain.iot.eventing.annotation.SmartBehaviourDefinition;

public class RemoteEventBusImpl implements RemoteEventBus {
	
	private final EventBusImpl eventBus;
	
	private ServiceRegistration<RemoteEventBus> reg;
	
	private final Map<Long, Map<String, String>> servicesToInterests = new HashMap<>();

	
	public RemoteEventBusImpl(EventBusImpl eventBus) {
		this.eventBus = eventBus;
	}
	
	public void init(BundleContext ctx) {
		ServiceRegistration<RemoteEventBus> reg = ctx.registerService(RemoteEventBus.class, this, null);
		synchronized(this) {
			this.reg = reg;
		}
		updateReg(getUpdatedFilters());
	}

	public void destroy() {
		try {
			ServiceRegistration<?> reg;
			synchronized (this) {
				reg = this.reg;
				this.reg = null;
			}
			
			if(reg != null) {
				reg.unregister();
			}
		} catch (IllegalStateException ise) {
			// TODO log
		}
	}
	
	@Override
	public void notify(String eventType, Map<String, Object> properties) {
		eventBus.deliverEventFromRemote(eventType, properties);
	}

	public void updateLocalInterest(Long id, List<String> consumed, Filter filter) {

		boolean doUpdate = false;

		Map<String, List<String>> updatedFilters = null; 

		if(filter == null) {
			// TODO log that we ignore local services with no filter
			
			synchronized (this) {
				if(servicesToInterests.keySet().remove(id)) {
					doUpdate = true;
					updatedFilters = getUpdatedFilters();
				}
			}
		} else {
			String filterString = filter.toString();
			
			// Build the map, ignoring duplicate keys as the value is the same
			Map<String, String> newData = consumed.stream()
					.collect(toMap(identity(), x -> filterString, (a,b) -> a));
			
			
			synchronized(this) {
				doUpdate = true;
				servicesToInterests.put(id, newData);
				updatedFilters = getUpdatedFilters();
			}
		}
		
		if(doUpdate) {
			updateReg(updatedFilters);
		}
	}

	private synchronized Map<String, List<String>> getUpdatedFilters() {
		List<String> consumed = servicesToInterests.values().stream()
				.flatMap(m -> m.keySet().stream())
				.distinct()
				.collect(toList());
		
		
		Map<String, List<String>> filters = servicesToInterests.values().stream()
				.flatMap(m -> m.entrySet().stream())
				.collect(toMap(
						e -> "filter-" + e.getKey(), 
						e -> Collections.singletonList(e.getValue()), 
						(a,b) -> Stream.concat(a.stream(), b.stream())
									.distinct()
									.collect(toList())));
		
		filters.put(SmartBehaviourDefinition.PREFIX_ + "consumed", consumed);
		
		return filters;
	}

	private void updateReg(Map<String, List<String>> updatedFilters) {
		
		Hashtable<String, Object> props = new Hashtable<>();
		
		props.put(Constants.SERVICE_EXPORTED_INTERFACES, RemoteEventBus.class.getName());
		props.put(Constants.SERVICE_EXPORTED_INTENTS, "osgi.basic");
		
		props.putAll(updatedFilters);
		
		ServiceRegistration<?> reg;
		synchronized (this) {
			reg = this.reg;
		}
		if(reg != null) {
			reg.setProperties(props);
			// Deal with a race condition if
			Map<String, List<String>> updatedFilters2 = getUpdatedFilters();
			if(!updatedFilters.equals(updatedFilters2)) {
				updateReg(updatedFilters2);
			}
		}
	}

	public void removeLocalInterest(Long id) {

		Map<String, List<String>> updatedFilters; 
		
		synchronized(this) {
			servicesToInterests.remove(id);
			updatedFilters = getUpdatedFilters();
		}
		
		updateReg(updatedFilters);
	}
}
