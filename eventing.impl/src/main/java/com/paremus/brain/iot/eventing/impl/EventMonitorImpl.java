/* Copyright 2019 Paremus, Ltd - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.paremus.brain.iot.eventing.impl;

import eu.brain.iot.eventing.monitoring.api.EventMonitor;
import eu.brain.iot.eventing.monitoring.api.FilterDTO;
import eu.brain.iot.eventing.monitoring.api.MonitorEvent;
import eu.brain.iot.eventing.monitoring.api.MonitorEvent.PublishType;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.function.Predicate;
import org.osgi.util.pushstream.PushEvent;
import org.osgi.util.pushstream.PushEventSource;
import org.osgi.util.pushstream.PushStream;
import org.osgi.util.pushstream.PushStreamProvider;
import org.osgi.util.pushstream.PushbackPolicyOption;
import org.osgi.util.pushstream.QueuePolicyOption;
import org.osgi.util.pushstream.SimplePushEventSource;

import java.time.Instant;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ServiceCapability(EventMonitor.class)
public class EventMonitorImpl implements EventMonitor {


    private final LinkedList<MonitorEvent> historicEvents = new LinkedList<MonitorEvent>();

    private final ExecutorService monitoringWorker;

    private final Object lock = new Object();

    private final PushStreamProvider psp;

    private final SimplePushEventSource<MonitorEvent> source;

    private final int historySize = 1024;

    private ServiceRegistration<EventMonitor> reg;

    private BundleContext context;

    public EventMonitorImpl() {

        monitoringWorker = Executors.newCachedThreadPool();

        psp = new PushStreamProvider();
        source = psp.buildSimpleEventSource(MonitorEvent.class)
                .withExecutor(monitoringWorker)
                .withQueuePolicy(QueuePolicyOption.BLOCK)
                .build();
    }

    public void init(BundleContext ctx) {
        this.context = ctx;
        Hashtable<String, Object> props = new Hashtable<>();
        props.put(Constants.SERVICE_EXPORTED_INTERFACES, EventMonitor.class.getName());
        ServiceRegistration<EventMonitor> reg = ctx.registerService(EventMonitor.class, this, props);
        synchronized (this) {
            this.reg = reg;
        }
    }

    public void destroy() {
        try {
            ServiceRegistration<?> reg;
            synchronized (lock) {
                reg = this.reg;
                this.reg = null;
            }

            if (reg != null) {
                reg.unregister();
            }
        } catch (IllegalStateException ise) {
            // TODO log
        }
        source.close();
        monitoringWorker.shutdown();
    }


    public void event(String eventType, Map<String, Object> eventData, boolean remote) {
        MonitorEvent me = new MonitorEvent();
        me.eventData = eventData;
        me.eventType = eventType;
        me.publishType = remote ? PublishType.REMOTE : PublishType.LOCAL;
        me.publicationTime = Instant.now();

        synchronized (lock) {
            historicEvents.add(me);
            int toRemove = historicEvents.size() - historySize;
            for (; toRemove > 0; toRemove--) {
                historicEvents.poll();
            }
            source.publish(me);
        }
    }

    @Override
    public PushStream<MonitorEvent> monitorEvents(FilterDTO... filters) {
        return monitorEvents(0, filters);
    }

    @Override
    public PushStream<MonitorEvent> monitorEvents(int history, FilterDTO...filters) {
        return psp.buildStream(eventSource(history))
                .withBuffer(new ArrayBlockingQueue<>(Math.max(historySize, history)))
                .withPushbackPolicy(PushbackPolicyOption.FIXED, 0)
                .withQueuePolicy(QueuePolicyOption.FAIL)
                .withExecutor(monitoringWorker)
                .build()
                .filter(createFilter(filters));
    }

    @Override
    public PushStream<MonitorEvent> monitorEvents(Instant history, FilterDTO...filters) {
        return psp.buildStream(eventSource(history))
                .withBuffer(new ArrayBlockingQueue<>(1024))
                .withPushbackPolicy(PushbackPolicyOption.FIXED, 0)
                .withQueuePolicy(QueuePolicyOption.FAIL)
                .withExecutor(monitoringWorker)
                .build()
                .filter(createFilter(filters));
    }

    private class FilterPair {
        Filter ldap;
        Pattern regex;

        FilterPair(FilterDTO filter) {
            if (filter.ldapExpression != null && !filter.ldapExpression.isEmpty()) {
                try {
                    ldap = context.createFilter(filter.ldapExpression);
                } catch (InvalidSyntaxException e) {
                    throw new IllegalArgumentException(e);
                }
            }

            if (filter.regularExpression != null && !filter.regularExpression.isEmpty()) {
                regex = Pattern.compile(filter.regularExpression);
            }
        }
    }

    private Predicate<MonitorEvent> createFilter(FilterDTO... filters) {
        List<FilterPair> filterPairs = Arrays.asList(filters).stream()
                .map(FilterPair::new).collect(Collectors.toList());

        if (filterPairs.isEmpty()) {
            return x -> true;
        }

        return event -> {
            // We use a TreeMap to ensure predictable ordering of keys
            // This is important for the regex matching contract.

            SortedMap<String, Object> toFilter = new TreeMap<>();

            // Using a collector blew up with null values, even though they are
            // supported by the TreeMap
            event.eventData.entrySet().stream()
                    .flatMap(e -> flatten("", e))
                    .forEach(e -> toFilter.put(e.getKey(), e.getValue()));

            toFilter.put("-eventType", event.eventType);
            toFilter.put("-publishType", event.publishType);

            StringBuilder eventText = new StringBuilder();

            if (filterPairs.stream().anyMatch(p -> p.regex != null)) {
                toFilter.forEach((k, v) -> {
                    eventText.append(k).append(':').append(v).append(',');
                });
            }

            // If a FilterDTO contains both LDAP and regular expressions, then both must match.
            return filterPairs.stream().anyMatch(p ->
                    (p.ldap == null || p.ldap.matches(toFilter)) &&
                    (p.regex == null || p.regex.matcher(eventText).find())
            );
        };
    }

    private Stream<Entry<String, Object>> flatten(String parentScope,
    		Entry<String, Object> entry) {

    	if (entry.getValue() instanceof Map) {

			String keyPrefix = parentScope + entry.getKey() + ".";

			@SuppressWarnings("unchecked")
			Map<String, Object> subMap = (Map<String, Object>) entry.getValue();

			// Recursively flatten maps that are inside our map
			return subMap.entrySet().stream()
    			.flatMap(e -> flatten(keyPrefix, e));
		} else if(parentScope.isEmpty()) {
			// Fast path for top-level entries
			return Stream.of(entry);
		} else {
			// Map the key of a nested entry into x.y.z
			return Stream.of(new AbstractMap.SimpleEntry<>(
					parentScope + entry.getKey(), entry.getValue()));
		}

    }

	PushEventSource<MonitorEvent> eventSource(int events) {

		return pec -> {
			synchronized (lock) {

				int size = historicEvents.size();
				int start = Math.max(0, size - events);

				List<MonitorEvent> list = historicEvents.subList(start, size);

				for(MonitorEvent me : list) {
					try {
						if(pec.accept(PushEvent.data(me)) < 0) {
							return () -> {};
						}
					} catch (Exception e) {
						return () -> {};
					}
				}
				return source.open(pec);
			}

		};
	}

	PushEventSource<MonitorEvent> eventSource(Instant since) {

		return pec -> {
			synchronized (lock) {

				ListIterator<MonitorEvent> it = historicEvents.listIterator();

				while(it.hasNext()) {
					MonitorEvent next = it.next();
					if(next.publicationTime.isAfter(since)) {
						it.previous();
						break;
					}
				}

				while(it.hasNext()) {
					try {
						if(pec.accept(PushEvent.data(it.next())) < 0) {
							return () -> {};
						}
					} catch (Exception e) {
						return () -> {};
					}
				}
				return source.open(pec);
			}

		};
	}

}
