package wyrw.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import wyautl.core.Automaton;
import wyautl.core.Schema;
import wyrw.core.Activation;
import wyrw.core.Rewrite;
import wyrw.core.RewriteRule;

public class AbstractRewrite implements Rewrite {
	/**
	 * The schema used by automata being reduced. This is primarily useful for
	 * debugging purposes.
	 */
	protected final Schema schema;
	
	/**
	 * The list of rewrite rules which the rewriter can apply.
	 */
	protected final RewriteRule[] rules;
	
	/**
	 * The list of states in the rewrite.
	 */
	protected final ArrayList<Rewrite.State> states = new ArrayList<Rewrite.State>();
	
	/**
	 * The list of rewrite steps which have been taken so far. Each step refers
	 * to a state in the list above.
	 */
	protected final ArrayList<Rewrite.Step> steps = new ArrayList<Rewrite.Step>();
	
	/**
	 * Used to sort activations generated for a given state. This allows for
	 * some heuristics which reduce the amount of rewriting required.
	 */
	protected final Comparator<Activation> comparator;

	public AbstractRewrite(Schema schema, Comparator<Activation> comparator,
			RewriteRule... rules) {
		this.schema = schema;
		this.rules = rules;
		this.comparator = comparator;
	}
	
	@Override
	public List<Step> steps() {
		return Collections.unmodifiableList(steps);
	}

	@Override
	public List<State> states() {
		return Collections.unmodifiableList(states);
	}

	@Override
	public int add(Automaton automaton) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int add(Step step) {
		int index = steps.size();
		steps.add(step);
		return index;
	}	
}
