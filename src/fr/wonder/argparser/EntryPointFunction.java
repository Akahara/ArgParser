package fr.wonder.argparser;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Objects;

import fr.wonder.argparser.annotations.Argument;
import fr.wonder.argparser.annotations.Arguments;

class EntryPointFunction {
	
	private final Method method;
	private final ProcessOptions options;
	private final Object[] defaultArgumentValues;
	private final Argument[] argumentsAnnotations;
	private final int optionalArgsCount;
	
	private EntryPointFunction(Method method, ProcessOptions options, Argument[] argumentsAnnotations,
			Object[] defaultValues, int optionalArgsCount) {
		this.method = Objects.requireNonNull(method);
		this.options = options;
		this.argumentsAnnotations = argumentsAnnotations;
		this.defaultArgumentValues = Objects.requireNonNull(defaultValues);
		this.optionalArgsCount = Objects.requireNonNull(optionalArgsCount);
	}
	
	public static EntryPointFunction createEntryPointFunction(Method method, ProcessOptions options) throws InvalidDeclarationError {
		boolean usesOptions = ArgParserHelper.doesMethodUseOptions(method);
		
		Object[] defaultValues = new Object[method.getParameterCount()];
		Argument[] argumentsAnnotations = getArgumentAnnotations(method);
		
		if(argumentsAnnotations == null)
			return new EntryPointFunction(method, options, null, defaultValues, 0);
		
		// Assertions.assertTrue(argumentsAnnotations.length == method.getParameterCount());
		
		int optionalArgsCount = 0;
		for(int i = method.getParameterCount()-1; i >= (usesOptions?1:0) && !argumentsAnnotations[i].defaultValue().isEmpty(); i--) {
			Parameter parameter = method.getParameters()[i];
			Argument annotation = argumentsAnnotations[i];
			
			try {
				defaultValues[i] = OptionsHelper.parseOptionValue(
						annotation.defaultValue(),
						parameter.getType(),
						annotation.name());
				optionalArgsCount++;
			} catch (ArgumentError e) {
				throw new InvalidDeclarationError("Invalid default value '" + annotation.defaultValue() +
						"' for argument '" + parameter.getName() + "' on method " + method, e);
			}
		}
		for(int i = method.getParameterCount()-1-optionalArgsCount; i >= 0; i--) {
			if(argumentsAnnotations[i] != null && !argumentsAnnotations[i].defaultValue().isEmpty())
				throw new InvalidDeclarationError("Parameter '" + method.getParameters()[i].getName() +
						"' has a default value but a later parameter does not specify one on method " + method);
		}
		
		return new EntryPointFunction(method, options, argumentsAnnotations, defaultValues, optionalArgsCount);
	}

	private static Argument[] getArgumentAnnotations(Method m) throws InvalidDeclarationError {
		Arguments arguments = m.getAnnotation(Arguments.class);
		Argument argument = m.getAnnotation(Argument.class);
		Argument[] args = null;
		boolean usesOptions = ArgParserHelper.doesMethodUseOptions(m);
		if(arguments != null)
			args = arguments.value();
		else if(argument != null)
			args = new Argument[] { argument };
		if(args != null && m.getParameterCount() != args.length + (usesOptions ? 1 : 0))
			throw new InvalidDeclarationError("Invalid number of arguments on " + m.getName() + ", either set all arguments or none");
		if(args != null && usesOptions) {
			// if method uses options append null at the start of the array, that way the array has the same size as the method parameters
			args = Arrays.copyOf(args, args.length+1);
			for(int i = args.length-1; i > 0; i--)
				args[i] = args[i-1];
			args[0] = null;
		}
		return args;
	}
	
	public boolean usesOptions() {
		return options != null;
	}
	
	public int normalParamCount() {
		return method.getParameterCount() - (usesOptions()?1:0);
	}
	
	public int optionalParamCount() {
		return optionalArgsCount;
	}
	
	public String getParamName(int argIndex) {
		return argumentsAnnotations == null ?
				method.getParameters()[argIndex].getName() :
				argumentsAnnotations[argIndex].name();
	}
	
	public Class<?> getParamType(int argIndex) {
		return method.getParameterTypes()[argIndex];
	}

	public String getParamDesc(int argIndex) {
		return argumentsAnnotations == null ? "" :
			argumentsAnnotations[argIndex].desc();
	}
	
	public Object getParamDefaultValue(int argIndex) {
		return defaultArgumentValues[argIndex];
	}

	public Method getMethod() {
		return method;
	}
	
	public ProcessOptions getOptions() {
		return options;
	}

	public boolean acceptsVarArgs() {
		return method.isVarArgs();
	}

	public int totalParameterCount() {
		return method.getParameterCount();
	}

}