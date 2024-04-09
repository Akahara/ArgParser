package fr.wonder.argparser.tests;

import java.util.Arrays;

import fr.wonder.argparser.annotations.EntryPoint;
import fr.wonder.argparser.annotations.InnerOptions;
import fr.wonder.argparser.annotations.Option;
import fr.wonder.argparser.annotations.OptionClass;
import fr.wonder.argparser.tests.ProcessArguments.EnumFoo;
import fr.wonder.argparser.utils.StringUtils;

public class ProcessArgumentsRootEntry extends ArgParseTester {

	public ProcessArgumentsRootEntry(boolean verbose) {
		super(ProcessArgumentsRootEntry.class, verbose);
	}

	public static void main(String[] args) {
		new ProcessArgumentsRootEntry(false)
			.test(true, "3")                  // O root entry point
			.test(false, "")                  // missing arguments (varargs take at least 1 element)
			.test(false, "test2")             // X invalid argument
			.test(true, "1 2 2.5 3")          // O varargs
			.test(true, "--help")             // O
			.test(true, "--suboption 2 .5")   // O fills an inner option
		;
	}
	
	@OptionClass
	public static class SubOptions {
		@Option(name = "--suboption")
		public int suboption;
	}

	@OptionClass
	public static class UnusedSubOptions {
		@Option(name = "--abc") // collides with Options#val but that does not matter as this class is not used as an inneroption
		public int suboption;
	}
	
	@OptionClass
	public static class Options {
		
		@Option(name = "--abc", shorthand = "-a", desc = "The #val option", valueName = "val")
		public int val;
		@Option(name = "--def", shorthand = "-g")
		public String val2;
		@Option(name = "--bool", shorthand = "-b")
		public boolean b;
		@Option(name = "--bool2", shorthand = "-d")
		public boolean d;
		@Option(name = "--bool3", shorthand = "-q")
		public boolean q = true;
		@Option(name = "--opt", desc = "Enum")
		public EnumFoo val4;
		@InnerOptions
		public SubOptions suboptions;
		
		public UnusedSubOptions unusedSuboptions;
		public int unused;
		
		@Override
		public String toString() {
			return StringUtils.toObjectString(this);
		}
		
	}

	@EntryPoint(path = EntryPoint.ROOT_ENTRY_POINT)
	public static void rootEntryPoint(Options opt, float... varargs) {
		if (verbose) {
			System.out.println("called a with " + Arrays.toString(varargs));
			System.out.println(opt);
		}
	}
	
}
