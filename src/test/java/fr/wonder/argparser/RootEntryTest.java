package fr.wonder.argparser;

import java.util.Arrays;

import fr.wonder.argparser.annotations.EntryPoint;
import fr.wonder.argparser.annotations.InnerOptions;
import fr.wonder.argparser.annotations.Option;
import fr.wonder.argparser.annotations.OptionClass;
import fr.wonder.argparser.utils.StringUtils;
import org.junit.Test;

import static fr.wonder.argparser.TestUtils.run;

public class RootEntryTest {

	/*
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
	 */
	
	@OptionClass
	public static class SubOptions {
		@Option(name = "--suboption")
		public int suboption;
	}

	@OptionClass
	public static class UnusedSubOptions {
		// collides with Options#val but that does not matter as this class is not used as an inner option
		@Option(name = "--abc")
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
		public GeneralArgumentsTests.EnumFoo val4;
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
		System.out.println("called a with " + Arrays.toString(varargs));
		System.out.println(opt);
	}

	// ----------------- Tests for the above methods -----------------

	@Test
	public void test_rootEntryPoint() {
		run(true, "3");
		run(false, "");
		run(false, "test2");
		run(true, "1 2 2.5 3");
		run(true, "--help");
		run(true, "--suboption 2 .5");
	}

}
