/*******************************************************************************
 * Copyright (C) 2021 Paremus Ltd
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/

package eu.brain.iot.eventing.api;

import java.util.Map;

import org.osgi.annotation.versioning.ConsumerType;

/**
 * The whiteboard service that can be used to receive events 
 * of unknown types from the Brain IoT Event Bus. 
 * 
 * Unlike a {@link SmartBehaviour} it is valid to advertise an
 * {@link UntypedSmartBehaviour} with no consumed message type,
 * making it a "consumer of last resort". Consumers of last resort
 * are called when no explicitly typed handler is available for
 * a given event.
 * 
 */

@ConsumerType
public interface UntypedSmartBehaviour {
    /**   
     * Receive event from the BRAIN IoT System   
     */
    public void notify(String eventType, Map<String, ?> properties);
}
