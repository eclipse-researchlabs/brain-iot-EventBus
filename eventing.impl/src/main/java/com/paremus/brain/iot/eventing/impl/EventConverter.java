/* Copyright 2019 Paremus, Ltd - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.paremus.brain.iot.eventing.impl;

import static java.lang.ThreadLocal.withInitial;
import static java.util.Collections.newSetFromMap;

import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.osgi.util.converter.Converter;
import org.osgi.util.converter.ConverterFunction;
import org.osgi.util.converter.Converters;
import org.osgi.util.converter.TypeReference;

import eu.brain.iot.eventing.api.BrainIoTEvent;

/**
 * This class is responsible for converting events to and from their
 * "flattened" representations. A "flattened" event uses Maps rather 
 * than DTOs and Strings rather than enums. This makes a flattened
 * event readable by anyone, even if they don't have access to the
 * necessary API types
 */
public class EventConverter {
	
	private static final TypeReference<List<Object>> LIST_OF_OBJECTS = 
					new TypeReference<List<Object>>() {};
	private static final TypeReference<Set<Object>> SET_OF_OBJECTS = 
							new TypeReference<Set<Object>>() {};
	private static final TypeReference<Map<String, Object>> MAP_WITH_STRING_KEYS = 
			new TypeReference<Map<String, Object>>() {};
	private static final TypeReference<Map<Object, Object>> MAP_OF_OBJECT_TO_OBJECT = 
					new TypeReference<Map<Object, Object>>() {};
	
	private static final Converter eventConverter;
	private static final Set<Class<?>> safeClasses;
	private static final Set<Class<?>> specialClasses;
	private static final ThreadLocal<Set<Object>> errorsBeingHandled =
			withInitial(() -> newSetFromMap(new IdentityHashMap<>()));
	
	static {
		safeClasses = new HashSet<>();
		safeClasses.add(String.class);
		safeClasses.add(Boolean.class);
		safeClasses.add(Byte.class);
		safeClasses.add(Short.class);
		safeClasses.add(Character.class);
		safeClasses.add(Integer.class);
		safeClasses.add(Long.class);
		safeClasses.add(Float.class);
		safeClasses.add(Double.class);
		
		specialClasses = new HashSet<Class<?>>();
		specialClasses.add(Date.class);
		specialClasses.add(Calendar.class);
		specialClasses.add(Duration.class);
		specialClasses.add(Instant.class);
		specialClasses.add(LocalDate.class);
		specialClasses.add(LocalDateTime.class);
		specialClasses.add(LocalTime.class);
		specialClasses.add(MonthDay.class);
		specialClasses.add(OffsetTime.class);
		specialClasses.add(OffsetDateTime.class);
		specialClasses.add(Year.class);
		specialClasses.add(YearMonth.class);
		specialClasses.add(ZonedDateTime.class);
		specialClasses.add(UUID.class);
		
		eventConverter = Converters.standardConverter()
			.newConverterBuilder()
			.rule(EventConverter::convert)
			.errorHandler(EventConverter::attemptRecovery)
			.build();
	}
	
	static Object convert(Object o, Type target) {
		
		if(target != Object.class || o == null) {
			return ConverterFunction.CANNOT_HANDLE;
		}
		
		Class<? extends Object> sourceClass = o.getClass();
		
		// "Safe" classes use an identity transform
		if(safeClasses.contains(sourceClass)) {
			return o;
		}
		
		// "Special" types and Enums map to strings
		if(specialClasses.contains(sourceClass) || sourceClass.isEnum()) {
			return eventConverter.convert(o).sourceAs(Object.class).to(String.class);
		}
		
		// Collections get remapped using the same converter to 
		// the relevant collection type containing objects, this
		// ensures we pick up any embedded lists of DTOs or enums
		if(o instanceof Collection) {
			if(o instanceof Set) {
				return eventConverter.convert(o).to(SET_OF_OBJECTS);
			} else {
				return eventConverter.convert(o).to(LIST_OF_OBJECTS);
			}
		}
		
		// As with collections we remap nested maps to clean up any
		// undesirable types in the keys or values
		if(o instanceof Map) {
			return eventConverter.convert(o).to(MAP_OF_OBJECT_TO_OBJECT);
		}
		
		if(sourceClass.isArray()) {
			int depth = 1;
			Class<?> arrayComponentType = sourceClass.getComponentType();
			Class<?> actualComponentType = sourceClass.getComponentType();
			while(actualComponentType.isArray()) {
				depth++;
				actualComponentType = actualComponentType.getComponentType();
			}
			if(safeClasses.contains(actualComponentType) || actualComponentType.isPrimitive()) {
				return o;
			} else if (actualComponentType.isEnum()) {
				// This becomes an n dimensional String array
				Class<?> stringArrayType = Array.newInstance(String.class, new int[depth]).getClass();
				return eventConverter.convert(o).to(stringArrayType);
			} else {
				// This is an array of something complicated, recursively turn it into a
				// list of something, then make it into an array of the right type
				List<Object> oList = eventConverter.convert(o).to(LIST_OF_OBJECTS);
				return oList.toArray((Object[])Array.newInstance(arrayComponentType, 0));
			}
		}
		
		// If we get here then treat the type as a DTO
		return eventConverter.convert(o).sourceAsDTO().to(MAP_WITH_STRING_KEYS);
	}
	
	static Object attemptRecovery(Object o, Type target) {
		if(o instanceof Map) {
			Set<Object> errors = errorsBeingHandled.get();
			
			if(errors.contains(o)) {
				// TODO log the warning in a big way
				return ConverterFunction.CANNOT_HANDLE;
			}
			
			try {
				errors.add(o);

				// TODO log the warning in a big way
				
				return eventConverter.convert(o).targetAsDTO().to(target);
 			} finally {
 				errors.remove(o);
 			}
		}
		return ConverterFunction.CANNOT_HANDLE;
	}
	
	public static Map<String, Object> convert(BrainIoTEvent event) {
		return eventConverter.convert(event).sourceAsDTO().to(MAP_WITH_STRING_KEYS);
	}

	public static Map<String, Object> convert(Map<String, Object> event) {
		return eventConverter.convert(event).to(MAP_WITH_STRING_KEYS);
	}

	public static <T> T convert(Map<String, Object> event, Class<T> target) {
		return eventConverter.convert(event).targetAsDTO().to(target);
	}
}