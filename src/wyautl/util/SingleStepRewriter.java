package wyautl.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import wyautl.core.*;
import wyautl.rw.*;

public class SingleStepRewriter implements Rewriter {
	
	/**
	 * The schema used by automata being reduced. This is primarily useful for
	 * debugging purposes.
	 */
	protected final Schema schema;
	
	/**
	 * The list of rewrite rules which the rewriter can apply.
	 */
	private final RewriteRule[] rules;
	
	/**
	 * Used to sort activations generated for a given state. This allows for
	 * some heuristics which reduce the amount of rewriting required.
	 */
	private final Comparator<Activation> comparator;
	
	/**
	 * The current state of the rewriter
	 */
	private RewriteState state;
	
	public SingleStepRewriter(Automaton automaton, Schema schema, RewriteRule... rules) {
		this(automaton,schema,Activation.RANK_COMPARATOR,rules);
	}

	public SingleStepRewriter(Automaton automaton, Schema schema, Comparator<Activation> comparator,
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
	public void reset(RewriteState state) {
		this.state = state;
	}
	
	/**
	 * Apply a given activation on this state to potentially produce an updated
	 * state.
	 * 
	 * @param activation
	 * @return
	 */
	@Override
	public RewriteStep apply(int index) {
		Activation activation = state.activation(index);
		Automaton automaton = new Automaton(state.automaton());
		int from = activation.root();
		int target = activation.apply(automaton);		
		RewriteState nextState;

		if (target != Automaton.K_VOID && from != target) {
			automaton.minimise();
			automaton.compact();			
			nextState = initialise(automaton);			
		} else {
			// activation did not apply
			nextState = state;
		}
		
		RewriteStep step = new RewriteStep(state, index, nextState);
		state.update(index, step);
		state = nextState;
		return step;
	}

	/**
	 * Probe every rule against every valid automaton state to produce the
	 * complete list of possible activations.
	 * 
	 * @param automaton
	 * @return
	 */
	private RewriteState initialise(Automaton automaton) {
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
}
