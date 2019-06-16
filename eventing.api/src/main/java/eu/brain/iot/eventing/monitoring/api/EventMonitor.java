/* Copyright 2019 Paremus, Ltd - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */
package eu.brain.iot.eventing.monitoring.api;

import java.time.Instant;

import org.osgi.annotation.versioning.ProviderType;
import org.osgi.util.pushstream.PushStream;

/**
 * The EventMonitor service can be used to monitor the events that are
 * sent using the EventBus, and that are received from remote EventBus
 * instances
 */
@ProviderType
public interface EventMonitor {

	/**
	 * Get a stream of events, starting now
	 * @return A stream of event data
	 */
	PushStream<MonitorEvent> monitorEvents();

	/**
	 * Get a stream of events, including up to the
	 * requested number of historical data events
	 * 
	 * @param history The requested number of historical
	 * events, note that fewer than this number of events
	 * may be returned if history is unavailable, or if
	 * insufficient events have been sent.
	 * 
	 * @return A stream of event data
	 */
	PushStream<MonitorEvent> monitorEvents(int history);

	/**
	 * Get a stream of events, including historical 
	 * data events prior to the supplied time
	 * 
	 * @param history The requested time after which
	 * historical events, should be included. Note 
	 * that events may have been discarded, or history
	 * unavailable.
	 * 
	 * @return A stream of event data
	 */
	PushStream<MonitorEvent> monitorEvents(Instant history);
	
}
