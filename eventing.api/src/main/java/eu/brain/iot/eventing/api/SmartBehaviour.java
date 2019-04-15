/* Copyright 2019 Paremus, Ltd - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package eu.brain.iot.eventing.api;

import org.osgi.annotation.versioning.ConsumerType;

/**
 * The whiteboard service that can be used to receive events 
 * from the Brain IoT Event Bus
 * 
 * @param <T> The type of event consumed by this smart behaviour.
 *            If multiple event types can be consumed then this
 *            may be a common supertype.
 */

@ConsumerType
public interface SmartBehaviour<T extends BrainIoTEvent> {

    /**   
     * Receive an Event from the BRAIN IoT System   
     */
    public void notify(T event);
}