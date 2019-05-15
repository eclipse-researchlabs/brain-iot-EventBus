/* Copyright 2019 Paremus, Ltd - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

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
