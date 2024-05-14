package fr.wonder.argparser.tests;

import fr.wonder.argparser.ArgParser;
import fr.wonder.argparser.InvalidDeclarationError;

/**
 * This is a minimal exemple or entrypoints and option classes,
 * see ProcessArgumentsGeneral for more detailed uses.
 */
public class ArgParseExemple {
  
  public static void main(String[] args) {
    try {
      ArgParser.runHere(args);
		} catch (InvalidDeclarationError e) {
			e.printStackTrace();
		}
  }

  @OptionClass
  public static class GitAddOptions {
    @Option(name = "--force", shorthand = "-f")
    public boolean force;
    @Option(name = "--interactive", shorthand = "-i")
    public boolean interactive;
  }
  
  @EntryPoint(path = "add")
  public void gitAdd(GitAddOptions options, String... paths) {
    System.out.println("You called 'git add' with force=" + options.force + " interactive=" + options.interactive + " and " + paths.length + " paths");
  }
}
