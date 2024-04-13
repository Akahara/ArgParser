# ArgParser

Utility to create command line interfaces.

The basic idea is to define entry points in a class and call `ArgParser.runHere(/*...*/)` to run the right one with user-supplied arguments and options.

Look [here](/src/wonder/argparser/tests/ProcessArgumentsGeneral.java) for an example.

Note that reflection is heavily used, impacting performances. This utility is
not meant to be used often! It can still be used to run user commands on the
fly in a shell-like interface and not only as a CLI.

## Anatomy of a command
```
(command name) [options...] (entry point path) [arguments...]
```

For example:
```bash
git --force -i add somedir1 somedir2
```
- `git` is the command, that part is not handled by `ArgParser`
- `--force` is a boolean long option
- `-i` is a boolean short option
- `add` is an entry point path, this implementation supports branching paths (eg. paths with multiple words)
- `somedir1/2` are arguments
That would translate as such
```java
@OptionClass
public static class GitAddOptions {
  @Option(name = "--force", shorthand = "-f")
  public boolean force;
  @Option(name = "--interactive", shorthand = "-i")
  public boolean interactive;
}

@EntryPoint(path = "add")
public void gitAdd(GitAddOptions options, String... paths) {
  // ...
}
```

- Boolean options do not take argument (`-a` instead of `-a true`)
- Boolean short options can be combined (`-a -b` can be shortened to `-ab`)
- It's currently impossible to have two option classes with options that have the same name but only one is boolean, because we cannot know if the next argument is the value of the option or not

## Anatomy of an entry point function
- `@EntryPoint` makes a function callable, the `path` arguments defines how to call it (eg. `path="add"` or `path="remove"` for `git add` or `git remove`)
- `@Argument` annotations are used to document entry points arguments and make them optional/give them default values, there must be none or one per argument
- Supported argument types are `String`, all native types (int, float...), all wrapped native types (Integer, Float...), `File` and any `enum` type
- The last argument can be a vararg (`void entrypoint(int... args)`) or an array `void entrypoint(int[] args)`

## Working with options

- Options are defined in Option Classes using `@OptionClass`
- Long option names must start with two dashes (`--name`) and are required, short options must start with a single dash and end with a single characted (`-v`)
- `--help`, `help` and `?` are built-in to display help for an entry point or for the program, they cannot be used as options or entry point paths
- In doubt see methods in [ArgParserHelper](/src/wonder/argparser/ArgParserHelper.java)
- Supported option types are the same as argument types, arrays are supported, options that are specified multiple times will fill the array (eg. `-a val1 -a val2` becomes `String[]{"val1","val2"}`)
- Option classes can be inherited by other option classes or contained as members using `@InnerOption`

See the [here](/src/wonder/argparser/tests/ProcessArgumentsGeneral.java) for more details.


## Implementation notes
- Methods in this package may throw `InvalidDeclarationError`, unless otherwise specified, this occurs when annotations are wrongs, methods or
fields are not public or entry points paths are messed up.
- `@EntryPoint(path = EntryPoint.ROOT_ENTRY_POINT)` can be used to define an entry point without a path, in that case no other entry point can be defined
- `@ProcessDoc` can be used on the class containing the entry points to define the documentation that will be printed when asking for help

> All classes, entry point methods and option classes must be `public` or `public static`\
> Option fields must be `public` and not `final`\
> When working with modules make sure that your packages are `open`\
> If any of these is not respected reflection will fail and error messages can be a bit cryptic.
