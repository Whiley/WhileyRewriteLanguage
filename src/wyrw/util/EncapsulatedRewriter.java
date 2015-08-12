package wyrw.util;

import java.util.Comparator;

import wyautl.core.Automaton;
import wyautl.core.Schema;
import wyrw.core.Activation;
import wyrw.core.RewriteProof;
import wyrw.core.RewriteRule;
import wyrw.core.RewriteState;
import wyrw.core.RewriteStep;
import wyrw.core.Rewriter;

/**
 * Represents a rewriter which works on top of another "internal" rewriter.
 * Rewrite steps from the internal rewriter are hidden, whilst steps from this
 * rewriter are visible.
 * 
 * @author David J. Pearce
 *
 */
public class EncapsulatedRewriter extends AbstractRewriter implements Rewriter {
	private final Constructor constructor;
		
	public EncapsulatedRewriter(Constructor constructor, Schema schema,
			Comparator<Activation> comparator, RewriteRule... rules) {
		super(schema, comparator, rules);
		this.constructor = constructor;			
	}
		
	@Override
	public RewriteStep apply(RewriteState state, int index) {	
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
		
	public RewriteState initialise(Automaton automaton) {
		Rewriter rewriter = constructor.construct();	
		RewriteState state = rewriter.initialise(automaton);
		RewriteProof proof = rewriter.apply(state);	
		
		if(proof.size() > 0) {
			automaton = proof.last().automaton();
		} else {
			// Cannot return automaton parameter here, in case it was reduced
			// during the Rewriter.initialise() function. 
			automaton = state.automaton();
		}
		return super.initialise(automaton); 
	}
	
	public static interface Constructor {
		public Rewriter construct();
	}
}
