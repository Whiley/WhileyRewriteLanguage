package wyautl.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import wyautl.core.Automaton;
import wyautl.core.Schema;
import wyautl.rw.Activation;
import wyautl.rw.*;

public abstract class AbstractRewriter implements Rewriter {
	
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
	 * Used to sort activations generated for a given state. This allows for
	 * some heuristics which reduce the amount of rewriting required.
	 */
	protected final Comparator<Activation> comparator;
	
	/**
	 * The current state of the rewriter
	 */
	protected RewriteState state;

	public AbstractRewriter(Schema schema, Comparator<Activation> comparator,
			RewriteRule... rules) {
		this.schema = schema;
		this.rules = rules;
		this.comparator = comparator;
	}
	
	public AbstractRewriter(Automaton automaton, Schema schema, Comparator<Activation> comparator,
			RewriteRule... rules) {
		this.schema = schema;
		this.rules = rules;
		this.comparator = comparator;
		this.state = initialise(automaton);
	}
	
	@Override
	public RewriteState state() {
		return state;
	}

	/**
	 * Reset the rewriter to a previous state. This is useful for backtracking,
	 * amongst other things.
	 * 
	 * @param state
	 */
	@Override
	public void reset(RewriteState state) {
		this.state = state;
	}
	
	@Override
	public abstract RewriteStep apply(int choice);
	
	@Override	
	public RewriteStep apply() {
		return apply(selectFirstUnvisited());
	}
	
	/**
	 * Probe every rule against every valid automaton state to produce the
	 * complete list of possible activations.
	 * 
	 * @param automaton
	 * @return
	 */
	protected RewriteState initialise(Automaton automaton) {
		ArrayList<Activation> activations = new ArrayList<Activation>();
		for (int s = 0; s != automaton.nStates(); ++s) {
			Automaton.State state = automaton.get(s);
			// Check whether this state is a term or not; that's because only
			// terms can be roots for rewrite rule applications.
			if (state instanceof Automaton.Term) {
				for (int r = 0; r != rules.length; ++r) {
					rules[r].probe(automaton, s, activations);
				}
			}
		}
		Activation[] array = activations.toArray(new Activation[activations.size()]);
		if(comparator != null) {
			Arrays.sort(array,comparator);
		}
		return new RewriteState(automaton, array);
	}
	
	protected int selectFirstUnvisited() {
		for(int i=0;i!=state.size();++i) {
			if(state.step(i) == null) {
				return i;
			}
		}
		return -1;
	}
}
