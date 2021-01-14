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

import java.time.Instant;

import org.osgi.annotation.versioning.ConsumerType;
import org.osgi.dto.DTO;

/**
 * An common super-type for all Brain IoT events containing 
 * information that must exist for all events.
 */
@ConsumerType
public abstract class BrainIoTEvent extends DTO {

	/**
	 * The identifier of the node creating the event   
	 */
    public String sourceNode;
 
    /**   
     * The time at which this event was created  
     */
    public Instant timestamp;
 
    /**   
     * The security token that can be used to authenticate/validate the event   
     */
    public byte[] securityToken;
}
