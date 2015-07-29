package wyrl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import wyautl.core.*;
import wyautl.io.PrettyAutomataReader;
import wyautl.io.PrettyAutomataWriter;
import wyautl.rw.*;
import wyautl.util.SimpleRewriter;

/**
 * Provides a general console-based interface for a given rewrite system. The
 * intention is that this interface can be reused by the generated rewriters.
 * 
 * @author David J. Pearce
 *
 */
public class ConsoleRewriter {
	
	/**
	 * Schema of rewrite system used in this session
	 */
	private final Schema schema;
	
	/**
	 * Set of rewrite rules for use in this session
	 */
	private final RewriteRule[] rules;
	
	/**
	 * Rewriter being used in this session
	 */
	private SimpleRewriter rewriter;
	
	/**
	 * List of indents being used
	 */
	private String[] indents = {};
	
	/**
	 * If true, generate verbose information about rewriting.
	 */
	private boolean verbose;
	
	public ConsoleRewriter(Schema schema, InferenceRule[] inferences, RewriteRule[] reductions) {
		this.schema = schema;
		this.rules = new RewriteRule[inferences.length + reductions.length];
		System.arraycopy(inferences, 0, rules, 0, inferences.length);
		System.arraycopy(reductions, 0, rules, inferences.length, reductions.length);
	}
	
	// =========================================================================
	// Commands.  
	// =========================================================================	
	// Below here is the set of all commands recognised by the interface. If you
	// want to add a new command, then add a public static function for it and
	// an appropriate entry in the commands array.

	/**
	 * The list of commands recognised by the readEvaluatePrintLoop(). To add
	 * more functions, simply extend this list!
	 */
	private Command[] commands = {
			this.new Command("quit",getMethod("quit")),
			this.new Command("help",getMethod("printHelp")),
			this.new Command("verbose",getMethod("setVerbose",boolean.class)),
			this.new Command("print",getMethod("print")),
			this.new Command("indent",getMethod("indent",String[].class)),
			this.new Command("rewrite",getMethod("startRewrite",String.class)),
			this.new Command("apply",getMethod("applyActivation",int.class)),
	};

	public void quit() {
		System.exit(0);
	}

	public void printHelp() {
		System.out.println("Model rail commands:");
		for(Command c : commands) {
			System.out.println("\t" + c.keyword);
		}
	}	
	
	public void setVerbose(boolean verbose) {
		verbose = true;
	}
	
	public void print() {
		try {
			RewriteState state = rewriter.state(); 
			PrettyAutomataWriter writer = new PrettyAutomataWriter(System.out,schema,indents);
			writer.write(state.automaton());
			writer.flush();
			System.out.println("\n");
			for(int i=0;i!=state.size();++i) {
				Activation activation = state.activation(i);
				System.out.println("[" + i + "] #" + activation.root() + ", " + activation.rule().name());
			}
		} catch(IOException e) { System.err.println("I/O error printing automaton"); }
	}
	
	public void indent(String[] indents) {
		this.indents = indents;
	}
	
	public void startRewrite(String input) throws Exception {
		PrettyAutomataReader reader = new PrettyAutomataReader(new StringReader(input), schema);
		Automaton automaton = reader.read();		
		rewriter = new SimpleRewriter(automaton,schema,rules);
		print();
	}
	
	public void applyActivation(int activation) {
		rewriter.apply(activation);
		print();
	}
	
	// =========================================================================
	// Read, Evaluate, Print loop
	// =========================================================================	
	// Below here is all the machinery for the REPL. You shouldn't need to touch
	// this.

	/**
	 * This function provides a simple interface to the model railway system. In
	 * essence, it waits for user input. Each command consists of a line of
	 * text, and has a specific form. The commands are dispatched to handlers
	 * which then interface with the railway. The interface remains in the loop
	 * continually waiting for user input.
	 */
	public void readEvaluatePrintLoop() {
		final BufferedReader input = new BufferedReader(new InputStreamReader(
				System.in));

		try {
			System.out.println("Rewriter read/evaluate/print loop.");
			while (true) {
				System.out.print("> ");
				// Read the input line
				String line = input.readLine();
				// Attempt to execute the input line
				boolean isOK = execute(line);
				if(!isOK) {
					// If we get here, then it means that the command was not
					// recognised. Therefore, print error!
					System.out.println("Error: command not recognised");
				}
			}
		} catch (IOException e) {
			System.err.println("I/O Error - " + e.getMessage());
		}
	}

	/**
	 * Attempt to execute a command-line
	 * @param line
	 * @param railway
	 * @return
	 */
	public boolean execute(String line) {
		for (Command c : commands) {
			Object[] args = c.match(line);
			if (args != null) {
				// Yes, this command was matched. Now, sanity check the
				// arguments.
				for (int i = 0; i != args.length; ++i) {
					if (args[i] == null) {
						// this indicates a problem converting this
						// argument, so report an error to the user.
						System.out.println("Command \"" + c.keyword
								+ "\": syntax error on argument " + (i+1));
						return false;
					}
				}
				try {
					// Ok, attemp to execute the command;
					c.method.invoke(this, args);
					return true;
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					throw (RuntimeException) e.getCause();
				}
			}
		}
		return false;
	}

	/**
	 * This simply returns a reference to a given name. If the method doesn't
	 * exist, then it will throw a runtime exception.
	 * 
	 * @param name
	 * @param paramTypes
	 * @return
	 */
	public static Method getMethod(String name, Class... paramTypes) {
		try {
			return ConsoleRewriter.class.getMethod(name, paramTypes);
		} catch (Exception e) {
			throw new RuntimeException("No such method: " + name, e);
		}
	}

	/**
	 * Represents a given interface command in the railway. Each command
	 * consists of an initial keyword, followed by zero or more parameters. The
	 * class provides simplistic checking of option types.
	 * 
	 * @author David J. Pearce
	 *
	 */
	private class Command {
		public final String keyword;
		public final Method method;

		public Command(String keyword, Method method) {
			this.keyword = keyword;
			this.method = method;
		}

		/**
		 * Check whether a given line of text matches the command or not. For
		 * this to be true, the number of arguments must match the expected
		 * number, and the given keyword must match as well. If so, an array of
		 * the converted arguments is returned; otherwise, null is returned.
		 * When we cannot convert a given argument because it has the wrong
		 * type, a null entry is recorded to help with error reporting,
		 * 
		 * @param line
		 * @return
		 */
		public Object[] match(String line) {
			Class[] parameters = method.getParameterTypes();
			String[] tokens = line.split(" ");
			if (tokens.length != parameters.length + 1) {
				return null;
			} else if (!tokens[0].equals(keyword)) {
				return null;
			} else {
				Object[] arguments = new Object[tokens.length-1];
				for(int i=1;i!=tokens.length;++i) {
					arguments[i-1] = convert(parameters[i-1],tokens[i]);
				}
				return arguments;
			}
		}

		/**
		 * Convert a string representation of this argument into an actual
		 * object form. If this fails for some reason, then null is returned.
		 * 
		 * @param token
		 * @return
		 */
		private Object convert(Class parameter, String token) {
			if(parameter == boolean.class) {
				if(token.equals("off")) {
					return false;
				} else if(token.equals("on")) {
					return true;
				} else {
					return null;
				}
			} else if(parameter == int.class) {
				try {
					return Integer.parseInt(token);
				} catch(NumberFormatException e) {
					return null;
				}
			} else if(parameter == float.class) {
				try {
					return Float.parseFloat(token);
				} catch(NumberFormatException e) {
					return null;
				}
			} else if (parameter == int[].class) {
				String[] numbers = token.split(",");
				int[] array = new int[numbers.length];
				for (int i = 0; i != numbers.length; ++i) {
					try {
						array[i] = Integer.parseInt(numbers[i]);
					} catch (NumberFormatException e) {
						return null;
					}
				}
				return array;
			} else if(parameter == String.class) {
				return token;
			} else if (parameter == String[].class) {
				return token.split(",");
			} else {
				// In this case, the argument was not recognised.
				return null;
			}
		}
	}	
}
