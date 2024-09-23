package fr.wonder.argparser.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Marks a member of an {@link OptionClass} as an options subset.
 * <p>
 * The annotated member must also be an {@code OptionClass}.
 * <p>
 * For example, here {@code OptionsBar} and {@code OptionsBaz} both
 * inherit {@code --verbose} as an option.
 * <blockquote><pre>
 * {@literal @}Options
 * class OptionsFoo {
 *   {@literal @}Option(name = "--verbose")
 *   boolean verbose = false;
 * }
 * 
 * {@literal @}Options
 * class OptionsBar {
 *   {@literal @}InnerOptions
 *   OptionsFoo foo;
 * }
 * 
 * {@literal @}Options
 * class OptionsBaz extends OptionsFoo {
 * }
 * </pre></blockquote>
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface InnerOptions {

}
