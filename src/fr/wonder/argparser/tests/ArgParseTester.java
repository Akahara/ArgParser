package fr.wonder.argparser.tests;

import java.io.PrintStream;

import fr.wonder.argparser.ArgParser;
import fr.wonder.argparser.InvalidDeclarationError;
import fr.wonder.argparser.utils.StringUtils;

public class ArgParseTester {
	
	private final Class<?> testedClass;
	protected static boolean verbose;
	
	public ArgParseTester(Class<?> testedClass, boolean verbose) {
		this.testedClass = testedClass;
		ArgParseTester.verbose = verbose;
	}
	
	ArgParseTester test(boolean expectSuccess, String args) {
		return test(expectSuccess, StringUtils.splitCLIArgs(args));
	}
	
	ArgParseTester test(boolean expectSuccess, String[] args) {
		String header = "=== " + StringUtils.toObjectString(args) + " ===";
		if (verbose) {
			System.out.println(header + '\n');
			try {
				new ArgParser(ArgParser.getExecutableName(), testedClass)
					.run(args);
			} catch (InvalidDeclarationError e) {
				e.printStackTrace();
			}
			System.err.flush();
			System.out.println();
			try { Thread.sleep(100); } catch (InterruptedException x) { }
		} else {
			PrintStream voidStream = new PrintStream(PrintStream.nullOutputStream());
			try {
				boolean success = new ArgParser(ArgParser.getExecutableName(), testedClass)
						.setErrorStream(voidStream)
						.setOutputStream(voidStream)
						.run(args);
				if (success && !expectSuccess)
					System.out.println("fail : expected fail but succeeded " + header);
				else if (!success && expectSuccess)
					System.out.println("fail : expected success but failed without error " + header);
				else
					System.out.println("success : got " + (success ? "success" : "error") + " " + header);
			} catch (InvalidDeclarationError e) {
				if (expectSuccess) {
					System.out.println("fail : expected success but got " + e.getClass().getSimpleName() + " " + header);
//					e.printStackTrace(System.out);
				} else {
					System.out.println("success : expected fail and got " + e.getClass().getSimpleName() + " " + header);
				}
			}
		}
		return this;
	}
	
}
