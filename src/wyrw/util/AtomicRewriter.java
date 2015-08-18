package wyrw.util;

import java.util.Comparator;
import java.util.List;

import wyautl.core.*;
import wyrw.core.Activation;
import wyrw.core.Rewrite;
import wyrw.core.RewriteRule;
import wyrw.core.Rewriter;

public class AtomicRewriter extends AbstractRewriter implements Rewriter {
	/**
	 * The current state being rewritten by this rewriter.
	 */
	protected int current;
	 	
	public AtomicRewriter(Rewrite rewrite) {
		super(rewrite);
	}
		
	@Override
	public void apply(int maxSteps) {
		int count = 0;
		Rewrite.State state = rewrite.states().get(current);
		Automaton automaton = new Automaton(state.automaton());
				
		while(count < maxSteps) {			
			int next = state.select();
			if(next != -1 && inplaceRewrite(state.activation(next),automaton)) {
				// An actual step occurred
				automaton.compact();
				automaton.minimise();				
				current = rewrite.add(automaton);
				count = count + 1;
			} else {
				// There are no activations left to try so we are done.
				break;
			}
		}		
		//
		RewriteStep step = new RewriteStep(before, index, state);
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
