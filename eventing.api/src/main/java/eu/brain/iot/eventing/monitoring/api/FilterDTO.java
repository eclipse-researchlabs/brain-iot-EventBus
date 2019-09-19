/* Copyright 2019 Paremus, Ltd - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */
package eu.brain.iot.eventing.monitoring.api;

import org.osgi.annotation.versioning.ProviderType;
import org.osgi.dto.DTO;

/**
 * A monitoring event filter.
 */
@ProviderType
public class FilterDTO extends DTO {

    public static enum FilterType {
        LDAP, REGEX;
    }

    public String expression;
    public FilterType type;
}
