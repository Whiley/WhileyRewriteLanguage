package wyrw.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;

import wyautl.core.Automata;
import wyautl.core.Automaton;
import wyautl.core.Schema;
import wyrw.util.AbstractActivation;
import wyrw.util.AbstractRewrite;
import wyrw.util.Reductions;
import wyrw.util.AbstractRewrite.State;
import wyrw.util.AbstractRewrite.Step;

public class Inference extends AbstractRewrite {
	private final int MAX_REDUCTIONS = 10000;
	
	private final InferenceRule[] inferences;
	
	private final ReductionRule[] reductions;
	
	private Automaton automaton;
	
	public Inference(Schema schema, Comparator<AbstractActivation> comparator, InferenceRule[] inferences,
			ReductionRule[] reductions) {
		super(schema, comparator);
		this.inferences = inferences;
		this.reductions = reductions;
		
	}

	@Override
	public int initialise(Automaton automaton) {
		this.automaton = automaton;
		Reductions.minimiseAndReduce(automaton,MAX_REDUCTIONS,reductions,comparator);		
		states.add(probeReachableInferences(automaton,0));
		return states.size()-1;
	}
	
	@Override
	public int step(int from, int activation) {		
		State state = states.get(from);					
		int pivot = automaton.nStates();		
		Activation a = (Activation) state.activation(activation);
		int nRoot = a.apply(automaton);		
		int to;
		if (nRoot != Automaton.K_VOID && nRoot != a.root()) {			
			// Rule application produced an updated automaton. Therefore, we now
			// want to reduce the automaton whilst preserving the root
			to = addState(automaton,reduce(automaton,pivot,nRoot));
			if(to >= states.size()) {
				//System.out.println(" *** STEP: " + from + " => " + to + ", " + a.rule().name() + " (" + automaton.nStates() + ")");
				//wyrl.util.Runtime.debug(automaton.getRoot(to), automaton, schema, "And","Or");
				// This is a new state not seen before
				states.add(probeReachableInferences(automaton,to));
			}
		} else {
			// Rule application had no effect
			automaton.resize(pivot);
			to = from;			
		}
		Step step = new Step(from, to, activation); 
		steps.add(step);
		state.steps[activation] = step;
		return to;
	}
	
	private State probeReachableInferences(Automaton automaton, int root) {
		int rootState = automaton.getRoot(root);
		int[] reachable = new int[automaton.nStates()];
		Automata.traverse(automaton,rootState,reachable);
		
		ArrayList<Activation> activations = new ArrayList<Activation>();
		for (int s = 0; s != automaton.nStates(); ++s) {
			if (reachable[s] != 0) {
				// State is reachable from the given root
				Automaton.State state = automaton.get(s);
				// Check whether this state is a term or not; that's because
				// only terms can be roots for rewrite rule applications.
				if (state instanceof Automaton.Term) {
					for (int r = 0; r != inferences.length; ++r) {
						inferences[r].probe(automaton, rootState, s, activations);
					}
				}
			}
		}
		Activation[] array = activations.toArray(new Activation[activations.size()]);
		if (comparator != null) {
			Arrays.sort(array, comparator);
		}
		return new State(automaton, array);
	}		
	
	/**
	 * Reduce the automaton whilst preserving the given root. In the case that
	 * the automaton changes shape in some way (e.g. is compacted or minimised),
	 * then the root is mapped to the corresponding automaton state.
	 * 
	 * @param automaton
	 * @param root
	 * @return
	 */
	private int reduce(Automaton automaton, int start, int root) {
		automaton.push(root);
		automaton.minimise();
		automaton.compact(0);
		Reductions.reduceOver(automaton, start, MAX_REDUCTIONS, reductions, comparator);		
		return automaton.pop();		
	}
	
	/**
	 * Add a state to the automaton. If a matching state already exists, then
	 * simply return that; otherwise, create a new state.
	 * 
	 * @param automaton
	 * @param root
	 * @return
	 */
	private int addState(Automaton automaton, int root) {
		for(int i = 0;i!=automaton.nRoots();++i) {
			if(automaton.getRoot(i) == root) {
				// Matching state found
				return i;
			}
		}
		// No match found, so create a new state
		return automaton.push(root);
	}
	
	public static class Activation extends AbstractActivation {

		/**
		 * The inference rule that this activation will apply.
		 */
		final InferenceRule rule;

		/**
		 * The automaton root that this activation is working off. This identifies a
		 * subset of the automaton being rewriten by this activation. It is
		 * necessary to know this for inference rules to operate correctly as they
		 * generate new roots.
		 */
		private final int root;
		

		public Activation(InferenceRule rule, int root, BitSet dependencies, int[] state) {
			super(dependencies, state);
			this.rule = rule;
			this.root = root;
		}


		@Override
		public RewriteRule rule() {
			return rule;
		}

		public int root() {
			return root;
		}

		@Override
		public int apply(Automaton automaton) {
			return rule.apply(automaton, root, state);
		}
	}
}
