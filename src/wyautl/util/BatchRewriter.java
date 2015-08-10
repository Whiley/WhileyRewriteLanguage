package wyautl.util;

import java.util.Comparator;

import wyautl.core.*;
import wyautl.rw.*;

public class BatchRewriter extends AbstractRewriter implements Rewriter {
	
	public BatchRewriter(Schema schema, RewriteRule... rules) {
		super(schema,Activation.RANK_COMPARATOR,rules);
	}

	public BatchRewriter(Schema schema, Comparator<Activation> comparator,
			RewriteRule... rules) {
		super(schema,comparator,rules);
	}
		
	/**
	 * Apply a given activation on this state to potentially produce an updated
	 * state.
	 * 
	 * @param activation
	 * @return
	 */
	@Override
	public RewriteStep apply(RewriteState state, int index) {
		RewriteState before = state;
		Automaton automaton = new Automaton(state.automaton());
		int r;
		while ((r = state.select()) != -1) {
			if(inplaceRewrite(state.activation(r), automaton)) {
				// In this case, something changed so we'd better create our new
				// state.
				state = initialise(automaton);				
			} else {
				// This is required to cross out any states which don't actually
				// apply, otherwise we end up in an infinite loop reapplying
				// them here.
				state.update(r, new RewriteStep(state,r,state));
			}
		}
		//
		RewriteStep step = new RewriteStep(before, index, state);
		before.update(index, step);
		return step; 
	}
	
	/**
	 * Apply an activation to the automaton in place. That is, the current
	 * automaton is itself updated.
	 * 
	 * @param index
	 * @param automaton
	 * @return
	 */
	private boolean inplaceRewrite(Activation activation, Automaton automaton) {
		int from = activation.root();
		int target = activation.apply(automaton);
		RewriteState nextState;

		if (target != Automaton.K_VOID && from != target) {
			// TODO: can we get rid of this?
			automaton.minimise();
			automaton.compact();
			return true;
		} else {
			// activation did not apply
			return false;
		}
	}
}
