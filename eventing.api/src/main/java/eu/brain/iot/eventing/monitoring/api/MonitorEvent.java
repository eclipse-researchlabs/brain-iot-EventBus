/* Copyright 2019 Paremus, Ltd - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */
package eu.brain.iot.eventing.monitoring.api;

import java.time.Instant;
import java.util.Map;

import org.osgi.annotation.versioning.ProviderType;
import org.osgi.dto.DTO;

/**
 * A monitoring event.
 */
@ProviderType
public class MonitorEvent extends DTO {

	public static enum PublishType {
		LOCAL, REMOTE;
	}
	
	public String eventType;
	
	public Map<String, Object> eventData;

	public PublishType publishType;
	
	public Instant publicationTime;
}
