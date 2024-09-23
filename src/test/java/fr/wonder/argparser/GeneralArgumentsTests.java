package fr.wonder.argparser;

import java.io.File;

import fr.wonder.argparser.annotations.Argument;
import fr.wonder.argparser.annotations.EntryPoint;
import fr.wonder.argparser.annotations.Option;
import fr.wonder.argparser.annotations.OptionClass;
import fr.wonder.argparser.annotations.ProcessDoc;
import fr.wonder.argparser.utils.StringUtils;
import org.junit.Test;

import static fr.wonder.argparser.TestUtils.run;

@ProcessDoc(doc = ""
		+ "----------------------------------\n"
		+ "Documentation example\n"
		+ "\n"
		+ "Will be printed if no arguments\n"
		+ "are provided and the root is not\n"
		+ "an entry point, or --help is used\n"
		+ "on the root branch\n"
		+ "----------------------------------\n")
public class GeneralArgumentsTests {
	
	public enum EnumFoo {
		
		E1,
		E2,
		E3;
		
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
		
		public int unused;
		
		@Override
		public String toString() {
			return StringUtils.toObjectString(this);
		}
		
	}
	
	private static final String PRINT_ENTRY_HELP =
			  "This will be displayed when the user asks\n"
			+ "for help for the 'print' entry point, for\n"
			+ "example using '--help print'.";

	@EntryPoint(path = "print", help = PRINT_ENTRY_HELP)
	@Argument(name = "i", desc = "i argument", defaultValue = "4")
	public static void print(Options opt, int i) {
		System.out.println("called a with " + i);
		System.out.println(opt);
	}
	
	@EntryPoint(path = "test")
	@Argument(name = "i", desc = "i argument")
	@Argument(name = "j", desc = "j argument")
	public static void test(Options opt, int i, int j) {
		System.out.println("called b with " + i + " " + j);
		System.out.println(opt);
	}

	@EntryPoint(path = "test2 file")
	@Argument(name = "f", desc = "file argument")
	public static void test2file(File f) {
		System.out.println("called test2file with " + f);
	}
	
	@EntryPoint(path = "test2 array")
	@Argument(name = "f", desc = "file argument")
	public static void test2array(String f) {
		System.out.println("called test2array with " + f);
	}
	
	@EntryPoint(path = "test2 enum")
	@Argument(name = "f", desc = "enum argument")
	public static void test2enum(EnumFoo f) {
		System.out.println("called test2enum with " + f);
	}
	
	@EntryPoint(path = "testempty")
	@Argument(name = "first", defaultValue = "defaultS1")
	@Argument(name = "second", defaultValue = Argument.DEFAULT_EMPTY)
	public static void testEmptyString(String s1, String s2) {
		System.out.println("called testempty with s1='" + s1 + "' s2='" + s2 + "'");
	}
	
	@EntryPoint(path = "test3")
	public static void test3(boolean b) {
		System.out.println(b);
	}


	// ----------------- Tests for the above methods -----------------

	@Test
	public void test_types() {
		run(false, "test2");
		run(true, "test2 enum E1");
		run(false, "test2 enum E1 e2");
		run(false, "test2 enum a");
		run(true, "test2 file f");
		run(false, "test2 file --bool fileArg");
		run(true, "test2 file fileArg");
		run(true, "test2 file /a.txt");
		run(true, "test2 array \"s t \\\\\"r\"");
	}

	@Test
	public void test_help() {
		run(true, "");
		run(true, "--help");
		run(true, "--help print");
		run(true, "? print -b -o E3");
	}

	@Test
	public void test_print() {
		run(true, "print");
		run(false, "print e");
		run(true, "print 6");
		run(false, "print --abc");
		run(false, "print --abc d");
		run(true, "print --abc 7");
		run(true, "print --bool --opt E3");
		run(true, "print -bdq");
		run(true, "print -bd -q");
		run(true, "print -bdg valForG");
	}

	@Test
	public void test_stringParsing() {
		run(false, "abc \\\" def \"gh ij\" kl");
	}

	@Test
	public void test_testEmpty() {
		run(true, "testempty");
		run(true, "testempty a");
		run(true, "testempty a b");
	}

	@Test
	public void test_booleanParsing() {
		run(true, "test3 true");
		run(true, "test3 0");
	}

}
