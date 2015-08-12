package wyrw.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import wyautl.core.Automaton;
import wyautl.core.Schema;
import wyrw.core.Activation;
import wyrw.core.RewriteProof;
import wyrw.core.RewriteRule;
import wyrw.core.RewriteState;
import wyrw.core.RewriteStep;
import wyrw.core.Rewriter;

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

	public AbstractRewriter(Schema schema, Comparator<Activation> comparator,
			RewriteRule... rules) {
		this.schema = schema;
		this.rules = rules;
		this.comparator = comparator;
	}
	
	@Override
	public abstract RewriteStep apply(RewriteState state, int choice);
	
	@Override
	public RewriteProof apply(RewriteState state) {
		ArrayList<RewriteStep> steps = new ArrayList<RewriteStep>();
		int r;
		while ((r = state.select()) != -1) {
			RewriteStep step = apply(state,r);
			state = step.after();
			steps.add(step);			
		}
		return new RewriteProof(steps.toArray(new RewriteStep[steps.size()]));
	}
	
	/**
	 * Probe every rule against every valid automaton state to produce the
	 * complete list of possible activations.
	 * 
	 * @param automaton
	 * @return
	 */
	@Override
	public RewriteState initialise(Automaton automaton) {
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
