package fr.wonder.argparser.utils;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class StringUtils {

	/**
	 * A {@code Pattern} object that can be used to find string literals in a
	 * string.<br>
	 * The pattern will look for quotes enclosing the text S with {@code S} being a
	 * string containing no {@code "} character except for those preceded by an odd
	 * number of {@code \} (escape char).
	 * 
	 * <p>
	 * Beware that only double quotes are matched.
	 * 
	 * <p>
	 * For example
	 * <blockquote><pre>
	 * String text = "Text: \"citation of \\\"The book\\\"\"";
	 * Matcher m = STRING_PATTERN.matcher(text);
	 * m.find();
	 * System.out.println(m.group());
	 * </pre></blockquote>
	 * 
	 * will print
	 * <blockquote><pre>
	 * citation of "The book"
	 * </pre></blockquote>
	 */
	public static final Pattern STRING_PATTERN = Pattern
			.compile("\"(([^\"\\\\]*)|(\\\\(\\\\\\\\)*[^\\\\]))*(\\\\\\\\)*\"");
	
	/**
	 * The word pattern is just "\w+"
	 */
	public static final Pattern WORD_PATTERN = Pattern
			.compile("\\w+");
	
	/**
	 * Default char sets, initialized at first use only.
	 */
	public static class Charsets {
		
		/** a-z A-Z */
		public static final char[] CHARSET_ALPHABET =
				( "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
				+ "abcdefghijklmnopqrstuvwxyz").toCharArray();
		
		/** 0-9 */
		public static final char[] CHARSET_NUMBERS =
				"0123456789".toCharArray();
		
		/** , . ; : ! ? */
		public static final char[] CHARSET_PONCTUATION =
				",.;:!?".toCharArray();

		/** ( ) { } [ ] ' " & */
		public static final char[] CHARSET_SECTIONS =
				"(){}[]\'\"&".toCharArray();
		
		/** + - / * % &lt; &gt; = & | ~ */
		public static final char[] CHARSET_OPERATORS =
				"+-*/%<>=&|~".toCharArray();
		
		/**
		 * <p>
		 * Not a static variable to avoid storing useless data, if you must use this
		 * value multiple times you should probably cache it yourself
		 * 
		 * <p>
		 * This value contains all the characters that can be used to name variables in
		 * most programming languages :
		 * <ul>
		 * <li>The alphabet (a-z + A-Z)</li>
		 * <li>All digits (0-9)</li>
		 * <li>The underscore (_)</li>
		 * </ul>
		 */
		public static final char[] getCharsetVarName() {
			return
				( "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
				+ "abcdefghijklmnopqrstuvwxyz"
				+ "_0123456789").toCharArray();
		}
	}
	
	/**
	 * This method will remove all white spaces ("\\s" regex) from the given text
	 * without modifying quoted text, see {@link #STRING_PATTERN} for the detection
	 * of quoted text.
	 * 
	 * @param text the text to strip of white spaces
	 * @return the stripped text
	 */
	public static String stripContent(String text) {
		Matcher m = STRING_PATTERN.matcher(text);
		String stripped = "";
		int start = 0;
		while (m.find(start)) {
			stripped += text.substring(start, m.start()).replaceAll("\\s", "");
			stripped += m.group();
			start = m.end();
		}
		stripped += text.substring(start).replaceAll("\\s", "");
		return stripped;
	}

	public static String escape(String text, char quoteSymbol) {
		return text
				.replaceAll("\\", "\\\\")
				.replaceAll(String.valueOf(quoteSymbol), "\\" + quoteSymbol);
	}

	public static String toString(Map<?, ?> map) {
		return map.keySet().stream().map(k -> k + "=" + deepToString(map.get(k)))
				.collect(Collectors.joining(", ", "{", "}"));
	}

	/**
	 * Returns the deep String representation of object {@code o}.
	 * <p>
	 * If {@code o} is {@code null}, {@code "null"} is returned, if
	 * {@code o} is an array, {@link Arrays#deepToString(Object[])}
	 * is returned, otherwise {@code o.toString()} is returned.
	 * 
	 * @param o the object
	 * @return the string representation of the object
	 */
	public static String deepToString(Object o) {
		if (o == null)
			return "null";
		if (o.getClass().isArray()) {
			// o cannot be casted to Object[], use a temporary object array
			String repr = Arrays.deepToString(new Object[] {o});
			return repr.substring(2, repr.length()-2);
		}
		return o.toString();
	}

	@SafeVarargs
	public static <T> String join(String delimiter, T... objects) {
		return join(delimiter, objects, String::valueOf);
	}
	
	public static String join(String delimiter, Collection<?> objects) {
		return join(delimiter, objects, String::valueOf);
	}
	
	public static <T> String join(String delimiter, T[] objects, Function<T, String> function) {
		String[] strings = ArrayOperator.map(objects, new String[objects.length], function);
		return String.join(delimiter, strings);
	}
	
	public static <T> String join(String delimiter, Collection<T> objects, Function<T, String> function) {
		String[] strings = objects.stream().map(function).toArray(String[]::new);
		return String.join(delimiter, strings);
	}
	
	/**
	 * Returns whether {@code c} is a letter (a-z or A-Z)
	 * 
	 * @param c the character
	 * @return {@code true} if {@code c} is a letter
	 */
	public static boolean isLetterChar(char c) {
		return ('a' <= c && c <= 'z') || ('A' <= c && c <= 'Z');
	}
	
	/**
	 * Returns whether {@code c} is a digit (0-9)
	 * 
	 * @param c the character
	 * @return {@code true} if {@code c} is a digit
	 */
	public static boolean isDigitChar(char c) {
		return '0' <= c && c <= '9';
	}
	
	/**
	 * Returns whether {@code c} is a word character, that is if it matches the "\w"
	 * regex.
	 * 
	 * @param c the character to test
	 * @return {@code true} if {@code c} is a word character
	 */
	public static boolean isWordChar(char c) {
		return isLetterChar(c) || c == '_' || isDigitChar(c);
	}
	
	/**
	 * Returns the character corresponding to the given escape sequence character.
	 * <p>
	 * Escaped characters contains special codes such as the line break '\n',
	 * form feed '\f', escape character '\\'...
	 * <p>
	 * This method does not handle some special characters, the ascii octal and
	 * hexadecimal '\xhh' notations are not supported, nor are '\a', '\v', '\?'.
	 * 
	 * @param escaped the escaped character
	 * @return the value of '\escaped'
	 */
	public static char getEscapedChar(char escaped) {
		switch(escaped) {
		case 'b': return '\b';
		case 'f': return '\f';
		case 'n': return '\n';
		case 'r': return '\r';
		case 't': return '\t';
		case '\'': return '\'';
		case '\"': return '\"';
		case '\\': return '\\';
		}
		throw new IllegalArgumentException("Invalid escape sequence \"\\" + escaped + "\"");
	}
	
	/**
	 * Creates a string representation of an object recursively.
	 * 
	 * <p>
	 * This method explores all fields of the given object (recursively)
	 * and appends them to a string representation. This method <b>does not</b>
	 * handle recursive data structures, if there is a cycle in the data
	 * (obj1 has a field with value obj2 and obj2 has a field with value obj1)
	 * this method will cause a stack overflow.
	 * 
	 * <p>
	 * Note that if the object's class is not exported by its module most fields
	 * won't be accessed and "{}" will be returned.
	 */
	public static String toObjectString(Object o) {
		if(o == null)
			return "null";
		Class<?> clazz = o.getClass();
		StringBuilder sb = new StringBuilder();
		if(Number.class.isAssignableFrom(clazz) || clazz == Boolean.class) {
			return o.toString();
		} else if(clazz == String.class) {
			return '"' + (String) o + '"';
		} else if(clazz.isArray()) {
			int len = Array.getLength(o);
			if(len == 0)
				return "[]";
			sb.append('[');
			for(int i = 0; i < len; i++) {
				sb.append(toObjectString(Array.get(o, i)));
				sb.append(", ");
			}
			sb.setLength(sb.length()-2);
			sb.append(']');
		} else if(clazz.isEnum()) {
			return o.toString();
		} else {
			Field[] fields = clazz.getFields();
			if(fields.length == 0)
				return "{}";
			sb.append('{');
			for(Field f : fields) {
				try {
					if(!f.trySetAccessible())
						continue;
					sb.append(f.getName() + "=");
					sb.append(toObjectString(f.get(o)));
					sb.append(", ");
				} catch (IllegalAccessException e) {
					throw new IllegalArgumentException("Unable to access field " + f.getName(), e);
				}
			}
			if(sb.length() > 1)
				sb.setLength(sb.length()-2);
			sb.append("}");
		}
		return sb.toString();
	}
	
	/**
	 * Calls {@link #splitWithQuotes(String, String, char[])} with simple and double quotes
	 * as quote markers.
	 * 
	 * @see #splitWithQuotes(String, String, char[])
	 */
	public static String[] splitWithQuotes(String text, String separator) throws IllegalArgumentException {
		return splitWithQuotes(text, separator, new char[] { '\'', '"' });
	}
	
	/**
	 * Splits a command-line-like line to a list of command parts, preserving quoted strings
	 * and unescaping escaped characters.
	 * 
	 * <code><pre>
	 * splitCLIArgs("git add --verbose "some dir with spaces/foo" "text\\twith\\ttabs")
	 * = new String[] { "git", "add", "--verbose", "some dir with spaces/foo", "text\twith\ttabs" }
	 * </pre></code>
	 * 
	 * @see #splitWithQuotes(String, String, char[])
	 */
	public static String[] splitCLIArgs(String cliLine) throws IllegalArgumentException {
		return splitWithQuotes(cliLine, " ");
	}
	
	/**
	 * Splits a text according to the given separator and quote markers.
	 * <p>
	 * This method is similar to {@code String.split} but handles quotes and not
	 * regexp as separator.
	 * 
	 * <pre><blockquote>
	 * splitWithQuotes("abc \\\" def \"gh ij\" kl", " ", {'"'}) == ["abc", "\"", "def", "gh ij", "kl"]
	 * </blockquote></pre>
	 * <p>
	 * When using multiple quote markers, note that they are not differentiated, for
	 * example if {@code quoteMarkers == ["\"","'"]}, a string that begins with
	 * {@code "} can end with {@code '}, so escape any of these character in a text
	 * sequence.
	 * <p>
	 * Original source code from java2s.com
	 * 
	 * @param text         the text to split
	 * @param separator    a non regexp separator that will be used to split the
	 *                     text
	 * @param quoteMarkers a set of markers used to delimit regions
	 * @return the spliced text
	 */
	public static String[] splitWithQuotes(String text, String separator, char[] quoteMarkers) {
		List<String> strings = new ArrayList<>();
		int level = 0;
		StringBuffer buffer = new StringBuffer();
		for(int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if(ArrayOperator.contains(quoteMarkers, c)) {
				if(i == 0) {
					level++;
					if(level == 1)
						continue;
				} else if(text.startsWith(separator, i - 1)) {
					i += separator.length()-1;
					level++;
					if(level == 1)
						continue;
				} else if(i == text.length() - 1) {
					level--;
					if(level == 0)
						continue;
				} else if(text.startsWith(separator, i + 1)) {
					level--;
					i += separator.length()-1;
					if(level == 0)
						continue;
				} else if(ArrayOperator.contains(quoteMarkers, text.charAt(i + 1))) {
					level--;
					if(level == 0)
						continue;
				}
			} else if(c == '\\' && i < text.length() - 1 &&
					ArrayOperator.contains(quoteMarkers, text.charAt(i + 1))) {
				c = text.charAt(i+1);
				i++;
			} else if(level == 0 && text.charAt(i) == ' ') {
				if(buffer.length() > 0) {
					strings.add(buffer.toString());
					buffer.setLength(0);
				}
			} else {
				buffer.append(c);
			}
		}
		if(buffer.length() != 0)
			strings.add(buffer.toString());
		return strings.toArray(String[]::new);
	}
	
	public static String formatAnsi(String format, Object... args) {
		StringBuilder sb = new StringBuilder(String.format(format, args));
		int sqMarker = -1;
		while((sqMarker = sb.indexOf("$", sqMarker+1)) != -1) {
			if(sqMarker == sb.length()-2)
				break;
			char c = sb.charAt(sqMarker+1);
			int read = 1;
			String ansi = null;
			if(isDigitChar(c)) {
				for(; read < 3; read++) {
					char cc = sb.charAt(sqMarker+1+read);
					if(cc == 'b')
						ansi = "\u001b[48;5;" + sb.substring(sqMarker+1, sqMarker+1+read) + "m";
					else if(cc == 'f')
						ansi = "\u001b[38;5;" + sb.substring(sqMarker+1, sqMarker+1+read) + "m";
					if(!isDigitChar(cc))
						break;
				}
				if(ansi == null)
					throw new IllegalArgumentException("Undefined color sequence " + sb.substring(sqMarker, sqMarker+3));
			} else {
				read++;
				switch(c) {
				case 'R': ansi = "\u001b[0m"; break; // R - reset
				case 'B': ansi = "\u001b[38;5;0m";   break; // B - black
				case 'w': ansi = "\u001b[38;5;255m"; break; // w - white
				case 'r': ansi = "\u001b[38;5;1m";   break; // r - red
				case 'g': ansi = "\u001b[38;5;2m";   break; // g - green
				case 'b': ansi = "\u001b[38;5;4m";   break; // b - blue
				case 'y': ansi = "\u001b[38;5;3m";   break; // y - yellow
				case 'p': ansi = "\u001b[38;5;93m";  break; // p - purple
				case 'c': ansi = "\u001b[38;5;6m";   break; // c - cyan
				case 'l': ansi = "\u001b[38;5;10m";  break; // l - light green
				case '$': ansi = "$"; break;
				default: throw new IllegalArgumentException("Undefined color sequence " + c);
				}
			}
			sb.replace(sqMarker, sqMarker+read, ansi);
		}
		sb.append("\u001b[0m");
		return sb.toString();
	}
	
}
