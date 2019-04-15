/* Copyright 2019 Paremus, Ltd - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package eu.brain.iot.eventing.api;

import org.osgi.annotation.versioning.ProviderType;

/**
 * The runtime service that can be used to send events using
 * the Brain IoT Event Bus
 */

@ProviderType
public interface EventBus {

    /**   
     * Asynchronously send an event   
     */
    public void deliver(BrainIoTEvent event);
}
