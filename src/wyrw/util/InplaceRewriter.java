package wyrw.util;

import java.util.Comparator;

import wyautl.core.*;
import wyrw.core.Activation;
import wyrw.core.RewriteRule;
import wyrw.core.RewriteState;
import wyrw.core.RewriteStep;
import wyrw.core.Rewriter;

public class InplaceRewriter extends AbstractRewriter implements Rewriter {
	private final int maxSteps;
	
	public InplaceRewriter(Schema schema, RewriteRule... rules) {
		this(Integer.MAX_VALUE,schema,Activation.RANK_COMPARATOR,rules);
	}
	
	public InplaceRewriter(Schema schema, Comparator<Activation> comparator, RewriteRule... rules) {
		this(Integer.MAX_VALUE,schema,comparator,rules);
	}
	
	public InplaceRewriter(int maxSteps, Schema schema, Comparator<Activation> comparator, RewriteRule... rules) {
		super(schema, comparator, rules);
		this.maxSteps = maxSteps;
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
		int count = 0;
		while (count < maxSteps && (r = state.select()) != -1) {
			if(inplaceRewrite(state.activation(r), automaton)) {
				// In this case, something changed so we'd better create our new
				// state.
				state = initialise(automaton);
				count = count + 1;
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
