/*******************************************************************************
 * Copyright (C) 2021 Paremus Ltd
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/

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
