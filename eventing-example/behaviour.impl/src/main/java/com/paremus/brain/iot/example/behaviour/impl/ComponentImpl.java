/* Copyright 2019 Paremus, Ltd - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */
package com.paremus.brain.iot.example.behaviour.impl;

import com.paremus.brain.iot.example.light.api.LightCommand;
import com.paremus.brain.iot.example.sensor.api.SensorReadingDTO;
import eu.brain.iot.eventing.annotation.SmartBehaviourDefinition;
import eu.brain.iot.eventing.api.EventBus;
import eu.brain.iot.eventing.api.SmartBehaviour;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A Smart Behaviour implementing a security light that slowly turns off
 */
@Component
@SmartBehaviourDefinition(consumed = SensorReadingDTO.class, filter = "(timestamp=*)",
        author = "Paremus", name = "Example Smart Security Light Behaviour",
        description = "Implements a security light that slowly turns off.")
@Designate(ocd = ComponentImpl.Config.class)
public class ComponentImpl implements SmartBehaviour<SensorReadingDTO> {
    @ObjectClassDefinition(
            name = "Smart Security Behaviour",
            description = "Configuration for the Smart Security Behaviour."
    )
    @interface Config {
        enum LightColour {
            WHITE, RED, YELLOW, MAGENTA, GREEN, BLUE, CYAN
        }

        @AttributeDefinition(type = AttributeType.INTEGER,
                name = "Time on",
                description = "How long light stays on after sensor is triggered (seconds)",
                min = "10",
                max = "300"
        )
        int duration() default 10;

        @AttributeDefinition(
                name = "Colour",
                description = "Colour light emits when activated"
        )
        LightColour colour() default LightColour.WHITE;
    }

    private final int MAX_BRIGHTNESS = 10;

	private final ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor();

	private final AtomicInteger brightness = new AtomicInteger();

	@Reference
	private EventBus eventBus;

	private Config config;

	@Activate
	void activate(Config config) {
	    this.config = config;
    }

    @Modified
    void modify(Config config) {
        this.config = config;
    }

	@Deactivate
	void stop() {
		worker.shutdown();
	}

	@Override
	public void notify(SensorReadingDTO event) {

		int oldValue = brightness.getAndSet(MAX_BRIGHTNESS);

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

		if (value != 0) {
		    long delayMs = config.duration() * 1000 / MAX_BRIGHTNESS;
			worker.schedule(this::updateBulb, delayMs, TimeUnit.MILLISECONDS);
		}
	}
}
