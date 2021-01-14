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

import org.osgi.annotation.versioning.ProviderType;

/**
 * The runtime service that can be used to send events using
 * the Brain IoT Event Bus
 */

@ProviderType
public interface EventBus {

    /**   
     * Asynchronously send an event   
     * 
     * @param event The event to send
     */
    public void deliver(BrainIoTEvent event);

    /**   
     * Asynchronously send an event  
     * 
     * @param eventType the type of the event - this must be a class name representing
     *  the event type
     * @param eventData the data for the event. This must match the schema defined by
     * the event type
     */
    public void deliver(String eventType, Map<String, Object> eventData);
}
