/* Copyright 2019 Paremus, Ltd - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.paremus.brain.iot.eventing.spi.remote;

import java.util.Map;

import org.osgi.annotation.versioning.ProviderType;

/**
 * The whiteboard service that is used internally by the Event Bus
 * to support the sending and receiving of remote events 
 * 
 * This interface should not be used by end users, and may be
 * modified in breaking ways between versions;
 */

@ProviderType
public interface RemoteEventBus {
    /**   
     * Represents listeners in a remote Event bus, used to support more
     * sophisticated filtering properties  
     */
    public void notify(String eventType, Map<String, Object> properties);
}
