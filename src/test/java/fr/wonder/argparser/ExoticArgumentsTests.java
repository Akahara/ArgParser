package fr.wonder.argparser;

import fr.wonder.argparser.annotations.Argument;
import fr.wonder.argparser.annotations.EntryPoint;
import fr.wonder.argparser.annotations.Option;
import fr.wonder.argparser.annotations.OptionClass;
import fr.wonder.argparser.utils.StringUtils;
import org.junit.Test;

import static fr.wonder.argparser.TestUtils.*;

public class ExoticArgumentsTests {

	public ExoticArgumentsTests() {}
	
	private int fieldValue = 3;
	
	@EntryPoint(path = "nonstatic")
	public void nonStatic() {
		System.out.println("Non static: " + fieldValue);
	}

	@EntryPoint(path = "varargsnonempty")
	public static void varargsNonEmpty(int... varargs) {
		int sum = 0;
		for (int x : varargs) sum += x;
		System.out.println("Varargs: " + sum);
	}
	
	@EntryPoint(path = "varargs")
	@Argument(name = "array", defaultValue = Argument.DEFAULT_EMPTY)
	public static void varargs(int... varargs) {
		int sum = 0;
		for (int x : varargs) sum += x;
		System.out.println("Varargs: " + sum);
	}
	
	@EntryPoint(path = "array")
	@Argument(name = "array", defaultValue = Argument.DEFAULT_EMPTY)
	public static void arrayEntry(int[] array) {
		int sum = 0;
		for (int x : array) sum += x;
		System.out.println("Array: " + sum);
	}
	
	@OptionClass
	public static class ExoticOptions {
		@Option(name = "--list", shorthand = "-l")
		public String[] stringList;
	}
	
	@EntryPoint(path = "options")
	@Argument(name = "text", defaultValue = Argument.DEFAULT_EMPTY)
	public static void optionsEntry(ExoticOptions options, String text) {
		System.out.println("Options: " + text + " - " + StringUtils.join(",", options.stringList));
	}

	// ----------------- Tests for the above methods -----------------

	@Test
	public void test_nonStatic() {
		runWithInstance(true, "nonstatic");
	}

	@Test
	public void test_varargsNonEmpty() {
		runWithInstance(true, "varargsnonempty 1 2 3");
		runWithInstance(false, "varargsnonempty");
	}

	@Test
	public void test_varargs() {
		runWithInstance(true, "varargs 1 2 3");
		runWithInstance(true, "varargs");
	}

	@Test
	public void test_array() {
		runWithInstance(true, "array 1 2 3");
		runWithInstance(true, "array");
	}

	@Test
	public void test_options() {
		runWithInstance(true, "options -l 1 -l 2");
		runWithInstance(true, "options -l 1 text -l 2");
	}
}
