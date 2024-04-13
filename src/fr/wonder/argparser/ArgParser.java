package fr.wonder.argparser;

import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import fr.wonder.argparser.annotations.Argument;
import fr.wonder.argparser.annotations.EntryPoint;
import fr.wonder.argparser.annotations.Option;
import fr.wonder.argparser.annotations.OptionClass;
import fr.wonder.argparser.annotations.ProcessDoc;
import fr.wonder.argparser.tests.ProcessArgumentsGeneral;
import fr.wonder.argparser.utils.ArrayOperator;
import fr.wonder.argparser.utils.ErrorWrapper;
import fr.wonder.argparser.utils.ErrorWrapper.WrappedException;
import fr.wonder.argparser.utils.ReflectUtils;
import fr.wonder.argparser.utils.StringUtils;

/**
 * Utility to create command line interfaces.
 * 
 * <p>
 * The basic idea is to define entry points in a class and call {@link #run(String[])}
 *  to run the right one with user-supplied arguments and options.
 * 
 * <p>
 * Look into {@link ProcessArgumentsGeneral} for an example.
 * 
 * <p>
 * Note that reflection is heavily used, impacting performances. This utility is
 * not meant to be used often! It can still be used to run user commands on the
 * fly in a shell-like interface and not only as a CLI.
 * 
 * <p>
 * <h2>Anatomy of a command</h2>
 * <blockquote><code>
 * (command name) [options...] (entry point path) [arguments...]
 * </code></blockquote>
 * 
 * For example:
 * <blockquote><code>
 * git --force -i add somedir1 somedir2
 * </code></blockquote>
 * Here {@code git} is the command, that part is not handled by {@code ArgParser}.
 * {@code --force} is a boolean long option, {@code -i} is a boolean short option.
 * {@code add} is an entry point path, this implementation supports branching paths
 * (eg. paths with multiple words). {@code somedir} are arguments.
 * 
 * <ul>
 * <li>Boolean options do not take argument ({@code -a} instead of {@code -a true})</li>
 * <li>Boolean short options can be combined ({@code -a -b} can be shortened to {@code -ab}).</li>
 * <li>It's currently impossible to have two option classes with options that have the same
 *     name but only one is boolean, because we cannot know if the next argument is the value of
 *     the option or not.</li>
 * </ul>
 * 
 * <p>
 * <h2>Anatomy of an entry point function</h2>
 * This one could be called using {@code mycommand myentrypoint somestring 42}:
 * <blockquote><pre>
 * {@literal @}EntryPoint(path = "myentrypoint")
 * public void myEntryPoint(String myStringArg, int myIntArg) {}
 * </pre></blockquote>
 * 
 * <ul>
 * <li>See {@link EntryPoint} and {@link Argument} annotations.</li>
 * <li>Supported argument types are {@code String}, all native types (int,
 *     float...), all wrapped native types (Integer, Float...), {@code File} and any
 *     {@code enum} type.</li>
 * </ul>
 * 
 * <p>
 * <h2>Working with paths and options</h2>
 * Examples of entry paths ({@code git} is the command, there are no arguments or options)
 * 
 * <ul>
 * <li>git add</li>
 * <li>git lfs pull</li>
 * <li>git lfs fetch</li>
 * </ul>
 * 
 * Entry points cannot overlap, use options or default values for arguments
 * instead. Entry points paths cannot be extended - there cannot be an entry
 * point accessible using {@code part1} and one using {@code part1 part2}
 * because {@code part2} would be interpreted as an argument.
 * <p>
 * Long options must start with two dashes ({@code --name}), short options
 * must start with a single dash and end with a single characted ({@code -v}).
 * <p>
 * {@code --help}, {@code help} and {@code ?} are built-in to display help
 * for an entry point or for the program, they cannot be used as options or
 * entry point paths.
 * <p>
 * In doubt see methods in {@link ArgParserHelper}.
 * 
 * <p>
 * <h2>Options</h2>
 * Options are defined in Option Classes using {@link OptionClass}. See the
 * examples.
 * 
 * <p>
 * <h2>Implementation notes</h2>
 * Methods in this package may throw {@link InvalidDeclarationError}, unless
 * otherwise specified, this occurs when annotations are wrongs, methods or
 * fields are not public or entry points paths are messed up.
 * <p>
 * {@link EntryPoint#ROOT_ENTRY_POINT} can be used to define an entry point
 * without a path, in that case no other entry point can be defined (as per
 * the previous rule) and that entry point will be used every time.
 * <p>
 * {@link ProcessDoc} can be used on the class containing the entry points to
 * define the documentation that will be printed when asking for help.
 * <p>
 * All classes, entry point methods and option classes must be <b>public static</b>,
 * option fields must be {@code public} and not {@code final}. When working with
 * modules make sure that your packages are {@code open}. If any of these is not
 * respected reflection will fail and error messages can be a bit cryptic.
 */
public class ArgParser {

	private final Class<?> entryPointClass;
	private final String progName;
	private final Object calleeInstance;
	
	private final Branch treeRoot = new Branch("");
	private final Map<Class<?>, ProcessOptions> optionClasses = new HashMap<>();
	private final Map<String, Boolean> optionsTakingArguments = new HashMap<>();
	
	private PrintStream outputStream = System.out;
	private PrintStream errorStream = System.err;
	
	/**
	 * Finds an entry point method in the calling class and executes it.
	 * <p>
	 * This method is designed to be called in the {@code main} function of a CLI tool.
	 * @see ArgParser
	 */
	public static void runHere(String[] args) {
		try {
			new ArgParser(getExecutableName(), ReflectUtils.getCallerClass()).run(args);
		} catch (InvalidDeclarationError e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Returns the name of the executable file containing this class, can be used as the
	 * program name for {@code ArgParser}. For example, if running {@code java -jar argparser.jar}
	 * this method will return {@code "argparser.jar"}.
	 * <p>
	 * If an error occurs, {@code "process"} will be returned instead
	 * 
	 * @return the name of the executable file containing this class
	 */
	public static String getExecutableName() {
		try {
			return new File(ArgParser.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getName();
		} catch (Exception e) {
			return "process";
		}
	}
	
	public ArgParser(String progName, Class<?> entryPointClass) throws InvalidDeclarationError {
		this(progName, entryPointClass, null);
	}
	
	public ArgParser(String progName, Class<?> entryPointClass, Object calleeInstance) throws InvalidDeclarationError {
		this.progName = Objects.requireNonNull(progName);
		this.entryPointClass = Objects.requireNonNull(entryPointClass);
		this.calleeInstance = calleeInstance;
		populateEntryPoints();
	}

	public ArgParser setOutputStream(PrintStream stream) {
		if (stream == null)
			throw new NullPointerException("The outputstream must not be null");
		this.outputStream = stream;
		return this;
	}
	
	public ArgParser setErrorStream(PrintStream stream) {
		if (stream == null)
			throw new NullPointerException("The outputstream must not be null");
		this.errorStream = stream;
		return this;
	}
	
	/**
	 * Calls {@link #run(String[])} after having split the given arguments.
	 * @see StringUtils#splitCLIArgs(String, String)
	 */
	public boolean run(String args) {
		return run(StringUtils.splitCLIArgs(args));
	}
	
	/**
	 * Finds the entry point to run and executes it with the given arguments.
	 * This is the primary method of {@code ArgParser}.
	 * @see ArgParser
	 */
	public boolean run(String[] args) {
		EntryPointFunction entry;
		Object[] argsArray;
		
		try {
			ErrorWrapper errors = new ErrorWrapper("Invalid arguments", false);
			
			List<OptionKeyValuePair> options = new ArrayList<>();
			List<String> entryArguments = new ArrayList<>();
			
			List<String> arguments = args == null ? Collections.emptyList() : new ArrayList<>(Arrays.asList(args));
			boolean isHelpPrint = !arguments.isEmpty() && ArgParserHelper.isHelpPrint(arguments.get(0));
			if(isHelpPrint) arguments.remove(0);
			
			
			// read arguments, options and find the entry point
			Branch entryPointBranch = readArguments(errors, arguments, options, entryArguments);
			
			if(entryPointBranch == treeRoot && (treeRoot.entryPoint == null || isHelpPrint)) {
				printRootHelp();
				return true;
			}
			
			entry = entryPointBranch.entryPoint;
			
			if(isHelpPrint) {
				if(entry == null)
					outputStream.println(getUnfinishedPathUsage(entryPointBranch));
				else
					printEntryPointHelp(entry);
				return true;
			}
			
			// validate that the entry point is valid and that there are enough arguments to match
			if(entry == null) {
				errors.addAndThrow(getUnfinishedPathUsage(entryPointBranch));
			} else if(entryArguments.size() + entry.optionalParamCount() < entry.normalParamCount()) {
				for(int i = entryArguments.size(); i < entry.normalParamCount() - entry.optionalParamCount(); i++)
					errors.add("Missing argument for <" + entry.getParamName(i+(entry.usesOptions()?1:0)) + ">");
				errors.addAndThrow(getEntryUsage(entry));
			} else if(entryArguments.size() > entry.normalParamCount() && !entry.acceptsVarArgs()) {
				errors.addAndThrow("Too many arguments given\n" + getEntryUsage(entry));
			}
			
			argsArray = createArgsArray(errors, entry, options, entryArguments);
			
		} catch (WrappedException e) {
			e.errors.dump(errorStream);
			return false;
		}
		
		runCommand(entry, argsArray);
		return true;
	}
	
	private void populateEntryPoints() throws InvalidDeclarationError {
		for(Method m : entryPointClass.getDeclaredMethods()) {
			EntryPoint annotation = m.getAnnotation(EntryPoint.class);
			if(annotation == null)
				continue;
			
			String path = annotation.path();
			
			try {
				Branch branch = getEntrylessBranch(path);
				ArgParserHelper.validateEntryMethodParameters(m, calleeInstance);
				ProcessOptions opt = getOrCreateOptionClass(m);
				branch.entryPoint = EntryPointFunction.createEntryPointFunction(m, opt);
			} catch (NoSuchMethodException | SecurityException | IllegalArgumentException e) {
				throw new InvalidDeclarationError("Cannot register branch '" + path + "' for method " + m, e);
			}
		}
		if(treeRoot.subBranches.isEmpty() && treeRoot.entryPoint == null)
			throw new InvalidDeclarationError("Class " + entryPointClass + " contains no entry points");
	}
	
	private Branch getEntrylessBranch(String path) throws InvalidDeclarationError {
		String[] parts = path.split(" ");
		Branch current = treeRoot;
		int pl = 0;
		
		if(!ArgParserHelper.isRootBranch(path)) {
			for(String p : parts) {
				if(!ArgParserHelper.canBeBranchName(p))
					throw new InvalidDeclarationError("Name '" + p + "' cannot be used as a branch path");
				
				if(current.entryPoint != null)
					throw new InvalidDeclarationError("Branch '" + path.substring(0, pl) + "' has a declared entry point, it cannot have sub-paths");
				current = current.subBranches.computeIfAbsent(p, _p -> new Branch(_p));
				pl += p.length()+1;
			}
		}
		
		if(current.entryPoint != null)
			throw new InvalidDeclarationError("Branch '" + path + "' already has an entry point");
		if(!current.subBranches.isEmpty())
			throw new InvalidDeclarationError("Branch '" + path + "' already has sub-paths, it cannot be an entry point");
		return current;
	}
	
	private ProcessOptions getOrCreateOptionClass(Method method) throws InvalidDeclarationError {
		if(!ArgParserHelper.doesMethodUseOptions(method))
			return null;
		Class<?> optionsType = method.getParameterTypes()[0];
		ProcessOptions optionsClass = optionClasses.get(optionsType);
		if(optionsClass != null)
			return optionsClass;
		
		optionsClass = ProcessOptions.createOptionsClass(optionsType);
		optionClasses.put(optionsType, optionsClass);
		for(Entry<String, Field> option : optionsClass.getOptionFields().entrySet()) {
			String optName = option.getKey();
			Boolean alreadyDefinedTakesArg = optionsTakingArguments.get(optName);
			boolean takesArg = OptionsHelper.doesOptionTakeArgument(option.getValue().getType());
			if(alreadyDefinedTakesArg != null && takesArg != alreadyDefinedTakesArg)
				throw new InvalidDeclarationError("Option '" + optName + "' was defined in two option classes,"
						+ " only one taking an argument: second occurence" + option.getValue());
			optionsTakingArguments.put(optName, takesArg);
		}
		return optionsClass;
	}
	
	private static Object[] createArgsArray(ErrorWrapper errors, EntryPointFunction entry, List<OptionKeyValuePair> options, List<String> argumentsStrings) throws WrappedException {
		
		Object[] arguments = new Object[entry.totalParameterCount()];
		int argIdx = 0;
		
		// copy default values
		for(int i = 0; i < arguments.length; i++)
			arguments[i] = entry.getParamDefaultValue(i);
		
		// create the OptionClass instance if there are options
		if(entry.usesOptions())
			arguments[argIdx++] = OptionsHelper.createOptionsInstance(options, entry.getOptions(), errors);
		else if(!options.isEmpty())
			errors.addAndThrow("Unexpected options: " + StringUtils.join(", ", options, t -> t.name));
		
		for(int i = 0; i < argumentsStrings.size(); i++) {
			if(argIdx == entry.totalParameterCount()-1 && entry.acceptsVarArgs()) {
				// consume remaing arguments into an array for varargs
				Class<?> varargsType = entry.getParamType(argIdx).componentType();
				Object varargsArray = Array.newInstance(varargsType, argumentsStrings.size()-i);
				for(int j = 0; i < argumentsStrings.size(); i++,j++) {
					try {
						Array.set(varargsArray, j, OptionsHelper.parseOptionValue(
								argumentsStrings.get(i),
								varargsType,
								entry.getParamName(argIdx)));
					} catch (ArgumentError e) {
						errors.add(e.getMessage());
					}
				}
				arguments[argIdx++] = varargsArray;
				break;
			}
			
			try {
				// read a normal argument
				arguments[argIdx] = OptionsHelper.parseOptionValue(
						argumentsStrings.get(i),
						entry.getParamType(argIdx),
						entry.getParamName(argIdx));
				argIdx++;
			} catch (ArgumentError e) {
				errors.add(e.getMessage());
			}
		}
		
		errors.assertNoErrors();
		
		// fail-safe, should be unreachable
		for(int i = 0; i < arguments.length; i++)
			if(arguments[i] == null)
				throw new IllegalStateException("Did not fill argument " + i + " for " + entry.getMethod());
		
		return arguments;
	}
	
	private void runCommand(EntryPointFunction entry, Object[] argsArray) {
		try {
			if (Modifier.isStatic(entry.getMethod().getModifiers()))
				entry.getMethod().invoke(null, argsArray);
			else
				entry.getMethod().invoke(calleeInstance, argsArray);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			if(e.getCause() instanceof Error) {
				cleanStackTrace(e.getCause());
				throw (Error) e.getCause();
			}
			if(e.getCause() instanceof RuntimeException) {
				cleanStackTrace(e.getCause());
				throw (RuntimeException) e.getCause();
			}
			throw new IllegalStateException("Unable to invoke method " + entry.getMethod(), e);
		}
	}
	
	/**
	 * Clean stack trace of uncaught exceptions thrown by the entry point method.
	 * 
	 * <p>
	 * This method removes stack trace elements that come from this class, the
	 * Method class and some jdk internal classes responsible for method invocations
	 * through the reflect api.
	 * 
	 * <p>
	 * It is used by the {@code run} functions of this class to make the trace
	 * clearer, the trace won't contain references to this class but the first call
	 * to a {@code run} function will still appear.
	 * 
	 * <p>
	 * This method may have undesired results if the entry point method uses the
	 * reflect api and does not catch invocation errors, as this method will also
	 * remove these calls from the final stack trace.
	 */
	private static void cleanStackTrace(Throwable t) {
		StackTraceElement[] trace = t.getStackTrace();
		Set<String> filteredClassNames = Set.of(
				ArgParser.class.getName(),
				Method.class.getName(),
				"jdk.internal.reflect.NativeMethodAccessorImpl",      // filter out inaccessible classes, might be
				"jdk.internal.reflect.DelegatingMethodAccessorImpl"); // ineffective based on JDK implementations
		
		// do not alter the stack trace if the exception was raised by us
		if (filteredClassNames.contains(trace[trace.length - 1].getClassName()))
			return;
		
		t.setStackTrace(ArrayOperator.filter(trace, el -> !filteredClassNames.contains(el.getClassName())));
	}
	
	private Branch readArguments(ErrorWrapper errors, List<String> args, List<OptionKeyValuePair> outOptions, List<String> outArguments) throws WrappedException {
		
		Branch currentBranch = treeRoot;
		
		boolean loggedPathError = false;
		boolean foundOptionsEnd = false;
		
		while (!args.isEmpty()) {
			String arg = args.get(0);
			
			if(arg.startsWith("-") && !foundOptionsEnd) {
				if (arg.equals("--")) {
					args.remove(0);
					foundOptionsEnd = true;
				} else {
					// read and consume an option (with or without value)
					readOptionArg(args, outOptions, errors);
				}
				
			} else if(currentBranch.entryPoint == null) {
				// search for the entry point
				args.remove(0);
				if(currentBranch.subBranches.containsKey(arg)) {
					currentBranch = currentBranch.subBranches.get(arg);
				} else if(!loggedPathError) {
					errors.add("Unknown usage - " + arg + "\n" + getUnfinishedPathUsage(currentBranch));
					loggedPathError = true;
				}
				
			} else {
				// read an argument
				args.remove(0);
				outArguments.add(arg);
			}
		}
		
		errors.assertNoErrors();
		return currentBranch;
	}
	
	private void readOptionArg(List<String> args, List<OptionKeyValuePair> outOptions, ErrorWrapper errors) {
		String option = args.remove(0);
		
		// read combined notation -abc
		if(!option.startsWith("--")) {
			char[] chars = option.toCharArray();
			for(int i = 1; i < chars.length-1; i++) {
				String opt = "-" + chars[i];
				Boolean takesArgument = optionsTakingArguments.get(option);
				if(takesArgument != null && takesArgument) {
					errors.add("Option " + opt + " requires a value");
				} else {
					outOptions.add(new OptionKeyValuePair(opt));
				}
			}
			option = "-" + chars[chars.length-1];
		}
		
		Boolean takesArgument = optionsTakingArguments.get(option);
		if(takesArgument != null && takesArgument) {
			if(args.isEmpty()) {
				errors.add("Option " + option + " requires a value");
			} else {
				String nextArg = args.remove(0);
				outOptions.add(new OptionKeyValuePair(option, nextArg));
			}
		} else {
			outOptions.add(new OptionKeyValuePair(option));
		}
	}
	
	private void printEntryPointHelp(EntryPointFunction entryPoint) {
		EntryPoint annotation = entryPoint.getMethod().getAnnotation(EntryPoint.class);
		if(!annotation.help().isBlank())
			outputStream.println(annotation.help());
		outputStream.println(getEntryUsage(entryPoint));
		
		int maxParamNameLength = 0;
		List<String> parameterNames = new ArrayList<>();
		
		int optionsOffset = entryPoint.usesOptions() ? 1 : 0;
		for(int i = 0; i < entryPoint.normalParamCount(); i++) {
			Class<?> argConcreteType = entryPoint.getParamType(i + optionsOffset);
			String argName = entryPoint.getParamName(i + optionsOffset);
			String argType = argConcreteType.isEnum() ?
					StringUtils.join("|", argConcreteType.getEnumConstants()) :
					argConcreteType.getSimpleName();
			String fullName = "  " + argName + " (" + argType + ")";
			parameterNames.add(fullName);
			if(maxParamNameLength < fullName.length())
				maxParamNameLength = fullName.length();
		}
		
		if(maxParamNameLength > 35)
			maxParamNameLength = 35;
		
		for(int i = 0; i < entryPoint.normalParamCount(); i++) {
			String argName = parameterNames.get(i);
			String argDesc = entryPoint.getParamDesc(i + optionsOffset);
			int padding = Math.max(0, maxParamNameLength - argName.length());
			if(!argDesc.isBlank())
				argDesc = " - " + argDesc.replaceAll("\n", "\n"+" ".repeat(maxParamNameLength+2));
			outputStream.println(argName + " ".repeat(padding) + argDesc);
		}
		
		if(!entryPoint.usesOptions())
			return;
		
		maxParamNameLength = 0;
		parameterNames.clear();
		
		Set<Field> optionFields = new HashSet<>(entryPoint.getOptions().getOptionFields().values());
		for(Field optionField : optionFields) {
			Option opt = optionField.getAnnotation(Option.class);
			String fullName = "  " + opt.name();
			if(!opt.shorthand().isBlank())
				fullName += " (" + opt.shorthand() + ")";
			if(OptionsHelper.doesOptionTakeArgument(optionField.getType()))
				fullName += " <" + opt.valueName() + ">";
			parameterNames.add(fullName);
			if(maxParamNameLength < fullName.length())
				maxParamNameLength = fullName.length();
		}

		if(maxParamNameLength > 35)
			maxParamNameLength = 35;
		
		for(Field optionField : optionFields) {
			Option opt = optionField.getAnnotation(Option.class);
			String optDesc = opt.desc();
			String optName = parameterNames.remove(0);
			if(!optDesc.isBlank())
				optDesc = " - " + optDesc.replaceAll("\n", "\n"+" ".repeat(maxParamNameLength+2));
			int padding = Math.max(0, maxParamNameLength - optName.length());
			outputStream.println(optName + " ".repeat(padding) + optDesc);
		}
	}
	
	private void printRootHelp() {
		ProcessDoc doc = entryPointClass.getAnnotation(ProcessDoc.class);
		if(doc != null)
			outputStream.println(doc.doc());
		EntryPointFunction entry = treeRoot.entryPoint;
		if(entry == null) {
			outputStream.println(getUnfinishedPathUsage(treeRoot));
		} else {
			printEntryPointHelp(entry);
		}
	}
	
	private String getUnfinishedPathUsage(Branch currentBranch) {
		return "Usage: " + currentBranch.path + " "
				+ StringUtils.join("|", currentBranch.subBranches.keySet())
				+ " ...\nUse '" + progName + " --help <cmd>' for help";
	}
	
	private String getEntryUsage(EntryPointFunction entry) {
		Method entryMethod = entry.getMethod();
		String usage = "Usage: " + progName;
		if(entry.usesOptions()) {
			Collection<String> availableOptions = entry.getOptions().getAvailableOptionNames();
			if(availableOptions.size() > 3) {
				usage += " (...options)";
			} else {
				for(String opt : availableOptions)
					usage += " (" + opt + ")";
			}
		}
		EntryPoint annotation = entryMethod.getAnnotation(EntryPoint.class);
		String entryPath = annotation.path();
		if(!ArgParserHelper.isRootBranch(entryPath))
			usage += " " + entryPath;
		int optionsOffset = entry.usesOptions() ? 1 : 0;
		int i = 0;
		for( ; i < entry.optionalParamCount(); i++)
			usage += " <" + entry.getParamName(i+optionsOffset) + ">";
		for( ; i < entry.normalParamCount(); i++)
			usage += " [" + entry.getParamName(i+optionsOffset) + "]";
		if(entry.acceptsVarArgs())
			usage += "...";
		return usage;
	}
	
}

class Branch {
	
	final Map<String, Branch> subBranches = new HashMap<>(0);
	final String path;
	EntryPointFunction entryPoint = null;
	
	Branch(String path) {
		this.path = Objects.requireNonNull(path);
	}
	
}