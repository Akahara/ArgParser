package fr.wonder.argparser.tests;

import fr.wonder.argparser.annotations.Argument;
import fr.wonder.argparser.annotations.EntryPoint;
import fr.wonder.argparser.annotations.Option;
import fr.wonder.argparser.annotations.OptionClass;
import fr.wonder.argparser.utils.StringUtils;

public class ProcessArgumentsExotic extends ArgParseTester {

	public ProcessArgumentsExotic(boolean verbose) {
		super(ProcessArgumentsExotic.class, verbose);
	}
	
	public static void main(String[] args) {
		new ProcessArgumentsExotic(false)
			.test(true,  "nonstatic")
			.test(true,  "varargs 1 2 3")
			.test(true,  "varargs")
			.test(true,  "varargsnonempty 1 2 3")
			.test(false, "varargsnonempty")
			.test(true,  "array 1 2 3")
			.test(true,  "options -l 1 -l 2")
			.test(true,  "options -l 1 text -l 2")
			;
	}
	
	private int fieldValue = 3;
	
	@EntryPoint(path = "nonstatic")
	public void nonStatic() {
		if (verbose)
			System.out.println("Non static: " + fieldValue);
	}

	@EntryPoint(path = "varargsnonempty")
	public void varargsNonEmpty(int... varargs) {
		if (verbose) {
			int sum = 0;
			for (int x : varargs) sum += x;
			System.out.println("Varargs: " + sum);
		}
	}
	
	@EntryPoint(path = "varargs")
	@Argument(name = "array", defaultValue = Argument.DEFAULT_EMPTY)
	public void varargs(int... varargs) {
		if (verbose) {
			int sum = 0;
			for (int x : varargs) sum += x;
			System.out.println("Varargs: " + sum);
		}
	}
	
	@EntryPoint(path = "array")
	@Argument(name = "array", defaultValue = Argument.DEFAULT_EMPTY)
	public void arrayEntry(int[] array) {
		if (verbose) {
			int sum = 0;
			for (int x : array) sum += x;
			System.out.println("Array: " + sum);
		}
	}
	
	@OptionClass
	public static class ExoticOptions {
		@Option(name = "--list", shorthand = "-l")
		public String[] stringList;
	}
	
	@EntryPoint(path = "options")
	@Argument(name = "text", defaultValue = Argument.DEFAULT_EMPTY)
	public void optionsEntry(ExoticOptions options, String text) {
		if (verbose) {
			System.out.println("Options: " + text + " - " + StringUtils.join(",", options.stringList));
		}
	}
}
