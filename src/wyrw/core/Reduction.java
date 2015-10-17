package wyrw.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;

import wyautl.core.Automaton;
import wyautl.core.Schema;
import wyrw.util.AbstractActivation;
import wyrw.util.AbstractRewrite;
import wyrw.util.AbstractRewrite.State;
import wyrw.util.AbstractRewrite.Step;

public class Reduction extends AbstractRewrite {

	private final ReductionRule[] reductions;
	
	public Reduction(Schema schema, Comparator<AbstractActivation> comparator,
			ReductionRule[] reductions) {
		super(schema, comparator);
		this.reductions = reductions;		
	}

	public int initialise(Automaton automaton) {
		states.add(probe(automaton));
		return states.size()-1;
	}
	
	@Override
	public int step(int from, int activation) {
		State state = states.get(from);
		Automaton automaton = new Automaton(state.automaton());
		AbstractActivation a = state.activation(activation);
		int target = a.apply(automaton);
		int to;
		if (target != Automaton.K_VOID && from != target) {
			// Rule application produced an updated automaton
			automaton.minimise(); // needed?
			automaton.compact(0);
			State newState = probe(automaton);
			to = states.size();
			states.add(newState);			
		} else {
			// Rule application had no effect
			to = from;
		}
		Step step = new Step(from, to, activation); 
		steps.add(step);
		state.steps[activation] = step;
		return to;
	}

	private State probe(Automaton automaton) {
		ArrayList<Reduction.Activation> activations = new ArrayList<Reduction.Activation>();
		for (int s = 0; s != automaton.nStates(); ++s) {
			Automaton.State state = automaton.get(s);
			// Check whether this state is a term or not; that's because only
			// terms can be roots for rewrite rule applications.
			if (state instanceof Automaton.Term) {
				for (int r = 0; r != reductions.length; ++r) {
					reductions[r].probe(automaton, s, activations);
				}
			}
		}
		AbstractActivation[] array = activations.toArray(new AbstractActivation[activations.size()]);
		if (comparator != null) {
			Arrays.sort(array, comparator);
		}
		return new State(automaton, array);
	}
	
	public static class Activation extends AbstractActivation {

		/**
		 * The inference rule that this activation will apply.
		 */
		private final ReductionRule rule;

		public Activation(ReductionRule rule, BitSet dependencies, int[] state) {
			super(dependencies, state);
			this.rule = rule;
		}


		@Override
		public RewriteRule rule() {
			return rule;
		}

		
		@Override
		public int apply(Automaton automaton) {
			return rule.apply(automaton, state);
		}
	}
}
