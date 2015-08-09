package wyautl.util;

import java.util.Comparator;

import wyautl.core.Automaton;
import wyautl.core.Schema;
import wyautl.rw.Activation;
import wyautl.rw.RewriteRule;
import wyautl.rw.RewriteState;
import wyautl.rw.RewriteStep;
import wyautl.rw.Rewriter;

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
		
	public EncapsulatedRewriter(Constructor constructor, Automaton automaton, Schema schema,
			Comparator<Activation> comparator, RewriteRule... rules) {
		super(schema, comparator, rules);
		this.constructor = constructor;		
		this.state = initialise(automaton);		
	}
	
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
	
	protected RewriteState initialise(Automaton automaton) {		
		Rewriter rewriter = constructor.construct(automaton);		
		RewriteStep step = rewriter.apply();		
		return super.initialise(step.after().automaton());
	}
	
	public static interface Constructor {
		public Rewriter construct(Automaton automaton);
	}
}
