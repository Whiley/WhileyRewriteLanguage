import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import wyautl.core.Automaton;
import wyautl.io.PrettyAutomataWriter;
import wyautl.rw.*;
import wyautl.util.*;

public final class Main {

    private Main() {} // avoid instantiation of this class

    public static void main(String[] args) {
	final BufferedReader input =
	    new BufferedReader(new InputStreamReader(System.in));

	try {
	    System.out.println("Welcome!\n");
	    while(true) {
		System.out.print("> ");
		String text = input.readLine();

		// commands goes here
		if(text.equals("help")) {
		    printHelp();
		} else if(text.equals("exit")) {
		    System.exit(0);
		} else {
		    reduce(text);
		}
	    }
	} catch(IOException e) {
	    System.err.println("I/O Error - " + e.getMessage());
	}
    }

    private static void reduce(String text) {
		try {
			Parser parser = new Parser(text);
			Automaton automaton = new Automaton();
			int root = parser.parse(automaton);
			automaton.setRoot(0, root);

			PrettyAutomataWriter writer = new PrettyAutomataWriter(System.out,
					Logic.SCHEMA, "Or", "And");
			System.out.println("------------------------------------");
			writer.write(automaton);
			writer.flush();
			InferenceRule[] inferences = Logic.inferences;
			ReductionRule[] reductions = Logic.reductions;
			RewriteRule[] rules = new RewriteRule[inferences.length + reductions.length];
			System.arraycopy(inferences, 0, rules, 0, inferences.length);
			System.arraycopy(reductions, 0, rules, inferences.length, reductions.length);
			SingleStepRewriter rewriter = new SingleStepRewriter(automaton,Logic.SCHEMA,rules);
			int count = 0;
			while(rewriter.state().size() > 0) {
			    rewriter.apply(0);
			    count = count + 1;
			}
			System.out.println("\n\n=> (" + count + " steps)\n");
			writer.write(rewriter.state().automaton());
			writer.flush();
			System.out.println("\n");
	} catch(RuntimeException e) {
	    // Catching runtime exceptions is actually rather bad style;
	    // see lecture about Exceptions later in the course!
	    System.err.println("error: " + e.getMessage());
	    e.printStackTrace(System.err);
	    System.err.println("Type \"help\" for help");
	} catch(IOException e) {
	    System.err.println("I/O Exception?");
	}
    }

    private static void printHelp() {
	System.out.println("Calculator commands:");
	System.out.println("\thelp --- access this help page");
	System.out.println("\texit --- quit");
    }
}
