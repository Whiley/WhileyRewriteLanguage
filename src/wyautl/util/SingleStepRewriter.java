package wyautl.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import wyautl.core.*;
import wyautl.rw.*;

public class SingleStepRewriter extends AbstractRewriter implements Rewriter {
	
	
	public SingleStepRewriter(Automaton automaton, Schema schema, RewriteRule... rules) {
		super(automaton,schema,Activation.RANK_COMPARATOR,rules);
	}

	public SingleStepRewriter(Automaton automaton, Schema schema, Comparator<Activation> comparator,
			RewriteRule... rules) {
		super(automaton,schema,comparator,rules);
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
}
