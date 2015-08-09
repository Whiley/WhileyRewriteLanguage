package wyrl;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import wyautl.core.*;
import wyautl.io.PrettyAutomataReader;
import wyautl.io.PrettyAutomataWriter;
import wyautl.rw.*;
import wyautl.util.BatchRewriter;
import wyautl.util.CachingRewriter;
import wyautl.util.EncapsulatedRewriter;
import wyautl.util.SingleStepRewriter;

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
	 * Set of reduction rules for use in this session
	 */
	private final ReductionRule[] reductions;
	
	/**
	 * Set of inference rules for use in this session
	 */
	private final InferenceRule[] inferences;
	
	
	/**
	 * Rewriter being used in this session
	 */
	private Rewriter rewriter;
	
	/**
	 * List of indents being used
	 */
	private String[] indents = {};
	
	/**
	 * Indicate whether or not to print term indices
	 */
	private boolean indices = true;
	
	/**
	 * Enable rewrite caching
	 */
	private boolean caching = true;
	
	/**
	 * Single step rewriting
	 */
	private boolean singleStep = true;
	
	/**
	 * Hide reduction rewrites
	 */
	private boolean hiding = false;	
	
	/**
	 * If true, generate verbose information about rewriting.
	 */
	private boolean verbose;
	
	/**
	 * Records the rewrite history in order to allow backtracking, etc.
	 */
	private final ArrayList<RewriteStep> history = new ArrayList<RewriteStep>();
	
	public ConsoleRewriter(Schema schema, InferenceRule[] inferences, ReductionRule[] reductions) {
		this.schema = schema;
		this.inferences = inferences;
		this.reductions = reductions;		
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
			this.new Command("indent",getMethod("setIndent",String[].class)),
			this.new Command("indices",getMethod("setIndices",boolean.class)),
			this.new Command("caching",getMethod("setCaching",boolean.class)),
			this.new Command("batch",getMethod("setBatch",boolean.class)),
			this.new Command("hiding",getMethod("setHiding",boolean.class)),
			this.new Command("log",getMethod("printLog")),
			this.new Command("rewrite",getMethod("startRewrite",String.class)),
			this.new Command("load",getMethod("loadRewrite",String.class)),
			this.new Command("reduce",getMethod("reduce")),			
			this.new Command("reduce",getMethod("reduce",int.class)),
			this.new Command("infer",getMethod("infer")),
			this.new Command("infer",getMethod("infer",int.class)),
			this.new Command("apply",getMethod("applyActivation",int.class)),
			this.new Command("reset",getMethod("reset",int.class)),
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
			writer.setIndices(indices);
			writer.write(state.automaton());
			writer.flush();
			System.out.println("\n");
			for(int i=0;i!=state.size();++i) {
				Activation activation = state.activation(i);
				System.out.print("[" + i + "] ");
				print(activation,state.step(i));	
				System.out.println();
			}
			String currentHash = String.format("%08x",state.automaton().hashCode());
			System.out.println("\nCurrent: " + currentHash + " (" + countUnvisited() + " / " + state.size() + " unvisited, " + System.identityHashCode(state) + "," + state.automaton().nStates() + ")");
		} catch(IOException e) { System.err.println("I/O error printing automaton"); }
	}
	
	private void print(Activation activation, RewriteStep step) {
		if(activation.rule() instanceof InferenceRule) {
			System.out.print("* ");
		}
		if(activation.rule().name() != null) {
			System.out.print(activation.rule().name());
		}
		System.out.print(" #" + activation.root());
		if(step != null) {
			String afterHash = String.format("%08x",step.after().automaton().hashCode());
			System.out.print(" (" + afterHash + ")");
		}
	}
	
	public void printLog() {
		for(int i = 0;i!=history.size();++i) {
			System.out.print("[" + i + "] ");
			RewriteStep step = history.get(i);			
			RewriteState before = step.before();
			Activation activation = before.activation(step.activation());
			RewriteState after = step.after();
			String beforeHash = String.format("%08x",before.automaton().hashCode());
			String afterHash = String.format("%08x",after.automaton().hashCode());				
			System.out.print(beforeHash + " => " + afterHash);			
			System.out.println(" (" + activation.root() + ", " + activation.rule().name() + ")");
		}
	}		
	
	public void setIndent(String[] indents) {
		this.indents = indents;
	}
	
	public void setIndices(boolean indices) {
		this.indices = indices;
	}
	
	public void setCaching(boolean flag) {
		this.caching = flag;
	}
	
	public void setBatch(boolean flag) {
		this.singleStep = !flag;
	}
	
	public void setHiding(boolean flag) {
		this.hiding = flag;
	}
	
	public void loadRewrite(String input) throws Exception {
		FileReader reader = new FileReader(input);
		startRewrite(reader);
	}
	
	public void startRewrite(String input) throws Exception {
		startRewrite(new StringReader(input));
	}
	
	public void startRewrite(Reader input) throws Exception {
		PrettyAutomataReader reader = new PrettyAutomataReader(input, schema);
		Automaton automaton = reader.read();		
		rewriter = constructRewriter(automaton,schema,reductions,inferences);
		print();
	}
	
	private Rewriter constructRewriter(Automaton automaton, final Schema schema, final ReductionRule[] reductions, InferenceRule[] inferences) {
		Rewriter rewriter;
		if(hiding) {
			EncapsulatedRewriter.Constructor constructor = new EncapsulatedRewriter.Constructor() {
				@Override
				public Rewriter construct(Automaton automaton) {
					return new BatchRewriter(automaton,schema,reductions);
				}				
			};
			rewriter = new EncapsulatedRewriter(constructor,automaton,schema,Activation.RANK_COMPARATOR,inferences); 
		} else {
			RewriteRule[] rules = append(reductions,inferences);
			if(singleStep) {
				rewriter = new SingleStepRewriter(automaton,schema,rules);		
			} else {
				rewriter = new BatchRewriter(automaton,schema,rules);
			}			
		}
		if(caching) {
			rewriter = new CachingRewriter(rewriter);
		}
		return rewriter;
	}
	
	private RewriteRule[] append(RewriteRule[] lhs, RewriteRule[] rhs) {
		RewriteRule[] rules = new RewriteRule[lhs.length+rhs.length];
		System.arraycopy(lhs, 0, rules, 0, lhs.length);
		System.arraycopy(rhs, 0, rules, lhs.length, rhs.length);
		return rules;
	}
	
	public void applyActivation(int activation) {
		RewriteStep step = rewriter.apply(activation);
		history.add(step);
		print();
	}
	
	public void reduce() {
		internalReduce();
		print();
	}
	
	public void internalReduce() {
		int r;
		while((r = selectFirstUnvisited(ReductionRule.class)) != -1) {
			RewriteStep step = rewriter.apply(r);
			history.add(step);
		}		
	}
	
	public void reduce(int count) {
		internalReduce(count);
		print();
	}
	
	public void internalReduce(int count) {
		int r;
		while(count >= 0 && (r = selectFirstUnvisited(ReductionRule.class)) != -1) {
			RewriteStep step = rewriter.apply(r);
			history.add(step);
			count = count - 1;
		}
		print();
	}
	
	public void infer() {
		int r;
		internalReduce();
		while((r = selectFirstUnvisited(InferenceRule.class)) != -1) {
			RewriteStep step = rewriter.apply(r);
			history.add(step);
			internalReduce();
		}
		print();
	}
	
	public void infer(int count) {
		int r;
		internalReduce();
		while(count >= 0 && (r = selectFirstUnvisited(InferenceRule.class)) != -1) {
			RewriteStep step = rewriter.apply(r);
			history.add(step);
			internalReduce();
			count = count - 1;
		}		
		print();
	}
	
	private <T extends RewriteRule> int selectFirstUnvisited(Class<T> kind) {
		RewriteState state = rewriter.state();
		for(int i=0;i!=state.size();++i) {
			if(state.step(i) == null) {
				Activation activation = state.activation(i);
				if(kind.isInstance(activation.rule())) {
					return i;
				}
			}
		}
		return -1;
	}
	
	public int countUnvisited() {
		int count = 0;
		RewriteState state = rewriter.state();
		for(int i=0;i!=state.size();++i) {
			if(state.step(i) == null) {
				count++;
			}
		}
		return count;
	}
	
	public void reset(int id) {
		rewriter.reset(history.get(id).before());		
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
		Command candidate = null;
		for (Command c : commands) {
			if(c.canMatch(line)) {
				if(candidate != null) {
					System.out.println("Ambiguos Command \"" + c.keyword);
				} else {
					candidate = c;
				}
			}
		}
		if(candidate != null) {
			Object[] args = candidate.match(line);
			if (args != null) {
				// Yes, this command was matched. Now, sanity check the
				// arguments.
				for (int i = 0; i != args.length; ++i) {
					if (args[i] == null) {
						// this indicates a problem converting this
						// argument, so report an error to the user.
						System.out.println("Command \"" + candidate.keyword
								+ "\": syntax error on argument " + (i+1));
						return false;
					}
				}
				try {
					// Ok, attemp to execute the command;
					candidate.method.invoke(this, args);
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
		 * Check whether a given line of text could match the given command or
		 * not. Specifically, a command can match if it has the right number of
		 * arguments, and the given command begins with the string command
		 * provided.
		 * 
		 * @param line
		 * @return
		 */
		public boolean canMatch(String line) {
			Class[] parameters = method.getParameterTypes();
			String[] tokens = line.split(" ");
			if (tokens.length != parameters.length + 1) {
				return false;
			} else if (!keyword.startsWith(tokens[0])) {
				return false;
			} else {
				return true;
			}
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
			} else if (!keyword.startsWith(tokens[0])) {
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
