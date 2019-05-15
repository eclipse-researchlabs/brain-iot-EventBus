/* Copyright 2019 Paremus, Ltd - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

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
