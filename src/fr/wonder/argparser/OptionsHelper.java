package fr.wonder.argparser;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import fr.wonder.argparser.annotations.Argument;
import fr.wonder.argparser.utils.ArrayOperator;
import fr.wonder.argparser.utils.ErrorWrapper;
import fr.wonder.argparser.utils.ErrorWrapper.WrappedException;
import fr.wonder.argparser.utils.PrimitiveUtils;
import fr.wonder.argparser.utils.ReflectUtils;
import fr.wonder.argparser.utils.StringUtils;
import fr.wonder.argparser.utils.UnreachableException;

class OptionsHelper {

	private static final String[] BOOLEAN_TRUE_VALUES = { "1", "true", "True" };
	private static final String[] BOOLEAN_FALSE_VALUES = { "0", "false", "False" };

	public static Object parseOptionValue(String argVal, Class<?> argType, String argName) throws ArgumentError {
		boolean isDefaultEmptyValue = Argument.DEFAULT_EMPTY.equals(argVal);
		
		if (argType == boolean.class || argType == Boolean.class) {
			if(isDefaultEmptyValue)
				throw new ArgumentError("Type " + argType.getCanonicalName() + " cannot be defaulted to empty for <" + argName + ">");
			if (ArrayOperator.contains(BOOLEAN_TRUE_VALUES, argVal))
				return true;
			if (ArrayOperator.contains(BOOLEAN_FALSE_VALUES, argVal))
				return false;
			throw new ArgumentError("Expected true or false for <" + argName + ">, got '" + argVal + "'");
			
		} else if(PrimitiveUtils.isPrimitiveType(argType)) {
			if(isDefaultEmptyValue)
				throw new ArgumentError("Type " + argType.getCanonicalName() + " cannot be defaulted to empty for <" + argName + ">");
			if(PrimitiveUtils.isFloatingPoint(argType)) {
				try {
					return PrimitiveUtils.castToPrimitive(Double.parseDouble(argVal), argType);
				} catch (IllegalArgumentException | NullPointerException e) {
					throw new ArgumentError("Expected a number for <" + argName + ">, got '" + argVal + "'");
				}
			} else {
				try {
					return PrimitiveUtils.castToPrimitive(Long.parseLong(argVal), argType);
				} catch (IllegalArgumentException | NullPointerException e) {
					throw new ArgumentError("Expected an integer for <" + argName + ">, got '" + argVal + "'");
				}
			}
		} else if(argType == String.class) {
			// DEFAULT_EMPTY can be used in @Argument annotations to specify that
			// the default value for the argument is the empty string.
			return isDefaultEmptyValue ? "" : argVal;
		} else if(argType == File.class) {
			if(isDefaultEmptyValue)
				throw new ArgumentError("Type File cannot be defaulted to empty for <" + argName + ">");
			try {
				return new File(argVal).getCanonicalFile();
			} catch (IOException | NullPointerException e) {
				throw new ArgumentError("Cannot resolve file " + argVal + ": " + e.getMessage());
			}
		} else if(argType.isEnum()) {
			try {
				return ReflectUtils.getEnumConstant(argType, argVal.toUpperCase());
			} catch (IllegalArgumentException | NullPointerException e) {
				throw new ArgumentError("Expected one of " + StringUtils.join("|", argType.getEnumConstants()) + " for <" + argName + ">, got '" + argVal + "'");
			}
		} else if(argType.isArray()) {
			Class<?> componentType = argType.componentType();
			if (isDefaultEmptyValue)
				return Array.newInstance(componentType, 0);
			
			String[] parts = StringUtils.splitCLIArgs(argVal);
			Object arrayVal = Array.newInstance(componentType, parts.length);
			for (int i = 0; i < parts.length; i++)
				Array.set(arrayVal, i, parseOptionValue(parts[i], componentType, argName + "[" + i + "]"));
			return arrayVal;
		} else {
			throw new UnreachableException("Invalid option type " + argType);
		}
	}
	
	public static Object createOptionsInstance(List<OptionKeyValuePair> rawOptions, ProcessOptions options, ErrorWrapper errors) throws WrappedException {
		Object instance = options.newInstance();
		Map<Class<?>, Object> complexFields;
		
		try {
			complexFields = options.getAllOptionFields(instance);
		} catch (IllegalAccessException e) {
			throw new IllegalStateException("Could not initialize an option instance", e);
		}
		
		for(OptionKeyValuePair optPair : rawOptions) {
			Field optField = options.getOptionFields().get(optPair.name);
			if(optField == null) {
				errors.add("Unknown option: " + optPair.name);
				continue;
			}
			Object fieldInstance = complexFields.get(optField.getDeclaringClass());
			if(fieldInstance == null)
				throw new IllegalStateException("Could not find an instance of " + optField.getType() + " for option class '" + optField.getName() + "' in a " + instance.getClass().getSimpleName());
			setOption(fieldInstance, optField, optPair.name, optPair.value, errors);
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
			
			if(optionType.isArray()) {
				Object oldArray = optionField.get(optionObj);
				Object newArray;
				int insertionIndex;
				if (oldArray == null) {
					insertionIndex = 0;
					newArray = Array.newInstance(optionType.componentType(), 1);
				} else {
					insertionIndex = Array.getLength(oldArray);
					newArray = Array.newInstance(optionType.componentType(), insertionIndex+1);
					for (int i = 0; i < insertionIndex; i++)
						Array.set(newArray, i, Array.get(oldArray, i));
				}
				try {
					Array.set(newArray, insertionIndex, parseOptionValue(value, optionType.componentType(), opt));
				} catch (ArgumentError e) {
					errors.add(e.getMessage());
					return;
				}
				optionField.set(optionObj, newArray);
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
			errors.add("Cannot set an option field value: " + e.getMessage());
		}
	}

	public static boolean doesOptionTakeArgument(Class<?> type) {
		return type != boolean.class;
	}
	
}

class OptionKeyValuePair {
	final String name;
	final String value; // null only for options that do not take values (ie. booleans, ie. -y or -n)
	
	public OptionKeyValuePair(String name, String value) {
		this.name = name;
		this.value = Objects.requireNonNull(value);
	}
	
	public OptionKeyValuePair(String name) {
		this.name = name;
		this.value = null;
	}
}
