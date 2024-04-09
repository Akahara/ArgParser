package fr.wonder.argparser.tests;

import java.io.File;

import fr.wonder.argparser.annotations.Argument;
import fr.wonder.argparser.annotations.EntryPoint;
import fr.wonder.argparser.annotations.Option;
import fr.wonder.argparser.annotations.OptionClass;
import fr.wonder.argparser.annotations.ProcessDoc;
import fr.wonder.argparser.utils.StringUtils;

@ProcessDoc(doc = ""
		+ "----------------------------------\n"
		+ "Documentation example\n"
		+ "\n"
		+ "Will be printed if no arguments\n"
		+ "are provided and the root is not\n"
		+ "an entry point, or --help is used\n"
		+ "on the root branch\n"
		+ "----------------------------------\n")
public class ProcessArguments extends ArgParseTester {
	
	public ProcessArguments(boolean verbose) {
		super(ProcessArguments.class, verbose);
	}
	
	public static void main(String[] args) {
		new ProcessArguments(false)
			.test(true,  "test2 enum E1")               // O
			.test(false, "test2 enum E1 e2")            // X too many arguments
			.test(false, "test2 enum a")                // X invalid enum value
			.test(false, "test2")                       // X incomplete path
			.test(true,  "test2 file f")                // O prints the absolute path of file 'f'
			.test(true,  "")                            // O prints help
			.test(true,  "--help")                      // O prints help
			.test(true,  "--help print")                // O prints help for the print entry point
			.test(true,  "print")                       // O prints with default value 4
			.test(false, "print e")                     // X invalid argument value
			.test(true,  "print 6")                     // O prints with value 6
			.test(false, "print --abc")                 // X missing option value
			.test(false, "print --abc d")               // X invalid option value
			.test(true,  "print --abc 7")               // O
			.test(false, "test2 file --bool fileArg")   // X extra option --bool
			.test(true,  "test2 file fileArg")          // O
			.test(true,  "test2 file /a.txt")           // O
			.test(true,  "print --bool --opt E3")       // O
			.test(true,  "? print -b -o E3")            // O
			.test(true,  "print -bdq")                  // O
			.test(true,  "print -bd -q")                // O
			.test(true,  "print -bdg valForG")          // O
			.test(true,  "test2 array \"s t \\\\\"r\"") // O quoted string
			.test(false, "abc \\\" def \"gh ij\" kl")   // X too many args
			.test(true,  "testempty")                   // O 2 default values
			.test(true,  "testempty a")                 // O 1 default value
			.test(true,  "testempty a b")               // O
			;
	}

	
	public static enum EnumFoo {
		
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

	@Argument(name = "i", desc = "i argument", defaultValue = "4")
	@EntryPoint(path = "print", help = PRINT_ENTRY_HELP)
	public static void print(Options opt, int i) {
		if (verbose) {
			System.out.println("called a with " + i);
			System.out.println(opt);
		}
	}
	
	@Argument(name = "i", desc = "i argument")
	@Argument(name = "j", desc = "j argument")
	@EntryPoint(path = "test")
	public static void test(Options opt, int i, int j) {
		if (verbose) {
			System.out.println("called b with " + i + " " + j);
			System.out.println(opt);
		}
	}

	@Argument(name = "f", desc = "file argument")
	@EntryPoint(path = "test2 file")
	public static void test2file(File f) {
		if (verbose)
			System.out.println("called test2file with " + f);
	}
	
	@Argument(name = "f", desc = "file argument")
	@EntryPoint(path = "test2 array")
	public static void test2array(String f) {
		if (verbose)
			System.out.println("called test2array with " + f);
	}
	
	@Argument(name = "f", desc = "enum argument")
	@EntryPoint(path = "test2 enum")
	public static void test2enum(EnumFoo f) {
		if (verbose)
			System.out.println("called test2enum with " + f);
	}
	
	@Argument(name = "first", defaultValue = "defaultS1")
	@Argument(name = "second", defaultValue = Argument.DEFAULT_EMPTY)
	@EntryPoint(path = "testempty")
	public static void testEmptyString(String s1, String s2) {
		if (verbose)
			System.out.println("called testempty with s1='" + s1 + "' s2='" + s2 + "'");
	}
	
}
