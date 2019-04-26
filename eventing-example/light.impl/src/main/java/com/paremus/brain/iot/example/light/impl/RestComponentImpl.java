/* Copyright 2019 Paremus, Ltd - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */
package com.paremus.brain.iot.example.light.impl;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Providers;
import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseBroadcaster;
import javax.ws.rs.sse.SseEventSink;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.whiteboard.propertytypes.HttpWhiteboardResource;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JSONRequired;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;

import com.paremus.brain.iot.example.light.api.LightCommand;
import com.paremus.brain.iot.example.light.api.LightQuery;
import com.paremus.brain.iot.example.light.api.LightQueryResponse;

import eu.brain.iot.eventing.annotation.SmartBehaviourDefinition;
import eu.brain.iot.eventing.api.BrainIoTEvent;
import eu.brain.iot.eventing.api.EventBus;
import eu.brain.iot.eventing.api.SmartBehaviour;

/**
 * This component represents two things:
 * 
 *  1. A Smart Light Bulb controlled using events
 *  2. A set of REST resources used by a web ui to display the light bulb
 *
 */
@Component(service= {SmartBehaviour.class, RestComponentImpl.class})
@JaxrsResource
@HttpWhiteboardResource(pattern="/light-ui/*", prefix="/static")
@JSONRequired
@SmartBehaviourDefinition(consumed = {LightCommand.class, LightQuery.class})
public class RestComponentImpl implements SmartBehaviour<BrainIoTEvent>{

	/*
	 * A Smart Behaviour representing a lightbulb
	 */
	
	@Reference
	private EventBus eventBus;
	
	private boolean status;
	
	private int brightness;
	
	@Override
	public void notify(BrainIoTEvent event) {
		if(event instanceof LightQuery) {
			LightQueryResponse response = new LightQueryResponse();
			response.requestor = event.sourceNode;
			synchronized (this) {
				response.brightness = brightness;
				response.status = status;
			}
			eventBus.deliver(response);
		} else if (event instanceof LightCommand) {
			LightCommand command = (LightCommand) event;
			LightDTO dto = new LightDTO();
			synchronized (this) {
				status = command.status;
				brightness = command.brightness;
				
				dto.status = status;
				dto.brightness = brightness;
			}
			brightnessChanged(dto);
		} else {
			System.out.println("Argh! Received an unknown event type " + event.getClass());
		}
		
	}
	
	/* 
	 * 
	 * JAX-RS logic for the lightbulb UI
	 * 
	 */

	private SseBroadcaster broadcaster;
	private Sse sse;
	private Providers providers;

	private void brightnessChanged(LightDTO brightness) {
		SseBroadcaster broadcasterToUse;
		Sse sseToUse;
		Providers providersToUse;
    	synchronized (this) {
			sseToUse = sse;
			broadcasterToUse = broadcaster;
			providersToUse = providers;
		}
    	
    	if(broadcasterToUse != null) {
    		try(ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
				MessageBodyWriter<LightDTO> writer = providersToUse.getMessageBodyWriter(LightDTO.class, null, null, APPLICATION_JSON_TYPE);
    			writer.writeTo(brightness, LightDTO.class, null, null, APPLICATION_JSON_TYPE, null, baos);
    			broadcasterToUse.broadcast(sseToUse.newEvent(baos.toString()));
    		} catch (IOException e) {
				broadcaster.broadcast(sseToUse.newEvent("error", e.getMessage()));
			}
    		
    	}
	}

	@Path("light")
	@GET
	@Produces(APPLICATION_JSON)
	public LightDTO light() {
		LightDTO dto = new LightDTO();
		
		synchronized (this) {
			dto.status = status;
			dto.brightness = brightness;
		}
		
		return dto;
	}

	@Path("light/stream")
	@GET
	@Produces("text/event-stream")
	public void getLiveUpdates(@Context SseEventSink eventSink, @Context Sse sse, @Context Providers providers) {

		SseBroadcaster toUse;
		synchronized (this) {
			if (this.sse == null) {
				this.sse = sse;
				broadcaster = sse.newBroadcaster();
				this.providers = providers;
			}
			toUse = broadcaster;
		}

		toUse.register(eventSink);

		brightnessChanged(light());
	}
}
