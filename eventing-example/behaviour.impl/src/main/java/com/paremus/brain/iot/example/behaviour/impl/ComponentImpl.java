/* Copyright 2019 Paremus, Ltd - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */
package com.paremus.brain.iot.example.behaviour.impl;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.paremus.brain.iot.example.light.api.LightCommand;
import com.paremus.brain.iot.example.sensor.api.SensorReadingDTO;

import eu.brain.iot.eventing.annotation.SmartBehaviourDefinition;
import eu.brain.iot.eventing.api.EventBus;
import eu.brain.iot.eventing.api.SmartBehaviour;

/**
 * A Smart Behaviour implementing a security light that slowly turns off
 */
@Component
@SmartBehaviourDefinition(consumed=SensorReadingDTO.class)
public class ComponentImpl implements SmartBehaviour<SensorReadingDTO>{

	private final ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor();
	
	private final AtomicInteger brightness = new AtomicInteger();
	
	@Reference
	private EventBus eventBus;
	
	@Deactivate
	void stop() {
		worker.shutdown();
	}
	
	@Override
	public void notify(SensorReadingDTO event) {
		
		int oldValue = brightness.getAndSet(10);
		
		if(oldValue == 0) {
			worker.execute(this::updateBulb);
		}
	}
	
	private void updateBulb() {
		int value = brightness.getAndAccumulate(-1, (a,b) -> Math.max(0, a + b));
		
		LightCommand command = new LightCommand();
		command.brightness = value;
		command.status = value > 0;
		
		eventBus.deliver(command);
		
		if(value != 0) {
			worker.schedule(this::updateBulb, 1, TimeUnit.SECONDS);
		}
	}
}
