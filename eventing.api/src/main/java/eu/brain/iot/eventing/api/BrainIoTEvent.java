/* Copyright 2019 Paremus, Ltd - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

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