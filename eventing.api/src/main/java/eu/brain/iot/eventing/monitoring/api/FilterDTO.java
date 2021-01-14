/*******************************************************************************
 * Copyright (C) 2021 Paremus Ltd
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/

package eu.brain.iot.eventing.monitoring.api;

import org.osgi.annotation.versioning.ProviderType;
import org.osgi.dto.DTO;

/**
 * A monitoring event filter.
 *
 * If both LDAP and regular expressions are supplied, then both must match.
 */
@ProviderType
public class FilterDTO extends DTO {

    public String ldapExpression;

    public String regularExpression;
}
