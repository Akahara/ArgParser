package fr.wonder.argparser;

import fr.wonder.argparser.annotations.EntryPoint;
import fr.wonder.argparser.annotations.Option;
import fr.wonder.argparser.annotations.OptionClass;

import java.io.File;

/**
 * This is a minimal exemple or entrypoints and option classes,
 * see {@link GeneralArgumentsTests} for more detailed uses.
 */
public class ArgParseExemple {
  
  public static void main(String[] args) {
    ArgParser.runHere(args);
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

  @EntryPoint(path = "clone")
  public void gitClone(String source, File destination) {
    System.out.println("You called 'git clone' from '" + source + "' to '" + destination.getAbsolutePath() + "'");
  }

}
