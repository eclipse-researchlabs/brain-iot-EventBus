/* Copyright 2019 Paremus, Ltd - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package eu.brain.iot.eventing.api;

import org.osgi.annotation.versioning.ConsumerType;

import eu.brain.iot.eventing.annotation.SmartBehaviourDefinition;

/**
 * The whiteboard service that can be used to receive events 
 * from the Brain IoT Event Bus.
 * 
 * The {@link SmartBehaviour} interface is designed to be registered
 * in the OSGi Service Registry alongside properties identifying the
 * event type(s) that are consumed. The {@link SmartBehaviourDefinition}
 * component property annotation can be used to set these properties
 * in a type safe way.
 * 
 * Note that a {@link SmartBehaviour} must consume typed events and
 * <em>cannot</em> be used to consume unknown event types. 
 * If unknown event types are to be consumed then an {@link UntypedSmartBehaviour}
 * must be used. 
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