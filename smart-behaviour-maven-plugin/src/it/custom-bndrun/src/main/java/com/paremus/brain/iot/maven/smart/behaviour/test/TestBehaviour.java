/*******************************************************************************
 * Copyright (C) 2021 Paremus Ltd
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/
package com.paremus.brain.iot.maven.smart.behaviour.test;

import eu.brain.iot.eventing.annotation.SmartBehaviourDefinition;
import eu.brain.iot.eventing.api.SmartBehaviour;

@SmartBehaviourDefinition(consumed = TestDTO.class, 
author = "Paremus", name = "Test Behaviour",
description = "Used in testing")
public class TestBehaviour implements SmartBehaviour<TestDTO> {
	
	@Override
	public void notify(TestDTO event) {
		// No need for an implementation
	}
	
}
