package fr.wonder.argparser.utils;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Useful class for error handling, a common use of an error wrapper is as follow:
 * <blockquote><pre>
 * void foo() {
 *   ErrorWrapper errors = new ErrorWrapper("Something happened");
 *   try {
 *     ...
 *     bar(..., errors.subErrors("Unable to bar!"));
 *     errors.assertNoErrors();
 *     ...
 *     // possible reuse of #errors
 *   } catch (WrappedException x) {
 *     errors.dump();
 *   }
 * }
 * 
 * void bar(..., ErrorWrapper errors) {
 *   ...
 *   if(thingsWentWrong)
 *     errors.add("Things went wrong!");
 *   ...
 * }
 * </pre></blockquote>
 */
public class ErrorWrapper {
	
	private final boolean logTraces;
	private final ErrorWrapper parent;
	
	private final String header;
	private final List<String> errors = new ArrayList<>();
	private boolean parentContainsThis = false;
	
	private final List<ErrorWrapper> subErrors = new ArrayList<>();

	public ErrorWrapper(String header) {
		this(header, false);
	}

	public ErrorWrapper(String header, boolean logTraces) {
		this(null, header, logTraces);
		this.parentContainsThis = true;
	}

	private ErrorWrapper(ErrorWrapper parent, String header, boolean logTraces) {
		this.parent = parent;
		this.header = header;
		this.logTraces = logTraces;
	}
	
	public void add(String s) {
		if(errors.isEmpty() && parent != null)
			addToParentChildren();
		for(String l : s.split("\n"))
			errors.add(l);
		if(logTraces) {
			StackTraceElement[] trace = new Exception().getStackTrace();
			for(int i = 1; i < trace.length; i++)
				errors.add(" from " + trace[i].toString());
		}
	}
	
	public void addAndThrow(String s) throws WrappedException {
		add(s);
		assertNoErrors();
	}
	
	private void addToParentChildren() {
		if(!parentContainsThis) {
			parent.addToParentChildren();
			parent.subErrors.add(this);
			parentContainsThis = true;
		}
	}

	public void trace(String s) {
		StringWriter w = new StringWriter();
		new Error().printStackTrace(new PrintWriter(w));
		add(s + w.toString());
	}
	
	public ErrorWrapper subErrors(String header) {
		return new ErrorWrapper(this, header, logTraces);
	}
	
	public boolean noErrors() {
		return errors.isEmpty() && subErrors.isEmpty();
	}
	
	public void dump() {
		dump(System.err);
	}
	
	public void dump(PrintStream out) {
		if(parent != null) {
			parent.dump(out);
		} else {
			dump(out, 0);
		}
	}
	
	private void dump(PrintStream out, int level) {
		if(noErrors())
			return;
		out.println("| ".repeat(level) + header+":");
		for(String e : errors)
			out.println("| ".repeat(level+1) + e);
		for(ErrorWrapper sub : subErrors)
			sub.dump(out, level+1);
	}
	
	/**
	 * A function that uses this method at least once should call it again after the
	 * last {@link #add(String)}, this way the catcher won't have to check if errors
	 * were generated. Usually the simplest is to put an additional
	 * {@code errors.assertNoErrors()} at the end of the function's body.
	 */
	public void assertNoErrors() throws WrappedException {
		if(!noErrors())
			throw new WrappedException(this);
	}
	
	/**
	 * The exception thrown by {@link ErrorWrapper#assertNoErrors()}, it contains
	 * the error wrapper that throw the exception
	 */
	public static class WrappedException extends Exception {

		private static final long serialVersionUID = 2860084951467460601L;
		
		public final ErrorWrapper errors;
		
		private WrappedException(ErrorWrapper wrapper) {
			super("Errors occured");
			this.errors = wrapper;
		}
		
		@Override
		public void printStackTrace(PrintStream stream) {
			super.printStackTrace(stream);
			errors.dump(stream);
		}
		
	}
	
}
