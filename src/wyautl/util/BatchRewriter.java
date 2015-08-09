package wyautl.util;

import java.util.Comparator;

import wyautl.core.*;
import wyautl.rw.*;

public class BatchRewriter extends AbstractRewriter implements Rewriter {
	
	public BatchRewriter(Automaton automaton, Schema schema, RewriteRule... rules) {
		super(automaton,schema,Activation.RANK_COMPARATOR,rules);
	}

	public BatchRewriter(Automaton automaton, Schema schema, Comparator<Activation> comparator,
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
		RewriteState before = state;
		Automaton automaton = new Automaton(state.automaton());
		int r;
		while ((r = selectFirstUnvisited(state)) != -1) {
			RewriteState nextState = inplaceRewrite(r, automaton);
			state.update(r, new RewriteStep(state,r,nextState));
			state = nextState;			
		}
		//
		return new RewriteStep(before, index, state);
	}
	
	/**
	 * Apply an activation to the automaton in place. That is, the current
	 * automaton is itself updated.
	 * 
	 * @param index
	 * @param automaton
	 * @return
	 */
	private RewriteState inplaceRewrite(int index, Automaton automaton) {
		Activation activation = state.activation(index);
		int from = activation.root();
		int target = activation.apply(automaton);
		RewriteState nextState;

		if (target != Automaton.K_VOID && from != target) {
			// TODO: can we get rid of this?
			automaton.minimise();
			automaton.compact();
			nextState = initialise(automaton);
		} else {
			// activation did not apply
			nextState = state;
		}
		return nextState;
	}
}
