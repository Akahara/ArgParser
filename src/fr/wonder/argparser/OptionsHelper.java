package fr.wonder.argparser;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.Map.Entry;

import fr.wonder.argparser.annotations.Argument;
import fr.wonder.argparser.utils.ErrorWrapper;
import fr.wonder.argparser.utils.ErrorWrapper.WrappedException;
import fr.wonder.argparser.utils.PrimitiveUtils;
import fr.wonder.argparser.utils.ReflectUtils;
import fr.wonder.argparser.utils.StringUtils;
import fr.wonder.argparser.utils.UnreachableException;

class OptionsHelper {

	public static Object parseOptionValue(String arg, Class<?> argType, String argName) throws ArgumentError {
		if(PrimitiveUtils.isPrimitiveType(argType)) {
			if(PrimitiveUtils.isFloatingPoint(argType)) {
				try {
					return PrimitiveUtils.castToPrimitive(Double.parseDouble(arg), argType);
				} catch (IllegalArgumentException | NullPointerException e) {
					throw new ArgumentError("Expected a number for <" + argName + ">, got '" + arg + "'");
				}
			} else {
				try {
					return PrimitiveUtils.castToPrimitive(Long.parseLong(arg), argType);
				} catch (IllegalArgumentException | NullPointerException e) {
					throw new ArgumentError("Expected an integer for <" + argName + ">, got '" + arg + "'");
				}
			}
		} else if(argType == String.class) {
			// DEFAULT_EMPTY can be used in @Argument annotations to specify that
			// the default value for the argument is the empty string.
			return Argument.DEFAULT_EMPTY.equals(arg) ? "" : arg;
		} else if(argType == File.class) {
			try {
				return new File(arg).getCanonicalFile();
			} catch (IOException | NullPointerException e) {
				throw new ArgumentError("Cannot resolve file " + arg + ": " + e.getMessage());
			}
		} else if(argType.isEnum()) {
			try {
				return ReflectUtils.getEnumConstant(argType, arg.toUpperCase());
			} catch (IllegalArgumentException | NullPointerException e) {
				throw new ArgumentError("Expected one of " + StringUtils.join("|", argType.getEnumConstants()) + " for <" + argName + ">, got '" + arg + "'");
			}
		} else if(argType.isArray()) { // varargs only, array arguments are not supported
			throw new ArgumentError("VarArgs cannot have default values");
		} else {
			throw new UnreachableException("Invalid option type " + argType);
		}
	}

	public static Object createOptionsInstance(Map<String, String> rawOptions, ProcessOptions options, ErrorWrapper errors) throws WrappedException {
		Object instance = options.newInstance();
		Map<Class<?>, Object> complexFields;
		try {
			complexFields = options.getAllOptionFields(instance);
		} catch (IllegalAccessException e) {
			throw new IllegalStateException("Could not initialize an option instance", e);
		}
		
		for(Entry<String, String> optPair : rawOptions.entrySet()) {
			Field optField = options.getOptionFields().get(optPair.getKey());
			if(optField == null) {
				errors.add("Unknown option: " + optPair.getKey());
				continue;
			}
			Object fieldInstance = complexFields.get(optField.getDeclaringClass());
			if(fieldInstance == null)
				throw new IllegalStateException("Could not find an instance of " + optField.getType() + " for option class '" + optField.getName() + "' in a " + instance.getClass().getSimpleName());
			setOption(fieldInstance, optField, optPair.getKey(), optPair.getValue(), errors);
		}
		
		errors.assertNoErrors();
		return instance;
	}

	private static void setOption(Object optionObj, Field optionField, String opt, String value, ErrorWrapper errors) {
		try {
			Class<?> optionType = optionField.getType();
			
			if(optionType == boolean.class) {
				// special case: toggle the boolean, that's to allow fields that default to true
				optionField.setBoolean(optionObj, !optionField.getBoolean(optionObj));
				return;
			}
			
			Object argVal;
			try {
				argVal = parseOptionValue(value, optionType, opt);
			} catch (ArgumentError e) {
				errors.add(e.getMessage());
				return;
			}
			
			if(PrimitiveUtils.isTruePrimitive(optionType)) {
				PrimitiveUtils.setPrimitive(optionObj, optionField, (Number) argVal);
			} else {
				optionField.set(optionObj, argVal);
			}
		} catch (IllegalAccessException e) {
			errors.add("Cannot Cannot set an option field value: " + e.getMessage());
		}
	}

	public static boolean doesOptionTakeArgument(Class<?> type) {
		return type != boolean.class;
	}
	
}
