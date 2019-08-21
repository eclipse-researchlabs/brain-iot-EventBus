package com.paremus.brain.iot.eventing.impl;

import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.osgi.annotation.bundle.Capability;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.pushstream.PushEvent;
import org.osgi.util.pushstream.PushEventSource;
import org.osgi.util.pushstream.PushStream;
import org.osgi.util.pushstream.PushStreamProvider;
import org.osgi.util.pushstream.PushbackPolicyOption;
import org.osgi.util.pushstream.QueuePolicyOption;
import org.osgi.util.pushstream.SimplePushEventSource;

import eu.brain.iot.eventing.monitoring.api.EventMonitor;
import eu.brain.iot.eventing.monitoring.api.MonitorEvent;
import eu.brain.iot.eventing.monitoring.api.MonitorEvent.PublishType;

@Capability(namespace = "osgi.service",
        attribute = {"objectClass:List<String>=eu.brain.iot.eventing.monitoring.api.EventMonitor"})
public class EventMonitorImpl implements EventMonitor {


	private final LinkedList<MonitorEvent> historicEvents = new LinkedList<MonitorEvent>();

	private final ExecutorService monitoringWorker;

	private final Object lock = new Object();

	private final PushStreamProvider psp;

	private final SimplePushEventSource<MonitorEvent> source;

	private final int historySize = 1024;

	private ServiceRegistration<EventMonitor> reg;

	public EventMonitorImpl() {

		monitoringWorker = Executors.newCachedThreadPool();

		psp = new PushStreamProvider();
		source = psp.buildSimpleEventSource(MonitorEvent.class)
				.withExecutor(monitoringWorker)
				.withQueuePolicy(QueuePolicyOption.BLOCK)
				.build();
	}

	public void init(BundleContext ctx) {
		ServiceRegistration<EventMonitor> reg = ctx.registerService(EventMonitor.class, this, null);
		synchronized(this) {
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

			if(reg != null) {
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
			for(;toRemove > 0; toRemove--) {
				historicEvents.poll();
			}
			source.publish(me);
		}
	}

	@Override
	public PushStream<MonitorEvent> monitorEvents() {
		return monitorEvents(0);
	}

	@Override
	public PushStream<MonitorEvent> monitorEvents(int history) {
		return psp.buildStream(eventSource(history))
				.withBuffer(new ArrayBlockingQueue<>(Math.max(historySize, history)))
				.withPushbackPolicy(PushbackPolicyOption.FIXED, 0)
				.withQueuePolicy(QueuePolicyOption.FAIL)
				.withExecutor(monitoringWorker)
				.build();
	}

	@Override
	public PushStream<MonitorEvent> monitorEvents(Instant history) {
		return psp.buildStream(eventSource(history))
				.withBuffer(new ArrayBlockingQueue<>(1024))
				.withPushbackPolicy(PushbackPolicyOption.FIXED, 0)
				.withQueuePolicy(QueuePolicyOption.FAIL)
				.withExecutor(monitoringWorker)
				.build();
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
