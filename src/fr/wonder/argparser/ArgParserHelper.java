package fr.wonder.argparser;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;

import fr.wonder.argparser.annotations.EntryPoint;
import fr.wonder.argparser.annotations.OptionClass;
import fr.wonder.argparser.utils.PrimitiveUtils;

public class ArgParserHelper {

	public static void validateEntryMethodParameters(Method method) throws NoSuchMethodException, SecurityException {
		if(!Modifier.isStatic(method.getModifiers()))
			throw new IllegalArgumentException("Method " + method + " cannot be accessed statically");
		if(!method.trySetAccessible() || !method.canAccess(null))
			throw new IllegalArgumentException("Method " + method + " cannot be accessed");
		Parameter[] params = method.getParameters();
		
		for(int i = doesMethodUseOptions(method) ? 1 : 0; i < params.length; i++) {
			Class<?> type = params[i].getType();
			
			if(i == params.length-1 && method.isVarArgs())
				type = type.componentType(); // for varargs check that the component type is valid
			
			if(!canBeArgumentType(type, i==0)) {
				throw new IllegalArgumentException("Argument " + params[i].getName() + " has an invalid type " + type.getName()
						+ (type.isAnnotationPresent(OptionClass.class) ? ", only the first argument can be a @OptionClass" : ""));
			}
		}
	}
	
	public static boolean isRootBranch(String text) {
		return EntryPoint.ROOT_ENTRY_POINT.equals(text);
	}

	public static boolean canBeBranchName(String text) {
		return (text.matches("[a-zA-Z]+([a-zA-Z\\-0-9]+[a-zA-Z0-9])?") ||
				isRootBranch(text)) &&
				!isHelpPrint(text);
	}

	public static boolean canBeOptionName(String text) {
		return text.matches("\\-\\-[a-zA-Z]+([a-zA-Z\\-0-9]+[a-zA-Z0-9])?") &&
				!isHelpPrint(text);
	}

	public static boolean canBeOptionShortand(String text) {
		return text.matches("\\-[a-zA-Z0-9]") &&
				!isHelpPrint(text);
	}

	/** Does not check for arrays, methods using varargs have an array as their last parameter */
	public static boolean canBeArgumentType(Class<?> type, boolean acceptsOptionClass) {
		return type == String.class ||
				type == File.class ||
				type.isEnum() ||
				PrimitiveUtils.isPrimitiveType(type) ||
				(acceptsOptionClass && type.isAnnotationPresent(OptionClass.class));
	}

	public static boolean doesMethodUseOptions(Method method) {
		Parameter[] params = method.getParameters();
		return params.length > 0 && params[0].getType().isAnnotationPresent(OptionClass.class);
	}

	public static boolean isHelpPrint(String arg) {
		return arg.equals("help") ||
				arg.equals("--help") ||
				arg.equals("?");
	}

}
