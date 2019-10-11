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