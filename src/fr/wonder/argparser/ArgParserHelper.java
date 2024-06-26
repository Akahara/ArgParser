package fr.wonder.argparser;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;

import fr.wonder.argparser.annotations.EntryPoint;
import fr.wonder.argparser.annotations.OptionClass;
import fr.wonder.argparser.utils.PrimitiveUtils;

public class ArgParserHelper {

	public static void validateEntryMethodParameters(Method method, Object calleeInstance) throws NoSuchMethodException, SecurityException {
		boolean isStaticMethod = Modifier.isStatic(method.getModifiers());
		if(calleeInstance == null && !isStaticMethod)
			throw new IllegalArgumentException("Method " + method + " cannot be accessed statically");
		if(!method.trySetAccessible() || !method.canAccess(isStaticMethod ? null : calleeInstance))
			throw new IllegalArgumentException("Method " + method + " cannot be accessed");
		Parameter[] params = method.getParameters();
		
		for(int i = doesMethodUseOptions(method) ? 1 : 0; i < params.length; i++) {
			Class<?> type = params[i].getType();
			
			if(!canBeArgumentType(type, i==0, i==params.length-1)) {
				if (type.isArray() && i != params.length - 1)
					throw new IllegalArgumentException("Argument " + params[i].getName() + " has an invalid type " + type.getName()
						+ ", only the last argument can be of array type");
				else
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
	public static boolean canBeArgumentType(Class<?> type, boolean acceptsOptionClass, boolean acceptsArray) {
		return type == String.class ||
				type == File.class ||
				type.isEnum() ||
				PrimitiveUtils.isPrimitiveType(type) ||
				(acceptsOptionClass && type.isAnnotationPresent(OptionClass.class)) ||
				(type.isArray() && canBeArgumentType(type.componentType(), false, false));
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
