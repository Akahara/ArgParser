package fr.wonder.argparser.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An entry point argument, makes the link between CLI
 * arguments and java method parameters.
 * 
 * <p>{@code Argument} annotations must either not be used or
 * be used for every (non option class) parameter. The order
 * of the annotations must match that of parameters.
 */
@Repeatable(Arguments.class)
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Argument {
	
	/**
	 * Can be used in {@link #defaultValue()} to indicate that the default
	 * value is the empty string, or an empty array for varargs/array types.
	 * 
	 * <p>Simply having {@code @Argument(defaultValue="")} won't do because
	 * it would be interpreted as not having specified {@code defaultValue}
	 * at all, making the parameter non-optional.
	 */
	public static final String DEFAULT_EMPTY = "DeFaUlTeMpTyThAtYoUrUsErWoNtTyPe";
	
	/**
	 * The argument name, does not need to match the parameter's name.
	 * 
	 * <p>The name will be used when displaying help or error messages
	 * regarding that particular parameter.
	 */
	public String name();
	/**
	 * The argument description, printed when help is asked for.
	 */
	public String desc() default "";
	/**
	 * The default value for the argument.
	 * 
	 * <p>When an argument has a default value all the arguments following
	 * it must also have one.
	 * <p>The default value will be interpreted as it would be if the user
	 * typed it in (ie. use "43" to have an int with default value 43).
	 * <p>Default values can be empty, use {@code @Argument(defaultValue = Argument.DEFAULT_EMPTY)}.
	 */
	public String defaultValue() default "";
	
}
